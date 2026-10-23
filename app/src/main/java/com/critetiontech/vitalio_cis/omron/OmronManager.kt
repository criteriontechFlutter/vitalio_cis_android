package com.critetiontech.ctvitalio.omron

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.utils.Patient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.OmronPeripheralManager
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.LibraryManager.SharedManager
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronErrorInfo
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.Model.OmronPeripheral
import com.omronhealthcare.OmronConnectivityLibrary.OmronLibrary.OmronUtility.OmronConstants
import kotlinx.coroutines.CancellationException
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

enum class OmronKind(val label: String) { BP("BP Machine"), SCALE("Weight Scale") }

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

    fun ofKind(kind: OmronKind) = ALL.filter { it.kind == kind }

    /** Screen code for "pair whichever supported [kind] device is nearby"; the model is detected while pairing. */
    fun autoCode(kind: OmronKind) = "auto-${kind.name.lowercase(Locale.US)}"

    fun autoKind(code: String) = OmronKind.entries.firstOrNull { autoCode(it) == code }
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
    val bmi: Double? = null,
    /** False when the device's clock wasn't set, so [time] is when we read it rather than when it was measured. */
    val timeFromDevice: Boolean = true,
    /** The device's record number; higher is newer. Orders readings when the device has no time. */
    val sequence: Int? = null
) {
    val isBloodPressure get() = systolic != null && diastolic != null
}

/** The newest reading on the device for this user, and whether it's already in the health record. */
data class OmronSyncResult(val latest: OmronReading?, val alreadySaved: Boolean)

/** Personal details scales need to work out body composition. */
data class OmronProfile(val heightCm: Double?, val dateOfBirth: LocalDate?, val isMale: Boolean)

class OmronException(val code: Int, message: String) : Exception(message)

// ─── Manager ──────────────────────────────────────────────────────────────────

/**
 * Wraps the OMRON Connectivity Library: pairing, reading stored measurements and saving them.
 *
 * A sync reads every reading stored on the device and returns the newest; the user then chooses
 * to save it. Nothing is lost if they don't — the device keeps its readings and the next sync
 * reads them again.
 */
object OmronManager {

    private const val TAG = "OmronManager"
    private const val PREFS = "omron_devices"
    private const val KEY_DEVICES = "paired"
    private const val KEY_SAVED = "saved"
    private const val SCAN_TIMEOUT_SECONDS = 30
    private const val INVALID = Int.MIN_VALUE
    /** Config keys the library has no constant for. */
    private const val GROUP_ID = "deviceGroupIncludedGroupIDKey"
    private const val CONFIG_ID = "id"

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

    private fun norm(s: String) = s.uppercase(Locale.US).filter { it.isLetterOrDigit() }

    private fun configNames(m: Map<String, String>) =
        listOf(OmronConstants.OMRONBLEConfigDevice.ModelName, OmronConstants.OMRONBLEConfigDevice.ModelDisplayName,
            OmronConstants.OMRONBLEConfigDevice.Identifier).mapNotNull { m[it] }.map(::norm)

