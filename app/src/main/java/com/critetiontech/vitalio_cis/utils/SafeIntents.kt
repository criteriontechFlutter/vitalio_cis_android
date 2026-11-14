package com.critetiontech.ctvitalio.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log

/**
 * Starts [intent], falling back to this app's details page in system settings when the phone has no
 * screen for it. Some phones (Android Go, several Xiaomi/Oppo/Vivo builds) lack the exact-alarm,
 * full-screen-intent or battery settings pages, and a bare startActivity crashes there.
 * Returns false only when neither could be opened.
 */
fun Context.startActivitySafely(intent: Intent): Boolean {
    try {
        startActivity(intent)
        return true
    } catch (e: ActivityNotFoundException) {
        Log.w("SafeIntents", "No activity for ${intent.action}, falling back to app settings")
    } catch (e: SecurityException) {
        Log.w("SafeIntents", "Not allowed to open ${intent.action}, falling back to app settings")
    }
    return try {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))
                .addFlags(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        true
    } catch (e: Exception) {
        Log.w("SafeIntents", "App settings unavailable too", e)
        false
    }
}
