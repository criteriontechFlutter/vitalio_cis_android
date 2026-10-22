package com.critetiontech.ctvitalio.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.critetiontech.ctvitalio.MainActivity
import com.critetiontech.ctvitalio.R

/**
 * Shows push messages from the server as a standard notification: title, text, tap to open
 * the app. Medicine alarms have their own UI in [com.critetiontech.ctvitalio.reminder.MedicineAlarmNotifier].
 */
class LocalNotificationManager(private val context: Context) {

    fun show(title: String, message: String) {
        val manager = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "Updates from your care team", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Appointments, prescriptions, reports and other messages"
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .build()

        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    companion object {
        /** Also set as Firebase's default channel, so pushes shown while the app is closed land here too. */
        const val CHANNEL_ID = "vitalio_channel"
    }
}
