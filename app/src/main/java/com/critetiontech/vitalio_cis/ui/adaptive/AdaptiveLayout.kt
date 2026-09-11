package com.critetiontech.vitalio_cis.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Window Size Provider ─────────────────────────────────────────────────────

/**
 * Reads the current screen width from [LocalConfiguration] and provides
 * the corresponding [WindowWidthSizeClass] to the composition tree.
 *
 * Wrap your root composable (or the NavHost) with this to enable
 * adaptive layouts throughout the app.
 */
@Composable
fun AdaptiveLayoutProvider(
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    val windowSizeClass = when {
        screenWidthDp < 600  -> WindowWidthSizeClass.Compact
        screenWidthDp < 840  -> WindowWidthSizeClass.Medium
        else                 -> WindowWidthSizeClass.Expanded
    }

    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

// ─── Content Container ────────────────────────────────────────────────────────

/**
 * Constrains content to a responsive max width and centers it horizontally.
 * On phones the content stretches to full width; on tablets it is capped.
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val dimens = rememberResponsiveDimens()

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        val innerModifier = if (dimens.maxContentWidth != Dp.Infinity) {
            Modifier
                .widthIn(max = dimens.maxContentWidth)
                .fillMaxWidth()
        } else {
            Modifier.fillMaxWidth()
        }
        Box(modifier = innerModifier) {
            content()
        }
    }
}

// ─── Adaptive Padding ─────────────────────────────────────────────────────────

/**
 * Applies horizontal padding that scales with screen size:
 * 16dp on phone, 24dp on medium tablet, 32dp on large tablet.
 */
@Composable
fun Modifier.adaptivePadding(): Modifier {
    val dimens = rememberResponsiveDimens()
    return this.padding(horizontal = dimens.screenPadding)
}

// ─── Two-Pane Layout ──────────────────────────────────────────────────────────

/**
 * On phones, stacks [firstPane] above [secondPane] vertically.
 * On tablets, places them side by side with configurable weight ratio.
 */
@Composable
fun AdaptiveTwoPane(
    modifier: Modifier = Modifier,
    firstPaneWeight: Float = 0.4f,
    secondPaneWeight: Float = 0.6f,
    spacing: Dp = 16.dp,
    firstPane: @Composable () -> Unit,
    secondPane: @Composable () -> Unit
) {
    val windowSize = LocalWindowSizeClass.current

    if (windowSize == WindowWidthSizeClass.Compact) {
        Column(modifier = modifier) {
            firstPane()
            Spacer(modifier = Modifier.height(spacing))
            secondPane()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            Box(modifier = Modifier.weight(firstPaneWeight)) {
                firstPane()
            }
            Box(modifier = Modifier.weight(secondPaneWeight)) {
                secondPane()
            }
        }
    }
}

// ─── Convenience Helpers ──────────────────────────────────────────────────────

/** Returns `true` when the screen is tablet-sized (≥ 600dp width). */
@Composable
fun isTablet(): Boolean {
    return LocalWindowSizeClass.current != WindowWidthSizeClass.Compact
}

/** Returns `true` only on large tablets (≥ 840dp width). */
@Composable
fun isExpandedTablet(): Boolean {
    return LocalWindowSizeClass.current == WindowWidthSizeClass.Expanded
}
