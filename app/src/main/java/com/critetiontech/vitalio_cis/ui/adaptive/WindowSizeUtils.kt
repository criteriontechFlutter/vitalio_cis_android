package com.critetiontech.ctvitalio.ui.adaptive

import androidx.compose.runtime.compositionLocalOf

/**
 * Custom window width size class following Material 3 breakpoints.
 *
 * - [Compact]  : < 600dp — phones (portrait)
 * - [Medium]   : 600–840dp — small tablets, foldables, phones (landscape)
 * - [Expanded] : > 840dp — large tablets, desktop
 */
enum class WindowWidthSizeClass {
    Compact,
    Medium,
    Expanded
}

/**
 * CompositionLocal that provides the current [WindowWidthSizeClass].
 * Defaults to [Compact] so existing phone layouts are unchanged
 * if a provider is missing.
 */
val LocalWindowSizeClass = compositionLocalOf { WindowWidthSizeClass.Compact }
