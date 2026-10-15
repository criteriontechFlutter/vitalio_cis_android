package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.contentStateOf

import com.critetiontech.ctvitalio.ui.feedback.ContentStateCrossfade

import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton


import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.Bloodtype
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Height
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import android.net.Uri
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.VitalDetailViewModel
import com.critetiontech.ctvitalio.ui.adaptive.rememberResponsiveDimens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ─── Shimmer ─────────────────────────────────────────────────────────────────

@Composable
private fun vitalShimmerBrush(): Brush {
    val shimmerColors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0))
    val transition = rememberInfiniteTransition(label = "vitalShimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "vitalShimmerX"
    )
    return Brush.linearGradient(shimmerColors, start = Offset(translateX, 0f), end = Offset(translateX + 600f, 0f))
}

@Composable
private fun VitalCardShimmer(brush: Brush) {
    val colors = LocalMyColorScheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Title placeholder
                Box(modifier = Modifier.width(130.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(modifier = Modifier.height(12.dp))
                // Value + unit row
                Row(verticalAlignment = Alignment.Bottom) {
                    Box(modifier = Modifier.width(64.dp).height(26.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(modifier = Modifier.width(36.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
                Spacer(modifier = Modifier.height(8.dp))
                // Time placeholder
                Box(modifier = Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
            // "Add Vital" placeholder
            Box(modifier = Modifier.width(52.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
    }
}

@Composable
private fun VitalListShimmer() {
    val brush = vitalShimmerBrush()
    repeat(6) { VitalCardShimmer(brush) }
}

// ─────────────────────────────────────────────────────────────────────────────

// Value colours: normal / borderline / abnormal.
private val VitalNormal     = Color(0xFF1F5BE0)
private val VitalBorderline = Color(0xFFE0A100)
private val VitalAbnormal   = Color(0xFFE5172F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(viewModel: VitalDetailViewModel = viewModel()) {

    val colors = LocalMyColorScheme.current
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchLastVital()
    }

    if (showInfoDialog) {
        VitalColorInfoDialog(onDismiss = { showInfoDialog = false })
    }

    val navController = LocalNavController.current

    CommonAppBar(
        title = "Vitals",
        actions = {
            HapticIconButton(onClick = { navController.navigate(Routes.HEALTH_ORBIT) }) {
                Icon(
                    imageVector = androidx.compose.material.icons.Icons.Default.Analytics,
                    contentDescription = "Health Orbit",
                    tint = colors.textDarkColor
                )
            }
            HapticIconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Color info",
                    tint = colors.textDarkColor
                )
            }
        }
    ) {
        val vitalDimens = rememberResponsiveDimens()

        Column(
            modifier = Modifier
                .padding(vitalDimens.screenPadding)
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            val vitals  by viewModel.vitalList.collectAsState()
            val loading by viewModel.loading.collectAsState()

            ContentStateCrossfade(
                state = contentStateOf(loading = loading, empty = vitals.isEmpty()),
                loading = {
                    VitalListShimmer()
                },
                empty = {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No vitals recorded yet", color = Color(0xFF9E9E9E))
                    }
                },
            ) {
                val bpSys = vitals.find { it.vitalName.equals("BP_Sys", true) }
                val bpDia = vitals.find { it.vitalName.equals("BP_Dias", true) }

                if (bpSys != null && bpDia != null) {
                    VitalCard(
                        title    = "Blood Pressure",
                        value    = "${bpSys.vitalValue.toInt()}/${bpDia.vitalValue.toInt()}",
                        unit     = "mmHg",
                        color    = getBpColor(bpSys.vitalValue, bpDia.vitalValue),
                        time     = formatTimeAgo(bpSys.vitalDateTime),
                        vitalIds = "${bpSys.vitalID},${bpDia.vitalID}"
                    )
                }

                vitals
                    .filter { it.vitalName != "BP_Sys" && it.vitalName != "BP_Dias" }
                    .forEach { vital ->
                        VitalCard(
                            title    = getVitalDisplayName(vital.vitalName),
                            value    = formatValue(vital.vitalValue),
                            unit     = vital.unit.trim().removePrefix("/"),
                            color    = getVitalValueColor(vital.vitalName, vital.vitalValue),
                            time     = formatTimeAgo(vital.vitalDateTime),
                            vitalIds = "${vital.vitalID}"
                        )
                    }

            }
        }
    }
}

@Composable
fun VitalColorInfoDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            HapticTextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("Vital Color Guide", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                ColorLegendRow(VitalNormal, "Normal", "Values within healthy range")
                ColorLegendRow(VitalBorderline, "Borderline", "Slightly outside normal range")
                ColorLegendRow(VitalAbnormal, "Abnormal", "Requires attention")

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text("Normal Ranges", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                val ranges = listOf(
                    "Blood Pressure" to "Sys 90–120 / Dia 60–80 mmHg",
                    "Pulse" to "60–100 bpm",
                    "Temperature" to "36.1–37.2 °C / 97–99 °F",
                    "SpO2" to "≥ 95%",
                    "Respiratory Rate" to "12–20 breaths/min",
                    "Blood Glucose" to "70–140 mg/dL"
                )
                ranges.forEach { (name, range) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(name, fontSize = 12.sp, color = Color(0xFF555555))
                        Text(range, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    )
}

@Composable
fun ColorLegendRow(color: Color, label: String, description: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = color)
            Text(description, fontSize = 11.sp, color = Color(0xFF777777))
        }
    }
}

fun formatValue(value: Double): String {
    return if (value % 1 == 0.0) {
        value.toInt().toString()
    } else {
        value.toString()
    }
}
fun getVitalDisplayName(vitalName: String): String = when (vitalName.lowercase()) {
    "pulse" -> "Pulse"
    "temperature" -> "Temperature"
    "resprate" -> "Respiratory Rate"
    "spo2" -> "SpO2"
    "heartrate" -> "Heart Rate"
    "weight" -> "Weight"
    "height" -> "Height"
    "rbs" -> "Blood Glucose"
    else -> vitalName
}

fun getVitalValueColor(vitalName: String, value: Double): Color {
    return when (vitalName.lowercase()) {
        "pulse", "heartrate", "heart rate" -> when {
            value in 60.0..100.0 -> VitalNormal
            value in 50.0..110.0 -> VitalBorderline
            else -> VitalAbnormal
        }
        "temperature" -> {
            val tempC = if (value > 45) (value - 32) * 5 / 9 else value
            when {
                tempC in 36.1..37.2 -> VitalNormal
                tempC in 35.0..38.0 -> VitalBorderline
                else -> VitalAbnormal
            }
        }
        "spo2", "oxygen saturation" -> when {
            value >= 95.0 -> VitalNormal
            value >= 90.0 -> VitalBorderline
            else -> VitalAbnormal
        }
        "respiratory rate", "resprate", "rr" -> when {
            value in 12.0..20.0 -> VitalNormal
            value in 10.0..24.0 -> VitalBorderline
            else -> VitalAbnormal
        }
        "blood glucose", "glucose", "fbs", "rbs" -> when {
            value in 70.0..140.0 -> VitalNormal
            value in 50.0..200.0 -> VitalBorderline
            else -> VitalAbnormal
        }
        "weight", "height" -> VitalNormal
        else -> VitalNormal
    }
}

fun getBpColor(sys: Double, dia: Double): Color {
    return when {
        sys < 90 || dia < 60 -> VitalAbnormal
        sys <= 120 && dia <= 80 -> VitalNormal
        sys <= 130 && dia <= 80 -> VitalBorderline
        sys <= 140 || dia <= 90 -> VitalBorderline
        else -> VitalAbnormal
    }
}

@Composable
fun VitalCard(
    title: String,
    value: String,
    unit: String,
    color: Color,
    time: String,
    vitalIds: String = ""
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .clickable {
                val encodedName = Uri.encode(title)
                navController.navigate("${Routes.VITALHISTORY}?vitalIds=$vitalIds&vitalName=$encodedName")
            }
            .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = vitalIcon(title),
                contentDescription = null,
                tint = Color(0xFF8A93A3),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = AppTextStyles.style14BCN().copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Text(
                "Add Vital",
                style = AppTextStyles.style12GCN().copy(fontSize = 11.sp, color = colors.textGreyColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { navController.navigate(Routes.CONNECTION + "/" + title + "/" + unit.replace("/", "")) }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        Spacer(Modifier.height(2.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                color = color,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 28.sp
            )
            if (unit.isNotBlank()) {
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    style = AppTextStyles.style12GCN().copy(fontSize = 13.sp),
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                time,
                style = AppTextStyles.style12GCN().copy(fontSize = 10.sp, color = colors.textDarkColor.copy(alpha = 0.7f)),
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

private fun vitalIcon(title: String): ImageVector = when {
    title.contains("pressure", true) -> Icons.Outlined.Speed
    title.contains("pulse", true) || title.contains("heart", true) -> Icons.Outlined.MonitorHeart
    title.contains("spo2", true) || title.contains("oxygen", true) -> Icons.Outlined.WaterDrop
    title.contains("temp", true) -> Icons.Outlined.DeviceThermostat
    title.contains("resp", true) -> Icons.Outlined.Air
    title.contains("glucose", true) || title.contains("rbs", true) || title.contains("sugar", true) -> Icons.Outlined.Bloodtype
    title.contains("weight", true) -> Icons.Outlined.MonitorWeight
    title.contains("height", true) -> Icons.Outlined.Height
    else -> Icons.Outlined.FavoriteBorder
}


fun formatTimeAgo(dateTime: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(dateTime)

        val diff = Date().time - (date?.time ?: 0)

        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)

        when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "$minutes min ago"
            hours < 24 -> "$hours hr ago"
            else -> "${TimeUnit.MILLISECONDS.toDays(diff)} days ago"
        }

    } catch (e: Exception) {
        ""
    }
}
@Composable
fun MiniGraph(color: Color) {

    Canvas(
        modifier = Modifier
            .width(100.dp)
            .height(50.dp)
    ) {

        val path = Path()

        path.moveTo(0f, size.height / 2)

        for (i in 1..10) {

            val x = size.width / 10 * i
            val y = (size.height / 2) + (-20..20).random()

            path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3f)
        )
    }
}


