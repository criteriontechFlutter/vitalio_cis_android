package com.critetiontech.ctvitalio.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.critetiontech.ctvitalio.MainActivity
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.STANDARD_FLUID_INTAKE_ML
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Reminders that run on the phone alone, with no push from the server: drink water,
 * a follow-up when a medicine alarm wasn't acted on, and upcoming appointments.
 */
class NudgeSettings(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("health_nudges", Context.MODE_PRIVATE)

    var fluidEnabled: Boolean
        get() = sp.getBoolean(KEY_FLUID, true)
        set(value) = sp.edit { putBoolean(KEY_FLUID, value) }

    var missedDoseEnabled: Boolean
        get() = sp.getBoolean(KEY_MISSED_DOSE, true)
        set(value) = sp.edit { putBoolean(KEY_MISSED_DOSE, value) }

    var appointmentEnabled: Boolean
        get() = sp.getBoolean(KEY_APPOINTMENT, true)
        set(value) = sp.edit { putBoolean(KEY_APPOINTMENT, value) }

    /** Day ("2026-10-30") the "goal reached" notice was last shown, so it shows once a day. */
    var fluidGoalNotifiedOn: String?
        get() = sp.getString(KEY_FLUID_GOAL_DAY, null)
        set(value) = sp.edit { putString(KEY_FLUID_GOAL_DAY, value) }

    /** foodID of "Water" in the patient's food list, needed to log water from the notification. */
    var waterFoodId: Int
        get() = sp.getInt(KEY_WATER_FOOD_ID, 0)
        set(value) = sp.edit { putInt(KEY_WATER_FOOD_ID, value) }

    /** Today's intake and goal as last seen, so a quick-add can show the new total right away. */
    fun fluidSnapshot(day: String): Pair<Int, Int>? =
        if (sp.getString(KEY_FLUID_DAY, null) == day) sp.getInt(KEY_FLUID_TOTAL, 0) to sp.getInt(KEY_FLUID_GOAL, 0) else null

    fun saveFluidSnapshot(day: String, total: Int, goal: Int) =
        sp.edit { putString(KEY_FLUID_DAY, day); putInt(KEY_FLUID_TOTAL, total); putInt(KEY_FLUID_GOAL, goal) }

    /** Request codes of the appointment alarms currently registered with AlarmManager. */
    var appointmentCodes: Set<Int>
        get() = sp.getStringSet(KEY_APPOINTMENT_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        set(value) = sp.edit { putStringSet(KEY_APPOINTMENT_CODES, value.map { it.toString() }.toSet()) }

    /** The appointment alerts last built from the server, to put back after a reboot while offline. */
    var appointmentAlerts: List<AppointmentAlert>
        get() = sp.getString(KEY_APPOINTMENT_ALERTS, null)?.let {
            try { gson.fromJson<List<AppointmentAlert>>(it, object : TypeToken<List<AppointmentAlert>>() {}.type) }
            catch (e: Exception) { null }
        }.orEmpty()
        set(value) = sp.edit { putString(KEY_APPOINTMENT_ALERTS, gson.toJson(value)) }

    private val gson = Gson()

    private companion object {
        const val KEY_FLUID = "fluid_enabled"
        const val KEY_MISSED_DOSE = "missed_dose_enabled"
        const val KEY_APPOINTMENT = "appointment_enabled"
        const val KEY_FLUID_GOAL_DAY = "fluid_goal_notified_on"
        const val KEY_APPOINTMENT_CODES = "appointment_codes"
        const val KEY_APPOINTMENT_ALERTS = "appointment_alerts"
        const val KEY_WATER_FOOD_ID = "water_food_id"
        const val KEY_FLUID_DAY = "fluid_day"
        const val KEY_FLUID_TOTAL = "fluid_total"
        const val KEY_FLUID_GOAL = "fluid_goal"
    }
}

object HealthNudges {

    private const val TAG = "HealthNudges"
    const val CHANNEL_ID = "health_nudges"
    private const val PERIODIC_WORK = "health-nudges"
    const val FLUID_NOTIFICATION_ID = 0x7A7E

    /** Starts or stops the background check to match the toggles. Safe to call repeatedly. */
    fun schedule(context: Context) {
        val settings = NudgeSettings(context)
        val work = WorkManager.getInstance(context)
        if (settings.fluidEnabled || settings.appointmentEnabled) {
            val request = PeriodicWorkRequestBuilder<HealthNudgeWorker>(2, TimeUnit.HOURS).build()
            work.enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        } else {
            work.cancelUniqueWork(PERIODIC_WORK)
        }
        if (!settings.fluidEnabled) {
            context.getSystemService(NotificationManager::class.java).cancel(FLUID_NOTIFICATION_ID)
        }
        AppointmentReminders.requestSync(context)
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.nudge_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.getString(R.string.nudge_channel_desc)
            }
        )
    }

    fun openAppIntent(context: Context, requestCode: Int): PendingIntent = PendingIntent.getActivity(
        context, requestCode,
        Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    fun notify(
        context: Context,
        id: Int,
        title: String,
        text: String,
        configure: NotificationCompat.Builder.() -> Unit = {},
    ) {
        ensureChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppIntent(context, id))
            .setAutoCancel(true)
            .apply(configure)
            .build()
        try {
            context.getSystemService(NotificationManager::class.java).notify(id, notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "notification permission missing", e)
        }
    }
}

