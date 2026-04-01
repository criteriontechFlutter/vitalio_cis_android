package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ManageMedicine() {

    val dates = (1..30).toList()
    val listState = rememberLazyListState()

    var selectedDate by remember { mutableStateOf(15) }

    // 🔥 Auto center detection
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        val centerIndex = listState.firstVisibleItemIndex + 2
        if (centerIndex in dates.indices) {
            selectedDate = dates[centerIndex]
        }
    }

    // 🔥 Snap to center after scroll
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex + 2
            listState.animateScrollToItem(centerIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(vertical = 16.dp)
    ) {

        // 🔹 Header
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Mon", color = Color.Gray)
            Spacer(modifier = Modifier.width(8.dp))
            Text("•")
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = selectedDate.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("•")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Sep", color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 🔥 CURVED DATE PICKER (SMOOTH U-SHAPE)
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            itemsIndexed(dates) { index, date ->

                val layoutInfo = listState.layoutInfo
                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }

                val center = layoutInfo.viewportEndOffset / 2

                val offset = itemInfo?.let {
                    val itemCenter = it.offset + it.size / 2
                    (itemCenter - center).toFloat()
                } ?: 0f

                // 🔥 NEW: PERFECT SMOOTH U-SHAPE (cosine curve)
                val maxOffset = layoutInfo.viewportEndOffset / 2f
                val normalized = (offset / maxOffset).coerceIn(-1f, 1f)

                val curve = (1 - kotlin.math.cos(normalized * Math.PI)).toFloat() * 40f

                // 🔥 SCALE
                val scale = 1f - (kotlin.math.abs(offset) / 800f)
                    .coerceIn(0f, 0.3f)

                val isSelected = date == selectedDate

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer {
                            translationY = curve   // 👈 smooth smile curve
                            scaleX = scale
                            scaleY = scale
                        }
                        .clickable {
                            CoroutineScope(Dispatchers.Main).launch {
                                listState.animateScrollToItem(index)
                            }
                        }
                ) {

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (isSelected) 48.dp else 40.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) Color(0xFF2979FF)
                                else Color.Transparent
                            )
                    ) {
                        Text(
                            text = date.toString(),
                            color = if (isSelected) Color.White else Color.Gray,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}