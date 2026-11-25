package com.critetiontech.ctvitalio.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import com.critetiontech.ctvitalio.ui.feedback.rememberOnScreen
import com.critetiontech.ctvitalio.ui.feedback.trackOnScreen
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

/**
 * Slowly turning blue → cyan → green ring round a profile photo, like a story ring.
 * [content] (the photo) is drawn inside, inset by [ringWidth] + [gap].
 */
@Composable
fun ProfileRing(
    size: Dp,
    modifier: Modifier = Modifier,
    ringWidth: Dp = 2.dp,
    gap: Dp = 2.dp,
    gapColor: Color = LocalMyColorScheme.current.dashboardBackgroundColor,
    content: @Composable () -> Unit,
) {
    val primary = LocalMyColorScheme.current.primaryBlueColor
    // Turns only while on screen; scrolled away it holds still instead of redrawing every frame
    val onScreen = rememberOnScreen()
    val angle = if (onScreen.visible) {
        val turning by rememberInfiniteTransition(label = "profileRing")
            .animateFloat(0f, 360f, infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "ringAngle")
        turning
    } else 0f
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(size).trackOnScreen(onScreen)) {
        Box(
            Modifier.size(size).graphicsLayer { rotationZ = angle }.clip(CircleShape)
                .background(Brush.sweepGradient(listOf(primary, Color(0xFF22D3EE), Color(0xFF34D399), primary)))
        )
        Box(Modifier.size(size - ringWidth * 2).clip(CircleShape).background(gapColor))
        Box(Modifier.size(size - (ringWidth + gap) * 2).clip(CircleShape), contentAlignment = Alignment.Center) { content() }
    }
}
