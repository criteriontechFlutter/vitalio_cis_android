package com.critetiontech.ctvitalio.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

// ─── Models ───────────────────────────────────────────────────────────────────

/**
 * Vitals read from wearables and saved to the patient's health record.
 *
 * Play's Health Connect policy only allows data types that back a user-facing feature, so read
 * nothing else here (Play rejected v2.0.27 for steps, sleep, workouts, glucose, weight, etc.).
 */
enum class VitalMetric(val label: String, val unit: String) {
    HEART_RATE("Heart Rate", "bpm"),
    BLOOD_PRESSURE("Blood Pressure", "mmHg"),
    SPO2("SpO₂", "%"),
    RESPIRATORY_RATE("Respiratory Rate", "br/min"),
    BODY_TEMPERATURE("Body Temperature", "°F")
}

data class VitalReading(
    val metric: VitalMetric,
    val value: String,
    val apiFields: Map<String, String>,
    val time: Instant,
    val source: String
)

data class WearableSnapshot(
    val vitals: List<VitalReading> = emptyList(),
    val sources: Set<String> = emptySet(),
    val readErrors: List<String> = emptyList()
)

data class WearableSyncResult(
    val snapshot: WearableSnapshot,
    val vitalsSaved: Int,
    val error: String?
)

// ─── Permissions & sources ────────────────────────────────────────────────────

object WearableData {

    /** Must match the READ_* health permissions in AndroidManifest.xml. */
    val RECORD_TYPES = listOf(
        HeartRateRecord::class, BloodPressureRecord::class, OxygenSaturationRecord::class,
        RespiratoryRateRecord::class, BodyTemperatureRecord::class
    )

    val READ_PERMISSIONS: Set<String> = RECORD_TYPES.map { HealthPermission.getReadPermission(it) }.toSet()
    const val BACKGROUND_PERMISSION = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

    fun permissionLabel(permission: String): String =
        permission.substringAfterLast("READ_").split('_')
            .joinToString(" ") { it.lowercase().replaceFirstChar(Char::uppercase) }

    private val SOURCE_NAMES = mapOf(
        "com.critetiontech.ctvitalio" to "Vitalio",
        "com.sec.android.app.shealth" to "Samsung Health",
        "com.samsung.android.wear.shealth" to "Galaxy Watch",
        "com.google.android.apps.fitness" to "Google Fit",
        "com.fitbit.FitbitMobile" to "Fitbit",
        "com.google.android.apps.healthdata" to "Health Connect",
        "com.garmin.android.apps.connectmobile" to "Garmin Connect",
        "com.huawei.health" to "Huawei Health",
        "com.mi.health" to "Mi Fitness",
        "com.xiaomi.wearable" to "Mi Fitness",
        "com.xiaomi.hm.health" to "Zepp Life",
        "com.huami.watch.hmwatchmanager" to "Zepp",
        "com.ouraring.oura" to "Oura",
        "com.whoop.android" to "WHOOP",
        "com.withings.wiscale2" to "Withings",
        "fi.polar.polarflow" to "Polar Flow",
        "com.oneplus.health.international" to "OHealth",
        "com.heytap.health.international" to "OHealth",
        "com.noisefit" to "NoiseFit",
        "com.boat.crest" to "boAt Crest",
        "com.fireboltt" to "Fire-Boltt",
        "com.amazfit" to "Zepp",
    )

    fun sourceName(packageName: String): String = SOURCE_NAMES[packageName]
        ?: packageName.substringAfterLast('.').replaceFirstChar(Char::uppercase)
}

private fun Record.source() = WearableData.sourceName(metadata.dataOrigin.packageName)

// ─── Reader ───────────────────────────────────────────────────────────────────

/** Reads whatever the granted permissions allow; types without permission are skipped. */
class WearableReader(private val client: HealthConnectClient) {

    suspend fun read(granted: Set<String>, now: Instant = Instant.now()): WearableSnapshot {
        val errors = mutableListOf<String>()
        val sources = mutableSetOf<String>()
        val last7Days = TimeRangeFilter.between(now.minus(7, ChronoUnit.DAYS), now)

        fun has(type: KClass<out Record>) = HealthPermission.getReadPermission(type) in granted

        suspend fun <T> attempt(name: String, block: suspend () -> T?): T? = try {
            block()
        } catch (e: Exception) {
            Log.w(TAG, "read $name failed: ${e.message}")
            errors += "$name: ${e.message}"
            null
        }

        suspend fun <T : Record> latest(type: KClass<T>, range: TimeRangeFilter = last7Days): T? =
            if (!has(type)) null else attempt(type.simpleName.orEmpty()) {
                client.readRecords(ReadRecordsRequest(type, range, ascendingOrder = false, pageSize = 1))
                    .records.firstOrNull()?.also { sources += it.source() }
            }

        val vitals = mutableListOf<VitalReading>()
        fun add(metric: VitalMetric, value: String, fields: Map<String, String>, time: Instant, record: Record) {
            vitals += VitalReading(metric, value, fields, time, record.source())
        }

        latest(HeartRateRecord::class)?.let { r ->
            r.samples.maxByOrNull { it.time }?.let {
                val bpm = it.beatsPerMinute.toString()
                add(VitalMetric.HEART_RATE, bpm, mapOf("vmValueHeartRate" to bpm, "vmValuePulse" to bpm), it.time, r)
            }
        }
        latest(BloodPressureRecord::class)?.let {
            val sys = it.systolic.inMillimetersOfMercury.toInt().toString()
            val dia = it.diastolic.inMillimetersOfMercury.toInt().toString()
            add(VitalMetric.BLOOD_PRESSURE, "$sys/$dia", mapOf("vmValueBPSys" to sys, "vmValueBPDias" to dia), it.time, it)
        }
        latest(OxygenSaturationRecord::class)?.let {
            val v = it.percentage.value.toInt().toString()
            add(VitalMetric.SPO2, v, mapOf("vmValueSPO2" to v), it.time, it)
        }
        latest(RespiratoryRateRecord::class)?.let {
            val v = it.rate.toInt().toString()
            add(VitalMetric.RESPIRATORY_RATE, v, mapOf("vmValueRespiratoryRate" to v), it.time, it)
        }
        latest(BodyTemperatureRecord::class)?.let {
            // Server stores temperature in °F, same as manual/voice entry
            val v = String.format(Locale.US, "%.1f", it.temperature.inFahrenheit)
            add(VitalMetric.BODY_TEMPERATURE, v, mapOf("vmValueTemperature" to v), it.time, it)
        }

        return WearableSnapshot(vitals, sources, errors)
    }

