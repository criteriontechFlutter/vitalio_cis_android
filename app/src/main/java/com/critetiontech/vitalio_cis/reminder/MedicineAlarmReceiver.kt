package com.critetiontech.ctvitalio.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Handles alarms firing, the notification's actions, and rescheduling after reboot. */
class MedicineAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        when (intent.action) {
            ACTION_FIRE -> ReminderAlarm.from(intent)?.let { onFire(app, it) }

            ACTION_SILENCE -> ReminderAlarm.from(intent)?.let { MedicineAlarmNotifier.quiet(app, it) }

            ACTION_SNOOZE -> ReminderAlarm.from(intent)?.let {
                MedicineAlarmNotifier.cancel(app, it)
                MedicineReminderScheduler.snooze(app, it)
                MedicineIntakeEvents.alarmHandled(it.requestCode)
                Toast.makeText(app, "Snoozed for ${ReminderSettings(app).snoozeMinutes} min", Toast.LENGTH_SHORT).show()
            }

            ACTION_DISMISS -> ReminderAlarm.from(intent)?.let { MedicineAlarmNotifier.cancel(app, it) }

            ACTION_TAKEN -> ReminderAlarm.from(intent)?.let { alarm ->
                MedicineAlarmNotifier.cancel(app, alarm)
                MedicineIntakeEvents.alarmHandled(alarm.requestCode)
                val pending = goAsync()
                scope.launch {
                    try {
                        val outcomes = alarm.doses.map { IntakeRecorder.markTaken(app, it) }
                        val message = when {
                            outcomes.all { it == IntakeRecorder.Outcome.Recorded } ->
                                if (alarm.doses.size == 1) "${alarm.doses[0].medicineName} marked as taken"
                                else "${alarm.doses.size} medicines marked as taken"
                            else -> "Saved. It will sync when you're back online."
                        }
                        withContext(Dispatchers.Main) { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
                        MedicineReminderScheduler.reschedule(app)
                    } finally {
                        pending.finish()
                    }
                }
            }

            ACTION_RESYNC,
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                // Put back what we know right away, then refresh from the server.
                MedicineReminderScheduler.reschedule(app)
                val pending = goAsync()
                scope.launch {
                    try { MedicineReminderScheduler.sync(app) } finally { pending.finish() }
                }
            }
        }
    }

    private fun onFire(context: Context, fired: ReminderAlarm) {
        if (!ReminderSettings(context).claimFire(fired.alertAt)) return
        val alarm = if (fired.isTest) fired else liveDoses(context, fired) ?: return

        MedicineAlarmNotifier.show(context, alarm, ringing = true)
        scheduleSilence(context, alarm)

        // Locked or screen off: the full-screen intent opens the alarm by itself.
        // In use: a full-screen intent only peeks, so open the alarm screen directly.
        if (AlarmPopupLauncher.isInUse(context)) {
            val pending = goAsync()
            AlarmPopupLauncher.launch(context, MedicineAlarmNotifier.activityIntent(context, alarm)) {
                pending.finish()
            }
        }
    }

    /**
     * Drops doses that were taken, muted or removed since the alarm was set (e.g. logged
     * from the dashboard while snoozed). Returns null when nothing is left to remind about.
     */
    private fun liveDoses(context: Context, alarm: ReminderAlarm): ReminderAlarm? {
        val settings = ReminderSettings(context)
        if (!settings.enabled) return null
        val live = settings.doses.map { it.key }.toSet()
        val doses = alarm.doses.filter { it.key in live && !settings.isMuted(it.medicineName) }
        return if (doses.isEmpty()) null else alarm.copy(doses = doses)
    }

    /** Stops the ringing after a while but leaves the reminder in the shade. */
    private fun scheduleSilence(context: Context, alarm: ReminderAlarm) {
        val silence = PendingIntent.getBroadcast(
            context, alarm.requestCode,
            alarm.putInto(Intent(context, MedicineAlarmReceiver::class.java).setAction(ACTION_SILENCE)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        context.getSystemService(AlarmManager::class.java).setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + MedicineAlarmNotifier.RING_DURATION_MS,
            silence,
        )
    }

    companion object {
        const val ACTION_FIRE = "com.critetiontech.ctvitalio.reminder.FIRE"
        const val ACTION_TAKEN = "com.critetiontech.ctvitalio.reminder.TAKEN"
        const val ACTION_SNOOZE = "com.critetiontech.ctvitalio.reminder.SNOOZE"
        const val ACTION_DISMISS = "com.critetiontech.ctvitalio.reminder.DISMISS"
        const val ACTION_SILENCE = "com.critetiontech.ctvitalio.reminder.SILENCE"
        const val ACTION_RESYNC = "com.critetiontech.ctvitalio.reminder.RESYNC"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
