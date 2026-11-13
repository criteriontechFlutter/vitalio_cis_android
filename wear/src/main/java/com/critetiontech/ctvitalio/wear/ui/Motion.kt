package com.critetiontech.ctvitalio.wear.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material3.MaterialTheme
import kotlin.math.PI
import kotlin.math.sin

/** Material "emphasized decelerate" — things arriving on screen. */
internal val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** Circular, press-to-shrink touch target used for custom round buttons. */
@Composable
internal fun PressableCircle(
    onClick: () -> Unit,
    size: Dp,
    color: Color,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    brush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .then(if (brush != null) Modifier.background(brush) else Modifier.background(color))
            .clickable(interactionSource = interaction, indication = null, role = Role.Button, onClick = onClick)
            .then(
                if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription }
                else Modifier
            ),
        content = content
    )
}

/** Two soft rings that slowly breathe out of the mic button — "ready to listen". Draws past its bounds. */
@Composable
internal fun BreathingRings(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "breath")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "breathPhase")
    Canvas(modifier) {
        val base = size.minDimension / 2f
        for (offset in floatArrayOf(0f, 0.5f)) {
            val f = (p + offset) % 1f
            drawCircle(color = color.copy(alpha = 0.30f * (1f - f)), radius = base * (1f + 0.42f * f))
        }
    }
}

/**
 * Voice-reactive bars. [level] is the live mic loudness (0..1); bars idle gently
 * when the user is quiet so the screen never looks frozen.
 */
@Composable
internal fun VoiceWaveform(level: Float, active: Boolean, modifier: Modifier = Modifier) {
    val smooth by animateFloatAsState(
        targetValue = if (active) level else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "level"
    )
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "wavePhase")
    val color = MaterialTheme.colorScheme.primary
    val weights = floatArrayOf(0.45f, 0.7f, 0.9f, 1f, 0.9f, 0.7f, 0.45f)
    Row(
        modifier = modifier.height(44.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        weights.forEachIndexed { i, w ->
            val wobble = 0.5f + 0.5f * sin((phase * 2f * PI + i * 0.9).toFloat())
            val idle = if (active) 0.12f + 0.08f * wobble else 0.1f
            val h = (idle + smooth * w * (0.55f + 0.45f * wobble)).coerceIn(0.1f, 1f)
            Box(
                Modifier
                    .width(5.dp)
                    .height(44.dp * h)
                    .clip(RoundedCornerShape(50))
                    .background(color.copy(alpha = 0.55f + 0.45f * h))
            )
        }
    }
}

/** Thin ring hugging the round display edge; glows brighter as the user speaks. */
@Composable
internal fun EdgeGlow(level: Float, color: Color, modifier: Modifier = Modifier) {
    val smooth by animateFloatAsState(level, spring(stiffness = Spring.StiffnessLow), label = "glow")
    Canvas(modifier.fillMaxSize()) {
        val stroke = 3.dp.toPx() + 3.dp.toPx() * smooth
        drawCircle(
            color = color.copy(alpha = 0.18f + 0.55f * smooth),
            radius = size.minDimension / 2f - stroke / 2f - 1.dp.toPx(),
            style = Stroke(width = stroke)
        )
    }
}

/** Indeterminate arc sweeping around the screen edge — "working on it". */
@Composable
internal fun EdgeSpinner(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "edgeSpin")
    val rotation by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "rot")
    val sweep by t.animateFloat(
        30f, 110f,
        infiniteRepeatable(tween(700, easing = EmphasizedDecelerate), RepeatMode.Reverse),
        label = "sweep"
    )
    Canvas(modifier.fillMaxSize()) {
        val w = 4.dp.toPx()
        val inset = w / 2f + 2.dp.toPx()
        drawCircle(color.copy(alpha = 0.12f), radius = size.minDimension / 2f - inset, style = Stroke(w))
        drawArc(
            color = color,
            startAngle = rotation - 90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(w, cap = StrokeCap.Round)
        )
    }
}

/** Countdown arc around the edge showing when a transient screen will dismiss itself. */
@Composable
internal fun EdgeCountdown(durationMs: Int, color: Color, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(Unit) { progress.animateTo(0f, tween(durationMs, easing = LinearEasing)) }
    Canvas(modifier.fillMaxSize()) {
        val w = 3.dp.toPx()
        val inset = w / 2f + 2.dp.toPx()
        drawArc(
            color = color.copy(alpha = 0.5f),
            startAngle = -90f,
            sweepAngle = 360f * progress.value,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
            size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
            style = Stroke(w, cap = StrokeCap.Round)
        )
    }
}

/** Three dots rising in sequence. */
@Composable
internal fun TypingDots(color: Color, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "dots")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1050, easing = LinearEasing)), label = "dotPhase")
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val f = ((p - i * 0.18f) % 1f + 1f) % 1f
            val bump = if (f < 0.4f) sin(f / 0.4f * PI).toFloat() else 0f
            Box(
                Modifier
                    .graphicsLayer { translationY = -bump * 6.dp.toPx() }
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.45f + 0.55f * bump))
            )
        }
    }
}

/** Filled disc that springs in, then a checkmark that draws itself. */
@Composable
internal fun AnimatedCheck(color: Color, onColor: Color, size: Dp, modifier: Modifier = Modifier) {
    val disc = remember { Animatable(0f) }
    val stroke = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        disc.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(140)
        stroke.animateTo(1f, tween(380, easing = EmphasizedDecelerate))
    }
    Canvas(modifier.size(size)) {
        drawCircle(color, radius = this.size.minDimension / 2f * disc.value)
        val w = this.size.width
        val path = Path().apply {
            moveTo(w * 0.28f, w * 0.52f)
            lineTo(w * 0.44f, w * 0.67f)
            lineTo(w * 0.73f, w * 0.37f)
        }
        val measure = PathMeasure().apply { setPath(path, false) }
        val seg = Path()
        measure.getSegment(0f, measure.length * stroke.value, seg, true)
        drawPath(seg, onColor, style = Stroke(w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
