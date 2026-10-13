package com.critetiontech.ctvitalio.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Holds all dimension values that change based on screen size.
 * Use [rememberResponsiveDimens] to obtain the correct instance.
 */
@Immutable
data class ResponsiveDimens(
    /** Horizontal padding for screen-level content */
    val screenPadding: Dp,
    /** Spacing between cards / grid items */
    val cardSpacing: Dp,
    /** Number of columns for grid layouts */
    val gridColumns: Int,
    /** Maximum width for content containers (Dp.Infinity = no limit) */
    val maxContentWidth: Dp,
    /** Corner radius for cards */
    val cardCornerRadius: Dp,
    /** Spacing between major sections */
    val sectionSpacing: Dp,
    /** Inner card padding */
    val cardInnerPadding: Dp,
)

private val CompactDimens = ResponsiveDimens(
    screenPadding = 16.dp,
    cardSpacing = 8.dp,
    gridColumns = 1,
    maxContentWidth = Dp.Infinity,
    cardCornerRadius = 16.dp,
    sectionSpacing = 12.dp,
    cardInnerPadding = 12.dp,
)

private val MediumDimens = ResponsiveDimens(
    screenPadding = 24.dp,
    cardSpacing = 12.dp,
    gridColumns = 2,
    maxContentWidth = 840.dp,
    cardCornerRadius = 16.dp,
    sectionSpacing = 16.dp,
    cardInnerPadding = 16.dp,
)

private val ExpandedDimens = ResponsiveDimens(
    screenPadding = 32.dp,
    cardSpacing = 16.dp,
    gridColumns = 3,
    maxContentWidth = 1040.dp,
    cardCornerRadius = 16.dp,
    sectionSpacing = 20.dp,
    cardInnerPadding = 20.dp,
)

/**
 * Returns responsive dimensions based on current window size class.
 * The result is remembered and only recomputed when the window size class changes.
 */
@Composable
fun rememberResponsiveDimens(): ResponsiveDimens {
    val windowSize = LocalWindowSizeClass.current
    return remember(windowSize) {
        when (windowSize) {
            WindowWidthSizeClass.Compact  -> CompactDimens
            WindowWidthSizeClass.Medium   -> MediumDimens
            WindowWidthSizeClass.Expanded -> ExpandedDimens
        }
    }
}
