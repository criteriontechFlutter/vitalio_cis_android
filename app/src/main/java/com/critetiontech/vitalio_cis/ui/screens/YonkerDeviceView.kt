package com.critetiontech.ctvitalio.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.components.*
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.AppMotion
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.OFFLINE
import com.critetiontech.ctvitalio.viewmodel.YonkerDeviceViewModel
import com.critetiontech.ctvitalio.viewmodel.YonkerPhase
import com.critetiontech.ctvitalio.yonker.YonkerKind
import com.critetiontech.ctvitalio.yonker.YonkerReading
import java.time.ZoneId

// ─── Screen ───────────────────────────────────────────────────────────────────

@Composable
fun YonkerDeviceScreen(kindCode: String, viewModel: YonkerDeviceViewModel = viewModel()) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    LaunchedEffect(kindCode) { viewModel.load(kindCode) }

    val phase by viewModel.phase.collectAsState()
    val connected by viewModel.connected.collectAsState()
    val kind = viewModel.kind ?: YonkerKind.byCode(kindCode)

    // A finished BP result, a save or a failure each get a tap the user can feel
    val reading = phase as? YonkerPhase.Reading
    LaunchedEffect(reading?.reading?.takeIf { it.isBloodPressure }) { if (reading?.reading?.isBloodPressure == true) haptic(HapticEvent.Success) }
    LaunchedEffect(reading?.saved) { if (reading?.saved == true) haptic(HapticEvent.Success) }
    LaunchedEffect(phase is YonkerPhase.Failed) { if (phase is YonkerPhase.Failed) haptic(HapticEvent.Error) }

    // Bluetooth permission → Bluetooth on → connect
    var pending by remember { mutableStateOf(false) }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.bluetoothOn()) viewModel.connect()
        else viewModel.showError("Bluetooth is off. Turn it on so we can reach your machine.")
        pending = false
    }
    val nearbyDevices = rememberPermissionRequest(
        AppPermission.NearbyDevices,
        "to find your Yonker device and read its measurements over Bluetooth",
        onDenied = { pending = false },
    ) {
        if (context.bluetoothOn()) { viewModel.connect(); pending = false }
        else enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
    val connect = { pending = true; nearbyDevices.launch() }

    CommonAppBar(title = "Yonker ${kind?.label.orEmpty()}") {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor).verticalScroll(rememberScrollState())
        ) {
            if (kind == null) {
                Text("This device isn't supported.", modifier = Modifier.padding(24.dp), color = colors.textGreyColor)
                return@Column
            }
            val busy = phase == YonkerPhase.Searching || phase == YonkerPhase.Connecting
            val bp = kind == YonkerKind.BP

            AnimatedEnter(index = 0) {
                DeviceHeader(
                    icon = if (bp) Icons.Filled.MonitorHeart else Icons.Filled.Bloodtype,
                    eyebrow = "Yonker · ${kind.label}",
                    title = if (bp) "Blood pressure" else "Oxygen & pulse",
                    status = when {
                        phase == YonkerPhase.Searching -> "Looking for your machine…"
                        phase == YonkerPhase.Connecting -> "Saying hello…"
                        connected -> "Connected"
                        else -> "Not connected"
                    },
                    live = connected,
                    busy = busy || pending,
                    action = {
                        if (connected) HeaderActionPill("Disconnect", Icons.Filled.LinkOff, enabled = true, onClick = viewModel::disconnect)
                        else HeaderActionPill(if (busy) "Connecting" else "Connect", Icons.Filled.Bluetooth, enabled = !busy, spinning = busy, onClick = connect)
                    }
                )
            }

            AnimatedContent(
                targetState = phase,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(AppMotion.MEDIUM)) + slideInVertically(tween(AppMotion.MEDIUM)) { it / 6 }) togetherWith
                        fadeOut(tween(AppMotion.SHORT)) using SizeTransform(clip = false)
                },
                label = "yonkerPhase"
            ) { p ->
                when (p) {
                    YonkerPhase.Idle -> ReadyCard(kind, onConnect = connect)
                    YonkerPhase.Searching -> RadarBusyCard(
                        "Looking for your ${kind.label.lowercase()}",
                        if (bp) "Make sure it's switched on and within arm's reach of your phone."
                        else "Slip a finger into the oximeter so it wakes up.",
                        onCancel = viewModel::disconnect
                    )
                    YonkerPhase.Connecting -> RadarBusyCard("Found it, connecting…", "This only takes a second.", onCancel = viewModel::disconnect)
                    is YonkerPhase.Listening -> ListeningCard(kind, p)
                    is YonkerPhase.Reading -> ReadingCard(kind, p, connected, onSave = viewModel::save, onAgain = viewModel::measureAgain)
                    YonkerPhase.Asleep -> StatusCard(
                        false, "Your machine took a nap",
                        "It switches itself off when it's left waiting, to save battery. Switch it on and reconnect when you're ready.",
                        icon = Icons.Filled.Bedtime, tint = DeviceAmber,
                        action = "Reconnect", actionIcon = Icons.Filled.Bluetooth, onAction = connect
                    )
                    is YonkerPhase.Failed -> StatusCard(
                        false, "We couldn't reach your machine", p.message,
                        icon = Icons.Filled.BluetoothDisabled,
                        action = "Try again", actionIcon = Icons.Filled.Refresh, onAction = connect,
                        extra = {
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Tip: switch the machine off and on, keep it close, and try again.",
                                fontSize = 11.sp, color = colors.textGreyColor, textAlign = TextAlign.Center
                            )
                        }
                    )
                }
            }

            if (phase == YonkerPhase.Idle || phase == YonkerPhase.Asleep || phase is YonkerPhase.Failed) AnimatedEnter(index = 1) {
                StepsCard(
                    "For an accurate reading",
                    if (bp) listOf(
                        Icons.Filled.EventSeat to "Sit quietly for 5 minutes with your back supported and feet flat.",
                        Icons.Filled.FrontHand to "Wrap the cuff on your bare upper arm, level with your heart.",
                        Icons.Filled.VolumeOff to "Stay still and don't talk while the cuff works."
                    ) else listOf(
                        Icons.Filled.WbSunny to "Warm hands read better. Rub them together if they're cold.",
                        Icons.Filled.TouchApp to "Use a finger without nail polish and rest your hand on a table.",
                        Icons.Filled.Timer to "Hold still for about 10 seconds while the numbers settle."
                    ),
                )
            }
            if (connected) AnimatedEnter(index = 2) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    HapticTextButton(onClick = viewModel::disconnect) {
                        Icon(Icons.Filled.LinkOff, null, tint = colors.textGreyColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("I'm done, disconnect", color = colors.textGreyColor)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── States ───────────────────────────────────────────────────────────────────

/** Before connecting: a friendly nudge and one clear action. */
@Composable
private fun ReadyCard(kind: YonkerKind, onConnect: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    val bp = kind == YonkerKind.BP
    val float = rememberInfiniteTransition(label = "ready")
    val lift by float.animateFloat(0f, 1f, infiniteRepeatable(tween(1800, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "lift")
    FlatCard(centered = true) {
        Box(
            Modifier.size(72.dp).graphicsLayer { translationY = -4.dp.toPx() * lift }.clip(CircleShape).background(tintFill(p)),
            contentAlignment = Alignment.Center
        ) { Icon(if (bp) Icons.Filled.MonitorHeart else Icons.Filled.Bloodtype, null, tint = p, modifier = Modifier.size(36.dp)) }
        Spacer(Modifier.height(12.dp))
        Text(
            if (bp) "Let's check your blood pressure" else "Let's check your oxygen level",
            fontWeight = FontWeight.Bold, fontSize = 17.sp, color = colors.textDarkColor, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (bp) "Switch on your Yonker machine and keep it near your phone. No pairing needed."
            else "Put a finger in your Yonker oximeter, then connect. No pairing needed.",
            fontSize = 13.sp, color = colors.textGreyColor, textAlign = TextAlign.Center, lineHeight = 18.sp
        )
        Spacer(Modifier.height(16.dp))
        GradientButton(if (bp) "Connect my BP machine" else "Connect my oximeter", Icons.Filled.Bluetooth, onClick = onConnect)
    }
}

/** Connected, before a result: the cuff gauge while measuring, or a prompt to start. */
@Composable
private fun ListeningCard(kind: YonkerKind, p: YonkerPhase.Listening) {
    val colors = LocalMyColorScheme.current
    FlatCard(centered = true) {
        when {
            p.pressure != null -> {
                CuffGauge(p.pressure)
                Spacer(Modifier.height(12.dp))
                Text(
                    if (p.inflating) "Tightening the cuff…" else "Measuring as the cuff relaxes…",
                    fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textDarkColor
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Breathe normally and keep your arm still. Almost there.",
                    fontSize = 12.sp, color = colors.textGreyColor, textAlign = TextAlign.Center
                )
            }
            p.noSignal -> PromptBody(
                Icons.Filled.TouchApp, DeviceAmber,
                "We can't see your pulse yet",
                "Push your finger all the way in and keep your hand relaxed and still."
            )
            kind == YonkerKind.BP -> PromptBody(
                Icons.Filled.PlayCircle, DeviceGreen,
                "You're connected",
                "Press START on the machine whenever you're ready. We'll show the reading here."
            )
            else -> PromptBody(
                Icons.Filled.Favorite, DeviceGreen,
                "You're connected",
                "Waiting for the first reading from your oximeter…"
            )
        }
    }
}

@Composable
private fun PromptBody(icon: ImageVector, tint: Color, title: String, message: String) {
    val colors = LocalMyColorScheme.current
    val pulse = rememberInfiniteTransition(label = "prompt")
    val ring by pulse.animateFloat(0f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing)), label = "ring")
    Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = 0.7f + 0.3f * ring; scaleY = 0.7f + 0.3f * ring; alpha = 1f - ring }
                .clip(CircleShape).background(tint.copy(alpha = 0.25f))
        )
        Box(Modifier.size(60.dp).clip(CircleShape).background(tintFill(tint)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(30.dp))
        }
    }
    Spacer(Modifier.height(10.dp))
    Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textDarkColor, textAlign = TextAlign.Center)
    Spacer(Modifier.height(4.dp))
    Text(message, fontSize = 13.sp, color = colors.textGreyColor, textAlign = TextAlign.Center, lineHeight = 18.sp)
}

