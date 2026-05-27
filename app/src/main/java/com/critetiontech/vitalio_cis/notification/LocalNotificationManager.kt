package com.critetiontech.vitalio_cis.notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews

import androidx.core.app.NotificationCompat

import com.critetiontech.vitalio_cis.MainActivity
import com.critetiontech.vitalio_cis.R

class LocalNotificationManager(

    private val context: Context

) {

    // ================= CHANNEL ID =================

    private val channelId = "vitalio_channel"

    // ================= SHOW NOTIFICATION =================

    fun show(

        title: String,

        message: String

    ) {

        // ================= NOTIFICATION MANAGER =================

        val manager =
            context.getSystemService(
                Context.NOTIFICATION_SERVICE
            ) as NotificationManager

        // ================= CREATE CHANNEL =================

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(

                channelId,

                "Vitalio Notification",

                NotificationManager.IMPORTANCE_HIGH

            )

            channel.description =
                "Medicine Reminder Notification"

            manager.createNotificationChannel(channel)
        }

        // ================= CUSTOM LAYOUT =================

        val remoteViews = RemoteViews(

            context.packageName,

            R.layout.custom_notification

        )

        // ================= SET TEXT =================

        remoteViews.setTextViewText(

            R.id.txtTitle,

            title

        )

        remoteViews.setTextViewText(

            R.id.txtMessage,

            message

        )



        remoteViews.setTextViewText(

            R.id.txtTime,

            "08:00 AM"

        )

        // ================= BUTTON CLICK =================

        val intent = Intent(

            context,

            MainActivity::class.java

        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        val pendingIntent = PendingIntent.getActivity(

            context,

            0,

            intent,

            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE

        )

        // BUTTON CLICK
        remoteViews.setOnClickPendingIntent(

            R.id.btnIntake,

            pendingIntent

        )

        // ================= NOTIFICATION =================

        val notification = NotificationCompat.Builder(

            context,

            channelId

        )

            // STATUS BAR ICON
            .setSmallIcon(R.mipmap.ic_launcher)

            // CUSTOM SMALL VIEW
            .setCustomContentView(remoteViews)

            // CUSTOM EXPANDED VIEW
            .setCustomBigContentView(remoteViews)

            // HIGH PRIORITY
            .setPriority(
                NotificationCompat.PRIORITY_MAX
            )

            // LOCK SCREEN SHOW
            .setVisibility(
                NotificationCompat.VISIBILITY_PUBLIC
            )

            // AUTO REMOVE
            .setAutoCancel(true)

            // EXPANDABLE
            .setStyle(
                NotificationCompat.BigTextStyle()
            )

            .build()

        // ================= SHOW =================

        manager.notify(

            System.currentTimeMillis().toInt(),

            notification

        )
    }
}