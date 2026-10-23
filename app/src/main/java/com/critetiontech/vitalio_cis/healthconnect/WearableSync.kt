package com.critetiontech.ctvitalio.healthconnect

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.AggregateRequest
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
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass

// ─── Models ───────────────────────────────────────────────────────────────────

/**
 * Vitals read from wearables. AddVital accepts weight/height but doesn't store them
 * (verified 2026-10-21), so those are shown in the app only.
 */
enum class VitalMetric(val label: String, val unit: String, val savedToServer: Boolean = true) {
    HEART_RATE("Heart Rate", "bpm"),
    BLOOD_PRESSURE("Blood Pressure", "mmHg"),
    SPO2("SpO₂", "%"),
    RESPIRATORY_RATE("Respiratory Rate", "br/min"),
    BODY_TEMPERATURE("Body Temperature", "°F"),
    BLOOD_GLUCOSE("Blood Glucose", "mg/dL"),
    WEIGHT("Weight", "kg", savedToServer = false),
    HEIGHT("Height", "cm", savedToServer = false)
}

data class VitalReading(
    val metric: VitalMetric,
    val value: String,
    val apiFields: Map<String, String>,
    val time: Instant,
    val source: String
)

/** Today's totals. These have no server API yet, so they are only shown in the app. */
data class DailySummary(
    val steps: Long? = null,
    val distanceKm: Double? = null,
    val activeKcal: Double? = null,
    val totalKcal: Double? = null,
    val sleep: Duration? = null,
    val restingHeartRate: Long? = null,
    val hrvMs: Double? = null
)

data class Workout(
    val id: String,
    val name: String,
    /** Name in the server's activity master, or null when the server has no matching activity. */
    val activityName: String?,
    val start: Instant,
    val end: Instant,
    val source: String
)

data class WearableSnapshot(
    val vitals: List<VitalReading> = emptyList(),
    val daily: DailySummary = DailySummary(),
    val workouts: List<Workout> = emptyList(),
    val sources: Set<String> = emptySet(),
    val readErrors: List<String> = emptyList()
)

data class WearableSyncResult(
    val snapshot: WearableSnapshot,
    val vitalsSaved: Int,
    val workoutsSaved: Int,
    val error: String?
)

// ─── Permissions & sources ────────────────────────────────────────────────────

object WearableData {

    private val VITAL_TYPES = listOf(
        HeartRateRecord::class, BloodPressureRecord::class, OxygenSaturationRecord::class,
        RespiratoryRateRecord::class, BodyTemperatureRecord::class, BloodGlucoseRecord::class,
        WeightRecord::class, HeightRecord::class
    )
    private val ACTIVITY_TYPES = listOf(
        StepsRecord::class, DistanceRecord::class, ActiveCaloriesBurnedRecord::class,
        TotalCaloriesBurnedRecord::class, ExerciseSessionRecord::class, SleepSessionRecord::class,
        RestingHeartRateRecord::class, HeartRateVariabilityRmssdRecord::class
    )
    val RECORD_TYPES = VITAL_TYPES + ACTIVITY_TYPES

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

    /** Health Connect exercise types the server's activity master has a match for. */
    private val ACTIVITY_NAMES = mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING to "Walking",
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to "Running",
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to "Running",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to "Cycling",
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to "Stationary Biking",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to "Swimming",
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to "Swimming",
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING to "Hiking",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to "Stair Climbing",
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING_MACHINE to "Stair Climbing",
        ExerciseSessionRecord.EXERCISE_TYPE_BADMINTON to "Badminton",
        ExerciseSessionRecord.EXERCISE_TYPE_BASEBALL to "Baseball",
        ExerciseSessionRecord.EXERCISE_TYPE_BASKETBALL to "Basketball",
        ExerciseSessionRecord.EXERCISE_TYPE_BOXING to "Boxing",
        ExerciseSessionRecord.EXERCISE_TYPE_CRICKET to "Cricket",
    )

    fun activityName(exerciseType: Int): String? = ACTIVITY_NAMES[exerciseType]

    fun exerciseLabel(exerciseType: Int): String =
        ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP[exerciseType]
            ?.split('_')?.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
            ?: "Workout"
}

private fun Record.source() = WearableData.sourceName(metadata.dataOrigin.packageName)

// ─── Reader ───────────────────────────────────────────────────────────────────

/** Reads whatever the granted permissions allow; types without permission are skipped. */
class WearableReader(private val client: HealthConnectClient) {