private const val CUFF_MAX = 300f

/** Three-quarter dial for the cuff pressure, easing to each new value. */
@Composable
private fun CuffGauge(pressure: Int) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    val track = tintFill(p)
    val sweep by animateFloatAsState((pressure / CUFF_MAX).coerceIn(0f, 1f), spring(stiffness = Spring.StiffnessLow), label = "cuff")
    val shown by animateIntAsState(pressure, tween(300), label = "cuffValue")
    Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 14.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(track, 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            drawArc(p, 135f, 270f * sweep, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$shown", fontSize = 46.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 48.sp)
            Text("mmHg", fontSize = 12.sp, color = colors.textGreyColor)
        }
    }
}

/** What the reading means, in plain words. Not a diagnosis; the doctor sees the numbers. */
private fun bpMeaning(index: Int): Pair<String, String> = when (index) {
    0 -> "Your blood pressure looks healthy" to "Nicely in the normal range. Keep it up."
    1 -> "Just a little above ideal" to "Nothing to worry about now, but worth keeping an eye on."
    2 -> "A bit higher than normal" to "Rest for 5 minutes and measure again to be sure."
    3 -> "Your blood pressure is high" to "Measure again after resting. If it stays this high, talk to your doctor."
    else -> "This reading is very high" to "Sit and rest, then measure again. If it's still this high or you feel unwell, get medical help right away."
}

