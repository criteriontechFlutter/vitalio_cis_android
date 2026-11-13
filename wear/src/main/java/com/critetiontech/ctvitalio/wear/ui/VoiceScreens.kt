package com.critetiontech.ctvitalio.wear.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.itemsIndexed
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.EdgeButton
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
import java.util.Calendar

private const val RESULT_DISPLAY_MS = 2_500L
private const val HINT_ROTATE_MS = 3_200L

/** Outcome banner shown briefly after a session ends. */
internal data class SessionResult(val message: String, val success: Boolean)

private enum class Stage { HOME, LISTENING, UNDERSTANDING, CONFIRM, RESULT }

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
    val audioLevel by vm.audioLevel.collectAsState()

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

    val stage = when {
        result != null -> Stage.RESULT
        showConfirm && pending != null -> Stage.CONFIRM
        overlayVisible && isExtracting -> Stage.UNDERSTANDING
        overlayVisible -> Stage.LISTENING
        else -> Stage.HOME
    }
    // Keep the last non-null values so a screen can finish its exit animation after its data clears
    var lastResult by remember { mutableStateOf<SessionResult?>(null) }
    var lastPending by remember { mutableStateOf<VoiceExtractedData?>(null) }
    result?.let { if (it != lastResult) lastResult = it }
    pending?.let { if (it != lastPending) lastPending = it }

    AnimatedContent(
        targetState = stage,
        transitionSpec = {
            (fadeIn(tween(260, delayMillis = 70)) +
                scaleIn(tween(380, easing = EmphasizedDecelerate), initialScale = 0.9f)) togetherWith
                (fadeOut(tween(150)) + scaleOut(tween(220), targetScale = 1.06f))
        },
        label = "stage"
    ) { target ->
        when (target) {
            Stage.RESULT -> lastResult?.let { ResultScreen(it) }
            Stage.CONFIRM -> lastPending?.let {
                ConfirmScreen(
                    data = it,
                    alerts = alerts,
                    isSaving = isSaving,
                    onSave = {
                        awaitingSave = true
                        vm.confirmAndSaveAllPendingData()
                    },
                    onCancel = ::cancelSession
                )
            }
            Stage.UNDERSTANDING -> UnderstandingScreen(processingText)
            Stage.LISTENING -> ListeningScreen(
                isListening = isListening,
                level = audioLevel,
                transcript = transcript,
                prompt = prompt,
                onStop = { vm.stopListening() }
            )
            Stage.HOME -> HomeScreen(session, onMicClick = ::startSession)
        }
    }
}

// ── Screens ───────────────────────────────────────────────────────────────────

@Composable
internal fun HomeScreen(session: WatchSession, onMicClick: () -> Unit) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val firstName = session.name.trim().substringBefore(' ').ifBlank { session.uhId }
    val hour = remember { Calendar.getInstance().get(Calendar.HOUR_OF_DAY) }
    val greeting = stringResource(
        when (hour) {
            in 5..11 -> R.string.good_morning
            in 12..16 -> R.string.good_afternoon
            else -> R.string.good_evening
        }
    )
    val examples = stringArrayResource(R.array.voice_examples)
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(examples.size) {
        while (true) {
            delay(HINT_ROTATE_MS)
            hintIndex = (hintIndex + 1) % examples.size
        }
    }

    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(greeting, style = MaterialTheme.typography.labelMedium, color = colors.onSurfaceVariant)
            Text(
                text = firstName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(16.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(76.dp)) {
                BreathingRings(colors.primaryContainer, Modifier.fillMaxSize())
                PressableCircle(
                    onClick = {
                        context.vibrate(HAPTIC_TICK)
                        onMicClick()
                    },
                    size = 76.dp,
                    color = colors.primaryContainer,
                    brush = Brush.linearGradient(listOf(colors.primaryDim, colors.primaryContainer)),
                    contentDescription = stringResource(R.string.tap_to_speak)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_mic),
                        contentDescription = null,
                        tint = colors.onPrimaryContainer,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.try_saying),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant
            )
            AnimatedContent(
                targetState = hintIndex,
                transitionSpec = {
                    (fadeIn(tween(300)) + slideInVertically(tween(300, easing = EmphasizedDecelerate)) { it / 2 }) togetherWith
                        (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 2 })
                },
                label = "hint"
            ) { i ->
                Text(
                    text = "“${examples[i]}”",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
internal fun ListeningScreen(
    isListening: Boolean,
    level: Float,
    transcript: String,
    prompt: String,
    onStop: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EdgeGlow(level = if (isListening) level else 0f, color = colors.primary)
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (prompt.isNotBlank()) {
                    Text(
                        text = prompt,
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                } else {
                    Text(
                        text = stringResource(if (isListening) R.string.listening else R.string.connecting),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.primary
                    )
                }
                Spacer(Modifier.height(10.dp))
                VoiceWaveform(level = level, active = isListening)
                Spacer(Modifier.height(10.dp))
                Text(
                    text = transcript.ifBlank { stringResource(R.string.listening_hint) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (transcript.isBlank()) colors.onSurfaceVariant else colors.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.animateContentSize()
                )
                Spacer(Modifier.height(12.dp))
                PressableCircle(
                    onClick = onStop,
                    size = 44.dp,
                    color = colors.surfaceContainerHigh,
                    contentDescription = stringResource(R.string.done)
                ) {
                    Box(
                        Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary)
                    )
                }
            }
        }
    }
}