    /** The library's configuration for [model], preferring its Asia-Pacific ("-AP") variant. */
    fun deviceConfig(context: Context, model: OmronModel): Map<String, String>? {
        val code = norm(model.code)
        val matches = deviceConfigs(context).filter { m -> configNames(m).any { it.startsWith(code) } }
        return matches.firstOrNull { m -> configNames(m).any { it.startsWith(code + "AP") } } ?: matches.firstOrNull()
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

    /** [configs] filters the scan: every supported model of the kind while pairing, the paired one when syncing. */
    private fun configure(
        context: Context, patient: Patient, configs: List<Map<String, String>>,
        kind: OmronKind, pairing: Boolean, profile: OmronProfile?
    ) {
        val m = manager(context)
        val c = m.configuration
        c.deviceFilters = ArrayList(configs.map { HashMap(it) })
        c.timeoutInterval = SCAN_TIMEOUT_SECONDS
        c.userHashId = userHash(patient)
        // Read everything stored, not just "new" readings: pairing already marks the device's
        // readings as read (they'd never reach us otherwise). Already-saved ones are skipped by key.
        c.enableAllDataRead = !pairing

        val scan = hashMapOf<String, Any>(
            OmronConstants.OMRONDeviceScanSettings.ModeKey to
                if (pairing) OmronConstants.OMRONDeviceScanSettingsMode.Pairing
                else OmronConstants.OMRONDeviceScanSettingsMode.MismatchSequence
        )
        val settings = ArrayList<HashMap<*, *>>()
        settings += hashMapOf(OmronConstants.OMRONDeviceScanSettingsKey to scan)
        if (profile != null) settings += personalSettings(profile, kind)
        @Suppress("UNCHECKED_CAST")
        c.deviceSettings = settings as List<HashMap<Any?, Any?>>
        m.setConfiguration(c)
        m.startManager()
    }

    /**
     * Height (cm × 100), gender and date of birth are mandatory for body-composition scales.
     * Gender and date of birth also go to BP monitors: models like HEM-7361T keep a date-of-birth
     * setting, and without it the library fails pairing with "DateIsNULL" (error 4).
     */
    private fun personalSettings(profile: OmronProfile, kind: OmronKind): HashMap<String, Any> {
        val personal = hashMapOf<String, Any>(
            OmronConstants.OMRONDevicePersonalSettings.UserGenderKey to
                if (profile.isMale) OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Male
                else OmronConstants.OMRONDevicePersonalSettingsUserGenderType.Female,
            OmronConstants.OMRONDevicePersonalSettings.UserDateOfBirthKey to
                (profile.dateOfBirth ?: LocalDate.of(1990, 1, 1)).toString().replace("-", "")
        )
        if (kind != OmronKind.SCALE) return hashMapOf(OmronConstants.OMRONDevicePersonalSettingsKey to personal)
        personal[OmronConstants.OMRONDevicePersonalSettings.WeightKey] = hashMapOf(
            OmronConstants.OMRONDeviceWeightSettingsKey to hashMapOf(
                OmronConstants.OMRONDeviceWeightSettings.UnitKey to OmronConstants.OMRONDeviceWeightUnit.Kg
            )
        )
        profile.heightCm?.let {
            personal[OmronConstants.OMRONDevicePersonalSettings.UserHeightKey] = String.format(Locale.US, "%.0f", it * 100)
        }
        return hashMapOf(OmronConstants.OMRONDevicePersonalSettingsKey to personal)
    }

    private fun OmronErrorInfo.toException() = OmronException(resultCode, messageFor(resultCode, messageInfo))

    // ── Pairing ──────────────────────────────────────────────────────────

    /**
     * Finds whichever supported [kind] device is in pairing mode ("P" blinking), works out its model
     * and pairs it for [user]. The user doesn't have to know the model number, and a monitor sold
     * under another name (one that reports itself as HEM-7156T) still pairs.
     *
     * The model comes from a plain BLE scan first: given several models at once, the library's own
     * scan finds nothing, so it's then run for the detected model only.
     */
    suspend fun pair(context: Context, kind: OmronKind, user: Int, profile: OmronProfile?): PairedOmronDevice = mutex.withLock {
        val patient = AppDependencies.prefs.getPatient() ?: throw OmronException(0, "Please log in again.")
        val candidates = OmronModels.ofKind(kind).mapNotNull { m -> deviceConfig(context, m)?.let { m to it } }
        if (candidates.isEmpty()) throw OmronException(8003, "Omron setup is still loading. Please try again in a moment.")
        val (model, config) = findNearby(context, candidates)
        configure(context, patient, listOf(config), kind, pairing = true, profile = profile)

        val found = scanFirst(context)
        val pairedUser = user.coerceIn(1, userCount(config))
        found.setSelectedUsers(arrayListOf(pairedUser))
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
            user = pairedUser,
            config = HashMap(config),
            pairedAt = System.currentTimeMillis()
        )
        savePaired(context, pairedDevices(context).filterNot { it.code == model.code } + device)
        Log.d(TAG, "paired ${model.code} ${device.localName}")
        device
    }

    /**
     * Which of [candidates] a device is: by its device group, then its model name, then the model id
     * in its Bluetooth name ("BLEsmart_0000057A…" → 0x57A = 1402).
     */
    internal fun identify(
        groupId: String?, modelName: String?, localName: String?,
        candidates: List<Pair<OmronModel, Map<String, String>>>
    ): Pair<OmronModel, Map<String, String>>? {
        if (!groupId.isNullOrBlank()) candidates.firstOrNull { it.second[GROUP_ID] == groupId }?.let { return it }
        val name = norm(modelName.orEmpty())
        if (name.isNotEmpty()) {
            candidates.firstOrNull { name in configNames(it.second) }?.let { return it }
            candidates.firstOrNull { name.startsWith(norm(it.first.code)) }?.let { return it }
        }
        val id = localName?.takeIf { it.startsWith("BLEsmart_", ignoreCase = true) }
            ?.substring("BLEsmart_".length)?.take(8)?.toIntOrNull(16)?.toString()
        return candidates.firstOrNull { id != null && (it.second[GROUP_ID] == id || it.second[CONFIG_ID] == id) }
    }

