package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.model.SleepStageItem
import com.critetiontech.ctvitalio.model.UltrahumanDashboardData
import com.critetiontech.ctvitalio.model.UltrahumanLastVitalItem
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.MyColorScheme
import com.critetiontech.ctvitalio.viewmodel.SmartRingViewModel

private val RingIndigo = Color(0xFF6366F1)
private val RingGreen  = Color(0xFF10B981)
private val RingBlue   = Color(0xFF0062FF)

private fun parseHexColor(hex: String?, default: Color): Color = try {
    if (hex.isNullOrBlank()) default else Color(android.graphics.Color.parseColor(hex.trim()))
} catch (e: Exception) { default }

// ─────────────────────────────────────────────────────────
// SCREEN (bottom-nav "Ring" tab)
// ─────────────────────────────────────────────────────────

@Composable
fun SmartRingScreen(viewModel: SmartRingViewModel = viewModel()) {
    val isAuthSuccess by viewModel.isAuthSuccess.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.onScreenResumed()
        onPauseOrDispose { }
    }

    AnimatedContent(
        targetState = isAuthSuccess,
        transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(350)) },
        label = "ring_state"
    ) { connected ->
        if (connected) SmartRingDashboard(viewModel) else SmartRingPairing(viewModel)
    }
}

// ─────────────────────────────────────────────────────────
// DASHBOARD (connected)
// ─────────────────────────────────────────────────────────

@Composable
private fun SmartRingDashboard(viewModel: SmartRingViewModel) {
    val colors    = LocalMyColorScheme.current
    val ringData  by viewModel.ringData.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    var showAllVitals by remember { mutableStateOf(false) }
    var showSleepInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + sync
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Ultrahuman Ring", style = AppTextStyles.style18BCB())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(RingGreen))
                    Spacer(Modifier.width(6.dp))
                    Text("Active • Live Telemetry", style = AppTextStyles.style12GCN(), color = RingGreen)
                }
            }
            val rotation by rememberInfiniteTransition(label = "sync").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
                label = "sync_rot"
            )
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(colors.primaryBlueColor, RingBlue)))
                    .clickable(enabled = !isSyncing) { viewModel.syncTelemetry() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Sync, contentDescription = null, tint = Color.White,
                    modifier = Modifier.size(16.dp).rotate(if (isSyncing) rotation else 0f)
                )
                Spacer(Modifier.width(6.dp))
                Text(if (isSyncing) "Syncing..." else "Sync", style = AppTextStyles.style12WCB())
            }
        }

        VitalInsightsCard(ringData, colors)
        SleepArchitectureCard(ringData, colors) { showSleepInfo = true }
        ClinicalVitalsCard(ringData.clinicalVitals, colors) { showAllVitals = true }

        // Disconnect / re-pair
        Row(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clip(CircleShape)
                .background(Color.Red.copy(alpha = 0.08f))
                .clickable { viewModel.disconnectRing() }
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.Red.copy(alpha = 0.85f), modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Disconnect / Re-pair Ring", style = AppTextStyles.style12GCB(), color = Color.Red.copy(alpha = 0.85f))
        }

        Spacer(Modifier.height(90.dp)) // clearance for voice FAB
    }

    if (showAllVitals) AllVitalsSheet(ringData.clinicalVitals, colors) { showAllVitals = false }
    if (showSleepInfo) SleepStagesInfoSheet(ringData.sleepStages, colors) { showSleepInfo = false }
}

@Composable
private fun RingSectionCard(colors: MyColorScheme, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
    }
}

@Composable
private fun RingSectionTitle(icon: ImageVector, title: String, tint: Color, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = AppTextStyles.style16BCB())
        Spacer(Modifier.width(6.dp))
        trailing()
    }
}

