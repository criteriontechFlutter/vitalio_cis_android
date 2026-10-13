package com.critetiontech.ctvitalio.utils

import android.app.Application
import android.content.Context
import com.critetiontech.ctvitalio.voice.AppVoiceBackend
import com.critetiontech.ctvitalio.voice.VoiceCore
import com.critetiontech.ctvitalio.watch.WatchSessionSync

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
        VoiceCore.backend = AppVoiceBackend
        WatchSessionSync.start(this)
    }
}
