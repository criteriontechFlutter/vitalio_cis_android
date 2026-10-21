package com.critetiontech.ctvitalio.omron

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.data.VitalUpload
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.utils.Patient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.SharedManager
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronErrorInfo
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

// ─── Models ───────────────────────────────────────────────────────────────────

enum class OmronKind { BP, SCALE }

/** A device our partner key supports. [code] is the model number printed on the device. */
data class OmronModel(val code: String, val label: String, val kind: OmronKind)

object OmronModels {
    val ALL = listOf(
        OmronModel("HEM-7280T", "BP Machine", OmronKind.BP),
        OmronModel("HEM-7600T", "BP Machine", OmronKind.BP),
        OmronModel("HEM-7361T", "BP Machine", OmronKind.BP),
        OmronModel("HEM-7141T1", "BP Machine", OmronKind.BP),
        OmronModel("HEM-7142T1", "BP Machine", OmronKind.BP),
        OmronModel("HEM-7156T", "BP Machine", OmronKind.BP),
        OmronModel("HEM-6232T", "Wrist BP Machine", OmronKind.BP),
        OmronModel("HBF-222T", "Body Composition Monitor", OmronKind.SCALE),
        OmronModel("HBF-255T", "Body Composition Monitor", OmronKind.SCALE),
        OmronModel("HBF-702T", "Body Composition Monitor", OmronKind.SCALE),
        OmronModel("HN-300T2", "Weight Scale", OmronKind.SCALE),
        // HWZ-1000T-E (WheezeScan) is also on the key, but wheeze has no field on our server
    )

    fun byCode(code: String) = ALL.firstOrNull { it.code == code }
}

/** A device the user has paired with this phone. [config] is the library's entry for the model. */
data class PairedOmronDevice(
    val code: String,
    val localName: String,
    val uuid: String,
    val user: Int,
    val config: Map<String, String>,
    val pairedAt: Long,
    val lastSyncAt: Long = 0
)

data class OmronReading(
    val key: String,
    val time: Instant,
    val systolic: Int? = null,
    val diastolic: Int? = null,
    val pulse: Int? = null,
    val irregularHeartbeat: Boolean = false,
    val weightKg: Double? = null,
    val bodyFatPercent: Double? = null,
    val bmi: Double? = null
) {
    val isBloodPressure get() = systolic != null && diastolic != null
}

data class OmronSyncResult(
    val readings: List<OmronReading>,
    val saved: Set<String>,
    val error: String?
)

/** Personal details scales need to work out body composition. */
data class OmronProfile(val heightCm: Double?, val dateOfBirth: LocalDate?, val isMale: Boolean)

class OmronException(val code: Int, message: String) : Exception(message)

// ─── Manager ──────────────────────────────────────────────────────────────────

/**
 * Wraps the OMRON Connectivity Library: pairing, reading stored measurements and saving them.
 *
 * Readings are kept on the phone before the device is told they were read (that clears them on
 * the device), and stay queued until the server accepts them, so nothing is lost if saving fails.
 */
object OmronManager {

    private const val TAG = "OmronManager"
    private const val PREFS = "omron_devices"
    private const val KEY_DEVICES = "paired"
    private const val KEY_PENDING = "pending"
    private const val KEY_SAVED = "saved"
    private const val SCAN_TIMEOUT_SECONDS = 30
    private const val INVALID = Int.MIN_VALUE

    private val gson = Gson()
    private val mutex = Mutex()

    private val _configReady = MutableStateFlow(false)
    val configReady: StateFlow<Boolean> = _configReady

