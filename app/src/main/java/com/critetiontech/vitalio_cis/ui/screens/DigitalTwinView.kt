package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope

// ─── Digital Twin Logic Engine ─────────────────────────────────────────────

enum class TwinState {
    PERFECT,
    DEHYDRATED,
    NEEDS_MEDICINE,
    CRITICAL
}

class DigitalTwinViewModel : ViewModel() {
    // Simulated inputs from the rest of the app
    val fluidIntakePercentage = MutableStateFlow(100f) // 0 to 100%
    val pendingMedicinesCount = MutableStateFlow(0) // 0 means all taken
    val vitalsNormal = MutableStateFlow(true)

    // The core engine logic
    val twinState: StateFlow<TwinState> = combine(
        fluidIntakePercentage, pendingMedicinesCount, vitalsNormal
    ) { fluid, meds, vitals ->
        when {
            !vitals -> TwinState.CRITICAL
            meds > 0 && fluid < 50f -> TwinState.CRITICAL
            meds > 0 -> TwinState.NEEDS_MEDICINE
            fluid < 50f -> TwinState.DEHYDRATED
            else -> TwinState.PERFECT
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), TwinState.PERFECT)
}

// ─── Screen UI ─────────────────────────────────────────────────────────────

@Composable
fun DigitalTwinScreen(viewModel: DigitalTwinViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val state by viewModel.twinState.collectAsState()
    
    val fluidLevel by viewModel.fluidIntakePercentage.collectAsState()
    val medsPending by viewModel.pendingMedicinesCount.collectAsState()
    val vitalsOk by viewModel.vitalsNormal.collectAsState()

    CommonAppBar(title = "Digital Twin (Beta)") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Your Health Avatar",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textDarkColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "The avatar responds to your daily logs in real-time.",
                fontSize = 14.sp,
                color = colors.textGreyColor
            )

            Spacer(modifier = Modifier.height(30.dp))

            // The Avatar View
            AvatarVisualizer(state = state)

            Spacer(modifier = Modifier.height(40.dp))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Current State: ${state.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (state) {
                            TwinState.PERFECT -> Color(0xFF4CAF50)
                            TwinState.DEHYDRATED -> Color(0xFFFFA000)
                            TwinState.NEEDS_MEDICINE -> Color(0xFFE53935)
                            TwinState.CRITICAL -> Color(0xFFB71C1C)
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = getStatusMessage(state),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = colors.textDarkColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Control Panel (For testing the logic)
            Text(
                text = "Developer Control Panel",
                fontWeight = FontWeight.Bold,
                color = colors.textDarkColor,
                modifier = Modifier.align(Alignment.Start)
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Fluid Slider
            Text(text = "Fluid Intake: ${fluidLevel.toInt()}%", color = colors.textDarkColor)
            Slider(
                value = fluidLevel,
                onValueChange = { viewModel.fluidIntakePercentage.value = it },
                valueRange = 0f..100f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Medicine Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Pending Medicines: $medsPending", color = colors.textDarkColor)
                Row {
                    Button(onClick = { if (medsPending > 0) viewModel.pendingMedicinesCount.value -= 1 }) { Text("-") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = { viewModel.pendingMedicinesCount.value += 1 }) { Text("+") }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Vitals Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Vitals Normal?", color = colors.textDarkColor)
                Switch(
                    checked = vitalsOk,
                    onCheckedChange = { viewModel.vitalsNormal.value = it }
                )
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ─── Visualizer ────────────────────────────────────────────────────────────

@Composable
fun AvatarVisualizer(state: TwinState) {
    // Animate Colors based on state
    val coreColor by animateColorAsState(
        targetValue = when (state) {
            TwinState.PERFECT -> Color(0xFF00E676)
            TwinState.DEHYDRATED -> Color(0xFFFFCA28)
            TwinState.NEEDS_MEDICINE -> Color(0xFFFF5252)
            TwinState.CRITICAL -> Color(0xFFB71C1C)
        },
        animationSpec = tween(1000)
    )

    val auraColor by animateColorAsState(
        targetValue = when (state) {
            TwinState.PERFECT -> Color(0x4400E676)
            TwinState.DEHYDRATED -> Color(0x44FFCA28)
            TwinState.NEEDS_MEDICINE -> Color(0x44FF5252)
            TwinState.CRITICAL -> Color(0x22B71C1C)
        },
        animationSpec = tween(1000)
    )

    // Pulse animation for the aura
    val pulseDuration = when (state) {
        TwinState.PERFECT -> 1500
        TwinState.DEHYDRATED -> 2500
        TwinState.NEEDS_MEDICINE -> 500
        TwinState.CRITICAL -> 0
    }

    val infiniteTransition = rememberInfiniteTransition()
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (state == TwinState.CRITICAL) 0.85f else 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    // Sci-fi scanning line animation
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(240.dp)
            .background(Color(0xFF0F172A), CircleShape) // Dark sci-fi background
    ) {
        // 1. Background Pulsing Aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            drawCircle(
                color = auraColor,
                radius = (size.width / 2.5f) * scale,
                center = center
            )
            drawCircle(
                color = auraColor.copy(alpha = auraColor.alpha * 1.5f),
                radius = (size.width / 3.5f) * (scale * 0.9f),
                center = center
            )
        }

        // 2. The Human Hologram
        // We use the Person icon but scale it up and tint it dynamically to look like a digital twin
        Icon(
            imageVector = androidx.compose.material.icons.Icons.Default.Person,
            contentDescription = "Human Digital Twin",
            tint = coreColor.copy(alpha = 0.9f),
            modifier = Modifier
                .size(160.dp)
                .offset(y = (-10).dp)
        )

        // 3. Sci-Fi Hologram Overlay (Grid & Scanner)
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw a subtle tech ring around the human
            drawCircle(
                color = coreColor.copy(alpha = 0.3f),
                radius = size.width / 2.2f,
                style = Stroke(width = 2.dp.toPx(), pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(20f, 20f)))
            )

            // Scanning Line
            if (state != TwinState.CRITICAL) {
                val scanY = size.height * scanLineY
                drawLine(
                    color = coreColor,
                    start = androidx.compose.ui.geometry.Offset(size.width * 0.2f, scanY),
                    end = androidx.compose.ui.geometry.Offset(size.width * 0.8f, scanY),
                    strokeWidth = 3.dp.toPx()
                )
                
                // Scanner glow
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, coreColor.copy(alpha = 0.4f), Color.Transparent),
                        startY = scanY - 30f,
                        endY = scanY + 30f
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(size.width * 0.2f, scanY - 30f),
                    size = androidx.compose.ui.geometry.Size(size.width * 0.6f, 60f)
                )
            }
        }
    }
}

fun getStatusMessage(state: TwinState): String {
    return when (state) {
        TwinState.PERFECT -> "Your twin is glowing! You are hydrated, taking your meds on time, and vitals are normal. Keep it up!"
        TwinState.DEHYDRATED -> "Your twin looks dull. You haven't reached your fluid goal today. Please drink some water."
        TwinState.NEEDS_MEDICINE -> "Your twin's heart rate is erratic. You have pending medications to take. Please check your schedule."
        TwinState.CRITICAL -> "Your twin is fading. Critical vitals detected or multiple missed health goals. Please attend to your health."
    }
}
