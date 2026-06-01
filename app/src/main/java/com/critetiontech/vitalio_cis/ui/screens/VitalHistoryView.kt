package com.critetiontech.vitalio_cis.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.VitalDetailViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.*

/* ================= DATA ================= */

data class VitalDisplayEntry(
    val time: String,
    val primaryValue: String,
    val primaryLabel: String,
    val unit: String,
    val numericValue: Double,
    val valueColor: Color
)

/* ================= ENUM ================= */

enum class DateMode { DAILY, WEEKLY, MONTHLY }
enum class ViewMode { LIST, GRAPH }

/* ================= HELPERS ================= */

fun getUnitByVitalName(name: String): String = when (name.lowercase()) {
    "bp_sys", "bp_dias" -> "mmHg"
    "pulse" -> "bpm"
    "temperature" -> "°C"
    "spo2" -> "%"
    "resprate" -> "/min"
    "heartrate" -> "bpm"
    "rbs", "blood glucose" -> "mg/dL"
    else -> ""
}

fun getStatusLabel(color: Color): String = when (color) {
    Color(0xFF4CAF50) -> "NORMAL"
    Color(0xFFFF9800) -> "BORDERLINE"
    else -> "ABNORMAL"
}

fun toDisplayEntries(
    entries: List<VitalGraphEntry>,
    isBP: Boolean,
    vitalName: String
): List<VitalDisplayEntry> = entries.map { entry ->
    val timePart = entry.dateTime.take(16)
    if (isBP) {
        val sys = entry.details.find { it.vitalName.equals("BP_Sys", true) }?.vitalValue?.toInt() ?: 0
        val dia = entry.details.find { it.vitalName.equals("BP_Dias", true) }?.vitalValue?.toInt() ?: 0
        VitalDisplayEntry(
            time = timePart,
            primaryValue = "$sys/$dia",
            primaryLabel = "SYS / DIA",
            unit = "mmHg",
            numericValue = sys.toDouble(),
            valueColor = getBpColor(sys.toDouble(), dia.toDouble())
        )
    } else {
        val detail = entry.details.firstOrNull()
        val value = detail?.vitalValue ?: 0.0
        val apiName = detail?.vitalName ?: ""
        VitalDisplayEntry(
            time = timePart,
            primaryValue = formatValue(value),
            primaryLabel = vitalName,
            unit = getUnitByVitalName(apiName),
            numericValue = value,
            valueColor = getVitalValueColor(apiName, value)
        )
    }
}

/* ================= MAIN SCREEN ================= */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VitalHistoryScreen(
    vitalIds: String = "",
    vitalName: String = "Vital",
    viewModel: VitalDetailViewModel = viewModel()
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val fromDate = remember(currentDate, mode) {
        when (mode) {
            DateMode.DAILY -> currentDate.format(dateFormatter)
            DateMode.WEEKLY -> currentDate.with(DayOfWeek.MONDAY).format(dateFormatter)
            DateMode.MONTHLY -> currentDate.withDayOfMonth(1).format(dateFormatter)
        }
    }
    val toDate = remember(currentDate, mode) {
        when (mode) {
            DateMode.DAILY -> currentDate.format(dateFormatter)
            DateMode.WEEKLY -> currentDate.with(DayOfWeek.MONDAY).plusDays(6).format(dateFormatter)
            DateMode.MONTHLY -> currentDate.withDayOfMonth(currentDate.lengthOfMonth()).format(dateFormatter)
        }
    }

    LaunchedEffect(fromDate, toDate, vitalIds) {
        if (vitalIds.isNotEmpty()) {
            viewModel.fetchVitalAnalytics(context, fromDate, toDate, vitalIds)
        }
    }

    val analyticsData by viewModel.analyticsData.collectAsState()
    val analyticsLoading by viewModel.analyticsLoading.collectAsState()

    val isBP = vitalIds.contains(",")
    val displayEntries = remember(analyticsData, isBP, vitalName) {
        toDisplayEntries(analyticsData, isBP, vitalName)
    }
    val latest = displayEntries.firstOrNull()

    CommonAppBar(title = "$vitalName History") {
        val colors = LocalMyColorScheme.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(16.dp))

            VitalTabSection(selected = selectedTab, onTabChange = { selectedTab = it })

            Spacer(Modifier.height(16.dp))

            vitalDateRow(
                currentDate = currentDate,
                mode = mode,
                onPrevious = { currentDate = changeDate(currentDate, mode, false) },
                onNext = { currentDate = changeDate(currentDate, mode, true) }
            )

            Spacer(Modifier.height(20.dp))

            if (analyticsLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                DynamicSummaryCircle(
                    value = latest?.primaryValue ?: "--",
                    unit = latest?.unit ?: "",
                    valueColor = latest?.valueColor ?: Color(0xFF2F6BFF),
                    status = if (latest != null) getStatusLabel(latest.valueColor) else "NO DATA"
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$vitalName Log", style = AppTextStyles.style16BCN())
                Row {
                    IconButton(onClick = { viewMode = ViewMode.LIST }) {
                        Icon(
                            Icons.Default.List,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.LIST) Color(0xFF2F6BFF) else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewMode = ViewMode.GRAPH }) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = null,
                            tint = if (viewMode == ViewMode.GRAPH) Color(0xFF2F6BFF) else Color.Gray
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (displayEntries.isEmpty() && !analyticsLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No data available for this period", color = Color.Gray)
                }
            } else {
                when (viewMode) {
                    ViewMode.LIST -> displayEntries.forEach { DynamicLogItem(it) }
                    ViewMode.GRAPH -> DynamicVitalGraph(displayEntries)
                }
            }
        }
    }
}

