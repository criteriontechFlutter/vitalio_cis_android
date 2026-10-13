package com.critetiontech.ctvitalio.ui.adaptive

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

// ─── Navigation Item Model ───────────────────────────────────────────────────

data class AdaptiveNavItem(
    val label: String,
    val icon: Painter,
    val selectedIcon: Painter? = null
)

// ─── Adaptive Scaffold ───────────────────────────────────────────────────────

/**
 * A scaffold that automatically switches between:
 * - **Bottom navigation bar** on phones (Compact)
 * - **Navigation rail** on tablets (Medium / Expanded)
 *
 * Usage:
 * ```
 * AdaptiveNavigationScaffold(
 *     items = navItems,
 *     selectedIndex = selectedIndex,
 *     onItemSelected = { selectedIndex = it },
 *     topBar = { MyAppBar(...) }
 * ) {
 *     // page content
 * }
 * ```
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveNavigationScaffold(
    items: List<AdaptiveNavItem>,
    selectedIndex: Int,
    onItemSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    content: @Composable () -> Unit
) {
    val windowSize = LocalWindowSizeClass.current
    val colors = LocalMyColorScheme.current

    if (windowSize == WindowWidthSizeClass.Compact) {
        // ── Phone: bottom navigation bar ──
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            containerColor = colors.dashboardBackgroundColor,
            floatingActionButton = floatingActionButton,
            bottomBar = {
                NavigationBar(
                    containerColor = colors.dashboardBackgroundColor
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) },
                            icon = {
                                Icon(
                                    painter = if (selectedIndex == index && item.selectedIcon != null)
                                        item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Blue,
                                selectedTextColor = Color.Blue,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.padding(padding)) {
                content()
            }
        }
    } else {
        // ── Tablet: navigation rail on the start side ──
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            containerColor = colors.dashboardBackgroundColor,
            floatingActionButton = floatingActionButton
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(80.dp),
                    containerColor = colors.dashboardContainerColor
                ) {
                    items.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = selectedIndex == index,
                            onClick = { onItemSelected(index) },
                            icon = {
                                Icon(
                                    painter = if (selectedIndex == index && item.selectedIcon != null)
                                        item.selectedIcon else item.icon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = Color.Blue,
                                selectedTextColor = Color.Blue,
                                unselectedIconColor = Color.Gray,
                                unselectedTextColor = Color.Gray,
                                indicatorColor = Color.Transparent
                            )
                        )
                    }
                }

                // Main content area
                Box(modifier = Modifier.weight(1f)) {
                    content()
                }
            }
        }
    }
}
