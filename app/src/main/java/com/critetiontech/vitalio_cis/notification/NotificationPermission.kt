package com.critetiontech.ctvitalio.notification

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest
import kotlinx.coroutines.delay

/**
 * Asks for notifications once, on the dashboard after sign-in, with the explanation sheet
 * first. Later the user turns them on from Medicine Reminders or Permissions & Privacy.
 */
@Composable
fun RequestNotificationPermission() {
    val context = LocalContext.current
    val request = rememberPermissionRequest(
        AppPermission.Notifications,
        "so your medicine alarms can ring and your care team's updates reach you",
    ) {}

    LaunchedEffect(Unit) {
        val permission = AppPermission.Notifications
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (!permission.isRequired || permission.isGranted(context) || prefs.getBoolean(KEY_PROMPTED, false)) return@LaunchedEffect
        delay(1_200) // let the dashboard settle so the sheet doesn't land on a blank screen
        // Once only, whatever the answer, so we don't nag on every launch
        prefs.edit().putBoolean(KEY_PROMPTED, true).apply()
        request.launch()
    }
}

private const val PREFS = "app_permissions"
private const val KEY_PROMPTED = "notifications_prompted"
