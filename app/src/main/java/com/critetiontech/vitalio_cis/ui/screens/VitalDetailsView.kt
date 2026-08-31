package com.critetiontech.vitalio_cis.ui.screens


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
import androidx.compose.ui.draw.clip
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.VitalDetailViewModel
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

    CommonAppBar(
        title = "Vitals",
        actions = {
            IconButton(onClick = { showInfoDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Color info",
                    tint = colors.textDarkColor
                )
            }
        }
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            val vitals  by viewModel.vitalList.collectAsState()
            val loading by viewModel.loading.collectAsState()

            when {
                loading && vitals.isEmpty() -> VitalListShimmer()

                !loading && vitals.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No vitals recorded yet", color = Color(0xFF9E9E9E))
                    }
                }

                else -> {
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
                                unit     = vital.unit.replace("/", ""),
                                color    = getVitalValueColor(vital.vitalName, vital.vitalValue),
                                time     = formatTimeAgo(vital.vitalDateTime),
                                vitalIds = "${vital.vitalID}"
                            )
                        }
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
            TextButton(onClick = onDismiss) { Text("Got it") }
        },
        title = { Text("Vital Color Guide", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                ColorLegendRow(Color(0xFF4CAF50), "Normal", "Values within healthy range")
                ColorLegendRow(Color(0xFFFF9800), "Borderline", "Slightly outside normal range")
                ColorLegendRow(Color(0xFFF44336), "Abnormal", "Requires attention")

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
        "pulse" -> when {
            value in 60.0..100.0 -> Color(0xFF4CAF50)
            value in 50.0..110.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        "temperature" -> {
            val tempC = if (value > 45) (value - 32) * 5 / 9 else value
            when {
                tempC in 36.1..37.2 -> Color(0xFF4CAF50)
                tempC in 35.0..38.0 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }
        }
        "spo2", "oxygen saturation" -> when {
            value >= 95.0 -> Color(0xFF4CAF50)
            value >= 90.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        "respiratory rate", "rr" -> when {
            value in 12.0..20.0 -> Color(0xFF4CAF50)
            value in 10.0..24.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        "blood glucose", "glucose", "fbs", "rbs" -> when {
            value in 70.0..140.0 -> Color(0xFF4CAF50)
            value in 50.0..200.0 -> Color(0xFFFF9800)
            else -> Color(0xFFF44336)
        }
        "weight", "height" -> Color(0xFF2962FF)
        else -> Color(0xFF4CAF50)
    }
}

fun getBpColor(sys: Double, dia: Double): Color {
    return when {
        sys < 90 || dia < 60 -> Color(0xFFF44336)
        sys <= 120 && dia <= 80 -> Color(0xFF4CAF50)
        sys <= 130 && dia <= 80 -> Color(0xFFFF9800)
        sys <= 140 || dia <= 90 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clickable {
                val encodedName = Uri.encode(title)
                navController.navigate("${Routes.VITALHISTORY}?vitalIds=$vitalIds&vitalName=$encodedName")
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    Text(
                        text = title,
                        style = AppTextStyles.style16BCB())



                }

                Spacer(modifier = Modifier.height(4.dp))


                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.Bottom) {

                    Text(
                        value,
                        color = color,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        unit,
                        style = AppTextStyles.style12GCN())

                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    time,
                    style = AppTextStyles.style12GCN().copy(fontSize = 11.sp))

            }

            Column   {
                Text(
                    "Add Vital",
                            style = AppTextStyles.style12PCN(),
                    modifier = Modifier.clickable {
                        navController.navigate(Routes.CONNECTION+"/"+title.toString()+"/"+unit.toString())
                    })

//                MiniGraph(color)
            }
        }
    }
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


