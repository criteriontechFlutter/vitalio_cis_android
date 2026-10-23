package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedContent
import coil.compose.AsyncImage
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.WindowInsets
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.CallEnd
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.PhoneInTalk
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.BluetoothAudio
import com.twilio.audioswitch.AudioDevice
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.VideocamOff
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DataSaverOn
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Hd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.animation.animateColorAsState
import io.livekit.android.room.participant.ConnectionQuality
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.SubcomposeAsyncImage
import com.critetiontech.ctvitalio.consultation.CallQuality
import com.critetiontech.ctvitalio.model.IncomingCall
import com.critetiontech.ctvitalio.viewmodel.CallPhase
import com.critetiontech.ctvitalio.viewmodel.CallViewModel
import com.critetiontech.ctvitalio.ui.components.DeviceGreen
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import io.livekit.android.renderer.TextureViewRenderer
import io.livekit.android.room.Room
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import livekit.org.webrtc.RendererCommon
import kotlin.math.roundToInt

private val CallRed = Color(0xFFFF3B3B)
private val CallGreen = Color(0xFF12C17E)
/** Hang-up inside the call: a warmer red than decline, as in the design. */
private val HangUpRed = Color(0xFFF2421B)
private val PanelText = Color(0xFF202529)
private val PanelGrey = Color(0xFF6B7280)

@Composable
fun CallScreen(
    vm: CallViewModel,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onClose: () -> Unit,
    onMinimize: () -> Unit = {},
    pip: Boolean = false,
) {
    val call = vm.call ?: return
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (pip) PipContent(vm, call) else AnimatedContent(
            targetState = vm.phase.let { if (it is CallPhase.Ended) "ended" else it::class.simpleName },
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "callPhase",
        ) { key ->
            when (key) {
                "Ringing" -> RingingContent(call, onAccept, onDecline)
                "Connecting" -> WaitingContent(call, "Connecting…")
                "InCall" -> InCallContent(vm, call, onMinimize)
                else -> EndedContent(call, vm.phase as? CallPhase.Ended, vm.elapsedSeconds(), onClose)
            }
        }
    }
}

/**
 * The doctor's photo, blurred and veiled in grey, behind everything that isn't live video.
 * Blur needs Android 12; older phones get the plain grey instead of a sharp photo.
 */
@Composable
private fun Backdrop(call: IncomingCall) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFA6A4A9), Color(0xFF8E8C91), Color(0xFF737176))))
    ) {
        if (!call.doctorAvatarUrl.isNullOrBlank() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AsyncImage(
                model = call.doctorAvatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().blur(48.dp),
            )
            Box(Modifier.fillMaxSize().background(Color(0xFF8E8C91).copy(alpha = 0.55f)))
        }
    }
}

@Composable
private fun VitalioMark(modifier: Modifier = Modifier) {
    Text(
        "vitalio", modifier = modifier, color = Color.White, fontSize = 30.sp,
        fontWeight = FontWeight.Light, letterSpacing = 1.sp,
    )
}

// ── Ringing ──────────────────────────────────────────────────────────────────

@Composable
private fun RingingContent(call: IncomingCall, onAccept: () -> Unit, onDecline: () -> Unit) {
    Backdrop(call)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(28.dp))
        VitalioMark()
        Spacer(Modifier.weight(1f))
        RippleAvatar(call, size = 168.dp)
        Text(
            call.doctorName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(call.title ?: "Incoming Call…", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 32.dp).padding(bottom = 56.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CallButton(Icons.Rounded.CallEnd, "Decline", CallRed, size = 64.dp, onClick = onDecline)
            CallButton(Icons.Rounded.PhoneInTalk, "Join", CallGreen, size = 64.dp, wiggle = true, onClick = onAccept)
        }
    }
}

/** Avatar with soft rings spreading out from it, like sound waves. */
@Composable
private fun RippleAvatar(call: IncomingCall, size: androidx.compose.ui.unit.Dp) {
    val transition = rememberInfiniteTransition(label = "ripple")
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size * 1.6f)) {
        repeat(2) { i ->
            val progress by transition.animateFloat(
                0f, 1f,
                infiniteRepeatable(tween(2600, easing = LinearEasing), initialStartOffset = StartOffset(i * 1300)),
                label = "ring$i",
            )
            Box(
                Modifier
                    .size(size)
                    .graphicsLayer {
                        val s = 1f + progress * 0.55f
                        scaleX = s; scaleY = s
                        alpha = (1f - progress) * 0.35f
                    }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.4f))
            )
        }
        Avatar(call, size)
    }
}