// ── Water ──────────────────────────────────────────────────────────────────────

/** Water reminders are only sent between these hours. */
private val FLUID_DAY_START: LocalTime = LocalTime.of(8, 0)
private val FLUID_DAY_END: LocalTime = LocalTime.of(21, 0)

/**
 * Runs every ~2 hours: nudges about water when today's intake is behind pace for the
 * recommended goal, and refreshes the appointment alarms.
 */
class HealthNudgeWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val settings = NudgeSettings(app)
        if (settings.appointmentEnabled) AppointmentReminders.sync(app)
        if (settings.fluidEnabled) checkFluid(app, settings)
        return Result.success()
    }

    private suspend fun checkFluid(context: Context, settings: NudgeSettings) {
        val now = LocalTime.now()
        if (now.isBefore(FLUID_DAY_START.plusHours(1)) || now.isAfter(FLUID_DAY_END)) return
        val patient = AppDependencies.prefs.getPatient() ?: return

        val today = LocalDate.now().toString()
        val fetched = AppDependencies.fetchIntake()(patient.uhId, patient.clientId.toString(), today)
        val items = (fetched as? DomainResult.Success)?.data
            ?: AppDependencies.fluidRepository.cachedIntakeItems(patient.uhId, today)
        val goal = items.firstNotNullOfOrNull { it.fluidRecommendedMl } ?: STANDARD_FLUID_INTAKE_ML
        // Offline the cache can predate water logged from this notification; don't count less than that.
        val total = items.sumOf { it.foodQuantity }.let { cachedTotal ->
            if (fetched is DomainResult.Success) cachedTotal
            else maxOf(cachedTotal, settings.fluidSnapshot(today)?.first ?: 0)
        }
        settings.saveFluidSnapshot(today, total, goal)

        if (total >= goal) {
            if (settings.fluidGoalNotifiedOn != today) {
                settings.fluidGoalNotifiedOn = today
                HealthNudges.notify(
                    context, HealthNudges.FLUID_NOTIFICATION_ID,
                    context.getString(R.string.nudge_fluid_goal_title),
                    context.getString(R.string.nudge_fluid_goal_text, total),
                )
            }
            return
        }

        // Where intake should be by now if spread evenly over the day; no nudge while on track.
        val dayMinutes = (FLUID_DAY_END.toSecondOfDay() - FLUID_DAY_START.toSecondOfDay()) / 60
        val elapsed = ((now.toSecondOfDay() - FLUID_DAY_START.toSecondOfDay()) / 60).coerceIn(0, dayMinutes)
        val expected = goal * elapsed / dayMinutes
        if (total >= expected) return

        val waterId = WaterQuickAdd.resolveWaterFoodId(context, patient.uhId)
        HealthNudges.notify(
            context, HealthNudges.FLUID_NOTIFICATION_ID,
            context.getString(R.string.nudge_fluid_title),
            context.getString(R.string.nudge_fluid_text, total, goal),
        ) {
            // Without the water foodId the server can't log it; the notification still opens the app.
            if (waterId > 0) WaterQuickAdd.AMOUNTS_ML.forEach { ml ->
                addAction(0, context.getString(R.string.nudge_action_add_ml, ml), WaterQuickAdd.actionIntent(context, ml))
            }
        }
    }
}

