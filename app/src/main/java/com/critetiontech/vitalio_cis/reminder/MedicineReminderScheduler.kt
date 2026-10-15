package com.critetiontech.ctvitalio.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.critetiontech.ctvitalio.MainActivity
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.MedicinePeriod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * Turns the server's medicine intake schedule into alarms on the device.
 *
 * Doses come from the same daily-intake API the dashboard uses (today + tomorrow), so each
 * alarm carries the dose ids and scheduled times needed to mark them taken.
 */
object MedicineReminderScheduler {

    private const val TAG = "MedicineReminder"
    private const val RESYNC_REQUEST_CODE = 0x5EED
    private const val BACKUP_CODE_MASK = 0x40000000
    private const val INEXACT_WINDOW_MS = 10 * 60_000L

    /** Doses more than this far past their time are stale and never ring. */
    private const val STALE_AFTER_MS = 6 * 60 * 60_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val syncLock = Mutex()

    /** Fire-and-forget [sync], safe to call from ViewModels and receivers alike. */
    fun requestSync(context: Context) {
        val app = context.applicationContext
        scope.launch { sync(app) }
    }

    /** Fetches today's and tomorrow's doses and reschedules every alarm. */
    suspend fun sync(context: Context): Boolean = syncLock.withLock {
        val settings = ReminderSettings(context)
        val patient = AppDependencies.prefs.getPatient()
        if (patient == null) {
            settings.doses = emptyList()
            settings.takenLocally = emptySet()
            reschedule(context)
            return false
        }

        val today = LocalDate.now()
        val results = listOf(today, today.plusDays(1)).map { date ->
            AppDependencies.fetchMedicineIntake()(patient.pid, date.toString(), patient.clientId)
        }
        if (results.none { it is DomainResult.Success }) {
            Log.w(TAG, "sync: could not fetch schedule, keeping existing alarms")
            scheduleDailyResync(context)
            return false
        }

        val items = results
            .filterIsInstance<DomainResult.Success<List<MedicinePeriod>>>()
            .flatMap { r -> r.data.flatMap { it.medicineData.orEmpty() } }
            .filter { !it.medicineName.isNullOrBlank() }

        // Once the server reports a locally-logged dose as taken, stop tracking it.
        val takenOnServer = items.filter { it.isActuallyTaken() }.map { ReminderDose.from(it).key }.toSet()
        settings.takenLocally = settings.takenLocally - takenOnServer

        val now = System.currentTimeMillis()
        val pendingLocally = settings.takenLocally
        settings.doses = items
            .filter { !it.isActuallyTaken() && !it.isSkipped }
            .map { ReminderDose.from(it) }
            .filter { it.key !in pendingLocally }
            .filter { (it.scheduledMillis() ?: 0L) > now - STALE_AFTER_MS }
            .distinctBy { it.key }
            .sortedBy { it.scheduledDateTime }

        reschedule(context)
        true
    }

    /** The alarms the cached doses produce with the current settings, soonest first. */
    fun upcomingAlarms(context: Context): List<ReminderAlarm> {
        val settings = ReminderSettings(context)
        if (!settings.enabled) return emptyList()
        val now = System.currentTimeMillis()
        return settings.doses
            .filterNot { settings.isMuted(it.medicineName) }
            .mapNotNull { dose -> alertTimeMillis(dose, settings.offsetMinutes)?.let { it to dose } }
            .filter { (at, _) -> at > now }
            .groupBy({ it.first }, { it.second })
            .map { (at, doses) -> ReminderAlarm(at, doses) }
            .sortedBy { it.alertAt }
    }

    /** Re-registers alarms from the cached dose list, e.g. after a settings change or reboot. */
    fun reschedule(context: Context) {
        val settings = ReminderSettings(context)
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        settings.scheduledCodes.forEach { code ->
            alarmManager.cancel(firePendingIntent(context, code, null))
            alarmManager.cancel(firePendingIntent(context, code xor BACKUP_CODE_MASK, null))
        }

        val alarms = upcomingAlarms(context)
        alarms.forEach { setAlarm(context, it) }
        settings.scheduledCodes = alarms.map { it.requestCode }.toSet()
        scheduleDailyResync(context)
        Log.d(TAG, "reschedule: ${alarms.size} alarm(s) for ${alarms.sumOf { it.doses.size }} dose(s)")
    }

    /** When the alarm for [dose] rings, given the user's "remind me before" offset. */
    fun alertTimeMillis(dose: ReminderDose, offsetMinutes: Int): Long? =
        dose.scheduledMillis()?.minus(offsetMinutes * 60_000L)

    fun snooze(context: Context, alarm: ReminderAlarm) {
        val at = System.currentTimeMillis() + ReminderSettings(context).snoozeMinutes * 60_000L
        setAlarm(context, alarm.copy(alertAt = at))
    }

    /** Rings a sample alarm a few seconds from now so the user can hear and see it. */
    fun scheduleTestAlarm(context: Context, delaySeconds: Int = 5) {
        val at = LocalDateTime.now().plusSeconds(delaySeconds.toLong()).withNano(0)
        val dose = ReminderDose(
            intakeId = ReminderDose.TEST_INTAKE_ID,
            medicineName = "Test reminder",
            dose = "This is how your medicine alarm will look",
            instructions = "",
            scheduledDateTime = at.toString(),
        )
        setAlarm(context, ReminderAlarm(System.currentTimeMillis() + delaySeconds * 1000L, listOf(dose)))
    }

    /** Android 12+ needs the user's OK for exact alarms; earlier versions always allow them. */
    fun canScheduleExactAlarms(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun setAlarm(context: Context, alarm: ReminderAlarm) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val operation = firePendingIntent(context, alarm.requestCode, alarm)
        if (canScheduleExactAlarms(context)) {
            // Alarm-clock alarms ring on time even in Doze and on aggressive OEM builds.
            val showIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(alarm.alertAt, showIntent), operation)
                return
            } catch (e: SecurityException) {
                // Permission was revoked between the check and the call.
                Log.w(TAG, "setAlarmClock denied, falling back to inexact alarm", e)
            }
        }
        // Without exact-alarm access Android batches alarms: this one still runs in Doze but
        // may come up to an hour late, so a second one with a 10-minute window backs it up.
        // Whichever rings first wins; the receiver ignores the other.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.alertAt, operation)
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP, alarm.alertAt, INEXACT_WINDOW_MS,
            firePendingIntent(context, alarm.requestCode xor BACKUP_CODE_MASK, alarm),
        )
    }

    /** Refreshes the schedule shortly after midnight so tomorrow's doses get alarms too. */
    private fun scheduleDailyResync(context: Context) {
        val triggerAt = LocalDate.now().plusDays(1).atTime(LocalTime.of(0, 5))
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
            .setAction(MedicineAlarmReceiver.ACTION_RESYNC)
        val pi = PendingIntent.getBroadcast(
            context, RESYNC_REQUEST_CODE, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java)
            .setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
    }

    private fun firePendingIntent(context: Context, requestCode: Int, alarm: ReminderAlarm?): PendingIntent {
        val intent = Intent(context, MedicineAlarmReceiver::class.java)
            .setAction(MedicineAlarmReceiver.ACTION_FIRE)
        alarm?.putInto(intent)
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