/* ================= SUMMARY CIRCLE ================= */

@Composable
fun DynamicSummaryCircle(
    value: String,
    unit: String,
    valueColor: Color,
    status: String
) {
    Box(
        modifier = Modifier.fillMaxWidth().size(240.dp),
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
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, Color(0xFF2F6BFF), style = Stroke(2.dp.toPx()))
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                value,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            if (unit.isNotEmpty()) {
                Text(unit, color = Color.Gray, fontSize = 12.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(status, color = valueColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

/* ================= LOG ITEM ================= */

@Composable
fun DynamicLogItem(entry: VitalDisplayEntry) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(entry.time, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "${entry.primaryValue} ${entry.unit}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = entry.valueColor
                )
                Text(entry.primaryLabel, fontSize = 10.sp, color = Color.Gray)
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = entry.valueColor.copy(alpha = 0.12f)
            ) {
                Text(
                    getStatusLabel(entry.valueColor),
                    color = entry.valueColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
    }
}

/* ================= GRAPH ================= */

@Composable
fun DynamicVitalGraph(entries: List<VitalDisplayEntry>) {
    if (entries.size < 2) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) { Text("Not enough data to draw graph", color = Color.Gray) }
        return
    }
    val values = entries.map { it.numericValue }
    val max = values.max()
    val min = values.min()
    val range = if (max == min) 1.0 else max - min
    val lineColor = entries.firstOrNull()?.valueColor ?: Color(0xFF2F6BFF)

    Canvas(
        modifier = Modifier.fillMaxWidth().height(220.dp).padding(16.dp)
    ) {
        val stepX = size.width / (entries.size - 1)
        val points = entries.mapIndexed { i, entry ->
            Offset(
                x = i * stepX,
                y = size.height - ((entry.numericValue - min) / range * size.height).toFloat()
            )
        }
        for (i in 0 until points.size - 1) {
            drawLine(color = lineColor, start = points[i], end = points[i + 1], strokeWidth = 4f)
        }
        points.forEach { drawCircle(color = lineColor, radius = 6f, center = it) }
    }
}

/* ================= DATE CHANGE LOGIC ================= */

@RequiresApi(Build.VERSION_CODES.O)
fun changeDate(current: LocalDate, mode: DateMode, isNext: Boolean): LocalDate = when (mode) {
    DateMode.DAILY -> if (isNext) current.plusDays(1) else current.minusDays(1)
    DateMode.WEEKLY -> if (isNext) current.plusWeeks(1) else current.minusWeeks(1)
    DateMode.MONTHLY -> if (isNext) current.plusMonths(1) else current.minusMonths(1)
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
                    .background(if (selected == i) Color(0xFF2F6BFF) else Color.Transparent)
                    .clickable { onTabChange(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(text = text, color = if (selected == i) Color.White else Color.Gray)
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
        DateMode.DAILY -> currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        DateMode.WEEKLY -> {
            val start = currentDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            "${start.dayOfMonth} - ${end.dayOfMonth} ${end.month}"
        }
        DateMode.MONTHLY -> currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = { onPrevious() },
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.btnDarkColor)
        ) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null) }

        Text(text, style = AppTextStyles.style14GCN())

        IconButton(
            onClick = { onNext() },
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.btnDarkColor)
        ) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = null) }
    }
}