// ── Water quick-add from the notification ──────────────────────────────────────

object WaterQuickAdd {

    val AMOUNTS_ML = listOf(250, 500)
    const val ACTION_ADD_WATER = "com.critetiontech.ctvitalio.reminder.ADD_WATER"
    const val EXTRA_ML = "ml"
    const val KEY_LOGGED_AT = "logged_at"

    fun actionIntent(context: Context, ml: Int): PendingIntent = PendingIntent.getBroadcast(
        context, ml,
        Intent(context, NudgeReceiver::class.java).setAction(ACTION_ADD_WATER).putExtra(EXTRA_ML, ml),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Finds "Water" in the patient's food list (cache first) and remembers its id. */
    suspend fun resolveWaterFoodId(context: Context, uhid: String): Int {
        val settings = NudgeSettings(context)
        val isWater = { name: String? -> name.orEmpty().contains("water", ignoreCase = true) }
        val item = AppDependencies.fluidRepository.cachedManualFoodList(uhid).firstOrNull { isWater(it.foodName) }
            ?: (AppDependencies.getManualFoodList()(uhid) as? DomainResult.Success)?.data?.firstOrNull { isWater(it.foodName) }
        item?.foodID?.takeIf { it > 0 }?.let { settings.waterFoodId = it }
        return settings.waterFoodId
    }

    /** Called from the notification button: shows "Saving…" and queues the upload (runs once online). */
    fun onTap(context: Context, ml: Int) {
        if (AppDependencies.prefs.getPatient() == null) {
            HealthNudges.notify(context, HealthNudges.FLUID_NOTIFICATION_ID,
                context.getString(R.string.nudge_water_login_title), context.getString(R.string.nudge_water_login_text))
            return
        }
        // No buttons while saving, so a second tap can't log the same glass twice.
        HealthNudges.notify(context, HealthNudges.FLUID_NOTIFICATION_ID,
            context.getString(R.string.nudge_water_saving_title, ml), context.getString(R.string.nudge_water_saving_text)) {
            setOnlyAlertOnce(true).setSilent(true)
        }
        val request = OneTimeWorkRequestBuilder<WaterQuickAddWorker>()
            .setInputData(workDataOf(EXTRA_ML to ml, KEY_LOGGED_AT to Instant.now().toString()))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueue(request)
    }
}

class WaterQuickAddWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        val ml = inputData.getInt(WaterQuickAdd.EXTRA_ML, 0)
        val patient = AppDependencies.prefs.getPatient() ?: return Result.success()
        val waterId = WaterQuickAdd.resolveWaterFoodId(app, patient.uhId)
        if (ml <= 0 || waterId <= 0) {
            failed(app)
            return Result.success()
        }
        // Same body the Fluid Intake screen sends; FoodDate is when the button was tapped.
        val body = mapOf<String, Any>(
            "FoodDate" to (inputData.getString(WaterQuickAdd.KEY_LOGGED_AT) ?: Instant.now().toString()),
            "FoodQuantity" to ml.toString(), "UserID" to patient.pid, "foodId" to waterId.toString(),
            "clientId" to patient.clientId, "FoodUnitId" to 27, "uhid" to patient.uhId,
            "isGiven" to 1, "entryType" to "N", "isFromPatient" to 1,
        )
        return when (AppDependencies.addIntake()(body)) {
            is DomainResult.Success -> { saved(app, ml); Result.success() }
            is DomainResult.Error ->
                if (runAttemptCount < 3) Result.retry() else { failed(app); Result.success() }
        }
    }

    private fun saved(context: Context, ml: Int) {
        val settings = NudgeSettings(context)
        val today = LocalDate.now().toString()
        val (before, lastGoal) = settings.fluidSnapshot(today) ?: (0 to 0)
        val goal = lastGoal.takeIf { it > 0 } ?: STANDARD_FLUID_INTAKE_ML
        val total = before + ml
        settings.saveFluidSnapshot(today, total, goal)

        if (total >= goal) {
            settings.fluidGoalNotifiedOn = today
            HealthNudges.notify(context, HealthNudges.FLUID_NOTIFICATION_ID,
                context.getString(R.string.nudge_fluid_goal_title), context.getString(R.string.nudge_fluid_goal_text, total))
        } else {
            HealthNudges.notify(context, HealthNudges.FLUID_NOTIFICATION_ID,
                context.getString(R.string.nudge_water_added_title, ml), context.getString(R.string.nudge_water_added_text, total, goal)) {
                setOnlyAlertOnce(true).setSilent(true).setTimeoutAfter(10_000L)
            }
        }
    }

    private fun failed(context: Context) {
        HealthNudges.notify(context, HealthNudges.FLUID_NOTIFICATION_ID,
            context.getString(R.string.nudge_water_failed_title), context.getString(R.string.nudge_water_failed_text))
    }
}