@Composable
private fun Avatar(call: IncomingCall, size: androidx.compose.ui.unit.Dp, name: String = call.doctorName) {
    val primary = LocalMyColorScheme.current.primaryBlueColor
    val initials = @Composable {
        Box(
            Modifier.fillMaxSize().background(Brush.linearGradient(listOf(primary, Color(0xFF22D3EE)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(initialsOf(name), color = Color.White, fontSize = (size.value * 0.32f).sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Box(Modifier.size(size).clip(CircleShape)) {
        if (call.doctorAvatarUrl.isNullOrBlank()) initials()
        else SubcomposeAsyncImage(
            model = call.doctorAvatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            loading = { initials() },
            error = { initials() },
        )
    }
}

private fun initialsOf(name: String): String =
    // "Dr.YASIR", "Dr. Yasir", "Dr Yasir" → initials of the name itself
    name.replace(Regex("^dr(\\.\\s*|\\s+)", RegexOption.IGNORE_CASE), "")
        .split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
        .ifBlank { "Dr" }

/** A round call button; [description] is read out by TalkBack. */
@Composable
private fun CallButton(
    icon: ImageVector,
    description: String,
    background: Color,
    tint: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 52.dp,
    wiggle: Boolean = false,
    onClick: () -> Unit,
) {
    // The accept button nudges every couple of seconds to draw the eye
    val rotation = remember { Animatable(0f) }
    if (wiggle) LaunchedEffect(Unit) {
        while (true) {
            delay(1400)
            rotation.animateTo(0f, keyframes {
                durationMillis = 600
                -14f at 80; 14f at 200; -10f at 320; 8f at 440; 0f at 600
            })
        }
    }
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 300f)) }
    val bg by animateColorAsState(background, tween(200), label = "callButton")

    Box(
        Modifier
            .size(size)
            .scale(pop.value)
            .graphicsLayer { rotationZ = rotation.value }
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(size * 0.44f))
    }
}

// ── Connecting / ended ───────────────────────────────────────────────────────

@Composable
private fun WaitingContent(call: IncomingCall, label: String) {
    Backdrop(call)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        RippleAvatar(call, size = 140.dp)
        Text(call.doctorName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

@Composable
private fun EndedContent(call: IncomingCall, ended: CallPhase.Ended?, seconds: Long, onClose: () -> Unit) {
    val failed = ended?.failed == true
    Backdrop(call)
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.graphicsLayer { alpha = 0.7f }) { Avatar(call, 120.dp) }
        Spacer(Modifier.height(20.dp))
        Text(call.doctorName, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        if (failed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.ErrorOutline, null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Couldn't connect the call", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            Text(ended?.message.orEmpty(), color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(28.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            ) { Text("Close", color = PanelText, fontSize = 15.sp, fontWeight = FontWeight.Bold) }
        } else {
            Text(
                listOfNotNull(ended?.message, seconds.takeIf { it > 0 }?.let(::formatDuration)).joinToString(" · "),
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp, letterSpacing = 0.5.sp,
            )
        }
    }
}

// ── In call ──────────────────────────────────────────────────────────────────

/**
 * The small picture-in-picture window: just the doctor (or, until they show up, yourself),
 * with mute and hang-up as PiP window buttons instead of on-screen controls.
 */
@Composable
private fun PipContent(vm: CallViewModel, call: IncomingCall) {
    val room = vm.room
    val remote = vm.remoteVideo
    val local = vm.localVideo?.takeIf { vm.cameraOn }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            room != null && remote != null -> VideoView(room, remote, mirror = false, modifier = Modifier.fillMaxSize())
            room != null && local != null && !vm.doctorJoined -> VideoView(room, local, mirror = vm.frontCamera, modifier = Modifier.fillMaxSize())
            else -> {
                Backdrop(call)
                Avatar(call, 56.dp, name = vm.remoteName ?: call.doctorName)
            }
        }
        if (vm.phase is CallPhase.Ended) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)), contentAlignment = Alignment.Center) {
                Text("Call ended", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        } else if (!vm.micOn) {
            Box(
                Modifier.align(Alignment.BottomStart).padding(8.dp).size(24.dp).clip(CircleShape).background(CallRed),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Rounded.MicOff, contentDescription = "Muted", tint = Color.White, modifier = Modifier.size(14.dp)) }
        }
    }
}

