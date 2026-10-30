package com.critetiontech.ctvitalio.consultation

import android.app.ActivityManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.critetiontech.ctvitalio.MainActivity
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.IncomingCall
import com.critetiontech.ctvitalio.utils.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

/** Rings for an incoming doctor's call: notification with Join / Decline, full screen when locked. */
object CallNotifier {

    private const val TAG = "CallNotifier"
    private const val CHANNEL_RING = "video_call_ring"
    private const val CHANNEL_MISSED = "video_call_missed"

    /** How long the phone rings before the call counts as missed. */
    const val RING_TIMEOUT_MS = 45_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _ended = MutableSharedFlow<String>(extraBufferCapacity = 4)
    /** Rooms the doctor cancelled while they were ringing here; the call screen listens to close itself. */
    val ended: SharedFlow<String> = _ended

    /** Strings in the in-app language; the caller's context may be a service that isn't wrapped. */
    private fun Context.str(id: Int, vararg args: Any): String = LocaleHelper.wrap(this).getString(id, *args)

    private fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(CHANNEL_RING, context.str(R.string.consult_channel_video_calls), NotificationManager.IMPORTANCE_HIGH).apply {
                description = context.str(R.string.consult_channel_video_calls_desc)
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 1000)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            },
            NotificationChannel(CHANNEL_MISSED, context.str(R.string.consult_channel_missed_calls), NotificationManager.IMPORTANCE_DEFAULT),
        ))
    }

    fun ring(context: Context, call: IncomingCall) {
        ensureChannels(context)
        val screen = PendingIntent.getActivity(
            context, call.notificationId, CallActivity.intent(context, call),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val join = PendingIntent.getActivity(
            context, call.notificationId + 1, CallActivity.intent(context, call, answer = true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val decline = PendingIntent.getBroadcast(
            context, call.notificationId,
            call.putInto(Intent(context, CallDeclineReceiver::class.java)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = call.title ?: context.str(R.string.consult_incoming_video_consultation)
        val text = call.message ?: context.str(R.string.consult_doctor_is_calling, call.doctorName)

        val notification = NotificationCompat.Builder(context, CHANNEL_RING)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setColor(0xFF1564ED.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(context.str(R.string.consult_video_call))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            // "Join" rather than CallStyle's fixed "Answer"
            .addAction(R.drawable.ic_call_decline, context.str(R.string.consult_decline), decline)
            .addAction(R.drawable.ic_call_join, context.str(R.string.consult_join), join)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(screen)
            .setFullScreenIntent(screen, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setTimeoutAfter(RING_TIMEOUT_MS)
            .build()
        // Ring until answered, declined or timed out
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        context.getSystemService(NotificationManager::class.java).notify(call.notificationId, notification)

        // In the app already: show the call screen straight away instead of only a heads-up
        if (isAppInForeground()) {
            runCatching { context.startActivity(CallActivity.intent(context, call)) }
        }
    }

    fun stopRinging(context: Context, call: IncomingCall) {
        context.getSystemService(NotificationManager::class.java).cancel(call.notificationId)
    }

    /** The doctor hung up before the patient answered. */
    fun cancelled(context: Context, roomName: String) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationIdFor(roomName))
        _ended.tryEmit(roomName)
    }

    fun showMissed(context: Context, call: IncomingCall) {
        ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, call.notificationId,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_MISSED)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(context.str(R.string.consult_missed_video_call))
            .setContentText(context.str(R.string.consult_doctor_called_you, call.doctorName))
            .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(call.notificationId, notification)
    }

    /** Tells the server the patient won't take the call; fire and forget. */
    fun decline(context: Context, call: IncomingCall, reason: String) {
        stopRinging(context, call)
        val userId = patientUserId(call)
        scope.launch {
            (AppDependencies.declineCall()(call.roomName, userId, reason) as? DomainResult.Error)
                ?.let { Log.w(TAG, "decline not sent: ${it.exception.message}") }
        }
    }

    /** The id the doctor's side called this patient by, else the logged-in patient's. */
    fun patientUserId(call: IncomingCall): String =
        call.patientUserId ?: AppDependencies.prefs.getPatient()?.pid?.toString().orEmpty()

    private fun isAppInForeground(): Boolean {
        val state = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(state)
        return state.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }
}

/** "Decline" on the ringing notification. */
class CallDeclineReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val call = intent.incomingCall() ?: return
        CallNotifier.decline(context, call, reason = "declined")
        CallNotifier.cancelled(context, call.roomName)
    }
}
