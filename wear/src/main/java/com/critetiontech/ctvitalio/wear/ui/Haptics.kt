package com.critetiontech.ctvitalio.wear.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

/** Short tick — data saved. */
val HAPTIC_SUCCESS = longArrayOf(0, 60)

/** Three strong pulses — critical vital alert. */
val HAPTIC_CRITICAL = longArrayOf(0, 300, 150, 300, 150, 300)

/** Double tap — nothing saved / error. */
val HAPTIC_FAILURE = longArrayOf(0, 80, 100, 80)

fun Context.vibrate(pattern: LongArray) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        getSystemService(Vibrator::class.java)
    }
    if (vibrator?.hasVibrator() == true) {
        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
    }
}