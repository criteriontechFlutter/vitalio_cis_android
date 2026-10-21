package com.critetiontech.ctvitalio.notification


import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        PrefsManager(applicationContext).saveDeviceToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: "Title"
        val body = message.notification?.body ?: "Message"

        // 👇 Local notification call here
        LocalNotificationManager(applicationContext).show(
            title = title,
            message = body
        )
    }
}