private fun spo2Meaning(spo2: Int): Triple<String, String, Color> = when {
    spo2 >= 95 -> Triple("Your oxygen level looks healthy", "Right where it should be.", Color(0xFF43A047))
    spo2 >= 90 -> Triple("Your oxygen is a little low", "Sit upright, take a few slow breaths and measure again.", Color(0xFFFB8C00))
    else -> Triple("Your oxygen level is low", "If you feel breathless or unwell, get medical help.", Color(0xFFE53935))
}

@Composable
private fun ReadingCard(kind: YonkerKind, result: YonkerPhase.Reading, connected: Boolean, onSave: () -> Unit, onAgain: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val r: YonkerReading = result.reading
    val local = r.time.atZone(ZoneId.systemDefault())
    val bp = if (r.isBloodPressure) bpCategory(r.systolic!!, r.diastolic!!) else null
    val oxi = r.spo2?.let(::spo2Meaning)
    val accent by animateColorAsState(bp?.color ?: oxi?.third ?: colors.primaryBlueColor, label = "accent")
    val (headline, advice) = bp?.let { bpMeaning(it.index) } ?: oxi?.let { it.first to it.second } ?: ("Your reading" to "")
    // Oximeter values keep updating until saved
    val live = kind == YonkerKind.OXIMETER && !result.locked
    val offline = result.error == OFFLINE

    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(tintFill(accent)), contentAlignment = Alignment.Center) {
                Icon(if (bp != null) Icons.Filled.MonitorHeart else Icons.Filled.Bloodtype, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(headline, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textDarkColor, lineHeight = 20.sp)
                Text(
                    if (live) "Live from your oximeter" else "${local.format(readingClock)} · ${local.format(readingDay)}",
                    fontSize = 12.sp, color = colors.textGreyColor
                )
            }
        }
        Spacer(Modifier.height(16.dp))

        if (bp != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Caption("Systolic")
                    Text("${rememberCountUp(r.systolic!!)}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 50.sp)
                }
                Text("/", fontSize = 36.sp, color = colors.textGreyColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Column {
                    Caption("Diastolic")
                    Text("${rememberCountUp(r.diastolic!!)}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 50.sp)
                }
                Text("mmHg", fontSize = 13.sp, color = colors.textGreyColor, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
            }
            Spacer(Modifier.height(14.dp))
            BpGauge(bp)
        } else if (r.spo2 != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Caption("SpO2")
                    Text("${r.spo2}", fontSize = 52.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 54.sp)
                }
                Text("%", fontSize = 18.sp, color = colors.textGreyColor, modifier = Modifier.padding(start = 6.dp, bottom = 12.dp))
                Spacer(Modifier.weight(1f))
                if (live) SteadyChip(result.steady)
            }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            InfoTile("Pulse", r.pulse?.let { "$it bpm" } ?: "--", Icons.Filled.Favorite, Color(0xFFE53935), Modifier.weight(1f), heartbeat = true)
            InfoTile("Measured", "${local.format(readingClock)}\n${local.format(readingDay)}", Icons.Filled.Schedule, colors.primaryBlueColor, Modifier.weight(1f))
        }
        if (advice.isNotEmpty()) {
            Text(advice, fontSize = 13.sp, color = colors.textDarkColor, lineHeight = 18.sp, modifier = Modifier.padding(top = 12.dp))
        }
        AnimatedVisibility(visible = result.error != null) {
            if (offline) NoteRow(Icons.Filled.WifiOff, OFFLINE, DeviceAmber, Modifier.padding(top = 10.dp))
            else NoteRow(Icons.Filled.ErrorOutline, "We couldn't save it: ${result.error.orEmpty()}", DeviceRed, Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(16.dp))

        AnimatedContent(
            targetState = result.saved,
            transitionSpec = { (fadeIn(tween(AppMotion.MEDIUM)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(AppMotion.SHORT)) },
            label = "saveState"
        ) { saved ->
            if (saved) Column {
                SavedBanner()
                if (connected) {
                    Spacer(Modifier.height(10.dp))
                    GradientButton("Take another reading", Icons.Filled.Refresh, onClick = onAgain)
                }
            } else Column {
                GradientButton(
                    when {
                        result.error != null -> "Try saving again"
                        live && !result.steady -> "Save anyway"
                        else -> "Save to my health record"
                    },
                    if (result.error != null) Icons.Filled.Refresh else Icons.Filled.CloudUpload,
                    colorsOverride = listOf(Color(0xFF1B7A43), DeviceGreen, Color(0xFF4CC17F)),
                    loading = result.saving,
                    onClick = onSave
                )
                if (bp != null && connected && !result.saving) {
                    HapticTextButton(onClick = onAgain, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text("Measure again instead", color = colors.textGreyColor, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

/** "Settling…" until the oximeter's last few values agree, then "Steady". */
@Composable
private fun SteadyChip(steady: Boolean) {
    val tint by animateColorAsState(if (steady) DeviceGreen else DeviceAmber, label = "steady")
    val blink = rememberInfiniteTransition(label = "settle")
    val alpha by blink.animateFloat(0.3f, 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "settleAlpha")
    Row(
        Modifier.padding(bottom = 12.dp).clip(RoundedCornerShape(50)).background(tintFill(tint)).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(7.dp).graphicsLayer { this.alpha = if (steady) 1f else alpha }.clip(CircleShape).background(tint))
        Spacer(Modifier.width(6.dp))
        Text(if (steady) "Steady · ready to save" else "Settling…", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = tint)
    }
}