/** Height of the bottom panel above the navigation bar; the self view keeps clear of it. */
private val PanelHeight = 168.dp

@Composable
private fun InCallContent(vm: CallViewModel, call: IncomingCall, onMinimize: () -> Unit) {
    val room = vm.room
    var controlsVisible by remember { mutableStateOf(true) }
    var lastTouch by remember { mutableLongStateOf(0L) }
    var showQuality by remember { mutableStateOf(false) }
    var showAudio by remember { mutableStateOf(false) }
    val remote = vm.remoteVideo
    val primary = LocalMyColorScheme.current.primaryBlueColor
    fun touched() { lastTouch = System.currentTimeMillis() }

    if (showQuality) QualitySheet(vm.quality, onPick = vm::changeQuality, onDismiss = { showQuality = false })
    if (showAudio) AudioSheet(vm.audioDevices, vm.audioDevice, onPick = vm::selectAudioDevice, onDismiss = { showAudio = false })

    // With the doctor on video, tuck the controls away after a few quiet seconds
    LaunchedEffect(controlsVisible, lastTouch, remote, showQuality, showAudio) {
        if (controlsVisible && remote != null && !showQuality && !showAudio) {
            delay(5_000)
            controlsVisible = false
        }
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                controlsVisible = !controlsVisible
                touched()
            }
    ) {
        // Doctor
        if (room != null && remote != null) {
            VideoView(room, remote, mirror = false, modifier = Modifier.fillMaxSize())
        } else {
            Backdrop(call)
            Column(
                Modifier.fillMaxSize().padding(bottom = PanelHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                RippleAvatar(call, size = 140.dp)
                Text(
                    if (vm.doctorJoined) "Camera is off" else "Waiting for the doctor to join…",
                    color = Color.White, fontSize = 14.sp,
                )
            }
        }

        // Top: back (keeps the call going in a small window), connection and quality
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.28f))
                        .clickable(onClick = onMinimize),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Minimise call", tint = Color.White,
                        modifier = Modifier.size(18.dp).offset(x = 3.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.35f))
                        .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SignalBars(vm.myConnection)
                    Spacer(Modifier.width(8.dp))
                    QualityPill(vm.quality) { showQuality = true; touched() }
                }
            }
        }

        // Weak link warning stays up even with the controls hidden
        val weak = connectionWarning(vm)
        AnimatedVisibility(
            visible = weak != null && !vm.reconnecting,
            enter = fadeIn() + scaleIn(initialScale = 0.9f),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp),
        ) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SignalBars(ConnectionQuality.POOR)
                Spacer(Modifier.width(8.dp))
                Text(weak.orEmpty(), color = Color.White, fontSize = 13.sp)
                if (vm.quality != CallQuality.DATA_SAVER && weak == WEAK_MINE) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Use data saver", color = Color(0xFF7FD1FF), fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { vm.changeQuality(CallQuality.DATA_SAVER) },
                    )
                }
            }
        }

        // Me, small and draggable
        SelfView(vm, maxWidthPx = constraints.maxWidth.toFloat(), maxHeightPx = constraints.maxHeight.toFloat())

        // Bottom panel: who, how long, and the controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    // Taps on the panel shouldn't fall through and hide it
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { touched() }
                    .navigationBarsPadding()
                    .padding(top = 20.dp, bottom = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    vm.remoteName ?: call.doctorName, color = PanelText, fontSize = 20.sp,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
                Spacer(Modifier.height(4.dp))
                if (vm.reconnecting) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LiveDot(Warn)
                        Spacer(Modifier.width(6.dp))
                        Text("Reconnecting…", color = Color(0xFFB26A00), fontSize = 14.sp)
                    }
                } else CallTimer(vm)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    // On: solid blue. Off: pale blue with the crossed-out icon, so the state reads at a glance.
                    val off = primary.copy(alpha = 0.12f)
                    CallButton(Icons.Rounded.CallEnd, "Hang up", HangUpRed, onClick = vm::hangUp)
                    CallButton(
                        if (vm.micOn) Icons.Rounded.Mic else Icons.Rounded.MicOff,
                        if (vm.micOn) "Mute" else "Unmute",
                        if (vm.micOn) primary else off, tint = if (vm.micOn) Color.White else primary,
                    ) { vm.toggleMic(); touched() }
                    CallButton(
                        if (vm.cameraOn) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                        if (vm.cameraOn) "Turn camera off" else "Turn camera on",
                        if (vm.cameraOn) primary else off, tint = if (vm.cameraOn) Color.White else primary,
                    ) { vm.toggleCamera(); touched() }
                    CallButton(Icons.Rounded.Cameraswitch, "Switch camera", primary) { vm.flipCamera(); touched() }
                    // Just speaker and earpiece: one tap flips between them. With a headset around, pick from a list.
                    val earpiece = vm.audioDevice is AudioDevice.Earpiece
                    CallButton(
                        audioIcon(vm.audioDevice), "Audio output",
                        if (earpiece) off else primary, tint = if (earpiece) primary else Color.White,
                    ) {
                        touched()
                        val others = vm.audioDevices.filter { it != vm.audioDevice }
                        if (vm.audioDevices.size <= 2 && others.size == 1) vm.selectAudioDevice(others.first())
                        else showAudio = true
                    }
                }
            }
        }
    }
}

