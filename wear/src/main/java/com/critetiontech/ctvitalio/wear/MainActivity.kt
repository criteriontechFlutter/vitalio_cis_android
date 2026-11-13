package com.critetiontech.ctvitalio.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.critetiontech.ctvitalio.wear.session.WatchSession
import com.critetiontech.ctvitalio.wear.session.WatchSessionStore
import com.critetiontech.ctvitalio.wear.ui.SignInOnPhoneScreen
import com.critetiontech.ctvitalio.wear.ui.VoiceAssistantFlow
import androidx.wear.compose.material3.AppScaffold
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import com.critetiontech.ctvitalio.wear.ui.VitalioWearTheme
import androidx.wear.tooling.preview.devices.WearDevices
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WearApp() }
    }

    override fun onStop() {
        super.onStop()
        // Don't keep the mic open once the app leaves the screen; captured speech is still processed
        if (VoiceNavigationManager.isListening.value) VoiceNavigationManager.stopListening()
    }
}

@Composable
fun WearApp() {
    val session by WatchSessionStore.session.collectAsState()
    WearAppContent(session)
}

@Composable
private fun WearAppContent(session: WatchSession?) {
    VitalioWearTheme {
        AppScaffold {
            // Key on signed-in state only, so a session refresh doesn't restart the voice flow
            Crossfade(targetState = session != null, animationSpec = tween(400), label = "auth") { signedIn ->
                val s = session
                if (signedIn && s != null) VoiceAssistantFlow(s) else SignInOnPhoneScreen()
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SignedOutPreview() = WearAppContent(session = null)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SignedInPreview() = WearAppContent(WatchSession(pid = 1, clientId = 1, uhId = "UHID001", name = "Test Patient"))