// ── Missed medicine ────────────────────────────────────────────────────────────

object MissedDoseCheck {

    /** How long after the alarm rings to check whether the dose was logged. */
    private const val DELAY_MINUTES = 30L
    const val KEY_ALERT_AT = "alert_at"
    const val KEY_DOSES = "dose_keys"

    /** Called when a medicine alarm rings; a snoozed re-ring replaces the pending check. */
    fun schedule(context: Context, alarm: ReminderAlarm) {
        if (alarm.isTest || !NudgeSettings(context).missedDoseEnabled) return
        val keys = alarm.doses.map { it.key }.sorted()
        val request = OneTimeWorkRequestBuilder<MissedDoseWorker>()
            .setInitialDelay(DELAY_MINUTES, TimeUnit.MINUTES)
            .setInputData(workDataOf(KEY_ALERT_AT to alarm.alertAt, KEY_DOSES to keys.toTypedArray()))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("missed-dose-${keys.hashCode()}", ExistingWorkPolicy.REPLACE, request)
    }
}

class MissedDoseWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext
        if (!NudgeSettings(app).missedDoseEnabled) return Result.success()
        val keys = inputData.getStringArray(MissedDoseCheck.KEY_DOSES)?.toSet().orEmpty()
        if (keys.isEmpty()) return Result.success()

        // Pick up doses logged from the dashboard since the alarm; on failure the cached list is used.
        MedicineReminderScheduler.sync(app)
        val settings = ReminderSettings(app)
        if (!settings.enabled) return Result.success()
        val pending = settings.doses.filter {
            it.key in keys && it.key !in settings.takenLocally && !settings.isMuted(it.medicineName)
        }
        if (pending.isEmpty()) return Result.success()

        // Same alertAt → same request code as the original alarm, so this replaces that
        // notification and its "Taken" action clears this one.
        val alarm = ReminderAlarm(inputData.getLong(MissedDoseCheck.KEY_ALERT_AT, 0L), pending)
        val text = if (pending.size == 1) {
            val dose = pending[0]
            app.getString(R.string.nudge_missed_dose_one, dose.medicineName, MedicineAlarmNotifier.formatTime(dose))
        } else {
            app.getString(R.string.nudge_missed_dose_many, pending.size, pending.joinToString(", ") { it.medicineName })
        }
        HealthNudges.notify(app, alarm.requestCode, app.getString(R.string.nudge_missed_dose_title), text) {
            addAction(0, app.getString(R.string.nudge_action_taken),
                MedicineAlarmNotifier.actionIntent(app, alarm, MedicineAlarmReceiver.ACTION_TAKEN))
        }
        return Result.success()
    }
}