/** Own camera in a corner above the panel; drag it anywhere, it settles against the nearest side. */
@Composable
private fun SelfView(vm: CallViewModel, maxWidthPx: Float, maxHeightPx: Float) {
    val density = LocalDensity.current
    val w = 104.dp; val h = 140.dp; val margin = 16.dp
    val wPx = with(density) { w.toPx() }
    val hPx = with(density) { h.toPx() }
    val mPx = with(density) { margin.toPx() }
    val topPx = with(density) { 72.dp.toPx() }
    val navPx = WindowInsets.navigationBars.getBottom(density).toFloat()
    val bottomLimit = (maxHeightPx - hPx - navPx - with(density) { (PanelHeight + margin).toPx() }).coerceAtLeast(topPx)

    val x = remember { Animatable(maxWidthPx - wPx - mPx) }
    val y = remember { Animatable(bottomLimit) }
    val scope = rememberCoroutineScope()
    fun settle() = scope.launch {
        val nearSide = if (x.value + wPx / 2 < maxWidthPx / 2) mPx else maxWidthPx - wPx - mPx
        launch { x.animateTo(nearSide, spring(dampingRatio = 0.75f, stiffness = 400f)) }
        launch { y.animateTo(y.value.coerceIn(topPx, bottomLimit), spring(dampingRatio = 0.75f, stiffness = 400f)) }
    }

    val room = vm.room
    val track = vm.localVideo
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 260f)) }

    Box(
        Modifier
            .offset { IntOffset(x.value.roundToInt(), y.value.roundToInt()) }
            .size(w, h)
            .scale(enter.value)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1B2333))
            .pointerInput(Unit) {
                detectDragGestures(onDragEnd = { settle() }, onDragCancel = { settle() }) { change, amount ->
                    change.consume()
                    scope.launch {
                        x.snapTo((x.value + amount.x).coerceIn(0f, maxWidthPx - wPx))
                        y.snapTo((y.value + amount.y).coerceIn(0f, maxHeightPx - hPx))
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (room != null && track != null && vm.cameraOn) {
            VideoView(room, track, mirror = vm.frontCamera, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Rounded.VideocamOff, contentDescription = "Camera off", tint = Color.White.copy(alpha = 0.6f))
        }
    }
}

/** A LiveKit video track drawn into a TextureView (so it can sit under and over other UI). */
@Composable
private fun VideoView(room: Room, track: VideoTrack, mirror: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextureViewRenderer(context).apply {
                room.initVideoRenderer(this)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
            }
        },
        update = { view ->
            view.setMirror(mirror)
            val current = view.tag as? VideoTrack
            if (current !== track) {
                current?.removeRenderer(view)
                track.addRenderer(view)
                view.tag = track
            }
        },
        onRelease = { view ->
            (view.tag as? VideoTrack)?.removeRenderer(view)
            view.release()
        },
    )
}

@Composable
private fun LiveDot(color: Color) {
    val pulse by rememberInfiniteTransition(label = "live").animateFloat(
        0.35f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "liveAlpha",
    )
    Box(Modifier.size(8.dp).graphicsLayer { alpha = pulse }.clip(CircleShape).background(color))
}