    suspend fun read(granted: Set<String>, now: Instant = Instant.now()): WearableSnapshot {
        val errors = mutableListOf<String>()
        val sources = mutableSetOf<String>()
        val zone = ZoneId.systemDefault()
        val last7Days = TimeRangeFilter.between(now.minus(7, ChronoUnit.DAYS), now)
        val startOfToday = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val today = TimeRangeFilter.between(startOfToday, now)

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

        suspend fun aggregate(type: KClass<out Record>, metric: AggregateMetric<*>, range: TimeRangeFilter): Any? =
            if (!has(type)) null else attempt(type.simpleName.orEmpty()) {
                val result = client.aggregate(AggregateRequest(setOf(metric), range))
                if (result[metric] != null) sources += result.dataOrigins.map { WearableData.sourceName(it.packageName) }
                result[metric]
            }

        /**
         * Aggregates leave out apps missing from Health Connect's data-source priority list, so when an
         * aggregate is empty fall back to raw records, taking the single app with the highest total so
         * phone + watch data isn't counted twice.
         */
        suspend fun <T : Record> bestSourceTotal(type: KClass<T>, range: TimeRangeFilter, value: (T) -> Double): Double? =
            if (!has(type)) null else attempt(type.simpleName.orEmpty()) {
                val records = mutableListOf<T>()
                var pageToken: String? = null
                do {
                    val page = client.readRecords(ReadRecordsRequest(type, range, pageToken = pageToken))
                    records += page.records
                    pageToken = page.pageToken
                } while (pageToken != null)
                records.groupBy { it.metadata.dataOrigin.packageName }
                    .mapValues { (_, recs) -> recs.sumOf(value) }
                    .maxByOrNull { it.value }
                    ?.also { sources += WearableData.sourceName(it.key) }
                    ?.value
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
        latest(BloodGlucoseRecord::class)?.let {
            val v = it.level.inMilligramsPerDeciliter.toInt().toString()
            add(VitalMetric.BLOOD_GLUCOSE, v, mapOf("vmValueRbs" to v), it.time, it)
        }
        latest(WeightRecord::class, TimeRangeFilter.before(now))?.let {
            val v = String.format(Locale.US, "%.1f", it.weight.inKilograms)
            add(VitalMetric.WEIGHT, v, mapOf("vmValueWeight" to v), it.time, it)
        }
        latest(HeightRecord::class, TimeRangeFilter.before(now))?.let {
            val v = (it.height.inMeters * 100).toInt().toString()
            add(VitalMetric.HEIGHT, v, mapOf("vmValueHeight" to v), it.time, it)
        }

        // Aggregates de-duplicate overlapping data (e.g. steps counted by both phone and watch)
        val sleepWindow = TimeRangeFilter.between(startOfToday.minus(6, ChronoUnit.HOURS), now)
        val daily = DailySummary(
            steps = (aggregate(StepsRecord::class, StepsRecord.COUNT_TOTAL, today) as Long?)
                ?: bestSourceTotal(StepsRecord::class, today) { it.count.toDouble() }?.toLong(),
            distanceKm = (aggregate(DistanceRecord::class, DistanceRecord.DISTANCE_TOTAL, today)
                    as androidx.health.connect.client.units.Length?)?.inKilometers
                ?: bestSourceTotal(DistanceRecord::class, today) { it.distance.inKilometers },
            activeKcal = (aggregate(ActiveCaloriesBurnedRecord::class, ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, today)
                    as androidx.health.connect.client.units.Energy?)?.inKilocalories
                ?: bestSourceTotal(ActiveCaloriesBurnedRecord::class, today) { it.energy.inKilocalories },
            totalKcal = (aggregate(TotalCaloriesBurnedRecord::class, TotalCaloriesBurnedRecord.ENERGY_TOTAL, today)
                    as androidx.health.connect.client.units.Energy?)?.inKilocalories
                ?: bestSourceTotal(TotalCaloriesBurnedRecord::class, today) { it.energy.inKilocalories },
            sleep = (aggregate(SleepSessionRecord::class, SleepSessionRecord.SLEEP_DURATION_TOTAL, sleepWindow) as Duration?)
                ?.takeIf { !it.isZero }
                ?: bestSourceTotal(SleepSessionRecord::class, sleepWindow) {
                    Duration.between(it.startTime, it.endTime).toMinutes().toDouble()
                }?.let { Duration.ofMinutes(it.toLong()) },
            restingHeartRate = latest(RestingHeartRateRecord::class)?.beatsPerMinute,
            hrvMs = latest(HeartRateVariabilityRmssdRecord::class)?.heartRateVariabilityMillis
        )

        val workouts = if (!has(ExerciseSessionRecord::class)) emptyList() else attempt("ExerciseSession") {
            client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, today)).records.map {
                sources += it.source()
                Workout(
                    id = it.metadata.id,
                    name = it.title?.takeIf(String::isNotBlank) ?: WearableData.exerciseLabel(it.exerciseType),
                    activityName = WearableData.activityName(it.exerciseType),
                    start = it.startTime, end = it.endTime, source = it.source()
                )
            }
        }.orEmpty().fold(emptyList<Workout>()) { kept, w ->
            // The same workout often arrives from two apps (e.g. Samsung Health and Google Fit); keep one
            val overlaps = kept.any { k -> k.start < w.end && w.start < k.end }
            if (overlaps) kept else kept + w
        }

