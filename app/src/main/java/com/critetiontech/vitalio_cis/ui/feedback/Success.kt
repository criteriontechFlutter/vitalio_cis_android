package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A tick that draws itself stroke by stroke when [checked] turns true and erases when it turns
 * false. Starts in its final state, so items that were already done don't animate on load.
 */
@Composable
fun AnimatedCheckMark(
    checked: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    strokeWidth: Dp = 2.2.dp,
) {
    val progress = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        progress.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = tween(if (checked) AppMotion.MEDIUM else AppMotion.SHORT, easing = FastOutSlowInEasing),
        )
    }
    val path = remember { Path() }
    val segment = remember { Path() }
    val measure = remember { PathMeasure() }

    Canvas(modifier) {
        if (progress.value <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        path.reset()
        path.moveTo(w * 0.20f, h * 0.52f)
        path.lineTo(w * 0.42f, h * 0.72f)
        path.lineTo(w * 0.80f, h * 0.30f)
        measure.setPath(path, false)
        segment.reset()
        measure.getSegment(0f, measure.length * progress.value, segment, true)
        drawPath(
            segment,
            color = color,
            style = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

/**
 * A ring that grows out and fades behind the element each time [done] turns true, e.g. when a
 * dose is marked taken. Silent on first composition.
 */
fun Modifier.successPulse(done: Boolean, color: Color): Modifier = composed {
    val ring = remember { Animatable(1f) }
    // Remembers the last value so only a false -> true change pulses.
    val wasDone = remember { mutableStateOf(done) }
    LaunchedEffect(done) {
        if (done && !wasDone.value) {
            ring.snapTo(0f)
            launch { ring.animateTo(1f, tween(AppMotion.LONG + 150, easing = FastOutSlowInEasing)) }
        }
        wasDone.value = done
    }
    drawBehind {
        val p = ring.value
        if (p >= 1f) return@drawBehind
        val base = size.minDimension / 2f
        drawCircle(color = color.copy(alpha = 0.35f * (1f - p)), radius = base * (0.6f + 0.6f * p))
    }
}

/**
 * Swaps between states with a springy pop instead of a hard cut, e.g. a "Take now" chip
 * turning into "Taken". Use a stable key (an enum or class) as [targetState].
 *
 * [content] gets `swappedIn = true` when it arrived through a state change rather than being
 * on screen from the start, so it can play its own entrance (like drawing a tick) only then.
 */
@Composable
fun <T> PopSwap(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (state: T, swappedIn: Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = targetState,
        modifier = modifier,
        contentAlignment = Alignment.Center,
        transitionSpec = {
            (scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow), initialScale = 0.6f) +
                fadeIn(tween(AppMotion.SHORT))) togetherWith
                (scaleOut(tween(AppMotion.SHORT), targetScale = 0.8f) + fadeOut(tween(AppMotion.SHORT / 2)))
        },
        label = "popSwap",
    ) { state ->
        // The first content starts already Visible; anything swapped in starts at PreEnter.
        val swappedIn = remember { transition.currentState == EnterExitState.PreEnter }
        content(state, swappedIn)
    }
}
