package com.critetiontech.vitalio_cis.utils

import android.app.Application
import android.content.Context

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
    }
}