    companion object { private const val TAG = "WearableReader" }
}

// ─── Sync ─────────────────────────────────────────────────────────────────────

/**
 * Pushes new wearable vitals to the server.
 *
 * Remembers, per patient, the time of the last reading saved for each vital, so foreground,
 * manual and background runs never send the same reading twice.
 */
object WearableSync {

    private const val TAG = "WearableSync"
    private const val PREFS = "wearable_sync"
    private const val KEY_AUTO_SYNC = "auto_sync"
    private const val KEY_LAST_RUN = "last_run"
    private const val WORK_NAME = "wearable-sync"
    private val FOREGROUND_INTERVAL = Duration.ofMinutes(15)

    private val mutex = Mutex()
    private val saveMeasuredVitals by lazy { AppDependencies.saveMeasuredVitals() }

    fun clientOrNull(context: Context): HealthConnectClient? =
        if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE)
            runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull()
        else null

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun isAutoSyncEnabled(context: Context) = prefs(context).getBoolean(KEY_AUTO_SYNC, false)

    fun setAutoSync(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_AUTO_SYNC, enabled).apply()
        schedule(context)
    }

    /** Keeps the hourly background job in line with the auto-sync setting. */
    fun schedule(context: Context) {
        val work = WorkManager.getInstance(context)
        if (!isAutoSyncEnabled(context)) {
            work.cancelUniqueWork(WORK_NAME)
            return
        }
        val request = PeriodicWorkRequestBuilder<WearableSyncWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        work.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    suspend fun canReadInBackground(client: HealthConnectClient): Boolean = runCatching {
        client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND) ==
                HealthConnectFeatures.FEATURE_STATUS_AVAILABLE &&
                WearableData.BACKGROUND_PERMISSION in client.permissionController.getGrantedPermissions()
    }.getOrDefault(false)

    /** Called when the app comes to the foreground; syncs at most every 15 minutes. */
    suspend fun syncIfDue(context: Context) {
        if (!isAutoSyncEnabled(context) || AppDependencies.prefs.getPatient() == null) return
        val lastRun = prefs(context).getLong(KEY_LAST_RUN, 0)
        if (Instant.now().isBefore(Instant.ofEpochMilli(lastRun).plus(FOREGROUND_INTERVAL))) return
        val client = clientOrNull(context) ?: return
        sync(context, client)
    }

    suspend fun sync(context: Context, client: HealthConnectClient): WearableSyncResult = mutex.withLock {
        val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
        val snapshot = WearableReader(client).read(granted)
        val patient = AppDependencies.prefs.getPatient()
            ?: return WearableSyncResult(snapshot, 0, "User session not found. Please log in again.")
        val store = prefs(context)
        val key = { name: String -> "${patient.uhId}_$name" }
        var error: String? = null

        // One AddVital call per minute of readings, stamped with when they were measured
        var vitalsSaved = 0
        snapshot.vitals
            .filter { it.time.toEpochMilli() > store.getLong(key("vital_${it.metric}"), 0) }
            .groupBy { it.time.truncatedTo(ChronoUnit.MINUTES) }
            .toSortedMap()
            .forEach { (minute, readings) ->
                val failure = (saveMeasuredVitals(patient, minute, readings.fold(emptyMap()) { acc, r -> acc + r.apiFields }) as? DomainResult.Error)
                    ?.let { it.exception.message ?: "Couldn't save the vitals. Please try again." }
                if (failure == null) {
                    vitalsSaved += readings.size
                    store.edit().apply {
                        readings.forEach { putLong(key("vital_${it.metric}"), it.time.toEpochMilli()) }
                    }.apply()
                } else {
                    Log.e(TAG, "addVital failed: $failure")
                    error = failure
                }
            }

        store.edit().putLong(KEY_LAST_RUN, System.currentTimeMillis()).apply()
        Log.d(TAG, "sync done: vitals=$vitalsSaved error=$error")
        WearableSyncResult(snapshot, vitalsSaved, error)
    }

    /** True when a reading has already been saved to the server for this patient. */
    fun isSaved(context: Context, reading: VitalReading): Boolean {
        val uhid = AppDependencies.prefs.getPatient()?.uhId ?: return false
        return reading.time.toEpochMilli() <= prefs(context).getLong("${uhid}_vital_${reading.metric}", 0)
    }

}

/** Hourly background sync; only runs where Health Connect allows background reads. */
class WearableSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val client = WearableSync.clientOrNull(applicationContext) ?: return Result.success()
        if (!WearableSync.canReadInBackground(client)) return Result.success()
        val result = WearableSync.sync(applicationContext, client)
        return if (result.error != null && runAttemptCount < 3) Result.retry() else Result.success()
    }
}
