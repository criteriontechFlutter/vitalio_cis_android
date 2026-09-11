package com.critetiontech.vitalio_cis.ui.adaptive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ─── Eager Grid (for small lists inside a scrollable Column) ──────────────────

/**
 * Lays out [items] in a responsive grid.
 * - Phone  → 1 column
 * - Tablet → 2 columns
 * - Large  → 3 columns
 *
 * Use inside a `Column` or `verticalScroll` container.
 *
 * @param items       The data list to display
 * @param modifier    Modifier applied to each row
 * @param spacing     Gap between items (horizontal & vertical)
 * @param columns     Override column count (defaults to responsive value)
 * @param content     Composable for each item
 */
@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    columns: Int? = null,
    content: @Composable (T) -> Unit
) {
    val dimens = rememberResponsiveDimens()
    val cols = columns ?: dimens.gridColumns

    items.chunked(cols).forEach { rowItems ->
        Row(
            modifier = modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            rowItems.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    content(item)
                }
            }
            // Fill remaining slots so items don't stretch
            repeat(cols - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
        Spacer(modifier = Modifier.fillMaxWidth()) // vertical gap handled by Arrangement
    }
}

// ─── Lazy Grid (for large lists inside LazyColumn) ────────────────────────────

/**
 * Extension function for [LazyListScope] that arranges items in a responsive grid.
 *
 * Usage:
 * ```
 * LazyColumn {
 *     adaptiveGridItems(
 *         items = myList,
 *         columns = dimens.gridColumns
 *     ) { item ->
 *         MyCard(item)
 *     }
 * }
 * ```
 */
fun <T> LazyListScope.adaptiveGridItems(
    items: List<T>,
    columns: Int,
    spacing: Dp = 12.dp,
    key: ((rowIndex: Int) -> Any)? = null,
    itemContent: @Composable (T) -> Unit
) {
    val chunked = items.chunked(columns)
    items(
        count = chunked.size,
        key = key
    ) { rowIndex ->
        val rowItems = chunked[rowIndex]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing)
        ) {
            rowItems.forEach { item ->
                Box(modifier = Modifier.weight(1f)) {
                    itemContent(item)
                }
            }
            repeat(columns - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// ─── Fixed-Column Grid ────────────────────────────────────────────────────────

/**
 * Simple grid with a fixed number of columns.
 * Useful when you always want 2 columns regardless of screen size
 * (e.g., a settings grid).
 */
@Composable
fun <T> FixedGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    spacing: Dp = 12.dp,
    content: @Composable (T) -> Unit
) {
    AdaptiveGrid(
        items = items,
        modifier = modifier,
        spacing = spacing,
        columns = columns,
        content = content
    )
}
