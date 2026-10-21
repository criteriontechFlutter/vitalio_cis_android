package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter

import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.model.VitalGraphEntry
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.VitalDetailViewModel
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.*

/* ================= DATA ================= */

data class VitalDisplayEntry(
    /** When the reading was taken; null if the server's timestamp couldn't be read. */
    val dateTime: LocalDateTime?,
    val time: String,
    val primaryValue: String,
    val primaryLabel: String,
    val unit: String,
    /** Value plotted and used for stats (systolic for blood pressure). */
    val numericValue: Double,
    /** Diastolic for blood pressure, otherwise null. */
    val secondaryValue: Double? = null,
    val status: VitalStatus,
) {
    val valueColor: Color get() = status.color
}

/* ================= ENUM ================= */

enum class DateMode { DAILY, WEEKLY, MONTHLY }
enum class ViewMode { LIST, GRAPH }

/* ================= HELPERS ================= */

private val ServerDateTime = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
private val LogTimeFormat = DateTimeFormatter.ofPattern("dd MMM, hh:mm a", Locale.getDefault())

/** "2026-10-16 11:54:10" or "2026-10-16T11:54:10" as a date-time. */
private fun parseVitalDateTime(raw: String): LocalDateTime? {
    val s = raw.trim().replace('T', ' ').substringBefore('.')
    return runCatching { LocalDateTime.parse(s, ServerDateTime) }.getOrNull()
        ?: runCatching { LocalDateTime.parse(s.take(16), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) }.getOrNull()
}

/**
 * One row per reading, newest first. [unit] is the backend's unit for this vital, passed in from the
 * vitals list (the analytics API has none).
 */
fun toDisplayEntries(
    entries: List<VitalGraphEntry>,
    isBP: Boolean,
    vitalName: String,
    unit: String,
): List<VitalDisplayEntry> = entries.mapNotNull { entry ->
    val dateTime = parseVitalDateTime(entry.dateTime)
    val time = dateTime?.format(LogTimeFormat) ?: entry.dateTime.take(16)
    if (isBP) {
        val sys = entry.details.find { it.vitalName.equals("BP_Sys", true) }?.vitalValue
        val dia = entry.details.find { it.vitalName.equals("BP_Dias", true) }?.vitalValue
        if (sys == null && dia == null) return@mapNotNull null
        VitalDisplayEntry(
            dateTime = dateTime,
            time = time,
            primaryValue = "${sys?.roundToInt() ?: "–"}/${dia?.roundToInt() ?: "–"}",
            primaryLabel = "Systolic / Diastolic",
            unit = unit,
            numericValue = sys ?: 0.0,
            secondaryValue = dia,
            // Both numbers are needed to judge blood pressure.
            status = if (sys != null && dia != null) bpStatus(sys, dia) else VitalStatus.None,
        )
    } else {
        val detail = entry.details.firstOrNull() ?: return@mapNotNull null
        VitalDisplayEntry(
            dateTime = dateTime,
            time = time,
            primaryValue = formatValue(detail.vitalValue),
            primaryLabel = vitalName,
            unit = unit,
            numericValue = detail.vitalValue,
            status = vitalStatus(detail.vitalName, detail.vitalValue),
        )
    }
}.sortedByDescending { it.dateTime ?: LocalDateTime.MIN }

/** Rounds to one decimal, dropping ".0". */
private fun oneDecimal(v: Double): String = formatValue((v * 10).roundToInt() / 10.0)

