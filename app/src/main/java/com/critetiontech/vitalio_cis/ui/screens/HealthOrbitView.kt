package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import kotlin.math.cos
import kotlin.math.sin

import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.vitalio_cis.viewmodel.VitalDetailViewModel

import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes

@Composable
fun HealthOrbitScreen(viewModel: VitalDetailViewModel = viewModel()) {
    val vitals by viewModel.vitalList.collectAsState()
    val navController = LocalNavController.current
    
    LaunchedEffect(Unit) {
        viewModel.fetchLastVital()
    }

    // Default values if API data is missing
    var heartRate = 75f 
    var oxygen = 98f    
    var bpSys = 120f
    var bpDia = 80f
    var temperature = 98.6f
    var respRate = 16f
    var glucose = 100f

    // Map dynamic values from API
    vitals.forEach { vital ->
        val v = vital.vitalValue.toFloat()
        when (vital.vitalName.lowercase()) {
            "pulse" -> heartRate = v
            "spo2" -> oxygen = v
            "bp_sys" -> bpSys = v
            "bp_dias" -> bpDia = v
            "temperature" -> temperature = v
            "resprate" -> respRate = v
            "rbs", "glucose" -> glucose = v
        }
    }

    val hasWarning = heartRate > 100 || oxygen < 95 || bpSys > 130 || bpDia > 90 || temperature > 99.5f || respRate > 20 || glucose !in 70f..140f

    CommonAppBar(title = "Biometric HUD") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF070B14))
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // 1. Live Diagnostic Terminal
            DiagnosticTerminal(heartRate, oxygen, bpSys, hasWarning)

            // 2. The HUD Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                BiometricRadarVisualizer(heartRate, oxygen, bpSys, bpDia, temperature, respRate, glucose) { title, unit ->
                    navController.navigate("${Routes.CONNECTION}/$title/$unit")
                }
            }
        }
    }
}

