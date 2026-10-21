package com.critetiontech.ctvitalio.utils

import android.app.Application
import android.content.Context
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import com.critetiontech.ctvitalio.omron.OmronManager
import com.critetiontech.ctvitalio.reminder.MedicineAlarmNotifier

// @HiltAndroidApp annotation is ready but requires a Hilt-compatible build configuration.
// Will be re-enabled once Hilt annotation processing is unblocked (Kotlin 2.1.0 + AGP 8.11.0 compatibility).
class MyApplication : Application() {
    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        MedicineAlarmNotifier.ensureChannels(this)
        WearableSync.schedule(this)
        OmronManager.init(this)
    }
}