/* ================= MAIN SCREEN ================= */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun VitalHistoryScreen(
    vitalIds: String = "",
    vitalName: String = "Vital",
    unit: String = "",
    viewModel: VitalDetailViewModel = viewModel()
) {
    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf(ViewMode.LIST) }

    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    val (rangeStart, rangeEnd) = remember(currentDate, mode) { periodRange(currentDate, mode) }
    val fromDate = rangeStart.format(dateFormatter)
    val toDate = rangeEnd.format(dateFormatter)
    // No readings can exist in the future, so stop at the current period.
    val canGoNext = rangeEnd.isBefore(LocalDate.now())

    LaunchedEffect(fromDate, toDate, vitalIds) {
        if (vitalIds.isNotEmpty()) {
            viewModel.fetchVitalAnalytics(fromDate, toDate, vitalIds)
        }
    }
    // Back from Add Vital: show the new reading.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, fromDate, toDate) {
        var first = true
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (first) first = false
                else if (vitalIds.isNotEmpty()) {
                    viewModel.fetchVitalAnalytics(fromDate, toDate, vitalIds)
                    if (vitalIds.contains(",")) viewModel.fetchLastVital()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val analyticsData by viewModel.analyticsData.collectAsState()
    val analyticsLoading by viewModel.analyticsLoading.collectAsState()

    val isBP = vitalIds.contains(",")
    val displayEntries = remember(analyticsData, isBP, vitalName, unit) {
        toDisplayEntries(analyticsData, isBP, vitalName, unit.trim())
    }
    val latest = displayEntries.firstOrNull()
    val heroKind = heroKindFor(vitalName, isBP)
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = com.critetiontech.myapplication.utils.LocalNavController.current
    val colors = LocalMyColorScheme.current

    // Blood pressure shows the latest pulse beside it, as in the design.
    val lastVitals by viewModel.vitalList.collectAsState()
    LaunchedEffect(isBP) { if (isBP) viewModel.fetchLastVital() }
    val latestPulse = if (isBP) lastVitals.firstOrNull { it.vitalName.equals("Pulse", true) }?.vitalValue?.let { formatValue(it) } else null

    fun share() {
        val text = buildString {
            append(heroTitleFor(vitalName))
            if (latest != null) {
                append(": ${latest.primaryValue} ${latest.unit}".trimEnd())
                if (latest.status != VitalStatus.None) append(" (${latest.status.label.lowercase().replaceFirstChar { it.uppercase() }})")
                append(" · ${latest.time}")
                if (displayEntries.size > 1) append("\n${displayEntries.size} readings in this period")
            } else append(": no readings in this period")
        }
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain")
            .putExtra(android.content.Intent.EXTRA_TEXT, text)
        runCatching { context.startActivity(android.content.Intent.createChooser(intent, "Share ${heroTitleFor(vitalName)}")) }
    }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        com.critetiontech.ctvitalio.ui.components.RoundBackTopBar(
            title = heroTitleFor(vitalName),
            actions = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(colors.dashboardContainerColor)
                        .clickable { share() },
                ) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.IosShare, contentDescription = "Share", tint = colors.textDarkColor, modifier = Modifier.size(20.dp))
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            VitalTabSection(selected = selectedTab, onTabChange = {
                selectedTab = it
                currentDate = LocalDate.now()
            })

            Spacer(Modifier.height(16.dp))

            vitalDateRow(
                currentDate = currentDate,
                mode = mode,
                canGoNext = canGoNext,
                onPrevious = { currentDate = changeDate(currentDate, mode, false) },
                onNext = { if (canGoNext) currentDate = changeDate(currentDate, mode, true) }
            )

            Spacer(Modifier.height(20.dp))

            if (analyticsLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else {
                VitalHero(
                    kind = heroKind,
                    value = latest?.primaryValue ?: "--",
                    unit = latest?.unit.orEmpty(),
                    status = latest?.status,
                    caption = latest?.time?.let { "Latest · $it" },
                    pulse = latestPulse,
                )
            }

            if (canAddVitalManually(vitalName)) {
                Text(
                    "Add Vital",
                    color = colors.primaryBlueColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 10.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            navController.navigate(
                                com.critetiontech.ctvitalio.Routes.CONNECTION + "/" + android.net.Uri.encode(vitalName) + "/" + android.net.Uri.encode(unit)
                            )
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }

            if (!analyticsLoading && displayEntries.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                VitalStatsRow(displayEntries, isBP)
            }

            Spacer(Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("$vitalName Log", style = AppTextStyles.style16BCN())
                Row {
                    HapticIconButton(onClick = { viewMode = ViewMode.LIST }) {
                        Icon(
                            Icons.AutoMirrored.Filled.List,
                            contentDescription = "List",
                            tint = if (viewMode == ViewMode.LIST) Color(0xFF2F6BFF) else Color.Gray
                        )
                    }
                    HapticIconButton(onClick = { viewMode = ViewMode.GRAPH }) {
                        Icon(
                            Icons.Default.ShowChart,
                            contentDescription = "Graph",
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
                    Text("No readings in this period", color = Color.Gray)
                }
            } else {
                when (viewMode) {
                    ViewMode.LIST -> displayEntries.forEachIndexed { i, entry -> AnimatedEnter(index = i) { DynamicLogItem(entry) } }
                    ViewMode.GRAPH -> DynamicVitalGraph(displayEntries, isBP)
                }
            }
        }
    }
}

/** First and last day of the period containing [date]. */
@RequiresApi(Build.VERSION_CODES.O)
private fun periodRange(date: LocalDate, mode: DateMode): Pair<LocalDate, LocalDate> = when (mode) {
    DateMode.DAILY -> date to date
    DateMode.WEEKLY -> date.with(DayOfWeek.MONDAY).let { it to it.plusDays(6) }
    DateMode.MONTHLY -> date.withDayOfMonth(1) to date.withDayOfMonth(date.lengthOfMonth())
}

/* ================= STATS ================= */

/** Readings, lowest, average and highest for the period. For blood pressure, both numbers. */
@Composable
private fun VitalStatsRow(entries: List<VitalDisplayEntry>, isBP: Boolean) {
    val colors = LocalMyColorScheme.current
    val stats: List<Pair<String, String>> = if (isBP) {
        val sys = entries.filter { it.numericValue > 0 }.map { it.numericValue }
        val dia = entries.mapNotNull { it.secondaryValue }
        fun pair(a: Double?, b: Double?) = "${a?.roundToInt() ?: "–"}/${b?.roundToInt() ?: "–"}"
        listOf(
            "Readings" to entries.size.toString(),
            "Lowest" to pair(sys.minOrNull(), dia.minOrNull()),
            "Average" to pair(sys.takeIf { it.isNotEmpty() }?.average(), dia.takeIf { it.isNotEmpty() }?.average()),
            "Highest" to pair(sys.maxOrNull(), dia.maxOrNull()),
        )
    } else {
        val values = entries.map { it.numericValue }
        listOf(
            "Readings" to entries.size.toString(),
            "Lowest" to oneDecimal(values.min()),
            "Average" to oneDecimal(values.average()),
            "Highest" to oneDecimal(values.max()),
        )
    }
    val outOfRange = entries.count { it.status == VitalStatus.Borderline || it.status == VitalStatus.Abnormal }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.dashboardContainerColor)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Row(Modifier.fillMaxWidth()) {
            stats.forEach { (label, value) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(value, color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(label, color = colors.textGreyColor, fontSize = 11.sp)
                }
            }
        }
        if (entries.any { it.status != VitalStatus.None }) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (outOfRange == 0) "All readings in the normal range"
                else "$outOfRange of ${entries.size} ${if (entries.size == 1) "reading" else "readings"} outside the normal range",
                color = if (outOfRange == 0) VitalStatus.Normal.color else VitalStatus.Abnormal.color,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
    }
}