@Composable
fun DiagnosticTerminal(hr: Float, spo2: Float, sys: Float, hasWarning: Boolean) {
    val clinicalBlue = Color(0xFF00D4FF)
    val alertRed = Color(0xFFFF4C4C)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A), RoundedCornerShape(8.dp))
            .border(1.dp, if (hasWarning) alertRed else Color(0xFF334155), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Column {
            Text("> SYSTEM.SCANNING()...", color = clinicalBlue, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            
            val hrText = if(hr > 100) "TACHYCARDIA WARN" else "NOMINAL"
            val o2Text = if(spo2 < 95) "HYPOXIA WARN" else "NOMINAL"
            val bpText = if(sys > 130) "HYPERTENSION WARN" else "NOMINAL"
            
            Spacer(modifier = Modifier.height(4.dp))
            Text("> HR CHECK : $hrText", color = if(hr > 100) alertRed else Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text("> O2 CHECK : $o2Text", color = if(spo2 < 95) alertRed else Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            Text("> BP CHECK : $bpText", color = if(sys > 130) alertRed else Color.White, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            
            Spacer(modifier = Modifier.height(8.dp))
            if (hasWarning) {
                Text(">> ACTION REQUIRED: REFER TO CARE PLAN", color = alertRed, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            } else {
                Text(">> OVERALL STATUS: ALL SYSTEMS CLEAR", color = clinicalBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

@Composable
fun BiometricRadarVisualizer(
    hr: Float, spo2: Float, bpSys: Float, bpDia: Float, temp: Float, rr: Float, glucose: Float,
    onNodeClick: (title: String, unit: String) -> Unit
) {
    val textMeasurer = rememberTextMeasurer()
    val infiniteTransition = rememberInfiniteTransition(label = "radar")

    val radarAngle by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(4000, easing = LinearEasing)), label = "sweep")
    val rot1State = infiniteTransition.animateFloat(initialValue = 0f, targetValue = 360f, animationSpec = infiniteRepeatable(animation = tween(25000, easing = LinearEasing)), label = "r1")
    val rot2State = infiniteTransition.animateFloat(initialValue = 120f, targetValue = 480f, animationSpec = infiniteRepeatable(animation = tween(35000, easing = LinearEasing)), label = "r2")
    val rot3State = infiniteTransition.animateFloat(initialValue = 240f, targetValue = 600f, animationSpec = infiniteRepeatable(animation = tween(45000, easing = LinearEasing)), label = "r3")
    val rot4State = infiniteTransition.animateFloat(initialValue = 180f, targetValue = 540f, animationSpec = infiniteRepeatable(animation = tween(30000, easing = LinearEasing)), label = "r4")

    val rot1 by rot1State
    val rot2 by rot2State
    val rot3 by rot3State
    val rot4 by rot4State

    val clinicalBlue = Color(0xFF00D4FF)
    val alertRed = Color(0xFFFF4C4C)
    
    val cHr = if (hr in 60f..100f) clinicalBlue else alertRed
    val cSpo2 = if (spo2 >= 95f) clinicalBlue else alertRed
    val cBpSys = if (bpSys <= 120f) clinicalBlue else alertRed
    val cBpDia = if (bpDia <= 80f) clinicalBlue else alertRed
    val cTemp = if (temp <= 99.0f) clinicalBlue else alertRed
    val cRr = if (rr in 12f..20f) clinicalBlue else alertRed
    val cGlu = if (glucose in 70f..140f) clinicalBlue else alertRed

    val radarColor = clinicalBlue
    val hasWarning = hr > 100 || spo2 < 95 || bpSys > 130 || bpDia > 90 || temp > 99.5f || rr > 20 || glucose !in 70f..140f
    val corePulse by infiniteTransition.animateFloat(initialValue = 0.8f, targetValue = 1.2f, animationSpec = infiniteRepeatable(animation = tween((60000 / hr.coerceAtLeast(40f)).toInt() / 2, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse), label = "pulse")
    val warningFlash by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 0.25f, animationSpec = infiniteRepeatable(animation = tween(800, easing = LinearEasing), repeatMode = RepeatMode.Reverse), label = "flash")

    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize().pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val maxRadius = size.width / 2.2f
                val orbit1 = maxRadius * 0.35f
                val orbit2 = maxRadius * 0.55f
                val orbit3 = maxRadius * 0.75f
                val orbit4 = maxRadius * 0.95f

                fun isHit(rot: Float, radius: Float): Boolean {
                    val nx = center.x + radius * cos(Math.toRadians(rot.toDouble())).toFloat()
                    val ny = center.y + radius * sin(Math.toRadians(rot.toDouble())).toFloat()
                    val dx = tapOffset.x - nx
                    val dy = tapOffset.y - ny
                    return (dx * dx + dy * dy) < (60f * 60f) // 60px hit area
                }

                if (isHit(rot1State.value, orbit1)) onNodeClick("Pulse", "BPM")
                else if (isHit(rot1State.value + 180f, orbit1)) onNodeClick("Temperature", "F")
                else if (isHit(rot2State.value, orbit2)) onNodeClick("SpO2", "%")
                else if (isHit(rot2State.value + 180f, orbit2)) onNodeClick("RespRate", "rpm")
                else if (isHit(rot3State.value, orbit3)) onNodeClick("BP_Sys", "mmHg")
                else if (isHit(rot3State.value + 180f, orbit3)) onNodeClick("BP_Dias", "mmHg")
                else if (isHit(rot4State.value, orbit4)) onNodeClick("Glucose", "mgdL")
            }
        }) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.width / 2.2f

            val orbit1 = maxRadius * 0.35f
            val orbit2 = maxRadius * 0.55f
            val orbit3 = maxRadius * 0.75f
            val orbit4 = maxRadius * 0.95f

            if (hasWarning) drawRect(brush = Brush.radialGradient(listOf(Color.Transparent, Color.Transparent, Color(0xFFFF3D00).copy(alpha = warningFlash)), center = center, radius = size.width), size = size)
            
            drawLine(Color.DarkGray.copy(0.3f), Offset(center.x, 0f), Offset(center.x, size.height), strokeWidth = 2f)
            drawLine(Color.DarkGray.copy(0.3f), Offset(0f, center.y), Offset(size.width, center.y), strokeWidth = 2f)

            drawCircle(Color.Gray.copy(0.2f), orbit1, center, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))))
            drawCircle(Color.Gray.copy(0.2f), orbit2, center, style = Stroke(4f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 20f))))
            drawCircle(Color.Gray.copy(0.2f), orbit3, center, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f))))
            drawCircle(Color.Gray.copy(0.2f), orbit4, center, style = Stroke(2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 20f))))

            drawArc(Brush.sweepGradient(listOf(Color.Transparent, radarColor.copy(0.15f), radarColor.copy(0.5f)), center), radarAngle - 90f, 90f, true, Offset(center.x - orbit4, center.y - orbit4), androidx.compose.ui.geometry.Size(orbit4 * 2, orbit4 * 2))

            drawCircle(radarColor.copy(0.2f), 24f * corePulse, center)
            drawCircle(radarColor.copy(0.1f), 32f * corePulse, center)

            fun drawDataNode(rotation: Float, radius: Float, color: Color, label: String, value: String, norm: String) {
                val nX = center.x + radius * cos(Math.toRadians(rotation.toDouble())).toFloat()
                val nY = center.y + radius * sin(Math.toRadians(rotation.toDouble())).toFloat()
                
                for (i in 1..8) {
                    val trailRot = rotation - (i * 3f) 
                    val tX = center.x + radius * cos(Math.toRadians(trailRot.toDouble())).toFloat()
                    val tY = center.y + radius * sin(Math.toRadians(trailRot.toDouble())).toFloat()
                    drawCircle(color.copy(alpha = 0.6f * (1f - (i / 8f))), 4f - (i * 0.3f), Offset(tX, tY))
                }

                drawLine(color.copy(0.4f), center, Offset(nX, nY), strokeWidth = 2f)
                drawCircle(color, 5f, Offset(nX, nY))
                drawCircle(color.copy(0.3f), 12f, Offset(nX, nY))

                val isLeft = nX < center.x
                val lineDir = if (isLeft) -20f else 20f
                val textStart = if (isLeft) -80f else 25f

                drawLine(color, Offset(nX + (if(isLeft) -5f else 5f), nY), Offset(nX + lineDir, nY - 10f), strokeWidth = 2f)
                drawLine(color, Offset(nX + lineDir, nY - 10f), Offset(nX + lineDir + (if(isLeft) -60f else 60f), nY - 10f), strokeWidth = 1f)
                
                // Text with Reference Range
                drawText(textMeasurer, "$label:$value\n[$norm]", Offset(nX + textStart, nY - 32f), TextStyle(color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
            }

            drawDataNode(rot1, orbit1, cHr, "HR", "${hr.toInt()}", "60-100")
            drawDataNode(rot1 + 180f, orbit1, cTemp, "TMP", String.format("%.1f", temp), "97-99")
            drawDataNode(rot2, orbit2, cSpo2, "O2", "${spo2.toInt()}%", "95-100")
            drawDataNode(rot2 + 180f, orbit2, cRr, "RR", "${rr.toInt()}", "12-20")
            drawDataNode(rot3, orbit3, cBpSys, "SYS", "${bpSys.toInt()}", "90-120")
            drawDataNode(rot3 + 180f, orbit3, cBpDia, "DIA", "${bpDia.toInt()}", "60-80")
            drawDataNode(rot4, orbit4, cGlu, "GLU", "${glucose.toInt()}", "70-140")
        }

        Icon(androidx.compose.material.icons.Icons.Default.Accessibility, "Human Core", tint = radarColor.copy(0.9f)
            , modifier = Modifier.size(36.dp))
    }
}