    /** Scans for an Omron device ("BLEsmart_…") that is one of [candidates] and returns which. */
    @SuppressLint("MissingPermission")
    private suspend fun findNearby(
        context: Context, candidates: List<Pair<OmronModel, Map<String, String>>>
    ): Pair<OmronModel, Map<String, String>> = suspendCancellableCoroutine { cont ->
        val scanner = context.getSystemService(BluetoothManager::class.java)?.adapter?.bluetoothLeScanner
        if (scanner == null) {
            cont.resumeWithException(OmronException(6016, messageFor(6016, null)))
            return@suspendCancellableCoroutine
        }
        val done = AtomicBoolean(false)
        val handler = Handler(Looper.getMainLooper())
        var unknown: String? = null
        val seen = java.util.concurrent.ConcurrentHashMap.newKeySet<String>() // TEMP
        lateinit var callback: ScanCallback
        fun finish(result: Result<Pair<OmronModel, Map<String, String>>>) {
            if (!done.compareAndSet(false, true)) return
            handler.removeCallbacksAndMessages(null)
            runCatching { scanner.stopScan(callback) }
            cont.resumeWith(result)
        }
        callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                // TEMP: every advertiser once, with its manufacturer ids (Omron is 0x020E)
                if (seen.add(result.device.address)) runCatching {
                    val mfr = result.scanRecord?.manufacturerSpecificData
                    java.io.File(context.filesDir, "omron_diag.txt").appendText(
                        "${java.time.LocalTime.now()} adv ${result.device.address} rec=${result.scanRecord?.deviceName} " +
                            "dev=${result.device.name} rssi=${result.rssi} mfr=${mfr?.let { m -> (0 until m.size()).joinToString { "0x%04X".format(m.keyAt(it)) } }}\n"
                    )
                }
                // A device already bonded to this phone may leave its name out; the phone remembers it
                val name = result.scanRecord?.deviceName ?: result.device.name ?: return
                if (!name.startsWith("BLEsmart_", ignoreCase = true)) return
                val match = identify(null, null, name, candidates)
                Log.d(TAG, "nearby $name rssi ${result.rssi} → ${match?.first?.code}")
                runCatching { java.io.File(context.filesDir, "omron_diag.txt").appendText("${java.time.LocalTime.now()} nearby $name → ${match?.first?.code}\n") } // TEMP
                if (match != null) finish(Result.success(match)) else unknown = name
            }