// ── Appointments ───────────────────────────────────────────────────────────────

/** One appointment notification and when it should show. */
data class AppointmentAlert(
    val at: Long,
    val title: String,
    val text: String,
    val requestCode: Int,
    val notificationId: Int,
)

/**
 * Sets local alarms 1 hour and 10 minutes before each upcoming appointment, or a morning
 * notice when the appointment has no slot time. Rebuilt from the server on every sync.
 */
object AppointmentReminders {

    private const val TAG = "AppointmentReminders"
    const val ACTION_APPOINTMENT = "com.critetiontech.ctvitalio.reminder.APPOINTMENT"
    private const val EXTRA_TITLE = "title"
    private const val EXTRA_TEXT = "text"
    private const val EXTRA_ID = "notification_id"
    private val MORNING_NOTICE: LocalTime = LocalTime.of(8, 0)
    private val LEAD_MINUTES = listOf(60L, 10L)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Mutex()

    private data class Upcoming(val key: String, val doctor: String, val date: LocalDate, val time: LocalTime?)

    fun requestSync(context: Context) {
        val app = context.applicationContext
        scope.launch { sync(app) }
    }

    suspend fun sync(context: Context) = lock.withLock {
        val settings = NudgeSettings(context)
        val patient = AppDependencies.prefs.getPatient()
        if (!settings.appointmentEnabled || patient == null) {
            cancelAll(context, settings)
            settings.appointmentAlerts = emptyList()
            return@withLock
        }
        val pid = patient.pid.toString()
        val clientId = patient.clientId.toString()
        val upcoming = AppDependencies.fetchUpcomingAppointments()(pid, clientId)
        if (upcoming !is DomainResult.Success) {
            // Alarms don't survive a reboot or app update; put back the last known ones.
            Log.w(TAG, "sync: could not fetch appointments, restoring cached alarms")
            register(context, settings, settings.appointmentAlerts)
            return@withLock
        }
        // The upcoming API's visitDate is the booking date; the real date comes from history.
        val history = (AppDependencies.fetchAppointmentHistory()(pid, clientId) as? DomainResult.Success)?.data.orEmpty()
        val byVisit = history.associateBy { it.visitNo }

        val appointments = upcoming.data.mapNotNull { item ->
            val match = byVisit[item.visitNo]
            if (match?.isCancelAppointment == true) return@mapNotNull null
            val date = parseIsoDate(match?.appointmentDate) ?: parseDmyDate(item.visitDate) ?: return@mapNotNull null
            Upcoming(
                key = "${item.appoientmentId}_${item.visitNo}",
                doctor = item.doctorName.trim().ifEmpty { context.getString(R.string.consult_doctor_fallback) },
                date = date,
                time = parseSlotTime(item.slotTime),
            )
        }

        val alerts = appointments.flatMap { appt ->
            alertsFor(context, appt).map { (at, title, text) ->
                AppointmentAlert(at, title, text, "appt|${appt.key}|$at".hashCode(), "appt|${appt.key}".hashCode())
            }
        }
        settings.appointmentAlerts = alerts
        register(context, settings, alerts)
        Log.d(TAG, "sync: ${settings.appointmentCodes.size} alarm(s) for ${appointments.size} appointment(s)")
    }