        return WearableSnapshot(vitals, daily, workouts, sources, errors)
    }

    companion object { private const val TAG = "WearableReader" }
}

// ─── Sync ─────────────────────────────────────────────────────────────────────

/**
 * Pushes new wearable vitals and today's workouts to the server.
 *
 * Remembers, per patient, the time of the last reading saved for each vital and the workouts
 * already sent, so foreground, manual and background runs never send the same reading twice.
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
    private val fetchActivityMaster by lazy { AppDependencies.fetchActivityMaster() }
    private val addPatientActivity by lazy { AppDependencies.addPatientActivity() }

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
            ?: return WearableSyncResult(snapshot, 0, 0, "User session not found. Please log in again.")
        val store = prefs(context)
        val key = { name: String -> "${patient.uhId}_$name" }
        var error: String? = null

        // One AddVital call per minute of readings, stamped with when they were measured
        var vitalsSaved = 0
        snapshot.vitals
            .filter { it.metric.savedToServer && it.time.toEpochMilli() > store.getLong(key("vital_${it.metric}"), 0) }
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

        // Activity API takes only a time of day, so only today's finished workouts can be sent
        var workoutsSaved = 0
        val sent = store.getStringSet(key("workouts"), emptySet()).orEmpty()
        val pending = snapshot.workouts.filter { it.activityName != null && it.id !in sent && it.end.isBefore(Instant.now()) }
        if (pending.isNotEmpty()) {
            val masterIds = fetchActivityIds()
            if (masterIds == null) error = error ?: "Couldn't load the activity list. Workouts will sync next time."
            val nowSent = sent.toMutableSet()
            pending.forEach { w ->
                val activityId = masterIds?.get(w.activityName!!.lowercase()) ?: return@forEach
                when (postWorkout(patient.uhId, patient.clientId, activityId, w)) {
                    WorkoutPost.SAVED -> { nowSent += w.id; workoutsSaved++ }
                    WorkoutPost.ALREADY_EXISTS -> nowSent += w.id
                    WorkoutPost.FAILED -> error = error ?: "Couldn't save the workout \"${w.name}\"."
                }
            }
            // Keep the set small: only workouts still visible today matter
            val todayIds = snapshot.workouts.map { it.id }.toSet()
            store.edit().putStringSet(key("workouts"), nowSent.filter { it in todayIds }.toSet()).apply()
        }

        store.edit().putLong(KEY_LAST_RUN, System.currentTimeMillis()).apply()
        Log.d(TAG, "sync done: vitals=$vitalsSaved workouts=$workoutsSaved error=$error")
        WearableSyncResult(snapshot, vitalsSaved, workoutsSaved, error)
    }

    /** True when a reading has already been saved to the server for this patient. */
    fun isSaved(context: Context, reading: VitalReading): Boolean {
        if (!reading.metric.savedToServer) return false
        val uhid = AppDependencies.prefs.getPatient()?.uhId ?: return false
        return reading.time.toEpochMilli() <= prefs(context).getLong("${uhid}_vital_${reading.metric}", 0)
    }

    fun isSaved(context: Context, workout: Workout): Boolean {
        val uhid = AppDependencies.prefs.getPatient()?.uhId ?: return false
        return workout.id in prefs(context).getStringSet("${uhid}_workouts", emptySet()).orEmpty()
    }

    private suspend fun fetchActivityIds(): Map<String, Int>? =
        when (val result = fetchActivityMaster()) {
            is DomainResult.Success -> result.data.associate { it.activityName.lowercase() to it.id }
            is DomainResult.Error -> { Log.e(TAG, "activity master failed: ${result.exception.message}"); null }
        }

    private enum class WorkoutPost { SAVED, ALREADY_EXISTS, FAILED }

    private suspend fun postWorkout(uhid: String, clientId: Int, activityId: Int, w: Workout): WorkoutPost {
        val request = AddPatientActivityRequest(
            uhid = uhid,
            activityId = activityId,
            timeFrom = w.start.atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("HH:mm:ss")),
            totalTimeInMinutes = Duration.between(w.start, w.end).toMinutes().coerceAtLeast(1).toString(),
            remark = "Synced from ${w.source}",
            userId = 0,
            clientID = clientId,
        )
        return when (val result = addPatientActivity(request)) {
            is DomainResult.Success -> if (result.data == ActivitySaveResult.ALREADY_EXISTS) WorkoutPost.ALREADY_EXISTS else WorkoutPost.SAVED
            is DomainResult.Error -> { Log.e(TAG, "addPatientActivity failed: ${result.exception.message}"); WorkoutPost.FAILED }
        }
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