    private val configReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val status = intent.getIntExtra(OmronConstants.OMRONConfigurationStatusKey, 0)
            Log.d(TAG, "configuration status $status")
            _configReady.value = status == OmronConstants.OMRONConfigurationStatus.OMRONConfigurationFileSuccess
        }
    }

    private fun manager(context: Context): SharedManager = OmronPeripheralManager.sharedManager(context.applicationContext)

    /** Call once at app start. */
    fun init(context: Context) {
        val key = BuildConfig.OMRON_PARTNER_KEY
        if (key.isBlank()) {
            Log.w(TAG, "OMRON_PARTNER_KEY missing in local.properties; Omron devices disabled")
            return
        }
        val app = context.applicationContext
        ContextCompat.registerReceiver(
            app, configReceiver,
            IntentFilter(OmronConstants.OMRONBLEConfigurationFileAvailabilityStatusNotification),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        runCatching { manager(app).setAPIKey(key, null) }.onFailure { Log.e(TAG, "setAPIKey failed", it) }
        if (deviceConfigs(app).isNotEmpty()) _configReady.value = true
    }

    private fun deviceConfigs(context: Context): List<Map<String, String>> = runCatching {
        @Suppress("UNCHECKED_CAST")
        (manager(context).retrieveManagerConfiguration()?.get(OmronConstants.OMRONBLEConfigDeviceKey) as? List<Map<String, String>>)
    }.getOrNull().orEmpty()

    /** The library's configuration for [model], preferring its Asia-Pacific ("-AP") variant. */
    fun deviceConfig(context: Context, model: OmronModel): Map<String, String>? {
        fun norm(s: String) = s.uppercase(Locale.US).filter { it.isLetterOrDigit() }
        val code = norm(model.code)
        val names = { m: Map<String, String> ->
            listOf(OmronConstants.OMRONBLEConfigDevice.ModelName, OmronConstants.OMRONBLEConfigDevice.ModelDisplayName,
                OmronConstants.OMRONBLEConfigDevice.Identifier).mapNotNull { m[it] }.map(::norm)
        }
        val matches = deviceConfigs(context).filter { m -> names(m).any { it.startsWith(code) } }
        return matches.firstOrNull { m -> names(m).any { it.startsWith(code + "AP") } } ?: matches.firstOrNull()
    }

    fun userCount(config: Map<String, String>?): Int =
        config?.get(OmronConstants.OMRONBLEConfigDevice.Users)?.toIntOrNull()?.coerceIn(1, 4) ?: 1

    // ── Paired devices ───────────────────────────────────────────────────

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun pairedDevices(context: Context): List<PairedOmronDevice> = runCatching {
        gson.fromJson<List<PairedOmronDevice>>(
            prefs(context).getString(KEY_DEVICES, null) ?: return emptyList(),
            object : TypeToken<List<PairedOmronDevice>>() {}.type
        )
    }.getOrNull().orEmpty()

    fun pairedDevice(context: Context, code: String) = pairedDevices(context).firstOrNull { it.code == code }

    private fun savePaired(context: Context, devices: List<PairedOmronDevice>) =
        prefs(context).edit().putString(KEY_DEVICES, gson.toJson(devices)).apply()

    /** Forgets the device in the app; the user also removes it from the phone's Bluetooth settings. */
    fun removeDevice(context: Context, code: String) =
        savePaired(context, pairedDevices(context).filterNot { it.code == code })

    // ── Library configuration ────────────────────────────────────────────

    /** Same value for pairing and every transfer, or the BP monitor refuses to talk (encryption key). */
    private fun userHash(patient: Patient) = patient.uhId

    private fun configure(
        context: Context, patient: Patient, config: Map<String, String>,
        model: OmronModel, pairing: Boolean, profile: OmronProfile?
    ) {
        val m = manager(context)
        val c = m.configuration
        c.deviceFilters = listOf(HashMap(config))
        c.timeoutInterval = SCAN_TIMEOUT_SECONDS
        c.userHashId = userHash(patient)
        c.enableAllDataRead = false

        val scan = hashMapOf<String, Any>(
            OmronConstants.OMRONDeviceScanSettings.ModeKey to
                if (pairing) OmronConstants.OMRONDeviceScanSettingsMode.Pairing
                else OmronConstants.OMRONDeviceScanSettingsMode.MismatchSequence
        )
        val settings = ArrayList<HashMap<*, *>>()
        settings += hashMapOf(OmronConstants.OMRONDeviceScanSettingsKey to scan)
        if (model.kind == OmronKind.SCALE && profile != null) settings += scaleSettings(profile)
        @Suppress("UNCHECKED_CAST")
        c.deviceSettings = settings as List<HashMap<Any?, Any?>>
        m.setConfiguration(c)
        m.startManager()
    }

    /** Height (cm × 100), gender and date of birth are mandatory for body-composition scales. */
    private fun scaleSettings(profile: OmronProfile): HashMap<String, Any> {
        val personal = hashMapOf<String, Any>(
            OmronConstants.OMRONDevicePersonalSettings.UserGenderKey to
                if (profile.isMale) OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male
                else OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Female,
            OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey to
                (profile.dateOfBirth ?: LocalDate.of(1990, 1, 1)).toString().replace("-", ""),
            OmronConstants.OMRONDevicePersonalSettings.WeightKey to hashMapOf(
                OmronConstants.OMRONDeviceWeightSettingsKey to hashMapOf(
                    OmronConstants.OMRONDeviceWeightSettings.UnitKey to OmronConstants.OMRONDeviceWeightUnit.Kg
                )
            )
        )
        profile.heightCm?.let {
            personal[OmronConstants.OMRONDevicePersonalSettings.UserHeightKey] = String.format(Locale.US, "%.0f", it * 100)
        }
        return hashMapOf(OmronConstants.OMRONDevicePersonalSettingsKey to personal)
    }

    private fun OmronErrorInfo.toException() = OmronException(resultCode, messageFor(resultCode, messageInfo))

    // ── Pairing ──────────────────────────────────────────────────────────

    /** Finds the device in pairing mode ("P" blinking) and pairs it for [user]. */
    suspend fun pair(context: Context, model: OmronModel, user: Int, profile: OmronProfile?): PairedOmronDevice = mutex.withLock {
        val patient = AppDependencies.prefs.getPatient() ?: throw OmronException(0, "Please log in again.")
        val config = deviceConfig(context, model)
            ?: throw OmronException(8003, "Omron setup is still loading or this model isn't supported. Please try again in a moment.")
        configure(context, patient, config, model, pairing = true, profile = profile)

        val found = scanFirst(context)
        found.setSelectedUsers(listOf(user))
        val paired = suspendCancellableCoroutine { cont ->
            manager(context).connectPeripheral(found) { peripheral, info ->
                if (info.isSuccess && peripheral != null) cont.resume(peripheral) else cont.resumeWithException(info.toException())
            }
            cont.invokeOnCancellation {
                runCatching { manager(context).disconnectPeripheral(found) { _, _ -> } }
            }
        }
        val device = PairedOmronDevice(
            code = model.code,
            localName = paired.localName ?: found.localName,
            uuid = paired.uuid ?: found.uuid,
            user = user,
            config = HashMap(config),
            pairedAt = System.currentTimeMillis()
        )
        savePaired(context, pairedDevices(context).filterNot { it.code == model.code } + device)
        Log.d(TAG, "paired ${model.code} ${device.localName}")
        device
    }

    private suspend fun scanFirst(context: Context): OmronPeripheral = suspendCancellableCoroutine { cont ->
        val done = AtomicBoolean(false)
        val m = manager(context)
        m.startScanPeripherals { list, info ->
            if (done.get()) return@startScanPeripherals
            when {
                !info.isSuccess -> if (done.compareAndSet(false, true)) cont.resumeWithException(info.toException())
                else -> list?.firstOrNull { !it.modelName.isNullOrEmpty() }?.let { peripheral ->
                    if (done.compareAndSet(false, true)) {
                        m.stopScanPeripherals { }
                        cont.resume(peripheral)
                    }
                }
            }
        }
        cont.invokeOnCancellation { if (done.compareAndSet(false, true)) runCatching { m.stopScanPeripherals { } } }
    }

    // ── Transfer ─────────────────────────────────────────────────────────

    /** Reads new measurements from a paired device and saves the BP readings to the server. */
    suspend fun sync(context: Context, device: PairedOmronDevice, profile: OmronProfile?): OmronSyncResult = mutex.withLock {
        val patient = AppDependencies.prefs.getPatient() ?: throw OmronException(0, "Please log in again.")
        val model = OmronModels.byCode(device.code) ?: throw OmronException(8003, "Unknown device.")
        configure(context, patient, device.config, model, pairing = false, profile = profile)

        val m = manager(context)
        val target = OmronPeripheral(device.localName, device.uuid)
        val transferred = suspendCancellableCoroutine { cont ->
            m.startDataTransferFromPeripheral(target, listOf(device.user), true) { peripheral, info ->
                if (info.isSuccess && peripheral != null) cont.resume(peripheral) else cont.resumeWithException(info.toException())
            }
            cont.invokeOnCancellation { runCatching { m.disconnectPeripheral(target) { _, _ -> } } }
        }

        // Keep the readings on the phone before the device is told they were read
        val fresh = parse(transferred.vitalData, device.user)
        queue(context, device, fresh)
        val ended = suspendCancellableCoroutine { cont ->
            m.endDataTransferFromPeripheral { peripheral, info ->
                cont.resume(if (info.isSuccess) peripheral else null)
            }
        }
        val afterEnd = ended?.let { parse(it.vitalData, device.user) }.orEmpty()
        queue(context, device, afterEnd)

        val readings = (fresh + afterEnd).distinctBy { it.key }.sortedByDescending { it.time }
        val (saved, error) = uploadPending(context, patient, device)
        savePaired(context, pairedDevices(context).map {
            if (it.code == device.code) it.copy(lastSyncAt = System.currentTimeMillis()) else it
        })
        OmronSyncResult(readings, saved, error)
    }

    // ── Pending queue ────────────────────────────────────────────────────

    private fun pending(context: Context, device: PairedOmronDevice): List<OmronReading> = runCatching {
        gson.fromJson<List<OmronReading>>(
            prefs(context).getString("${KEY_PENDING}_${device.code}", null) ?: return emptyList(),
            object : TypeToken<List<OmronReading>>() {}.type
        )
    }.getOrNull().orEmpty()

    private fun savedKeys(context: Context, device: PairedOmronDevice): Set<String> =
        prefs(context).getStringSet("${KEY_SAVED}_${device.code}", emptySet()).orEmpty()

    private fun queue(context: Context, device: PairedOmronDevice, readings: List<OmronReading>) {
        val alreadySaved = savedKeys(context, device)
        val merged = (pending(context, device) + readings.filter { it.isBloodPressure && it.key !in alreadySaved })
            .distinctBy { it.key }
        prefs(context).edit().putString("${KEY_PENDING}_${device.code}", gson.toJson(merged)).apply()
    }

    /** Sends every queued BP reading; keeps the ones the server didn't accept for next time. */
    private suspend fun uploadPending(context: Context, patient: Patient, device: PairedOmronDevice): Pair<Set<String>, String?> {
        var error: String? = null
        val left = mutableListOf<OmronReading>()
        val saved = mutableSetOf<String>()
        pending(context, device).sortedBy { it.time }.forEach { r ->
            val fields = mapOf(
                "vmValueBPSys" to r.systolic.toString(),
                "vmValueBPDias" to r.diastolic.toString(),
                "vmValuePulse" to (r.pulse?.toString() ?: "")
            )
            val failure = VitalUpload.post(patient, r.time, fields)
            if (failure == null) saved += r.key else { left += r; error = failure }
        }
        // Remember recent saved keys so a re-sent reading isn't saved twice
        val keep = (savedKeys(context, device) + saved).toList().takeLast(500).toSet()
        prefs(context).edit()
            .putString("${KEY_PENDING}_${device.code}", gson.toJson(left))
            .putStringSet("${KEY_SAVED}_${device.code}", keep)
            .apply()
        return saved to error
    }

    fun isSaved(context: Context, device: PairedOmronDevice, reading: OmronReading) = reading.key in savedKeys(context, device)

    // ── Parsing ──────────────────────────────────────────────────────────

    private fun Any?.asInt(): Int? = when (this) {
        is Number -> toInt()
        is String -> toDoubleOrNull()?.toInt()
        else -> null
    }?.takeIf { it != INVALID }

    private fun Any?.asDouble(): Double? = when (this) {
        is Number -> toDouble()
        is String -> toDoubleOrNull()
        else -> null
    }?.takeIf { it != INVALID.toDouble() }

    /** Library timestamps are Unix time in seconds or milliseconds; 0 / 2014–2015 means the clock wasn't set. */
    private fun Any?.asInstant(): Instant? {
        val raw = (this as? Number)?.toLong() ?: (this as? String)?.toLongOrNull() ?: return null
        val instant = if (raw < 100_000_000_000L) Instant.ofEpochSecond(raw) else Instant.ofEpochMilli(raw)
        return instant.takeIf { it.isAfter(Instant.parse("2016-01-01T00:00:00Z")) && it.isBefore(Instant.now().plusSeconds(86_400)) }
    }

    @Suppress("UNCHECKED_CAST")
    internal fun parse(vitalData: Any?, user: Int): List<OmronReading> {
        val data = vitalData as? Map<String, Any?> ?: return emptyList()
        val now = Instant.now()
        val bp = (data[OmronConstants.OMRONVitalDataBloodPressureKey] as? List<Map<String, Any?>>).orEmpty().mapNotNull { item ->
            val userId = item[OmronConstants.OMRONVitalData.UserIdKey].asInt()
            if (userId != null && userId != user) return@mapNotNull null
            val sys = item[OmronConstants.OMRONVitalData.SystolicKey].asInt() ?: return@mapNotNull null
            val dia = item[OmronConstants.OMRONVitalData.DiastolicKey].asInt() ?: return@mapNotNull null
            val time = item[OmronConstants.OMRONVitalData.StartDateKey].asInstant() ?: now
            val seq = item[OmronConstants.OMRONVitalData.SequenceKey].asInt()
            OmronReading(
                key = "bp_${seq}_${time.epochSecond}_$sys/$dia",
                time = time, systolic = sys, diastolic = dia,
                pulse = item[OmronConstants.OMRONVitalData.PulseKey].asInt(),
                irregularHeartbeat = item[OmronConstants.OMRONVitalData.IrregularFlagKey].asInt() == 1
            )
        }
        val weight = (data[OmronConstants.OMRONVitalDataWeightKey] as? List<Map<String, Any?>>).orEmpty().mapNotNull { item ->
            val userId = item[OmronConstants.OMRONWeightData.UserIdKey].asInt()
            if (userId != null && userId != user) return@mapNotNull null
            val kg = item[OmronConstants.OMRONWeightData.WeightKey].asDouble() ?: return@mapNotNull null
            val time = item[OmronConstants.OMRONWeightData.StartDateKey].asInstant() ?: now
            OmronReading(
                key = "wt_${item[OmronConstants.OMRONWeightData.SequenceKey].asInt()}_${time.epochSecond}_$kg",
                time = time, weightKg = kg,
                bodyFatPercent = item[OmronConstants.OMRONWeightData.BodyFatPercentageKey].asDouble(),
                bmi = item[OmronConstants.OMRONWeightData.BMIKey].asDouble()
            )
        }
        return bp + weight
    }

    // ── Errors ───────────────────────────────────────────────────────────

    /** User-facing messages for the library's error codes (Implementation Guide, Appendix 3). */
    fun messageFor(code: Int, fallback: String?): String = when (code) {
        6000 -> "Omron partner key isn't valid. Please contact support."
        8002 -> "Omron setup is incomplete (user ID missing). Please contact support."
        8003 -> "This device isn't supported."
        6011, 6017, 6100, 6206, 6020 -> "The device is busy. Wait a few seconds and try again."
        6016 -> "Bluetooth is turned off. Turn it on and try again."
        6015 -> "This phone doesn't support Bluetooth."
        6207, 6221 -> "Another app is talking to the Omron device. Close the OMRON connect app and try again."
        6021 -> "Pairing failed. Remove the device from your phone's Bluetooth settings and pair again."
        6022 -> "The device isn't in pairing mode. Hold its Bluetooth button until \"P\" blinks, then try again."
        6024 -> "Pairing was cancelled. Accept the Bluetooth pairing request to continue."
        6029 -> "Device not found. Keep it close to your phone and make sure it's on (\"P\" blinking for pairing)."
        6030 -> "The connection timed out. Keep the device close to your phone and try again."
        6019, 6223 -> "The device disconnected. Move closer to your phone and try again."
        9000 -> "User number doesn't match the one on the device. Remove the device and pair again."
        else -> fallback?.takeIf { it.isNotBlank() } ?: "Something went wrong (error $code). Please try again."
    }
}
