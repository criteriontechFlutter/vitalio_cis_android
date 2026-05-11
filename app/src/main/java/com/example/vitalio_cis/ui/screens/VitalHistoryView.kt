package com.example.vitalio_cis.ui.screens


import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

/* ================= DATA ================= */

data class VitalReading(
    val time: String,
    val sys: Int,
    val dia: Int,
    val pulse: Int,
    val highlight: Boolean = false
)

data class VitalSummary(
    val sys: Int,
    val dia: Int,
    val status: String,
    val pulse: Int
)

/* ================= ENUM ================= */

enum class DateMode {
    DAILY, WEEKLY, MONTHLY
}
enum class ViewMode {
    LIST, GRAPH
}
/* ================= MAIN SCREEN ================= */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VitalHistoryScreen() {

    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }

    val summary = VitalSummary(120, 78, "NORMAL", 76)

    val list = listOf(
        VitalReading("Just Now",120,78,76,false),
        VitalReading("02:26 PM",105,62,62,true),
        VitalReading("11:30 AM",120,71,60,false),
    )
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }
    CommonAppBar(title = "Vital History") {

        val colors = LocalMyColorScheme.current

        Column(
            modifier = Modifier
                .fillMaxSize().padding(16.dp)
                .background(colors.dashboardBackgroundColor)
        ) {

            Spacer(Modifier.height(16.dp))

            VitalTabSection(
                selected = selectedTab,
                onTabChange = { selectedTab = it }
            )

            Spacer(Modifier.height(16.dp))

            vitalDateRow(
                currentDate = currentDate,
                mode = mode,
                onPrevious = {
                    currentDate = changeDate(currentDate, mode, false)
                },
                onNext = {
                    currentDate = changeDate(currentDate, mode, true)
                }
            )

            Spacer(Modifier.height(20.dp))

            ScribbleCircle(summary)

            Spacer(Modifier.height(20.dp))

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text("Blood Pressure Log", style = AppTextStyles.style16BCN())

                Row {

                    // 📋 List Icon
                    IconButton(
                        onClick = { viewMode = ViewMode.LIST }
                    ) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.LIST)
                                Color(0xFF2F6BFF) else Color.Gray
                        )
                    }

                    // 📊 Graph Icon
                    IconButton(
                        onClick = { viewMode = ViewMode.GRAPH }
                    ) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.GRAPH)
                                Color(0xFF2F6BFF) else Color.Gray
                        )
                    }
                }
            }
            when (viewMode) {

                ViewMode.LIST -> {
                    list.forEach {
                        LogItem(it)
                    }
                }

                ViewMode.GRAPH -> {
                    VitalGraph(list)
                }
            }
        }
    }
}

@Composable
fun LogItem(item: VitalReading) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            // LEFT SIDE (Time + BP)
            Column {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        item.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    "${item.sys}/${item.dia} mmHg",
                    fontSize = 16.sp,
                    color = when {
                        item.sys < 90 -> Color.Red
                        item.highlight -> Color(0xFFFF9800)
                        else -> Color(0xFF2F6BFF)
                    }
                )

                Text(
                    "SYS - DIA",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            // RIGHT SIDE (Pulse)
            Column(
                horizontalAlignment = Alignment.End
            ) {

                Text(
                    "${item.pulse}",
                    fontSize = 18.sp,
                    color = Color(0xFF2F6BFF)
                )

                Text(
                    "BPM",
                    fontSize = 10.sp,
                    color = Color.Gray
                )

                Text(
                    "Pulse",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Divider(color = Color(0xFFE0E0E0))
    }
}
@Composable
fun VitalGraph(list: List<VitalReading>) {

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .padding(16.dp)
    ) {

        val maxSys = list.maxOf { it.sys }
        val minSys = list.minOf { it.sys }

        val stepX = size.width / (list.size - 1)

        val points = list.mapIndexed { i, item ->
            val x = i * stepX

            val y = size.height - (
                    (item.sys - minSys).toFloat() /
                            (maxSys - minSys) * size.height
                    )

            Offset(x, y)
        }

        // Draw line
        for (i in 0 until points.size - 1) {
            drawLine(
                color = Color(0xFFFF9800),
                start = points[i],
                end = points[i + 1],
                strokeWidth = 4f
            )
        }

        // Draw points
        points.forEach {
            drawCircle(
                color = Color(0xFFFF9800),
                radius = 6f,
                center = it
            )
        }
    }
}
/* ================= DATE CHANGE LOGIC ================= */

@RequiresApi(Build.VERSION_CODES.O)
fun changeDate(
    current: LocalDate,
    mode: DateMode,
    isNext: Boolean
): LocalDate {
    return when (mode) {
        DateMode.DAILY ->
            if (isNext) current.plusDays(1) else current.minusDays(1)

        DateMode.WEEKLY ->
            if (isNext) current.plusWeeks(1) else current.minusWeeks(1)

        DateMode.MONTHLY ->
            if (isNext) current.plusMonths(1) else current.minusMonths(1)
    }
}

/* ================= TABS ================= */

@Composable
fun VitalTabSection(selected: Int, onTabChange: (Int) -> Unit) {

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
fun vitalDateRow(
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

/* ================= CIRCLE ================= */

@Composable
fun ScribbleCircle(summary: VitalSummary) {

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .size(240.dp),
        contentAlignment = Alignment.Center
    ) {

        Canvas(modifier = Modifier.fillMaxSize()) {

            val center = Offset(size.width / 2, size.height / 2)
            val baseRadius = size.minDimension / 2.4f

            repeat(4) { layer ->

                val path = Path()
                val points = 120
                val offset = layer * 15f

                for (i in 0..points) {

                    val angle = (i * (2 * Math.PI / points)).toFloat()

                    val radius = baseRadius +
                            sin(angle * 3 + offset) * 12f +
                            cos(angle * 2 + offset) * 8f

                    val x = center.x + radius * cos(angle)
                    val y = center.y + radius * sin(angle)

                    if (i == 0) path.moveTo(x, y)
                    else path.lineTo(x, y)
                }

                drawPath(
                    path,
                    Color(0xFF2F6BFF),
                    style = Stroke(2.dp.toPx())
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            Text("${summary.sys}/${summary.dia}", style = AppTextStyles.style14GCN().copy(fontSize = 30.sp),  )
            Text(summary.status, color = Color(0xFF2F6BFF))
        }
    }
}

/* ================= LOG ================= */

@Composable
fun LogCard(list: List<VitalReading>) {

    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .padding(16.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {

        Text("Blood Pressure Log")

        Spacer(Modifier.height(12.dp))

        list.forEach {
            Text("${it.time}  ${it.sys}/${it.dia}")
        }
    }
}