@Composable
private fun CallTimer(vm: CallViewModel) {
    var seconds by remember { mutableLongStateOf(vm.elapsedSeconds()) }
    LaunchedEffect(Unit) {
        while (true) {
            seconds = vm.elapsedSeconds()
            delay(1_000)
        }
    }
    Text(formatDuration(seconds), color = PanelGrey, fontSize = 15.sp, letterSpacing = 1.5.sp)
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600; val m = (seconds % 3600) / 60; val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

// ── Connection & quality ─────────────────────────────────────────────────────

private val Warn = Color(0xFFFFB74D)
private const val WEAK_MINE = "Your connection is weak"
private const val WEAK_DOCTOR = "Doctor's connection is weak"

/** What to warn about, if anything: this phone's link first, then the doctor's. */
private fun connectionWarning(vm: CallViewModel): String? = when {
    vm.myConnection == ConnectionQuality.POOR || vm.myConnection == ConnectionQuality.LOST -> WEAK_MINE
    vm.doctorJoined && (vm.doctorConnection == ConnectionQuality.POOR || vm.doctorConnection == ConnectionQuality.LOST) -> WEAK_DOCTOR
    else -> null
}

/** Phone-style signal bars: 4 excellent, 3 good, 1 poor, none lost; dim while unknown. */
@Composable
private fun SignalBars(quality: ConnectionQuality) {
    val (lit, color) = when (quality) {
        ConnectionQuality.EXCELLENT -> 4 to DeviceGreen
        ConnectionQuality.GOOD -> 3 to DeviceGreen
        ConnectionQuality.POOR -> 1 to Warn
        ConnectionQuality.LOST -> 0 to CallRed
        else -> 0 to Color.White
    }
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(4) { i ->
            val target = if (i < lit) color else Color.White.copy(alpha = 0.28f)
            val barColor by animateColorAsState(target, tween(400), label = "bar$i")
            Box(Modifier.width(3.dp).height((5 + i * 3).dp).clip(RoundedCornerShape(1.dp)).background(barColor))
        }
    }
}

@Composable
private fun QualityPill(quality: CallQuality, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.16f))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 3.dp, bottom = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(quality.label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Rounded.ExpandMore, contentDescription = "Video quality", tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QualitySheet(current: CallQuality, onPick: (CallQuality) -> Unit, onDismiss: () -> Unit) {
    val primary = LocalMyColorScheme.current.primaryBlueColor
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141B2B),
        contentColor = Color.White,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.3f))) },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
            Text("Video quality", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Applies to your camera and the doctor's video", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))
            CallQuality.entries.forEach { option ->
                val selected = option == current
                val bg by animateColorAsState(if (selected) primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f), label = "qbg")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { onPick(option); onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (selected) primary else Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            when (option) {
                                CallQuality.AUTO -> Icons.Rounded.AutoMode
                                CallQuality.DATA_SAVER -> Icons.Rounded.DataSaverOn
                                CallQuality.HD -> Icons.Rounded.Hd
                            },
                            contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp),
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(option.description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    }
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color(0xFF7FD1FF))
                }
            }
        }
    }
}

// ── Audio output ─────────────────────────────────────────────────────────────

private fun audioIcon(device: AudioDevice?): ImageVector = when (device) {
    is AudioDevice.BluetoothHeadset -> Icons.Rounded.BluetoothAudio
    is AudioDevice.WiredHeadset -> Icons.Rounded.Headphones
    is AudioDevice.Earpiece -> Icons.Rounded.PhoneInTalk
    else -> Icons.Rounded.VolumeUp
}

private fun audioLabel(device: AudioDevice): Pair<String, String> = when (device) {
    is AudioDevice.BluetoothHeadset -> "Bluetooth" to device.name.takeUnless { it.isBlank() || it == "Bluetooth" }.orEmpty().ifBlank { "Wireless headset" }
    is AudioDevice.WiredHeadset -> "Headphones" to "Wired headset"
    is AudioDevice.Earpiece -> "Phone" to "Hold the phone to your ear"
    else -> "Speaker" to "Loudspeaker"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AudioSheet(devices: List<AudioDevice>, current: AudioDevice?, onPick: (AudioDevice) -> Unit, onDismiss: () -> Unit) {
    val primary = LocalMyColorScheme.current.primaryBlueColor
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF141B2B),
        contentColor = Color.White,
        dragHandle = { Box(Modifier.padding(top = 10.dp).size(36.dp, 4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.3f))) },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 20.dp)) {
            Text("Audio", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("Where you hear the doctor", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
            Spacer(Modifier.height(14.dp))
            devices.forEach { device ->
                val selected = device == current
                val (title, subtitle) = audioLabel(device)
                val bg by animateColorAsState(if (selected) primary.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.05f), label = "abg")
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .clickable { onPick(device); onDismiss() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(if (selected) primary else Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center,
                    ) { Icon(audioIcon(device), contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(subtitle, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = Color(0xFF7FD1FF))
                }
            }
        }
    }
}
