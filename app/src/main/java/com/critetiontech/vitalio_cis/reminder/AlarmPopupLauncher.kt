package com.critetiontech.ctvitalio.reminder

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager

/**
 * Opens the full-screen alarm on top of whatever the user is doing.
 *
 * How Android lets a background app do that differs by version:
 * - 8.x–9 (API 26–28): starting an activity from the background is allowed.
 * - 10+ (API 29+): blocked, except for apps the user lets "display over other apps".
 * - 15+ (API 35+): that exemption also needs one of the app's overlay windows to be
 *   visible, so a 1px invisible overlay is shown for the moment of the launch.
 *
 * When the screen is off or locked, the notification's full-screen intent opens the
 * alarm instead, so this is only used while the phone is unlocked and in use.
 */
object AlarmPopupLauncher {

    private const val TAG = "AlarmPopupLauncher"
    private const val OVERLAY_SETTLE_MS = 150L
    private const val OVERLAY_HOLD_MS = 2_000L

    fun needsOverlayPermission(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun canPopUpOverApps(context: Context): Boolean =
        !needsOverlayPermission() || Settings.canDrawOverlays(context)

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))

    /** True when the phone is awake and unlocked, i.e. a full-screen intent would only peek. */
    fun isInUse(context: Context): Boolean {
        val interactive = context.getSystemService(PowerManager::class.java).isInteractive
        val locked = context.getSystemService(KeyguardManager::class.java).isKeyguardLocked
        return interactive && !locked
    }

    /**
     * Starts the alarm screen. Returns false if Android won't allow it, in which case the
     * caller should rely on the notification. [onDone] runs once the launch was attempted.
     */
    fun launch(context: Context, intent: Intent, onDone: () -> Unit = {}): Boolean {
        val app = context.applicationContext
        if (!needsOverlayPermission()) {
            app.startActivity(intent)
            onDone()
            return true
        }
        if (!Settings.canDrawOverlays(app)) {
            onDone()
            return false
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val started = tryStart(app, intent)
            onDone()
            return started
        }

        val windowManager = app.getSystemService(WindowManager::class.java)
        val anchor = View(app)
        val params = WindowManager.LayoutParams(
            1, 1,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.START }

        try {
            windowManager.addView(anchor, params)
        } catch (e: Exception) {
            Log.w(TAG, "overlay anchor failed, trying direct start", e)
            val started = tryStart(app, intent)
            onDone()
            return started
        }

        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed({
            tryStart(app, intent)
            handler.postDelayed({
                try { windowManager.removeView(anchor) } catch (_: Exception) {}
                onDone()
            }, OVERLAY_HOLD_MS)
        }, OVERLAY_SETTLE_MS)
        return true
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: Exception) {
        Log.w(TAG, "could not start alarm screen", e)
        false
    }
}
