package com.critetiontech.ctvitalio.wear

import android.app.Application
import com.critetiontech.ctvitalio.voice.VoiceCore
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.critetiontech.ctvitalio.wear.session.WatchSessionStore
import com.critetiontech.ctvitalio.wear.voice.WearVoiceBackend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        WatchSessionStore.init(this)
        VoiceCore.backend = WearVoiceBackend
        // Watch voice sessions only record data — there are no phone screens to navigate to
        VoiceNavigationManager.navigationEnabled = false
        VoiceNavigationManager.initTts(this)

        appScope.launch { WatchSessionStore.refreshFromDataLayer(this@WearApplication) }
    }
}