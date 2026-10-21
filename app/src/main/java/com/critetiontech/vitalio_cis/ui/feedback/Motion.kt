package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.Crossfade
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ripple
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry

/** Shared timings so motion feels the same on every screen. */
object AppMotion {
    const val SHORT = 200
    const val MEDIUM = 350
    const val LONG = 450
    /** Gap between items of a staggered list entrance. */
    const val STAGGER = 50
    /** Only this many leading items stagger, so long lists don't feel slow. */
    const val MAX_STAGGER_ITEMS = 8
}

/** Shrinks slightly while [interactionSource] is pressed and springs back on release. */
fun Modifier.pressScale(interactionSource: InteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
}

/**
 * `clickable` for cards and tiles: press-scale bounce, ripple and tap haptic (from the app's
 * LocalIndication) in one modifier.
 */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.96f,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    this
        .pressScale(interactionSource, pressedScale)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            enabled = enabled,
            onClick = onClick,
        )
}

/**
 * Fades and slides [content] up once when it first appears. Pass the item's position as
 * [index] for a staggered list entrance. Runs once per screen visit, not on every scroll back.
 */
@Composable
fun AnimatedEnter(
    modifier: Modifier = Modifier,
    index: Int = 0,
    // Only the first screenful staggers; items scrolled into view later appear without delay.
    delayMillis: Int = if (index < AppMotion.MAX_STAGGER_ITEMS) index * AppMotion.STAGGER else 0,
    content: @Composable () -> Unit,
) {
    var shown by rememberSaveable { mutableStateOf(false) }
    var visible by remember { mutableStateOf(shown) }
    LaunchedEffect(Unit) { visible = true; shown = true }
    val progress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(AppMotion.LONG, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "enter",
    )
    // Column so an item can be a card plus its divider or spacer.
    Column(modifier.graphicsLayer { alpha = progress; translationY = (1f - progress) * 24.dp.toPx() }) {
        content()
    }
}

/** Screen-to-screen transitions for the NavHost: push slides in from the right, pop reverses it. */
object AppNavTransitions {
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(AppMotion.MEDIUM, easing = FastOutSlowInEasing)) { it / 4 } +
            fadeIn(tween(AppMotion.MEDIUM))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(AppMotion.MEDIUM, easing = FastOutSlowInEasing)) { -it / 8 } +
            fadeOut(tween(AppMotion.SHORT))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideInHorizontally(tween(AppMotion.MEDIUM, easing = FastOutSlowInEasing)) { -it / 8 } +
            fadeIn(tween(AppMotion.MEDIUM))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutHorizontally(tween(AppMotion.MEDIUM, easing = FastOutSlowInEasing)) { it / 4 } +
            fadeOut(tween(AppMotion.SHORT))
    }
}

/** What a data-backed section is showing. */
enum class ContentState { Loading, Empty, Content }

/** Shimmer while the first load runs, the empty message when there's nothing, data otherwise. */
fun contentStateOf(loading: Boolean, empty: Boolean): ContentState = when {
    loading && empty -> ContentState.Loading
    empty -> ContentState.Empty
    else -> ContentState.Content
}

/**
 * Crossfades between a section's shimmer, empty state and content so data fades in over the
 * placeholder instead of popping in.
 */
@Composable
fun ContentStateCrossfade(
    state: ContentState,
    modifier: Modifier = Modifier,
    loading: @Composable () -> Unit,
    empty: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Crossfade(
        targetState = state,
        modifier = modifier,
        animationSpec = tween(AppMotion.MEDIUM),
        label = "contentState",
    ) { shown ->
        // Column, not Crossfade's Box, so a state made of several items stacks instead of overlapping.
        Column {
            when (shown) {
                ContentState.Loading -> loading()
                ContentState.Empty -> empty()
                ContentState.Content -> content()
            }
        }
    }
}

/**
 * Tap and long-press on one element, e.g. long-press a card to open its menu. Long-press gets
 * the heavier LongPress haptic; the tap haptic is played here instead of by LocalIndication so
 * lifting the finger after a long-press doesn't buzz a second time.
 */
fun Modifier.hapticCombinedClick(
    onClick: (() -> Unit)?,
    onLongClick: () -> Unit,
    enabled: Boolean = true,
): Modifier = composed {
    val haptic = rememberHaptics()
    combinedClickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = ripple(),
        enabled = enabled,
        onLongClick = { haptic(HapticEvent.LongPress); onLongClick() },
        onClick = { if (onClick != null) { haptic(HapticEvent.Tap); onClick() } },
    )
}
