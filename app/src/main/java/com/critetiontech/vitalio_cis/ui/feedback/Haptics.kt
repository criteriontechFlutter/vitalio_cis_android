package com.critetiontech.ctvitalio.ui.feedback

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.core.content.edit
import com.critetiontech.ctvitalio.utils.MyApplication

/** What just happened, so every screen gives the same feel for the same kind of event. */
enum class HapticEvent {
    /** Any button, card or row tap. */
    Tap,
    /** Switch, checkbox or chip flipped. */
    Toggle,
    /** Tab, page or option changed. */
    Selection,
    /** A drag crossed its trigger point, e.g. pull-to-refresh "release now". */
    Threshold,
    LongPress,
    /** Something was saved, added or marked done. */
    Success,
    Warning,
    /** A user action failed. */
    Error,
}

/**
 * One place for haptics across the app. Composables use [rememberHaptics]; ViewModels and
 * receivers, which have no View, call [perform] with the app context.
 *
 * Follows the system "touch feedback" setting and the app's own [enabled] switch.
 */
object Haptics {

    private const val PREFS = "app_feedback"
    private const val KEY_ENABLED = "haptics_enabled"

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putBoolean(KEY_ENABLED, enabled) }

    /** Something the user started (save, add, mark taken) finished well. Safe from ViewModels. */
    fun success() = perform(MyApplication.appContext, HapticEvent.Success)

    /** Something the user started failed. Safe from ViewModels. */
    fun error() = perform(MyApplication.appContext, HapticEvent.Error)

    /** Haptic through the view: cheapest path, and the system decides the exact feel. */
    fun perform(view: View, event: HapticEvent) {
        if (!isEnabled(view.context)) return
        view.performHapticFeedback(viewConstant(event))
    }

    /** Haptic without a view, e.g. after a save finishes in a ViewModel. */
    fun perform(context: Context, event: HapticEvent) {
        val app = context.applicationContext
        if (!isEnabled(app) || !systemTouchFeedbackOn(app)) return
        val vibrator = vibrator(app)?.takeIf { it.hasVibrator() } ?: return
        try {
            vibrator.vibrate(effect(event))
        } catch (e: SecurityException) {
            // VIBRATE permission missing on a custom build; haptics are optional.
        }
    }

    private fun viewConstant(event: HapticEvent): Int = when (event) {
        HapticEvent.Tap -> HapticFeedbackConstants.KEYBOARD_TAP
        HapticEvent.Toggle ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.TOGGLE_ON
            else HapticFeedbackConstants.CLOCK_TICK
        HapticEvent.Selection ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.SEGMENT_TICK
            else HapticFeedbackConstants.CLOCK_TICK
        HapticEvent.Threshold ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) HapticFeedbackConstants.GESTURE_THRESHOLD_ACTIVATE
            else HapticFeedbackConstants.CONTEXT_CLICK
        HapticEvent.LongPress -> HapticFeedbackConstants.LONG_PRESS
        HapticEvent.Success ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM
            else HapticFeedbackConstants.CONTEXT_CLICK
        HapticEvent.Warning -> HapticFeedbackConstants.LONG_PRESS
        HapticEvent.Error ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.REJECT
            else HapticFeedbackConstants.LONG_PRESS
    }

    private fun effect(event: HapticEvent): VibrationEffect {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val predefined = when (event) {
                HapticEvent.Tap, HapticEvent.Toggle -> VibrationEffect.EFFECT_CLICK
                HapticEvent.Selection, HapticEvent.Threshold -> VibrationEffect.EFFECT_TICK
                HapticEvent.LongPress, HapticEvent.Warning -> VibrationEffect.EFFECT_HEAVY_CLICK
                HapticEvent.Success, HapticEvent.Error -> VibrationEffect.EFFECT_DOUBLE_CLICK
            }
            if (event != HapticEvent.Error) return VibrationEffect.createPredefined(predefined)
        }
        // Oreo/Pie have no predefined effects; Error also gets a longer pattern on every version.
        return when (event) {
            HapticEvent.Success -> VibrationEffect.createWaveform(longArrayOf(0, 20, 80, 30), -1)
            HapticEvent.Error -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40, 60, 40), -1)
            HapticEvent.LongPress, HapticEvent.Warning -> VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE)
            else -> VibrationEffect.createOneShot(12, VibrationEffect.DEFAULT_AMPLITUDE)
        }
    }

    private fun vibrator(context: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Vibrator::class.java)
        }

    private fun systemTouchFeedbackOn(context: Context): Boolean =
        Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
}

/** `val haptic = rememberHaptics(); haptic(HapticEvent.Success)` */
@Composable
fun rememberHaptics(): (HapticEvent) -> Unit {
    val view = LocalView.current
    return remember(view) { { event -> Haptics.perform(view, event) } }
}
