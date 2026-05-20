package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.animation.core.*
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.viewmodel.IntakeOutputViewModel
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val legendColors = listOf(
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF9C27B0),
    Color(0xFFFF9800)
)

@Composable
fun FluidInputHistoryScreen(
    viewModel: IntakeOutputViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    val intakeList by viewModel.intakeList.collectAsState()
    val fluidSummaryList by viewModel.fluidSummaryList.collectAsState()
    val isLoading by viewModel.intakeLoading.collectAsState()

    val colors = LocalMyColorScheme.current
    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }

    LaunchedEffect(currentDate, selectedTab) {
        when (selectedTab) {
            0 -> viewModel.fetchIntakeItems(context, currentDate.toString())
            1 -> {
                val start = currentDate.with(DayOfWeek.MONDAY)
                val end = start.plusDays(6)
                viewModel.fetchFluidSummary(context, start.toString(), end.toString())
            }
            else -> {
                val start = currentDate.withDayOfMonth(1)
                val end = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
                viewModel.fetchFluidSummary(context, start.toString(), end.toString())
            }
        }
    }

    val totalIntake = when (selectedTab) {
        0 -> intakeList.orEmpty().sumOf { it.foodQuantity }
        else -> fluidSummaryList.orEmpty().sumOf { it.foodQuantity }
    }

    val recommendedLimit = if (fluidSummaryList.orEmpty().isNotEmpty()) {
        fluidSummaryList.orEmpty().maxOfOrNull { it.assignedLimit }?.takeIf { it > 0 } ?: 2000
    } else 2000

    val progressFraction = (totalIntake.toFloat() / recommendedLimit).coerceIn(0f, 1f)
    val remaining = (recommendedLimit - totalIntake).coerceAtLeast(0)

    val progressColor = when {
        progressFraction < 0.5f -> Color(0xFF4CAF50)
        progressFraction < 0.85f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val foodLegendNames = remember(intakeList) {
        intakeList.orEmpty().mapNotNull { it.foodName?.takeIf { n -> n.isNotEmpty() } }.distinct().take(4)
    }

    // Screen-level entrance
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }
    val tabAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing), label = "tabAlpha"
    )
    val tabY by animateFloatAsState(
        targetValue = if (screenVisible) 0f else -20f,
        animationSpec = tween(350, easing = FastOutSlowInEasing), label = "tabY"
    )
    val progressCardAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 100, easing = FastOutSlowInEasing), label = "progAlpha"
    )
    val progressCardY by animateFloatAsState(
        targetValue = if (screenVisible) 0f else 24f,
        animationSpec = tween(400, delayMillis = 100, easing = FastOutSlowInEasing), label = "progY"
    )
    val logCardAlpha by animateFloatAsState(
        targetValue = if (screenVisible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 200, easing = FastOutSlowInEasing), label = "logAlpha"
    )
    val logCardY by animateFloatAsState(
        targetValue = if (screenVisible) 0f else 24f,
        animationSpec = tween(400, delayMillis = 200, easing = FastOutSlowInEasing), label = "logY"
    )
    // Animated progress bar — fills from 0 to actual value
    val animatedProgress by animateFloatAsState(
        targetValue = if (screenVisible) progressFraction else 0f,
        animationSpec = tween(900, delayMillis = 300, easing = FastOutSlowInEasing),
        label = "progressBar"
    )

    CommonAppBar(title = "Fluid Input History") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {
            Box(modifier = Modifier.graphicsLayer { alpha = tabAlpha; translationY = tabY }) {
                fluidTabSection(selected = selectedTab, onTabChange = { selectedTab = it })
            }

            Spacer(Modifier.height(16.dp))

            Box(modifier = Modifier.graphicsLayer { alpha = tabAlpha; translationY = tabY }) {
                fluidDateRow(
                    currentDate = currentDate,
                    mode = mode,
                    onPrevious = { currentDate = changeDate(currentDate, mode, false) },
                    onNext = { currentDate = changeDate(currentDate, mode, true) }
                )
            }

            Spacer(Modifier.height(16.dp))

            // Progress card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                modifier = Modifier.graphicsLayer { alpha = progressCardAlpha; translationY = progressCardY }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recommended", style = AppTextStyles.style12GCN())
                            Text(
                                "${"%,d".format(recommendedLimit)} ml",
                                style = AppTextStyles.style14BCN()
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Intake", style = AppTextStyles.style12GCN())
                            Text("$totalIntake ml", style = AppTextStyles.style14BCN())
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color(0xFFE8EFFF))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(animatedProgress)   // animated width
                                .fillMaxHeight()
                                .background(progressColor, RoundedCornerShape(5.dp))
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (foodLegendNames.isEmpty()) {
                                listOf("Water", "Juice", "Milk", "Tea").forEachIndexed { idx, name ->
                                    LegendDot(name, legendColors.getOrElse(idx) { Color.Gray })
                                }
                            } else {
                                foodLegendNames.forEachIndexed { idx, name ->
                                    LegendDot(name, legendColors.getOrElse(idx) { Color.Gray })
                                }
                            }
                        }
                        Text(
                            "Remaining: $remaining ml",
                            style = AppTextStyles.style12GCN()
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Log card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                modifier = Modifier.graphicsLayer { alpha = logCardAlpha; translationY = logCardY }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Fluid Intake Log", style = AppTextStyles.style16BCN())
                        Row {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF2F6BFF))
                        }
                    } else if (selectedTab == 0) {
                        if (intakeList.isEmpty()) {
                            Text(
                                "No records found",
                                style = AppTextStyles.style14GCN(),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            intakeList.forEachIndexed { idx, item ->
                                val itemAlpha by animateFloatAsState(
                                    targetValue = if (screenVisible) 1f else 0f,
                                    animationSpec = tween(300, delayMillis = 300 + idx * 55, easing = FastOutSlowInEasing),
                                    label = "intakeItem_$idx"
                                )
                                val itemY by animateFloatAsState(
                                    targetValue = if (screenVisible) 0f else 16f,
                                    animationSpec = tween(300, delayMillis = 300 + idx * 55, easing = FastOutSlowInEasing),
                                    label = "intakeItemY_$idx"
                                )
                                val displayTime = if (!item.intakeTimeFormat.isNullOrEmpty()) {
                                    item.intakeTimeFormat
                                } else {
                                    try {
                                        LocalDateTime.parse(
                                            item.foodDate.orEmpty(),
                                            DateTimeFormatter.ISO_DATE_TIME
                                        ).format(DateTimeFormatter.ofPattern("hh:mm a"))
                                    } catch (e: Exception) {
                                        item.foodDate.orEmpty()
                                    }
                                }
                                Box(modifier = Modifier.graphicsLayer { alpha = itemAlpha; translationY = itemY }) {
                                FluidInputItem(
                                    title = item.foodName?.takeIf { it.isNotEmpty() } ?: "Unknown",
                                    time = displayTime,
                                    value = "${item.foodQuantity} ml",
                                    color = legendColors.getOrElse(idx % legendColors.size) { Color.Gray }
                                )
                                }
                            }
                        }
                    } else {
                        if (fluidSummaryList.isEmpty()) {
                            Text(
                                "No records found",
                                style = AppTextStyles.style14GCN(),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            fluidSummaryList.forEachIndexed { idx, item ->
                                val itemAlpha by animateFloatAsState(
                                    targetValue = if (screenVisible) 1f else 0f,
                                    animationSpec = tween(300, delayMillis = 300 + idx * 55, easing = FastOutSlowInEasing),
                                    label = "summaryItem_$idx"
                                )
                                val itemY by animateFloatAsState(
                                    targetValue = if (screenVisible) 0f else 16f,
                                    animationSpec = tween(300, delayMillis = 300 + idx * 55, easing = FastOutSlowInEasing),
                                    label = "summaryItemY_$idx"
                                )
                                val formattedDate = try {
                                    LocalDateTime.parse(
                                        item.givenFoodDate,
                                        DateTimeFormatter.ISO_LOCAL_DATE_TIME
                                    ).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                } catch (e: Exception) {
                                    item.givenFoodDate
                                }
                                Box(modifier = Modifier.graphicsLayer { alpha = itemAlpha; translationY = itemY }) {
                                FluidInputItem(
                                    title = formattedDate,
                                    time = "Limit: ${item.assignedLimit} ml",
                                    value = "${item.foodQuantity} ml",
                                    color = legendColors.getOrElse(idx % legendColors.size) { Color.Gray }
                                )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Total Intake  ", style = AppTextStyles.style14GCN())
                Text("$totalIntake ml", style = AppTextStyles.style16BCN())
            }
        }
    }
}

@Composable
private fun LegendDot(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(name, style = AppTextStyles.style12GCN())
    }
}

@Composable
fun fluidTabSection(selected: Int, onTabChange: (Int) -> Unit) {
    val tabs = listOf("Daily", "Weekly", "Monthly")
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        tabs.forEachIndexed { i, text ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected == i) Color(0xFF2F6BFF)
                        else Color.Transparent
                    )
                    .clickable { onTabChange(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (selected == i) Color.White else Color.Gray
                )
            }
        }
    }
}

@Composable
fun fluidDateRow(
    currentDate: LocalDate,
    mode: DateMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val text = when (mode) {
        DateMode.DAILY -> currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        DateMode.WEEKLY -> {
            val start = currentDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            "${start.dayOfMonth} - ${end.dayOfMonth} ${end.month}"
        }
        DateMode.MONTHLY -> currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPrevious() },
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.btnDarkColor)
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
        }
        Text(text, style = AppTextStyles.style14GCN())
        IconButton(
            onClick = { onNext() },
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.btnDarkColor)
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun FluidInputItem(
    title: String,
    time: String,
    value: String,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = AppTextStyles.style14BCN())
                Text(time, style = AppTextStyles.style12GCN())
            }
            Text(value, style = AppTextStyles.style14BCN())
        }
        Divider(color = Color(0xFFE0E0E0), thickness = 0.7.dp)
    }
}

@Composable
fun RowScope.ToggleInputItem(text: String, selected: Boolean) {
    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(30.dp))
            .background(
                if (selected) Color(0xFF2F6BFF)
                else Color.Transparent
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 14.sp
        )
    }
}
