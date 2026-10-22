package com.critetiontech.ctvitalio.notification


import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.critetiontech.ctvitalio.consultation.CallNotifier
import com.critetiontech.ctvitalio.model.CallPush
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage


class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        PrefsManager(applicationContext).saveDeviceToken(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Push received: ${describe(message)}")

        // Doctor's video call (needs a data-only push so it also arrives with the app closed)
        when (val call = CallPush.parse(message.data)) {
            is CallPush.Incoming -> return CallNotifier.ring(applicationContext, call.call)
            is CallPush.Ended -> return CallNotifier.cancelled(applicationContext, call.roomName)
            null -> Unit
        }

        // Notification payload, or a data-only message carrying the same fields
        val title = message.notification?.title ?: message.data["title"] ?: "Vitalio"
        val body = message.notification?.body ?: message.data["body"] ?: message.data["message"] ?: return

        LocalNotificationManager(applicationContext).show(title = title, message = body)
    }
}

private const val TAG = "FCM"

/**
 * Everything the server sent, for logcat (filter by tag "FCM"). Only called while the app is
 * open: when it's in the background Firebase shows a notification-type push itself and this
 * service never runs; its data then arrives in MainActivity's intent when the user taps it.
 */
private fun describe(message: RemoteMessage): String {
    val n = message.notification
    return buildString {
        append("id=${message.messageId} from=${message.from} priority=${message.priority}")
        if (n != null) append(" notification={title=${n.title}, body=${n.body}, channel=${n.channelId}, click=${n.clickAction}}")
        append(" data=${message.data}")
    }
}
