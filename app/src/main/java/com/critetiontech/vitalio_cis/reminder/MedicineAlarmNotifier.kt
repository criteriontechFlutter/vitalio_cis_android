package com.critetiontech.ctvitalio.reminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.critetiontech.ctvitalio.R
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Notification side of the medicine alarm: channels, the ringing alert and its actions. */
object MedicineAlarmNotifier {

    // Channel sound can't change after creation, so each sound mode gets its own channel.
    private const val CHANNEL_SOUND = "medicine_alarm_sound"
    private const val CHANNEL_VIBRATE = "medicine_alarm_vibrate"
    private const val CHANNEL_QUIET = "medicine_alarm_quiet"
    const val CHANNEL_STATUS = "medicine_status"
    private val RETIRED_CHANNELS = listOf("medicine_alarm_silent")

    /** How long an alarm keeps ringing before it settles into a quiet reminder. */
    const val RING_DURATION_MS = 2 * 60_000L

    val VIBRATION_PATTERN = longArrayOf(0, 700, 500, 700, 500, 700)

    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    fun formatTime(dose: ReminderDose): String = dose.scheduledAt()?.format(timeFormat).orEmpty()

    fun alarmSoundUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

    val alarmAudioAttributes: AudioAttributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_ALARM)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        RETIRED_CHANNELS.forEach { manager.deleteNotificationChannel(it) }
        manager.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_SOUND, "Medicine alarms", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Rings with alarm sound when it's time to take a medicine"
                setSound(alarmSoundUri(), alarmAudioAttributes)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
            NotificationChannel(CHANNEL_VIBRATE, "Medicine alarms (vibrate only)", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Medicine alarms when alarm sound is turned off"
                setSound(null, null)
                enableVibration(true)
                vibrationPattern = VIBRATION_PATTERN
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            },
            NotificationChannel(CHANNEL_QUIET, "Pending medicine reminders", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Stays in the shade after an alarm until you respond"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(CHANNEL_STATUS, "Medicine log status", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you if a dose couldn't be logged"
            },
        ))
    }

    /** Intent for the full-screen alarm screen. */
    fun activityIntent(context: Context, alarm: ReminderAlarm): Intent =
        alarm.putInto(Intent(context, MedicineAlarmActivity::class.java))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)

    /**
     * Posts the alarm. Ringing: alarm sound on loop and the full-screen intent.
     * Quiet: no sound, just the reminder and its actions left in the shade.
     */
    fun show(context: Context, alarm: ReminderAlarm, ringing: Boolean) {
        ensureChannels(context)
        val settings = ReminderSettings(context)
        val channel = when {
            !ringing -> CHANNEL_QUIET
            settings.soundEnabled -> CHANNEL_SOUND
            else -> CHANNEL_VIBRATE
        }

        val fullScreen = PendingIntent.getActivity(
            context, alarm.requestCode, activityIntent(context, alarm),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val single = alarm.doses.singleOrNull()
        val title = single?.let { "Time for ${it.medicineName}" }
            ?: "Time for ${alarm.doses.size} medicines"
        val text = single?.let { listOf(it.dose, formatTime(it)).filter(String::isNotBlank).joinToString(" · ") }
            ?: alarm.doses.joinToString(", ") { it.medicineName }
        val bigText = alarm.doses.joinToString("\n") { d ->
            listOf(d.medicineName, d.dose, formatTime(d)).filter(String::isNotBlank).joinToString(" · ")
        }

        val builder = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(if (ringing) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(fullScreen)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(!ringing)
            .setDeleteIntent(actionIntent(context, alarm, MedicineAlarmReceiver.ACTION_DISMISS))
            .addAction(0, if (single != null) "Mark as taken" else "Mark all taken",
                actionIntent(context, alarm, MedicineAlarmReceiver.ACTION_TAKEN))
            .addAction(0, "Snooze ${settings.snoozeMinutes} min",
                actionIntent(context, alarm, MedicineAlarmReceiver.ACTION_SNOOZE))

        if (ringing) {
            builder.setFullScreenIntent(fullScreen, true)
        } else {
            builder.setSilent(true)
        }

        val notification = builder.build()
        if (ringing && settings.soundEnabled) {
            // Loop the alarm sound until the user responds or it's silenced.
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
        }
        val manager = context.getSystemService(NotificationManager::class.java)
        // Cancelling first is what reliably stops a looping sound when switching to quiet.
        if (!ringing) manager.cancel(alarm.requestCode)
        manager.notify(alarm.requestCode, notification)
    }

    fun cancel(context: Context, alarm: ReminderAlarm) {
        context.getSystemService(NotificationManager::class.java).cancel(alarm.requestCode)
    }

    fun isShowing(context: Context, alarm: ReminderAlarm): Boolean =
        context.getSystemService(NotificationManager::class.java)
            .activeNotifications.any { it.id == alarm.requestCode }

    /** Swaps a ringing alarm for a quiet one that still offers the actions. */
    fun quiet(context: Context, alarm: ReminderAlarm) {
        if (isShowing(context, alarm)) show(context, alarm, ringing = false)
    }

    /** Android 14+ lets users revoke full-screen alarms; earlier versions always allow them. */
    fun canUseFullScreen(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            context.getSystemService(NotificationManager::class.java).canUseFullScreenIntent()

    fun notificationsEnabled(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun actionIntent(context: Context, alarm: ReminderAlarm, action: String): PendingIntent =
        PendingIntent.getBroadcast(
            context, alarm.requestCode,
            alarm.putInto(Intent(context, MedicineAlarmReceiver::class.java).setAction(action)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
