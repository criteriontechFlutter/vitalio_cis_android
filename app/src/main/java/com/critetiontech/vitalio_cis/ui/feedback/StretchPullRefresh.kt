package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

/**
 * How far content slides down when the pull reaches the refresh threshold. Tall enough for the
 * waving care-team illustration above the label.
 */
val PullStretchHeight = 200.dp

/** The art every pull-to-refresh shows above its label. */
val DefaultRefreshIllustration: @Composable () -> Unit = {
    com.critetiontech.ctvitalio.ui.components.DoctorsWaveAnimation(
        Modifier.height(130.dp).aspectRatio(405f / 240f)
    )
}

/** How far the pull has moved the content, for screens that lay out [PullStretchHeader] themselves. */
@OptIn(ExperimentalMaterial3Api::class)
fun PullToRefreshState.stretchOffsetPx(
    density: androidx.compose.ui.unit.Density,
    stretchHeight: Dp = PullStretchHeight,
): Float = with(density) { (stretchHeight * distanceFraction.coerceIn(0f, 1.6f)).toPx() }

/**
 * Haptics for a pull-to-refresh: a tick when the pull crosses "release to refresh" and a
 * success buzz when a refresh finishes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullRefreshHaptics(state: PullToRefreshState, isRefreshing: Boolean) {
    val haptic = rememberHaptics()
    val refreshing by rememberUpdatedState(isRefreshing)
    LaunchedEffect(state) {
        snapshotFlow { state.distanceFraction >= 1f }
            .distinctUntilChanged()
            .drop(1)
            .collect { armed -> if (armed && !isRefreshingNow(state)) haptic(HapticEvent.Threshold) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { refreshing }.distinctUntilChanged().drop(1).collect { nowRefreshing ->
            if (!nowRefreshing) haptic(HapticEvent.Success)
        }
    }
}

// The spinner holds distanceFraction at 1 while refreshing; only user drags should tick.
@OptIn(ExperimentalMaterial3Api::class)
private fun isRefreshingNow(state: PullToRefreshState) = state.isAnimating

/**
 * Pull-to-refresh with the app's stretch look: a primary-colored area grows from the top as
 * content is pulled down. Use it in place of `PullToRefreshBox`.
 *
 * @param behindStatusBar draw the colored area up through the status bar, for screens without
 * an app bar (the dashboard). Leave false under a top app bar.
 * @param illustration optional art shown above the label (it plays while pulling and
 * refreshing); give a taller [stretchHeight] to make room for it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StretchPullRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    behindStatusBar: Boolean = false,
    state: PullToRefreshState = rememberPullToRefreshState(),
    refreshingText: String = "Updating your health data…",
    stretchHeight: Dp = PullStretchHeight,
    illustration: (@Composable () -> Unit)? = DefaultRefreshIllustration,
    content: @Composable BoxScope.() -> Unit,
) {
    val density = LocalDensity.current
    PullRefreshHaptics(state, isRefreshing)
    Box(
        modifier = modifier.pullToRefresh(
            isRefreshing = isRefreshing,
            state = state,
            enabled = enabled,
            onRefresh = onRefresh,
        )
    ) {
        Box(Modifier.graphicsLayer { translationY = state.stretchOffsetPx(density, stretchHeight) }) {
            content()
        }
        PullStretchHeader(
            state = state,
            isRefreshing = isRefreshing,
            behindStatusBar = behindStatusBar,
            refreshingText = refreshingText,
            stretchHeight = stretchHeight,
            illustration = illustration,
        )
    }
}

/**
 * The colored area itself: an arrow that flips at the threshold, then a spinner while
 * refreshing. Draw it at the top of the container whose content is offset by [stretchOffsetPx].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullStretchHeader(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    color: Color = LocalMyColorScheme.current.primaryBlueColor,
    behindStatusBar: Boolean = false,
    refreshingText: String = "Updating your health data…",
    stretchHeight: Dp = PullStretchHeight,
    illustration: (@Composable () -> Unit)? = DefaultRefreshIllustration,
) {
    val fraction = state.distanceFraction.coerceIn(0f, 1.6f)
    if (fraction <= 0f && !isRefreshing) return

    val topInset: Dp = if (behindStatusBar) WindowInsets.statusBars.asPaddingValues().calculateTopPadding() else 0.dp
    val pastThreshold = fraction >= 1f
    val arrowRotation by animateFloatAsState(
        targetValue = if (pastThreshold) 180f else 0f,
        animationSpec = tween(AppMotion.SHORT),
        label = "pullArrow",
    )
    val corner = 24.dp * fraction.coerceAtMost(1f)
    val label by remember(isRefreshing) {
        derivedStateOf {
            when {
                isRefreshing -> refreshingText
                state.distanceFraction >= 1f -> "Release to refresh"
                else -> "Pull to refresh"
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(topInset + stretchHeight * fraction)
            .graphicsLayer { alpha = (fraction * 3f).coerceIn(0f, 1f) }
            .clip(RoundedCornerShape(bottomStart = corner, bottomEnd = corner))
            .background(Brush.verticalGradient(listOf(color, color.copy(alpha = 0.85f).compositeOver(Color(0xFF0B3FA8))))),
        contentAlignment = Alignment.BottomCenter,
    ) {
        androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (illustration != null) {
            Box(
                Modifier
                    .padding(bottom = 4.dp)
                    .graphicsLayer {
                        alpha = ((fraction - 0.2f) / 0.5f).coerceIn(0f, 1f)
                        val scale = 0.7f + 0.3f * fraction.coerceAtMost(1f)
                        scaleX = scale; scaleY = scale
                        transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 1f)
                    }
            ) { illustration() }
        }
        Row(
            modifier = Modifier
                .padding(bottom = 14.dp)
                .graphicsLayer {
                    alpha = ((fraction - 0.35f) / 0.4f).coerceIn(0f, 1f)
                    val scale = 0.8f + 0.2f * fraction.coerceAtMost(1f)
                    scaleX = scale; scaleY = scale
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                if (isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = Icons.Default.ArrowDownward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp).rotate(arrowRotation),
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        }
    }
}