            override fun onScanFailed(errorCode: Int) {
                finish(Result.failure(OmronException(6029, "Bluetooth scan failed (error $errorCode). Turn Bluetooth off and on, then try again.")))
            }
        }
        scanner.startScan(null, ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build(), callback)
        handler.postDelayed({
            finish(Result.failure(
                unknown?.let { OmronException(8003, "Found an Omron device ($it), but it isn't a model we support yet.") }
                    ?: OmronException(6029, messageFor(6029, null))
            ))
        }, SCAN_TIMEOUT_SECONDS * 1000L)
        cont.invokeOnCancellation { finish(Result.failure(it ?: CancellationException())) }
    }

    private suspend fun scanFirst(context: Context): OmronPeripheral = suspendCancellableCoroutine { cont ->
        val done = AtomicBoolean(false)
        val m = manager(context)
        m.startScanPeripherals { list, info ->
            if (done.get()) return@startScanPeripherals
            // TEMP diagnostics: this phone drops app logcat
            runCatching {
                java.io.File(context.filesDir, "omron_diag.txt").appendText(
                    "${java.time.LocalTime.now()} scan ok=${info.isSuccess} code=${info.resultCode} " +
                        "${list?.map { "${it.localName}|${it.modelName}|${it.deviceGroupIncludedGroupIDKey}" }}\n"
                )
            }
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

    /** Reads the measurements stored on a paired device and returns the newest one; saving is up to the user. */
    suspend fun sync(context: Context, device: PairedOmronDevice, profile: OmronProfile?): OmronSyncResult = mutex.withLock {
        val patient = AppDependencies.prefs.getPatient() ?: throw OmronException(0, "Please log in again.")
        val model = OmronModels.byCode(device.code) ?: throw OmronException(8003, "Unknown device.")
        configure(context, patient, listOf(device.config), model.kind, pairing = false, profile = profile)

        val m = manager(context)
        val target = OmronPeripheral(device.localName, device.uuid)
        val transferred = suspendCancellableCoroutine { cont ->
            m.startDataTransferFromPeripheral(target, arrayListOf(device.user), true) { peripheral, info ->
                if (info.isSuccess && peripheral != null) cont.resume(peripheral) else cont.resumeWithException(info.toException())
            }
            cont.invokeOnCancellation { runCatching { m.disconnectPeripheral(target) { _, _ -> } } }
        }
        val fresh = parse(transferred.vitalData, device.user)
        val ended = suspendCancellableCoroutine { cont ->
            m.endDataTransferFromPeripheral { peripheral, info ->
                cont.resume(if (info.isSuccess) peripheral else null)
            }
        }
        val afterEnd = ended?.let { parse(it.vitalData, device.user) }.orEmpty()

        val wanted = if (model.kind == OmronKind.BP) (fresh + afterEnd).filter { it.isBloodPressure } else fresh + afterEnd
        // Newest by device time; readings without one (clock not set) fall back to the record number
        val latest = wanted.maxWithOrNull(
            compareBy<OmronReading>({ it.timeFromDevice }, { if (it.timeFromDevice) it.time else Instant.EPOCH }, { it.sequence ?: Int.MIN_VALUE })
        )
        savePaired(context, pairedDevices(context).map {
            if (it.code == device.code) it.copy(lastSyncAt = System.currentTimeMillis()) else it
        })
        OmronSyncResult(latest, latest != null && latest.key in savedKeys(context, device))
    }

    // ── Saving ───────────────────────────────────────────────────────────

    private fun savedKeys(context: Context, device: PairedOmronDevice): Set<String> =
        prefs(context).getStringSet("${KEY_SAVED}_${device.code}", emptySet()).orEmpty()

    /** AddVital fields for a BP reading, or null if the reading can't be saved as one. */
    fun vitalFields(reading: OmronReading): Map<String, String>? {
        if (!reading.isBloodPressure) return null
        return mapOf(
            "vmValueBPSys" to reading.systolic.toString(),
            "vmValueBPDias" to reading.diastolic.toString(),
            "vmValuePulse" to (reading.pulse?.toString() ?: "")
        )
    }

    /** Remembers a saved reading so the same one isn't offered for saving again. */
    fun markSaved(context: Context, device: PairedOmronDevice, reading: OmronReading) {
        val keep = (savedKeys(context, device) + reading.key).toList().takeLast(500).toSet()
        prefs(context).edit().putStringSet("${KEY_SAVED}_${device.code}", keep).apply()
    }

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
            val measured = item[OmronConstants.OMRONVitalData.StartDateKey].asInstant()
            val seq = item[OmronConstants.OMRONVitalData.SequenceKey].asInt()
            Log.d(TAG, "bp $sys/$dia seq=$seq start=${item[OmronConstants.OMRONVitalData.StartDateKey]}")
            OmronReading(
                // Without a device time the key must not include the sync time, or the same reading looks new every sync
                key = "bp_${seq}_${measured?.epochSecond ?: "notime"}_$sys/$dia",
                time = measured ?: now, systolic = sys, diastolic = dia, timeFromDevice = measured != null, sequence = seq,
                pulse = item[OmronConstants.OMRONVitalData.PulseKey].asInt(),
                irregularHeartbeat = item[OmronConstants.OMRONVitalData.IrregularFlagKey].asInt() == 1
            )
        }
        val weight = (data[OmronConstants.OMRONVitalDataWeightKey] as? List<Map<String, Any?>>).orEmpty().mapNotNull { item ->
            val userId = item[OmronConstants.OMRONWeightData.UserIdKey].asInt()
            if (userId != null && userId != user) return@mapNotNull null
            val kg = item[OmronConstants.OMRONWeightData.WeightKey].asDouble() ?: return@mapNotNull null
            val measured = item[OmronConstants.OMRONWeightData.StartDateKey].asInstant()
            val seq = item[OmronConstants.OMRONWeightData.SequenceKey].asInt()
            OmronReading(
                key = "wt_${seq}_${measured?.epochSecond ?: "notime"}_$kg",
                time = measured ?: now, weightKg = kg, timeFromDevice = measured != null, sequence = seq,
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