/* ================= SUMMARY CIRCLE ================= */

@Composable
fun DynamicSummaryCircle(
    value: String,
    unit: String,
    valueColor: Color,
    status: String,
    caption: String? = null,
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
            if (status.isNotEmpty()) Text(status, color = valueColor, fontWeight = FontWeight.SemiBold)
            if (caption != null) {
                Text(caption, color = Color.Gray, fontSize = 11.sp)
            }
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
                    "${entry.primaryValue} ${entry.unit}".trim(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = entry.valueColor
                )
                Text(entry.primaryLabel, fontSize = 10.sp, color = Color.Gray)
            }

            // Height and weight have no normal range of their own, so no tag.
            if (entry.status != VitalStatus.None) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = entry.valueColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        entry.status.label,
                        color = entry.valueColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = Color(0xFFE0E0E0))
    }
}

/* ================= GRAPH ================= */

/** Readings oldest → newest, each dot coloured by its status; blood pressure shows both lines. */
@Composable
fun DynamicVitalGraph(entries: List<VitalDisplayEntry>, isBP: Boolean = false) {
    if (entries.size < 2) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp),
            contentAlignment = Alignment.Center
        ) { Text("At least 2 readings are needed for a graph", color = Color.Gray) }
        return
    }
    val chronological = entries.reversed()
    val all = chronological.flatMap { listOfNotNull(it.numericValue.takeIf { v -> v > 0 || !isBP }, it.secondaryValue) }
    val max = all.max()
    val min = all.min()
    val range = if (max == min) 1.0 else max - min
    val lineColor = Color(0xFF2F6BFF)
    val diaColor = Color(0xFF8B5CF6)

    Column {
        if (isBP) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(start = 16.dp)) {
                LegendDot(lineColor, "Systolic")
                LegendDot(diaColor, "Diastolic")
            }
        }
        Row {
            // Y axis: highest and lowest value in the period.
            Column(
                Modifier.height(220.dp).padding(vertical = 16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(oneDecimal(max), fontSize = 10.sp, color = Color.Gray)
                Text(oneDecimal(min), fontSize = 10.sp, color = Color.Gray)
            }
            Canvas(
                modifier = Modifier.weight(1f).height(220.dp).padding(16.dp)
            ) {
                val stepX = size.width / (chronological.size - 1)
                fun y(v: Double) = size.height - ((v - min) / range * size.height).toFloat()
                fun drawSeries(values: List<Double?>, color: Color, statuses: List<VitalStatus>) {
                    val points = values.mapIndexed { i, v -> v?.let { Offset(i * stepX, y(it)) } }
                    points.zipWithNext().forEach { (a, b) -> if (a != null && b != null) drawLine(color, a, b, strokeWidth = 4f) }
                    points.forEachIndexed { i, p ->
                        if (p != null) {
                            val dot = if (statuses[i] == VitalStatus.None) color else statuses[i].color
                            drawCircle(Color.White, radius = 9f, center = p)
                            drawCircle(dot, radius = 6f, center = p)
                        }
                    }
                }
                val statuses = chronological.map { it.status }
                drawSeries(chronological.map { e -> e.numericValue.takeIf { it > 0 || !isBP } }, lineColor, statuses)
                if (isBP) drawSeries(chronological.map { it.secondaryValue }, diaColor, statuses)
            }
        }
        // X axis: first and last reading.
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(chronological.first().time, fontSize = 10.sp, color = Color.Gray)
            Text(chronological.last().time, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 11.sp, color = Color.Gray)
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
    canGoNext: Boolean = true,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val today = LocalDate.now()
    val text = when (mode) {
        DateMode.DAILY -> when (currentDate) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }
        DateMode.WEEKLY -> {
            val start = currentDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)
            "${start.format(DateTimeFormatter.ofPattern("dd MMM"))} – ${end.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))}"
        }
        DateMode.MONTHLY -> currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        HapticIconButton(
            onClick = { onPrevious() },
            colors = IconButtonDefaults.iconButtonColors(contentColor = colors.btnDarkColor)
        ) { Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Previous") }

        Text(text, color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)

        HapticIconButton(
            onClick = { onNext() },
            enabled = canGoNext,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = colors.btnDarkColor,
                disabledContentColor = colors.btnDarkColor.copy(alpha = 0.25f),
            )
        ) { Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Next") }
    }
}
