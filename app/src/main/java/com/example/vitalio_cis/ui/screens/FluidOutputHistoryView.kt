package com.example.vitalio_cis.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
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

private fun colourNameToColor(colour: String): Color {
    if (colour.startsWith("#")) {
        return try {
            Color(android.graphics.Color.parseColor(colour))
        } catch (e: Exception) {
            Color.Gray
        }
    }
    return when (colour.trim().lowercase()) {
        "red" -> Color(0xFFE53935)
        "yellow" -> Color(0xFFE9DB8A)
        "light yellow" -> Color(0xFFF4F1C9)
        "dark yellow" -> Color(0xFFF4C400)
        "amber" -> Color(0xFFFFA000)
        "orange" -> Color(0xFFFF6D00)
        "brown" -> Color(0xFF6D4C41)
        "dark" -> Color(0xFF8D4E2A)
        else -> Color.Gray
    }
}

@Composable
fun FluidOutputHistoryScreen(
    viewModel: IntakeOutputViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    val outputList by viewModel.outputList.collectAsState()
    val outputSummaryList by viewModel.outputSummaryList.collectAsState()
    val isLoading by viewModel.outputLoading.collectAsState()

    val colors = LocalMyColorScheme.current
    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }

    LaunchedEffect(currentDate, selectedTab) {
        when (selectedTab) {
            0 -> viewModel.fetchOutput(context, currentDate.toString())
            1 -> {
                val start = currentDate.with(DayOfWeek.MONDAY)
                val end = start.plusDays(6)
                viewModel.fetchOutputSummary(context, start.toString(), end.toString())
            }
            else -> {
                val start = currentDate.withDayOfMonth(1)
                val end = currentDate.withDayOfMonth(currentDate.lengthOfMonth())
                viewModel.fetchOutputSummary(context, start.toString(), end.toString())
            }
        }
    }

    val filteredDaily = remember(outputList, currentDate) {
        outputList.filter { it.outputDateFormat == currentDate.toString() }
    }

    val totalQuantity = when (selectedTab) {
        0 -> filteredDaily.sumOf { it.quantity }
        else -> outputSummaryList.sumOf { it.quantity }
    }

    CommonAppBar(
        title = "Fluid Output History",
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {
            outputTabSection(
                selected = selectedTab,
                onTabChange = { selectedTab = it }
            )

            Spacer(Modifier.height(16.dp))

            outputDateRow(
                currentDate = currentDate,
                mode = mode,
                onPrevious = {
                    currentDate = changeDate(currentDate, mode, false)
                },
                onNext = {
                    currentDate = changeDate(currentDate, mode, true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Fluid Output Log", style = AppTextStyles.style16BCN()
                        )
                        Row {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                        if (filteredDaily.isEmpty()) {
                            Text(
                                "No records found",
                                style = AppTextStyles.style14GCN(),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            filteredDaily.forEach { item ->
                                FluidOutputItem(
                                    title = item.colour.ifEmpty { "Unknown" },
                                    time = item.outputTimeFormat,
                                    value = "${item.quantity} ml",
                                    color = colourNameToColor(item.colour)
                                )
                            }
                        }
                    } else {
                        if (outputSummaryList.isEmpty()) {
                            Text(
                                "No records found",
                                style = AppTextStyles.style14GCN(),
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                        } else {
                            outputSummaryList.forEach { item ->
                                val formattedDate = try {
                                    LocalDateTime.parse(item.outputDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                        .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                                } catch (e: Exception) {
                                    item.outputDate
                                }
                                FluidOutputItem(
                                    title = formattedDate,
                                    time = "${item.repetition} record${if (item.repetition != 1) "s" else ""}",
                                    value = "${item.quantity} ml",
                                    color = Color(0xFF2F6BFF)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text("Total Output ", style = AppTextStyles.style14GCN())
                Text("$totalQuantity ml", style = AppTextStyles.style14GCN())
            }
        }
    }
}


@Composable
fun outputTabSection(selected: Int, onTabChange: (Int) -> Unit) {

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

/* ================= DATE ROW ================= */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun outputDateRow(
    currentDate: LocalDate,
    mode: DateMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {

    val colors = LocalMyColorScheme.current
    val text = when (mode) {

        DateMode.DAILY -> {
            currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }

        DateMode.WEEKLY -> {
            val start = currentDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)

            "${start.dayOfMonth} - ${end.dayOfMonth} ${end.month}"
        }

        DateMode.MONTHLY -> {
            currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
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
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = colors.btnDarkColor
            )
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
        }

        Text(text, style = AppTextStyles.style14GCN())
        IconButton(
            onClick = { onNext() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = colors.btnDarkColor
            )
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }

    }
}

@Composable
fun FluidOutputItem(
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
                Text(title,
                    style = AppTextStyles.style14BCN())
                Text(time,
                    style = AppTextStyles.style12GCN())
            }

            Text(
                value,
                style = AppTextStyles.style14BCN())
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 0.7.dp)
    }
}

@Composable
fun RowScope.ToggleOutputItem(text: String, selected: Boolean) {

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
