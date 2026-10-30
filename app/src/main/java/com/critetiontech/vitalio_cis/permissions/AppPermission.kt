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
import androidx.annotation.StringRes
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication

/**
 * Every runtime permission Vitalio asks for, with the copy shown before the system dialog and on
 * the Permissions & Privacy screen. Copy is written for the user: what it unlocks, where it's
 * used, and what we never do with it.
 */
enum class AppPermission(
    @StringRes private val titleRes: Int,
    val icon: ImageVector,
    /** One line shown under the title on the permissions screen. */
    @StringRes private val summaryRes: Int,
    /** Features that use it, shown as "Used for" bullets. */
    private val usedForRes: List<Int>,
    /** What we promise not to do. */
    @StringRes private val privacyRes: Int,
) {
    Notifications(
        titleRes = R.string.perm_notifications_title,
        icon = Icons.Rounded.Notifications,
        summaryRes = R.string.perm_notifications_summary,
        usedForRes = listOf(
            R.string.perm_notifications_used_reminders,
            R.string.perm_notifications_used_updates,
            R.string.perm_notifications_used_alerts,
        ),
        privacyRes = R.string.perm_notifications_privacy,
    ),
    Camera(
        titleRes = R.string.perm_camera_title,
        icon = Icons.Rounded.CameraAlt,
        summaryRes = R.string.perm_camera_summary,
        usedForRes = listOf(
            R.string.perm_camera_used_prescription,
            R.string.perm_camera_used_lab_report,
            R.string.perm_camera_used_profile_photo,
        ),
        privacyRes = R.string.perm_camera_privacy,
    ),
    Microphone(
        titleRes = R.string.perm_mic_title,
        icon = Icons.Rounded.Mic,
        summaryRes = R.string.perm_mic_summary,
        usedForRes = listOf(
            R.string.perm_mic_used_voice,
            R.string.perm_mic_used_breathing,
        ),
        privacyRes = R.string.perm_mic_privacy,
    ),
    NearbyDevices(
        titleRes = R.string.perm_nearby_title,
        icon = Icons.Rounded.Bluetooth,
        summaryRes = R.string.perm_nearby_summary,
        usedForRes = listOf(
            R.string.perm_nearby_used_pair,
            R.string.perm_nearby_used_transfer,
        ),
        privacyRes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            R.string.perm_nearby_privacy
        else
            R.string.perm_nearby_privacy_legacy,
    ),
    Storage(
        titleRes = R.string.perm_storage_title,
        icon = Icons.Rounded.Folder,
        summaryRes = R.string.perm_storage_summary,
        usedForRes = listOf(R.string.perm_storage_used_download),
        privacyRes = R.string.perm_storage_privacy,
    );

    // Read on every access (not stored) so the copy follows an in-app language switch
    val title: String get() = MyApplication.appContext.getString(titleRes)
    val summary: String get() = MyApplication.appContext.getString(summaryRes)
    val usedFor: List<String> get() = usedForRes.map { MyApplication.appContext.getString(it) }
    val privacy: String get() = MyApplication.appContext.getString(privacyRes)

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
        get() = MyApplication.appContext.getString(
            when (this) {
                Notifications -> R.string.perm_notifications_reason
                Camera -> R.string.perm_camera_reason
                Microphone -> R.string.perm_mic_reason
                NearbyDevices -> R.string.perm_nearby_reason
                Storage -> R.string.perm_storage_reason
            }
        )

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
