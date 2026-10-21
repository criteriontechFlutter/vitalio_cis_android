package com.critetiontech.ctvitalio.reminder

import android.content.Context
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Plays the alarm sound on loop and vibrates while the alarm screen is up, stopping on its
 * own once the ring window ends. Uses the alarm audio stream, so it follows alarm volume.
 */
class AlarmRinger(private val context: Context) {

    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    fun start(durationMs: Long, sound: Boolean) {
        if (durationMs <= 0) return
        stop()
        if (sound) startSound()
        startVibration()
        handler.postDelayed({ stop() }, durationMs)
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        player?.runCatching { if (isPlaying) stop(); release() }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun startSound() {
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(MedicineAlarmNotifier.alarmAudioAttributes)
                setDataSource(context, MedicineAlarmNotifier.alarmSoundUri())
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e("AlarmRinger", "could not play alarm sound", e)
            player?.release()
            player = null
        }
    }

    private fun startVibration() {
        val v = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }
        if (v?.hasVibrator() != true) return
        vibrator = v
        // Repeat from index 0 until cancelled.
        v.vibrate(VibrationEffect.createWaveform(MedicineAlarmNotifier.VIBRATION_PATTERN, 0))
    }
}