    private fun register(context: Context, settings: NudgeSettings, alerts: List<AppointmentAlert>) {
        cancelAll(context, settings)
        val now = System.currentTimeMillis()
        val upcoming = alerts.filter { it.at > now }
        upcoming.forEach { setAlarm(context, it) }
        settings.appointmentCodes = upcoming.map { it.requestCode }.toSet()
    }

    private fun alertsFor(context: Context, appt: Upcoming): List<Triple<Long, String, String>> {
        val zone = ZoneId.systemDefault()
        if (appt.time == null) {
            val at = appt.date.atTime(MORNING_NOTICE).atZone(zone).toInstant().toEpochMilli()
            return listOf(Triple(at, context.getString(R.string.nudge_appt_today_title),
                context.getString(R.string.nudge_appt_today_text, appt.doctor)))
        }
        val start = appt.date.atTime(appt.time)
        val timeLabel = start.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault()))
        return LEAD_MINUTES.map { lead ->
            val at = start.minusMinutes(lead).atZone(zone).toInstant().toEpochMilli()
            val title = if (lead >= 60) context.getString(R.string.nudge_appt_hour_title)
                else context.getString(R.string.nudge_appt_minutes_title, lead.toInt())
            Triple(at, title, context.getString(R.string.nudge_appt_text, appt.doctor, timeLabel))
        }
    }

    private fun setAlarm(context: Context, alert: AppointmentAlert) {
        val at = alert.at
        val pi = pendingIntent(context, alert.requestCode, Intent(context, NudgeReceiver::class.java)
            .setAction(ACTION_APPOINTMENT)
            .putExtra(EXTRA_TITLE, alert.title)
            .putExtra(EXTRA_TEXT, alert.text)
            .putExtra(EXTRA_ID, alert.notificationId))
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        try {
            if (MedicineReminderScheduler.canScheduleExactAlarms(context)) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                return
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "exact alarm denied, falling back to inexact", e)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    private fun cancelAll(context: Context, settings: NudgeSettings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        settings.appointmentCodes.forEach { code ->
            alarmManager.cancel(pendingIntent(context, code, Intent(context, NudgeReceiver::class.java).setAction(ACTION_APPOINTMENT)))
        }
        settings.appointmentCodes = emptySet()
    }

    private fun pendingIntent(context: Context, code: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(context, code, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

    fun show(context: Context, intent: Intent) {
        if (!NudgeSettings(context).appointmentEnabled) return
        val title = intent.getStringExtra(EXTRA_TITLE) ?: return
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        HealthNudges.notify(context, intent.getIntExtra(EXTRA_ID, 0), title, text)
    }

    /** "2026-10-16T00:00:00" → date. */
    private fun parseIsoDate(raw: String?): LocalDate? =
        runCatching { LocalDate.parse(raw.orEmpty().substringBefore("T").trim()) }.getOrNull()

    /** "16-10-2026 11:17:29" → date. */
    private fun parseDmyDate(raw: String?): LocalDate? =
        runCatching { LocalDate.parse(raw.orEmpty().substringBefore(" ").trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy")) }.getOrNull()

    /** "10:00:00.0000000" → 10:00; null for a missing or midnight placeholder time. */
    private fun parseSlotTime(raw: String?): LocalTime? {
        val t = raw.orEmpty().trim().substringBefore(".")
        if (t.isEmpty() || t.startsWith("00:00")) return null
        return runCatching { LocalTime.parse(t) }.getOrNull()
    }
}

/** Fires appointment reminders and puts the alarms back after a reboot or time change. */
class NudgeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            AppointmentReminders.ACTION_APPOINTMENT -> AppointmentReminders.show(app, intent)
            WaterQuickAdd.ACTION_ADD_WATER -> WaterQuickAdd.onTap(app, intent.getIntExtra(WaterQuickAdd.EXTRA_ML, 0))
            else -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try { AppointmentReminders.sync(app) } finally { pending.finish() }
                }
            }
        }
    }
}
