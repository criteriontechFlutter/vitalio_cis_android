package com.critetiontech.ctvitalio.reminder

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import com.critetiontech.ctvitalio.R

/**
 * What a medicine alarm needs from the system on this Android version, and where the user
 * grants each piece. Items that don't apply to the running version are left out.
 */
object AlarmSetup {

    enum class Item { Notifications, ExactAlarms, FullScreen, PopupOverApps, Battery }

    data class Requirement(
        val item: Item,
        val title: String,
        val description: String,
        val granted: Boolean,
        /** Alarms are unreliable or never pop up without it. */
        val critical: Boolean,
    )

    fun requirements(context: Context): List<Requirement> = buildList {
        add(Requirement(
            Item.Notifications, context.getString(R.string.med_setup_notifications_title),
            context.getString(R.string.med_setup_notifications_desc),
            MedicineAlarmNotifier.notificationsEnabled(context), critical = true,
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Requirement(
            Item.ExactAlarms, context.getString(R.string.med_setup_exact_alarms_title),
            context.getString(R.string.med_setup_exact_alarms_desc),
            MedicineReminderScheduler.canScheduleExactAlarms(context), critical = true,
        ))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) add(Requirement(
            Item.FullScreen, context.getString(R.string.med_setup_full_screen_title),
            context.getString(R.string.med_setup_full_screen_desc),
            MedicineAlarmNotifier.canUseFullScreen(context), critical = true,
        ))
        if (AlarmPopupLauncher.needsOverlayPermission()) add(Requirement(
            Item.PopupOverApps, context.getString(R.string.med_setup_popup_title),
            context.getString(R.string.med_setup_popup_desc),
            AlarmPopupLauncher.canPopUpOverApps(context), critical = true,
        ))
        add(Requirement(
            Item.Battery, context.getString(R.string.med_setup_battery_title),
            context.getString(R.string.med_setup_battery_desc),
            context.getSystemService(PowerManager::class.java).isIgnoringBatteryOptimizations(context.packageName),
            critical = false,
        ))
    }

    fun isComplete(context: Context): Boolean = requirements(context).none { it.critical && !it.granted }

    /**
     * Settings screen for [item]. Notifications on Android 13+ are requested with the runtime
     * permission dialog instead; this is the fallback once that dialog stops showing.
     */
    fun settingsIntent(context: Context, item: Item): Intent {
        val pkg = Uri.parse("package:${context.packageName}")
        return when (item) {
            Item.Notifications -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            Item.ExactAlarms ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, pkg)
                else appDetails(pkg)
            Item.FullScreen ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
                else appDetails(pkg)
            Item.PopupOverApps -> AlarmPopupLauncher.overlaySettingsIntent(context)
            Item.Battery -> Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
        }
    }

    private fun appDetails(pkg: Uri) = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
}
