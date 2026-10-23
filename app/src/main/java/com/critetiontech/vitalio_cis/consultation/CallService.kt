package com.critetiontech.ctvitalio.consultation

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.IncomingCall

/**
 * Keeps a video consultation alive while the call screen isn't in front (PiP, home, other apps):
 * Android cuts camera and microphone to background apps unless a camera/microphone foreground
 * service is running. Its ongoing-call notification leads back to the call and can hang up.
 */
class CallService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val call = intent.incomingCall() ?: run { stopSelf(); return START_NOT_STICKY }
        val connectedAt = intent?.getLongExtra(EXTRA_CONNECTED_AT, 0L) ?: 0L
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, notification(call, connectedAt), serviceTypes())
        } catch (e: Exception) {
            // e.g. started while the app was already in the background; the call still works in front
            Log.w(TAG, "could not start call foreground service", e)
            stopSelf()
        }
        // Not restarted by the system: a call can't be resumed without the screen that owns it
        return START_NOT_STICKY
    }

    /** Only claim what the patient allowed; Android 14+ rejects a type without its permission. */
    private fun serviceTypes(): Int {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return 0
        var types = 0
        if (granted(Manifest.permission.CAMERA)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA
        if (granted(Manifest.permission.RECORD_AUDIO)) types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        return types
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun notification(call: IncomingCall, connectedAt: Long): android.app.Notification {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Ongoing video call", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Shown while you're in a video consultation"
                setSound(null, null)
            }
        )
        val open = PendingIntent.getActivity(
            this, 0, CallActivity.intent(this, call),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hangUp = PendingIntent.getActivity(
            this, 1, CallActivity.intent(this, call).putExtra(CallActivity.EXTRA_HANG_UP, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val doctor = Person.Builder().setName(call.doctorName).setImportant(true).build()
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(call.doctorName)
            .setContentText("Video consultation in progress")
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(doctor, hangUp).setIsVideo(true))
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setContentIntent(open)
            .setOngoing(true)
            .apply {
                // Call timer in the shade, counting from when the call connected
                if (connectedAt > 0) setWhen(System.currentTimeMillis() - (android.os.SystemClock.elapsedRealtime() - connectedAt)).setUsesChronometer(true)
            }
            .build()
    }

    companion object {
        private const val TAG = "CallService"
        private const val CHANNEL = "video_call_ongoing"
        private const val NOTIFICATION_ID = 0x7C0FF
        private const val EXTRA_CONNECTED_AT = "call_connected_at"

        /** [connectedAt] is [android.os.SystemClock.elapsedRealtime] at connect, for the shade's timer. */
        fun start(context: Context, call: IncomingCall, connectedAt: Long) {
            val intent = call.putInto(Intent(context, CallService::class.java)).putExtra(EXTRA_CONNECTED_AT, connectedAt)
            runCatching { ContextCompat.startForegroundService(context, intent) }
                .onFailure { Log.w(TAG, "could not start call service", it) }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CallService::class.java))
        }
    }
}
