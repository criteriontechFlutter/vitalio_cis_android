package com.critetiontech.ctvitalio.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.material3.AppScaffold
import com.critetiontech.ctvitalio.wear.ui.VitalioWearTheme
import com.critetiontech.ctvitalio.voice.AnomalySeverity
import com.critetiontech.ctvitalio.voice.VitalAlert
import com.critetiontech.ctvitalio.voice.VoiceExtractedData
import com.critetiontech.ctvitalio.voice.VoiceFluidIntakeItem
import com.critetiontech.ctvitalio.wear.session.WatchSession
import com.critetiontech.ctvitalio.wear.ui.ConfirmScreen
import com.critetiontech.ctvitalio.wear.ui.HomeScreen
import com.critetiontech.ctvitalio.wear.ui.ListeningScreen
import com.critetiontech.ctvitalio.wear.ui.ResultScreen
import com.critetiontech.ctvitalio.wear.ui.SessionResult
import com.critetiontech.ctvitalio.wear.ui.SignInOnPhoneScreen
import com.critetiontech.ctvitalio.wear.ui.UnderstandingScreen

/**
 * Debug-only screen renderer for Play Store screenshots. Usage:
 *   adb shell am start -n com.critetiontech.ctvitalio/com.critetiontech.ctvitalio.wear.ScreenshotActivity --es screen home
 * Screens: signin, home, listening, understanding, confirm, saved
 */
class ScreenshotActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screen = intent.getStringExtra("screen") ?: "home"
        setContent {
            VitalioWearTheme {
                AppScaffold { DemoScreen(screen) }
            }
        }
    }
}

private val demoSession = WatchSession(pid = 1, clientId = 1, uhId = "UHID001", name = "Rahul Sharma")

private val demoData = VoiceExtractedData(
    vitals = mapOf("vmValueBPSys" to "120", "vmValueBPDias" to "80", "vmValuePulse" to "72"),
    fluidIntakes = listOf(VoiceFluidIntakeItem(foodId = 1, name = "Water", quantity = 500))
)

@Composable
private fun DemoScreen(screen: String) {
    when (screen) {
        "signin" -> SignInOnPhoneScreen()
        "listening" -> ListeningScreen(
            isListening = true,
            level = 0.75f,
            transcript = "BP 120 by 80, pulse 72",
            prompt = "",
            onStop = {}
        )
        "understanding" -> UnderstandingScreen("BP 120 by 80, pulse 72 and 2 glass paani")
        "confirm" -> ConfirmScreen(data = demoData, alerts = emptyList(), isSaving = false, onSave = {}, onCancel = {})
        "alert" -> ConfirmScreen(
            data = VoiceExtractedData(vitals = mapOf("vmValueBPSys" to "182", "vmValueBPDias" to "110")),
            alerts = listOf(
                VitalAlert("vmValueBPSys", "BP", "182/110", AnomalySeverity.CRITICAL, "Very high — contact your doctor", "🚨")
            ),
            isSaving = false, onSave = {}, onCancel = {}
        )
        "saved" -> ResultScreen(SessionResult("Saved BP, pulse and water intake", success = true))
        else -> HomeScreen(demoSession, onMicClick = {})
    }
}
