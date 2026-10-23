package com.critetiontech.ctvitalio.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Every runtime permission Vitalio asks for, with the copy shown before the system dialog and on
 * the Permissions & Privacy screen. Copy is written for the user: what it unlocks, where it's
 * used, and what we never do with it.
 */
enum class AppPermission(
    val title: String,
    val icon: ImageVector,
    /** One line shown under the title on the permissions screen. */
    val summary: String,
    /** Features that use it, shown as "Used for" bullets. */
    val usedFor: List<String>,
    /** What we promise not to do. */
    val privacy: String,
) {
    Notifications(
        title = "Notifications",
        icon = Icons.Rounded.Notifications,
        summary = "Medicine alarms, appointment updates and health alerts",
        usedFor = listOf(
            "Medicine reminders that ring at your dose time",
            "Appointment and prescription updates from your care team",
            "Alerts when a vital reading needs attention",
        ),
        privacy = "We never send promotional spam. You can mute any type later in system settings.",
    ),
    Camera(
        title = "Camera",
        icon = Icons.Rounded.CameraAlt,
        summary = "Photograph prescriptions, lab reports and your profile photo",
        usedFor = listOf(
            "Scan a prescription so we can read your medicines",
            "Capture a lab report to add it to your records",
            "Take a profile photo",
        ),
        privacy = "The camera only opens when you tap a capture button. Photos are uploaded only when you choose to save them.",
    ),
    Microphone(
        title = "Microphone",
        icon = Icons.Rounded.Mic,
        summary = "Voice assistant and breathing exercises",
        usedFor = listOf(
            "Speak to log vitals or open a screen with the voice assistant",
            "Breathing therapy listens to your breath to guide each round",
        ),
        privacy = "We listen only while the mic button or exercise is active. Audio is never recorded or stored.",
    ),
    NearbyDevices(
        title = "Nearby devices",
        icon = Icons.Rounded.Bluetooth,
        summary = "Connect BP monitors, oximeters and weighing scales over Bluetooth",
        usedFor = listOf(
            "Find and pair your Omron or Yonker device",
            "Transfer new readings from the device to your health record",
        ),
        privacy = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            "Used only to talk to your health devices. We never track your location."
        else
            "Older Android versions call this \"Location\" because Bluetooth scanning needs it. We never read or store your location.",
    ),
    Storage(
        title = "Storage",
        icon = Icons.Rounded.Folder,
        summary = "Save downloaded reports to your Downloads folder",
        usedFor = listOf("Download lab reports and prescriptions to your phone"),
        privacy = "We only write the files you download. We don't read your other files.",
    );

    /** System permission names for the running Android version; empty when none is needed. */
    val manifestPermissions: Array<String>
        get() = when (this) {
            Notifications ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS)
                else emptyArray()
            Camera -> arrayOf(Manifest.permission.CAMERA)
            Microphone -> arrayOf(Manifest.permission.RECORD_AUDIO)
            NearbyDevices ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                    arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            // Android 10+ saves to public Downloads without a permission
            Storage ->
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                else emptyArray()
        }

    /** Finishes "Vitalio needs … access" when asked from the permissions screen rather than a feature. */
    val defaultReason: String
        get() = when (this) {
            Notifications -> "so your medicine alarms can ring and your care team's updates reach you"
            Camera -> "to photograph prescriptions, lab reports and your profile photo"
            Microphone -> "for the voice assistant and breathing exercises"
            NearbyDevices -> "to connect your BP monitor, oximeter or scale over Bluetooth"
            Storage -> "to save downloaded reports in your Downloads folder"
        }

    /** False when this Android version grants the feature without asking. */
    val isRequired: Boolean get() = manifestPermissions.isNotEmpty()

    fun isGranted(context: Context): Boolean = manifestPermissions.all {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Android stops showing its dialog after the user declines twice. We can tell that apart
     * from "never asked" only by remembering that we asked before.
     */
    fun isBlocked(context: Context): Boolean {
        if (isGranted(context) || !wasAsked(context)) return false
        val activity = context.findActivity() ?: return false
        return manifestPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    fun wasAsked(context: Context) = prefs(context).getBoolean(name, false)

    fun markAsked(context: Context) = prefs(context).edit().putBoolean(name, true).apply()

    companion object {
        private fun prefs(context: Context) = context.getSharedPreferences("app_permissions", Context.MODE_PRIVATE)

        /** Vitalio's page in system settings, where blocked permissions are turned back on. */
        fun appSettingsIntent(context: Context): Intent =
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
