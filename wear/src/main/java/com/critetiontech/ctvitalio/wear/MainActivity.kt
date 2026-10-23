package com.critetiontech.ctvitalio.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.critetiontech.ctvitalio.wear.session.WatchSession
import com.critetiontech.ctvitalio.wear.session.WatchSessionStore
import com.critetiontech.ctvitalio.wear.ui.VoiceAssistantFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
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
    MaterialTheme {
        AppScaffold {
            if (session == null) {
                ScreenScaffold { MessageScreen(stringResource(R.string.sign_in_on_phone)) }
            } else {
                VoiceAssistantFlow(session)
            }
        }
    }
}

@Composable
private fun MessageScreen(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SignedOutPreview() = WearAppContent(session = null)

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun SignedInPreview() = WearAppContent(WatchSession(pid = 1, clientId = 1, uhId = "UHID001", name = "Test Patient"))