@Composable
internal fun UnderstandingScreen(text: String) {
    val colors = MaterialTheme.colorScheme
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EdgeSpinner(colors.primary)
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 30.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TypingDots(colors.primary)
                Spacer(Modifier.height(14.dp))
                Text(
                    stringResource(R.string.understanding),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                if (text.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "“$text”",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
internal fun ConfirmScreen(
    data: VoiceExtractedData,
    alerts: List<VitalAlert>,
    isSaving: Boolean,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val items = remember(data) { data.summaryItems() }
    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(onClick = onSave, enabled = !isSaving) {
                if (!isSaving) {
                    Icon(
                        painter = painterResource(R.drawable.ic_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
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
            itemsIndexed(alerts) { i, alert -> StaggeredIn(i) { AlertCard(alert) } }
            itemsIndexed(items) { i, item -> StaggeredIn(alerts.size + i) { ItemRow(item) } }
            item {
                // Outlined so it reads as secondary to the Save edge button
                OutlinedButton(onClick = onCancel, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.cancel),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
internal fun ResultScreen(result: SessionResult) {
    val colors = MaterialTheme.colorScheme
    val accent = if (result.success) colors.tertiary else colors.error
    val textIn = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(220)
        textIn.animateTo(1f, tween(320, easing = EmphasizedDecelerate))
    }
    ScreenScaffold {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EdgeCountdown(RESULT_DISPLAY_MS.toInt(), accent)
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (result.success) {
                    AnimatedCheck(color = colors.tertiary, onColor = colors.onTertiary, size = 60.dp)
                } else {
                    ErrorBadge()
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.graphicsLayer {
                        alpha = textIn.value
                        translationY = (1f - textIn.value) * 10.dp.toPx()
                    }
                )
            }
        }
    }
}

// ── Components ────────────────────────────────────────────────────────────────

/** Fades and lifts a list row in, offset by its position for a cascading entrance. */
@Composable
private fun StaggeredIn(index: Int, content: @Composable () -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 60L)
        p.animateTo(1f, tween(340, easing = EmphasizedDecelerate))
    }
    Box(
        Modifier.graphicsLayer {
            alpha = p.value
            translationY = (1f - p.value) * 16.dp.toPx()
        }
    ) { content() }
}

@Composable
private fun ItemRow(item: SummaryItem) {
    val colors = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(colors.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(item.kind.accent.copy(alpha = 0.18f))
        ) {
            Icon(
                painter = painterResource(item.kind.icon),
                contentDescription = null,
                tint = item.kind.accent,
                modifier = Modifier.size(17.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                item.value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AlertCard(alert: VitalAlert) {
    val colors = MaterialTheme.colorScheme
    val critical = alert.severity == AnomalySeverity.CRITICAL
    val accent = if (critical) colors.error else WarningColor
    val container = if (critical) colors.errorContainer else WarningContainer
    // Critical alerts get a slow pulsing outline so they can't be skimmed past
    val pulse by rememberInfiniteTransition(label = "alertPulse").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alertAlpha"
    )
    val shape = RoundedCornerShape(22.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(shape)
            .background(container)
            .then(if (critical) Modifier.border(1.5.dp, accent.copy(alpha = pulse), shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(26.dp).clip(CircleShape).background(accent)
        ) {
            Text("!", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = container)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${alert.vitalLabel} ${alert.value}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
            Text(alert.message, style = MaterialTheme.typography.bodySmall, color = colors.onSurface)
        }
    }
}

/** Error disc that pops in and gives a short "no" shake. */
@Composable
private fun ErrorBadge() {
    val colors = MaterialTheme.colorScheme
    val scale = remember { Animatable(0.4f) }
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.5f)) }
    LaunchedEffect(Unit) {
        delay(160)
        for (x in floatArrayOf(-8f, 7f, -5f, 3f, 0f)) shake.animateTo(x, tween(55))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                translationX = shake.value.dp.toPx()
            }
            .size(60.dp)
            .clip(CircleShape)
            .background(colors.errorContainer)
    ) {
        Text("!", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, color = colors.error)
    }
}