// ── Section 1: Vital insights ───────────────────────────
@Composable
private fun VitalInsightsCard(data: UltrahumanDashboardData, colors: MyColorScheme) {
    val wellnessColor = parseHexColor(data.wellnessColorHex, Color(0xFF0DC289))
    val insights = data.insightDetail?.insights

    RingSectionCard(colors) {
        RingSectionTitle(Icons.Default.AutoAwesome, "Vital Insights", colors.primaryBlueColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.dashboardBackgroundColor.copy(alpha = 0.6f))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ScoreRing(score = data.wellnessScore, color = wellnessColor, colors = colors)
            Spacer(Modifier.width(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Overall Wellness", style = AppTextStyles.style14BCB())
                    data.wellnessStatus?.let {
                        Spacer(Modifier.width(6.dp))
                        Text(
                            it, color = wellnessColor, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(wellnessColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(data.wellnessMessage, style = AppTextStyles.style12GCN(), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillarChip("Sleep", data.sleepScore, insights?.sleep?.displayTag, Color(0xFF0090F6), colors, Modifier.weight(1f))
            PillarChip("Recovery", data.recoveryScore, insights?.recovery?.displayTag, Color(0xFF0DC289), colors, Modifier.weight(1f))
            PillarChip("Movement", data.movementScore, insights?.movement?.displayTag, Color(0xFFF59E0B), colors, Modifier.weight(1f))
            PillarChip("Stress", data.stressScore, insights?.stress?.displayTag, Color(0xFF8B5CF6), colors, Modifier.weight(1f))
        }

        insights?.sleep?.message?.takeIf { it.isNotEmpty() }?.let {
            InsightBanner(Icons.Default.Bedtime, it, Color(0xFF0090F6))
        }
        insights?.recovery?.message?.takeIf { it.isNotEmpty() }?.let {
            InsightBanner(Icons.Default.Bolt, it, Color(0xFF0DC289))
        }
    }
}

@Composable
private fun ScoreRing(score: Int?, color: Color, colors: MyColorScheme) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(70.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            drawArc(color.copy(alpha = 0.2f), 0f, 360f, false, style = Stroke(stroke))
            drawArc(
                color, -90f, 360f * ((score ?: 0).coerceIn(0, 100) / 100f), false,
                style = Stroke(stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(score?.toString() ?: "--", fontSize = 22.sp, fontWeight = FontWeight.Black, color = colors.textDarkColor)
            Text("/100", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
        }
    }
}

@Composable
private fun PillarChip(title: String, score: Int?, tag: String?, color: Color, colors: MyColorScheme, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor.copy(alpha = 0.6f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray)
        Text(score?.toString() ?: "--", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Text(tag ?: "—", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = colors.textGreyColor, maxLines = 1)
    }
}

@Composable
private fun InsightBanner(icon: ImageVector, message: String, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(8.dp))
        Text(message, style = AppTextStyles.style12GCN())
    }
}

// ── Section 2: Sleep architecture ───────────────────────
@Composable
private fun SleepArchitectureCard(data: UltrahumanDashboardData, colors: MyColorScheme, onInfo: () -> Unit) {
    RingSectionCard(colors) {
        RingSectionTitle(Icons.Default.Hotel, "Sleep Architecture", colors.textDarkColor) {
            Icon(
                Icons.Default.Info, contentDescription = "Sleep stages guide", tint = RingIndigo,
                modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onInfo() }
            )
        }

        Row {
            Column(Modifier.weight(1f)) {
                Text("TOTAL SLEEP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(data.totalSleepText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RingIndigo)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("EFFICIENCY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Text(data.sleepEfficiencyText, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = RingGreen)
            }
        }

        val stages = data.sleepStages
        if (stages.isNotEmpty()) {
            // Proportional stage bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                stages.forEach { stage ->
                    Box(
                        Modifier
                            .weight(((stage.percentage ?: 5.0).coerceAtLeast(1.0)).toFloat())
                            .fillMaxHeight()
                            .background(Color(stage.colorHex))
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                stages.forEach { SleepStageRow(it, colors) }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SleepTile("Tosses & Turns", data.tossAndTurnsText, "times", Icons.Default.SwapHoriz, colors, Modifier.weight(1f))
            SleepTile("Night Avg HR", data.nightAvgHrText, "bpm", Icons.Default.Favorite, colors, Modifier.weight(1f))
            SleepTile("Night Avg HRV", data.nightAvgHrvText, "ms", Icons.Default.MonitorHeart, colors, Modifier.weight(1f))
        }
    }
}

private fun stageIcon(stage: SleepStageItem): ImageVector = when (stage.type?.lowercase()) {
    "deep_sleep" -> Icons.Default.NightsStay
    "rem_sleep" -> Icons.Default.AutoAwesome
    "light_sleep" -> Icons.Default.Cloud
    "awake" -> Icons.Default.Visibility
    else -> Icons.Default.Hotel
}

@Composable
private fun SleepStageRow(stage: SleepStageItem, colors: MyColorScheme) {
    val c = Color(stage.colorHex)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.dashboardBackgroundColor.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(c.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { Icon(stageIcon(stage), contentDescription = null, tint = c, modifier = Modifier.size(14.dp)) }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stage.title.orEmpty(), style = AppTextStyles.style12BCB())
                Spacer(Modifier.width(6.dp))
                Text(
                    stage.stageTimeText ?: "${(stage.percentage ?: 0.0).toInt()}%",
                    fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = c,
                    modifier = Modifier.clip(CircleShape).background(c.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Text(stage.meaning, style = AppTextStyles.style12GCN(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(
            stage.targetRange, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = Color.Gray,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(colors.dashboardContainerColor).padding(horizontal = 6.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun SleepTile(title: String, value: String, unit: String, icon: ImageVector, colors: MyColorScheme, modifier: Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor.copy(alpha = 0.6f))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RingIndigo, modifier = Modifier.size(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor)
            Spacer(Modifier.width(2.dp))
            Text(unit, fontSize = 9.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 2.dp))
        }
        Text(title, fontSize = 10.sp, color = Color.Gray, maxLines = 1)
    }
}

// ── Section 3: Clinical vitals (lastVital) ──────────────
private val prioritizedKeywords = listOf("recovery", "hrv", "movement", "vo2", "cardio", "stress", "glucose", "waste", "calorie", "step")

private fun previewVitals(vitals: List<UltrahumanLastVitalItem>): List<UltrahumanLastVitalItem> {
    val result = mutableListOf<UltrahumanLastVitalItem>()
    for (kw in prioritizedKeywords) {
        vitals.firstOrNull { v -> v.vitalName?.lowercase()?.contains(kw) == true && result.none { it.key == v.key } }
            ?.let { result += it }
        if (result.size == 6) return result
    }
    for (v in vitals) {
        if (result.size == 6) break
        if (result.none { it.key == v.key }) result += v
    }
    return result
}

private fun vitalIcon(name: String): ImageVector {
    val l = name.lowercase()
    return when {
        l.contains("recovery") -> Icons.Default.BatteryChargingFull
        l.contains("hrv") -> Icons.Default.MonitorHeart
        l.contains("movement") -> Icons.Default.DirectionsWalk
        l.contains("vo2") -> Icons.Default.Air
        l.contains("cardio") -> Icons.Default.Favorite
        l.contains("stress") -> Icons.Default.Psychology
        l.contains("glucose") -> Icons.Default.WaterDrop
        l.contains("waste") -> Icons.Default.AutoAwesome
        l.contains("calorie") -> Icons.Default.LocalFireDepartment
        l.contains("step") -> Icons.Default.DirectionsRun
        l.contains("active") -> Icons.Default.Bolt
        l.contains("restinghr") || l.contains("rhr") -> Icons.Default.Bedtime
        l.contains("water") -> Icons.Default.LocalDrink
        else -> Icons.Default.BarChart
    }
}

private fun vitalDefaultColor(name: String, fallback: Color): Color {
    val l = name.lowercase()
    return when {
        l.contains("recovery") -> Color(0xFF10B981)
        l.contains("hrv") -> Color(0xFF8B5CF6)
        l.contains("movement") -> Color(0xFF3B82F6)
        l.contains("vo2") -> Color(0xFF06B6D4)
        l.contains("cardio") -> Color(0xFFEC4899)
        l.contains("stress") || l.contains("glucose") -> Color(0xFFF59E0B)
        else -> fallback
    }
}

@Composable
private fun ClinicalVitalsCard(vitals: List<UltrahumanLastVitalItem>, colors: MyColorScheme, onViewAll: () -> Unit) {
    val preview = remember(vitals) { previewVitals(vitals) }
    RingSectionCard(colors) {
        RingSectionTitle(Icons.Default.MonitorHeart, "Clinical Vitals", colors.textDarkColor)

        if (preview.isEmpty()) {
            Text("No ring vitals recorded yet. Tap Sync to refresh.", style = AppTextStyles.style12GCN())
        } else {
            preview.chunked(2).forEach { rowItems ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    rowItems.forEach { VitalTile(it, colors, Modifier.weight(1f)) }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        if (vitals.size > 6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAll() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("View all ${vitals.size} vitals recorded", style = AppTextStyles.style14BCB(), color = colors.primaryBlueColor)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun VitalTile(item: UltrahumanLastVitalItem, colors: MyColorScheme, modifier: Modifier) {
    val name = item.vitalName.orEmpty()
    val c = parseHexColor(item.vitalColor, vitalDefaultColor(name, colors.primaryBlueColor))
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colors.dashboardBackgroundColor.copy(alpha = 0.6f))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(vitalIcon(name), contentDescription = null, tint = c, modifier = Modifier.size(16.dp))
            Spacer(Modifier.weight(1f))
            item.severityLevel?.takeIf { it.isNotEmpty() }?.let {
                Text(
                    it, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = c,
                    modifier = Modifier.clip(CircleShape).background(c.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(item.formattedValue, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor)
            Spacer(Modifier.width(3.dp))
            Text(item.unit.orEmpty(), fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 3.dp))
        }
        Text(name.ifEmpty { "Vital" }, style = AppTextStyles.style12GCN(), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ── Sheets ──────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AllVitalsSheet(vitals: List<UltrahumanLastVitalItem>, colors: MyColorScheme, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.dashboardContainerColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("All Recorded Vitals", style = AppTextStyles.style16BCB(), modifier = Modifier.padding(bottom = 12.dp))
            vitals.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(item.vitalName ?: "Vital", style = AppTextStyles.style14BCB())
                        item.vitalDateTime?.let { Text(it, style = AppTextStyles.style12GCN()) }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${item.formattedValue} ${item.unit.orEmpty()}",
                            fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            color = parseHexColor(item.vitalColor, colors.primaryBlueColor)
                        )
                        item.severityLevel?.let { Text(it, fontSize = 10.sp, color = Color.Gray) }
                    }
                }
                HorizontalDivider(color = colors.borderGreyLightColor)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepStagesInfoSheet(stages: List<SleepStageItem>, colors: MyColorScheme, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.dashboardContainerColor) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Understanding Sleep Stages", style = AppTextStyles.style18BCB())
            Text(
                "Your smart ring monitors biometric shifts including heart rate, HRV, and skin temperature to classify your sleep into four distinct stages:",
                style = AppTextStyles.style14GCN()
            )
            stages.forEach { stage ->
                val c = Color(stage.colorHex)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.dashboardBackgroundColor)
                        .padding(14.dp)
                ) {
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(c.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) { Icon(stageIcon(stage), contentDescription = null, tint = c, modifier = Modifier.size(18.dp)) }
                    Spacer(Modifier.width(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stage.title.orEmpty(), style = AppTextStyles.style14BCB(), modifier = Modifier.weight(1f))
                            Text(
                                stage.targetRange, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = c,
                                modifier = Modifier.clip(CircleShape).background(c.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(stage.meaning, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c)
                        Text(stage.detailedDescription, style = AppTextStyles.style12GCN())
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// PAIRING (disconnected)
// ─────────────────────────────────────────────────────────

@Composable
private fun SmartRingPairing(viewModel: SmartRingViewModel) {
    val context      = LocalContext.current
    val colors       = LocalMyColorScheme.current
    val isLoading    by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val transition = rememberInfiniteTransition(label = "pairing")
    val pulse by transition.animateFloat(
        initialValue = 0.94f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1600, easing = EaseInOut), RepeatMode.Reverse),
        label = "pulse"
    )
    val badgeAlpha by transition.animateFloat(
        initialValue = 0.4f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = EaseInOut), RepeatMode.Reverse),
        label = "badge"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(10.dp))

        // Ring graphic with aura
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Box(
                Modifier
                    .size(140.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(colors.primaryBlueColor.copy(alpha = 0.25f), Color.Transparent)))
            )
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(colors.dashboardContainerColor)
                    .border(
                        9.dp,
                        Brush.sweepGradient(listOf(colors.primaryBlueColor, Color.Cyan, Color.White, colors.primaryBlueColor)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.MonitorHeart, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(24.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Connect Your Smart Ring", style = AppTextStyles.style24BCB(), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text("Let's get your ring talking to Vitalio!", style = AppTextStyles.style16GCN(), color = Color(0xFF3C5A87), textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Text(
            "To securely link your Ultrahuman Ring and begin syncing your activity, recovery, and vitals data, Vitalio requires your approval to access the Ultrahuman platform.",
            style = AppTextStyles.style14GCN(), textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(26.dp))

        // Vitalio <-> Ultrahuman link
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LinkBadge(Brush.linearGradient(listOf(colors.primaryBlueColor, Color(0xFF0247C6))), badgeAlpha) {
                Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Box(contentAlignment = Alignment.Center, modifier = Modifier.width(80.dp)) {
                HorizontalDivider(color = RingGreen.copy(alpha = badgeAlpha), thickness = 2.dp)
                Icon(
                    Icons.Default.Link, contentDescription = null, tint = Color.Gray,
                    modifier = Modifier.background(colors.dashboardBackgroundColor).padding(horizontal = 4.dp).size(20.dp)
                )
            }
            LinkBadge(Brush.linearGradient(listOf(Color(0xFF111827), Color.Black)), badgeAlpha) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.RadioButtonChecked, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                    Text("RING", fontSize = 8.sp, fontWeight = FontWeight.Black, color = Color.Cyan)
                }
            }
        }

        Spacer(Modifier.height(26.dp))

        errorMessage?.let {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFFFA500).copy(alpha = 0.12f))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFA500), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(it, fontSize = 13.sp, color = Color(0xFFFFA500))
            }
            Spacer(Modifier.height(16.dp))
        }

        Text(
            "We will take you to Ultrahuman to authorize secure access. You will immediately return to Vitalio once connection is confirmed.",
            style = AppTextStyles.style12GCN(), textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        HapticButton(
            onClick = { viewModel.startOAuthFlow(context) },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primaryBlueColor,
                disabledContainerColor = colors.primaryBlueColor.copy(alpha = 0.7f),
                disabledContentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(if (isLoading) "Connecting..." else "Continue to Ultrahuman", style = AppTextStyles.style16WCB())
        }

        Spacer(Modifier.height(90.dp)) // clearance for voice FAB
    }
}

@Composable
private fun LinkBadge(brush: Brush, alpha: Float, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush, alpha = alpha),
        contentAlignment = Alignment.Center
    ) { content() }
}