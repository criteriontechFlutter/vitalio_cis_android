package com.critetiontech.ctvitalio.wear.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.FilledIconButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.OutlinedButton
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.critetiontech.ctvitalio.voice.AnomalySeverity
import com.critetiontech.ctvitalio.voice.VitalAlert
import com.critetiontech.ctvitalio.voice.VoiceExtractedData
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.critetiontech.ctvitalio.wear.R
import com.critetiontech.ctvitalio.wear.session.WatchSession
import kotlinx.coroutines.delay

private const val RESULT_DISPLAY_MS = 2_500L

/** Outcome banner shown briefly after a session ends. */
private data class SessionResult(val message: String, val success: Boolean)

/**
 * Watch voice flow driven by the shared [VoiceNavigationManager]:
 * Home (mic) → Listening → Understanding → Confirm → Result.
 */
@Composable
fun VoiceAssistantFlow(session: WatchSession) {
    val vm = VoiceNavigationManager
    val context = LocalContext.current

    val overlayVisible by vm.showListeningOverlay.collectAsState()
    val isListening by vm.isListening.collectAsState()
    val showConfirm by vm.showConfirmationCard.collectAsState()
    val pending by vm.pendingData.collectAsState()
    val isSaving by vm.isSavingData.collectAsState()
    val isExtracting by vm.isExtractingData.collectAsState()
    val transcript by vm.transcript.collectAsState()
    val processingText by vm.processingTranscript.collectAsState()
    val alerts by vm.anomalyAlerts.collectAsState()
    val prompt by vm.multiTurnPrompt.collectAsState()

    var result by remember { mutableStateOf<SessionResult?>(null) }
    var awaitingSave by remember { mutableStateOf(false) }
    var sessionActive by remember { mutableStateOf(false) }
    var userCancelled by remember { mutableStateOf(false) }

    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            sessionActive = true
            vm.startListening { /* navigation disabled on the watch */ }
        } else {
            result = SessionResult(context.getString(R.string.mic_permission_needed), success = false)
        }
    }

    fun startSession() {
        result = null
        userCancelled = false
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            sessionActive = true
            vm.startListening { }
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun cancelSession() {
        userCancelled = true
        awaitingSave = false
        vm.cancelPendingData()
    }

    // Save finished → the engine has written its summary into `transcript` on the same Main pass
    LaunchedEffect(isSaving) {
        if (awaitingSave && !isSaving) {
            awaitingSave = false
            val summary = vm.transcript.value
            val ok = !summary.contains("error", ignoreCase = true)
            context.vibrate(if (ok) HAPTIC_SUCCESS else HAPTIC_FAILURE)
            result = SessionResult(summary.ifBlank { context.getString(R.string.saved) }, ok)
        }
    }

    // Session ended without anything to confirm → tell the user nothing was understood
    LaunchedEffect(overlayVisible, showConfirm) {
        if (sessionActive && !overlayVisible && !showConfirm) {
            sessionActive = false
            if (!userCancelled && !awaitingSave && result == null) {
                context.vibrate(HAPTIC_FAILURE)
                result = SessionResult(context.getString(R.string.nothing_understood), success = false)
            }
        }
    }

    LaunchedEffect(alerts) {
        if (alerts.any { it.severity == AnomalySeverity.CRITICAL }) context.vibrate(HAPTIC_CRITICAL)
    }

    LaunchedEffect(result) {
        if (result != null) {
            delay(RESULT_DISPLAY_MS)
            result = null
        }
    }

    // Swipe-back cancels an active session instead of closing the app
    BackHandler(enabled = overlayVisible || showConfirm) { cancelSession() }

    val currentResult = result
    val currentPending = pending
    when {
        currentResult != null -> ResultScreen(currentResult)
        showConfirm && currentPending != null -> ConfirmScreen(
            data = currentPending,
            alerts = alerts,
            isSaving = isSaving,
            onSave = {
                awaitingSave = true
                vm.confirmAndSaveAllPendingData()
            },
            onCancel = ::cancelSession
        )
        overlayVisible && isExtracting -> UnderstandingScreen(processingText)
        overlayVisible -> ListeningScreen(
            isListening = isListening,
            transcript = transcript,
            prompt = prompt,
            onStop = { vm.stopListening() }
        )
        else -> HomeScreen(session, onMicClick = ::startSession)
    }
}

// ── Screens ───────────────────────────────────────────────────────────────────

@Composable
private fun HomeScreen(session: WatchSession, onMicClick: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.greeting, session.name.substringBefore(' ').ifBlank { session.uhId }),
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))
            FilledIconButton(onClick = onMicClick, modifier = Modifier.size(72.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = stringResource(R.string.tap_to_speak),
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.voice_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ListeningScreen(isListening: Boolean, transcript: String, prompt: String, onStop: () -> Unit) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PulsingMic(active = isListening)
            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    prompt.isNotBlank() -> prompt
                    transcript.isNotBlank() -> transcript
                    isListening -> stringResource(R.string.listening)
                    else -> stringResource(R.string.connecting)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onStop) { Text(stringResource(R.string.done)) }
        }
    }
}

@Composable
private fun UnderstandingScreen(text: String) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spinner()
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.understanding), style = MaterialTheme.typography.titleSmall)
            if (text.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "“$text”",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ConfirmScreen(
    data: VoiceExtractedData,
    alerts: List<VitalAlert>,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onSave, enabled = !isSaving) {
                Text(stringResource(if (isSaving) R.string.saving else R.string.save))
            }
        }
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize()
        ) {
            item { ListHeader { Text(stringResource(R.string.confirm_title)) } }
            items(alerts) { AlertRow(it) }
            items(data.summaryLines()) { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                )
            }
            item {
                Button(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    }
}

@Composable
private fun ResultScreen(result: SessionResult) {
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (result.success) "✓" else "!",
                style = MaterialTheme.typography.displayMedium,
                color = if (result.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = result.message,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

@Composable
private fun AlertRow(alert: VitalAlert) {
    val critical = alert.severity == AnomalySeverity.CRITICAL
    val bg = if (critical) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
    val fg = if (critical) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onTertiary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(bg, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "${alert.emoji} ${alert.vitalLabel} ${alert.value}\n${alert.message}",
            style = MaterialTheme.typography.bodySmall,
            color = fg
        )
    }
}

@Composable
private fun PulsingMic(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "pulseScale"
    )
    val primary = MaterialTheme.colorScheme.primary
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .scale(if (active) pulse else 1f)
                .background(primary.copy(alpha = 0.25f), CircleShape)
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(44.dp).background(primary, CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_mic),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun Spinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spinnerRotation"
    )
    val color = MaterialTheme.colorScheme.primary
    Canvas(Modifier.size(40.dp).graphicsLayer { rotationZ = rotation }) {
        drawArc(
            color = color,
            startAngle = 0f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.1f),
            startAngle = 270f,
            sweepAngle = 90f,
            useCenter = false,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}