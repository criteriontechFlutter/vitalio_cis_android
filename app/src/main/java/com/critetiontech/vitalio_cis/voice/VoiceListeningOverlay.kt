package com.critetiontech.vitalio_cis.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceListeningOverlay
//  Full-screen voice assistant overlay – Android equivalent of iOS
//  VoiceListeningOverlay + UnifiedVoiceConfirmationCardView.
//  Now supports all 5 iOS data types: Vitals, Symptoms, Fluid Intake,
//  Fluid Output, Medicines. Includes LiveDictationBanner, AI extraction
//  indicator, and full iOS-matching confirmation card UI.
// ═══════════════════════════════════════════════════════════════════════════════

// ── Animated waveform bar (matches iOS VoiceWaveBar) ─────────────────────────

@Composable
private fun VoiceWaveBar(delayMillis: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveBar$delayMillis")
    val height by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue  = 54f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(delayMillis)
        ),
        label = "waveBarHeight$delayMillis"
    )
    Box(
        modifier = Modifier
            .width(6.dp)
            .height(height.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF2563EB),
                        Color(0xFF9333EA).copy(alpha = 0.55f),
                        Color(0xFF06B6D4)
                    )
                )
            )
    )
}

// ── Micro wave bars for banner (matches iOS MicroVoiceWaveVisualizer) ─────────

@Composable
private fun MicroVoiceWaveVisualizer(barColor: Color = Color(0xFF2563EB)) {
    val infiniteTransition = rememberInfiniteTransition(label = "microWave")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * PI * 100).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "microWaveTime"
    )
    Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp), verticalAlignment = Alignment.CenterVertically) {
        val delays     = listOf(0.0, 0.18, 0.36, 0.12)
        val maxHeights = listOf(15f, 18f, 13f, 17f)
        val minHeights = listOf(4f,  5f,  3f,  4.5f)
        for (i in 0..3) {
            val speed  = 8.5
            val sinVal = (sin((time / 100.0 * speed) + (delays[i] * 10)) + 1.0) / 2.0
            val h      = (minHeights[i] + sinVal * (maxHeights[i] - minHeights[i])).toFloat()
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(h.dp)
                    .clip(RoundedCornerShape(1.5.dp))
                    .background(barColor)
            )
        }
    }
}

// ── Animated AI Orb Mesh (matches iOS AIOrbMesh) ─────────────────────────────

@Composable
private fun AIOrbMesh(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbMesh")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * PI * 100).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbMeshTime"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val t  = time / 100.0

        // 14 distorted orbit layers (matching iOS layer count)
        repeat(14) { layer ->
            val progress = layer / 36.0
            val path = Path()
            var first = true

            var angle = 0.0
            while (angle <= 2 * PI) {
                val radius    = 78 + sin(t + progress * 6) * 10
                val distorted = radius + sin(angle * 6 + t * 1.8 + progress * 12) * 14
                val x = (cx + cos(angle + progress * 4 + t * 0.15) * distorted).toFloat()
                val y = (cy + sin(angle + progress * 4 + t * 0.15) * distorted).toFloat()

                if (first) { path.moveTo(x, y); first = false }
                else        path.lineTo(x, y)

                angle += 0.06
            }

            val fraction    = layer / 14f
            val strokeColor = lerpColor(Color(0xFF3B82F6), Color(0xFF06B6D4), fraction).copy(alpha = 0.85f)

            drawPath(
                path  = path,
                color = strokeColor,
                style = androidx.compose.ui.graphics.drawscope.Stroke()
            )
        }
    }
}

// ── Main full-screen overlay ──────────────────────────────────────────────────

@Composable
fun VoiceListeningOverlay(
    isListening: Boolean,
    showListeningOverlay: Boolean,
    showConfirmationCard: Boolean,
    pendingData: VoiceExtractedData?,
    transcript: String,
    isSavingData: Boolean,
    isExtractingData: Boolean,
    processingTranscript: String,
    anomalyAlerts: List<VitalAlert> = emptyList(),
    multiTurnPrompt: String = "",
    vitalTrends: Map<String, VitalTrend> = emptyMap(),
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSaveAll: () -> Unit,
    onStartListening: () -> Unit,
    onRemoveVital: (List<String>) -> Unit,
    onRemoveSymptom: (String, String) -> Unit,
    onRemoveFluidIntake: (String) -> Unit,
    onRemoveFluidOutput: () -> Unit,
    onRemoveMedicine: (String) -> Unit
) {
    AnimatedVisibility(
        visible = showListeningOverlay,
        enter   = fadeIn(tween(300)),
        exit    = fadeOut(tween(300))
    ) {
        if (showConfirmationCard && pendingData != null) {
            // Full-screen confirmation view (mirrors iOS UnifiedVoiceConfirmationCardView)
            UnifiedVoiceConfirmationFullScreen(
                data                 = pendingData,
                isListening          = isListening,
                transcript           = transcript,
                isSavingData         = isSavingData,
                isExtractingData     = isExtractingData,
                processingTranscript = processingTranscript,
                anomalyAlerts        = anomalyAlerts,
                multiTurnPrompt      = multiTurnPrompt,
                vitalTrends          = vitalTrends,
                onCancel             = onCancel,
                onSaveAll            = onSaveAll,
                onStartListening     = onStartListening,
                onRemoveVital        = onRemoveVital,
                onRemoveSymptom      = onRemoveSymptom,
                onRemoveFluidIntake  = onRemoveFluidIntake,
                onRemoveFluidOutput  = onRemoveFluidOutput,
                onRemoveMedicine     = onRemoveMedicine
            )
        } else {
            // Orb + waveform listening screen (mirrors iOS VoiceListeningOverlay)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.65f))
                    .clickable(enabled = !showConfirmationCard) { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(Modifier.weight(1f))

                    Box(contentAlignment = Alignment.Center) {
                        AIOrbMesh(modifier = Modifier.size(260.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(0, 80, 160, 240, 320).forEach { delay ->
                                VoiceWaveBar(delayMillis = delay)
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // Show multi-turn guided prompt when active, otherwise live transcript
                    if (multiTurnPrompt.isNotEmpty()) {
                        // Guided Q&A prompt card
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF2563EB).copy(alpha = 0.88f),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.QuestionAnswer,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = multiTurnPrompt,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                "Please speak your response",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.55f),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = transcript.ifEmpty { "Listening..." },
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = if (transcript.isEmpty()) 0.7f else 1f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 34.dp)
                        )

                        Spacer(Modifier.height(12.dp))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tap to cancel", fontSize = 14.sp, color = Color.Gray)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Speak to navigate or dictate health vitals",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 26.dp)
                            )
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEF4444).copy(alpha = 0.9f))
                            .clickable { onStop() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Stop listening",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.height(70.dp))
                }
            }
        }
    }
}

// ── Full-screen Unified Confirmation View ─────────────────────────────────────

@Composable
private fun UnifiedVoiceConfirmationFullScreen(
    data: VoiceExtractedData,
    isListening: Boolean,
    transcript: String,
    isSavingData: Boolean,
    isExtractingData: Boolean,
    processingTranscript: String,
    anomalyAlerts: List<VitalAlert> = emptyList(),
    multiTurnPrompt: String = "",
    vitalTrends: Map<String, VitalTrend> = emptyMap(),
    onCancel: () -> Unit,
    onSaveAll: () -> Unit,
    onStartListening: () -> Unit,
    onRemoveVital: (List<String>) -> Unit,
    onRemoveSymptom: (String, String) -> Unit,
    onRemoveFluidIntake: (String) -> Unit,
    onRemoveFluidOutput: () -> Unit,
    onRemoveMedicine: (String) -> Unit
) {
    val totalItems = (data.vitals?.size ?: 0) +
            (data.symptoms?.size ?: 0) +
            (data.fluidIntakes?.size ?: 0) +
            (if (data.fluidOutput != null) 1 else 0) +
            (data.medicines?.size ?: 0)

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF0F4F8))) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable { onCancel() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Voice Health Log", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "$totalItems ${if (totalItems == 1) "item" else "items"} ready to review",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Status badge
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when {
                        isExtractingData -> Color(0xFF9333EA).copy(alpha = 0.12f)
                        isListening      -> Color(0xFF22C55E).copy(alpha = 0.12f)
                        else             -> Color(0xFF2563EB).copy(alpha = 0.12f)
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isExtractingData -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(10.dp),
                                    strokeWidth = 1.5.dp,
                                    color = Color(0xFF9333EA)
                                )
                                Spacer(Modifier.width(5.dp))
                                Text("Analyzing...", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9333EA))
                            }
                            isListening -> {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF22C55E)))
                                Spacer(Modifier.width(4.dp))
                                Text("Listening", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            }
                            else -> {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(0xFF2563EB)))
                                Spacer(Modifier.width(4.dp))
                                Text("Ready", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            // ── Live dictation banner ─────────────────────────────────────────
            LiveDictationBanner(
                isExtractingData     = isExtractingData,
                isListening          = isListening,
                transcript           = transcript,
                processingTranscript = processingTranscript,
                onStartListening     = onStartListening,
                modifier             = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            // ── Scrollable content ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 120.dp)
            ) {
                // AI extracting indicator card
                if (isExtractingData) {
                    Card(
                        shape  = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier  = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF9333EA).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Color(0xFF9333EA))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Analyzing Speech with AI...", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    if (processingTranscript.isEmpty()) "Extracting readings..." else "\"$processingTranscript\"",
                                    fontSize = 12.sp, color = Color.Gray, maxLines = 1
                                )
                            }
                            Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF9333EA))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Anomaly alerts — shown BEFORE vitals so user sees warnings first
                if (anomalyAlerts.isNotEmpty()) {
                    AnomalyAlertsSection(alerts = anomalyAlerts)
                    Spacer(Modifier.height(12.dp))
                }

                // Multi-turn guided prompt card (shown when actively collecting a slot)
                if (multiTurnPrompt.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF2563EB).copy(alpha = 0.1f)),
                        elevation = CardDefaults.cardElevation(0.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).clip(CircleShape)
                                    .background(Color(0xFF2563EB).copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.QuestionAnswer, null,
                                    tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Please answer the question:",
                                    fontWeight = FontWeight.Bold, fontSize = 13.sp,
                                    color = Color(0xFF2563EB)
                                )
                                Text(
                                    multiTurnPrompt,
                                    fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Vitals section
                data.vitals?.takeIf { it.isNotEmpty() }?.let { vitals ->
                    VitalsSection(vitals = vitals, vitalTrends = vitalTrends, onRemoveVital = onRemoveVital)
                    Spacer(Modifier.height(12.dp))
                }

                // Symptoms section
                data.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                    SymptomsSection(symptoms = symptoms, onRemoveSymptom = onRemoveSymptom)
                    Spacer(Modifier.height(12.dp))
                }

                // Fluid intake section
                data.fluidIntakes?.takeIf { it.isNotEmpty() }?.let { intakes ->
                    FluidIntakesSection(intakes = intakes, onRemove = onRemoveFluidIntake)
                    Spacer(Modifier.height(12.dp))
                }

                // Fluid output section
                data.fluidOutput?.let { output ->
                    FluidOutputSection(output = output, onRemove = onRemoveFluidOutput)
                    Spacer(Modifier.height(12.dp))
                }

                // Medicines section
                data.medicines?.takeIf { it.isNotEmpty() }?.let { medicines ->
                    MedicinesSection(medicines = medicines, onRemove = onRemoveMedicine)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // ── Sticky bottom dock ────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color(0xFFF0F4F8).copy(alpha = 0.97f))
        ) {
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Save All button
                Button(
                    onClick  = onSaveAll,
                    enabled  = !isSavingData && !isExtractingData,
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape    = CircleShape,
                    colors   = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                if (isSavingData || isExtractingData)
                                    Brush.horizontalGradient(listOf(Color.Gray.copy(alpha = 0.45f), Color.Gray.copy(alpha = 0.45f)))
                                else
                                    Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isSavingData -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Saving...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                            isExtractingData -> Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("Analyzing...", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                            else -> Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Save All", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Live Dictation Banner (matches iOS LiveVoiceDictationBannerView) ──────────

@Composable
private fun LiveDictationBanner(
    isExtractingData: Boolean,
    isListening: Boolean,
    transcript: String,
    processingTranscript: String,
    onStartListening: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth()) {
        when {
            isExtractingData -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF9333EA).copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(34.dp).clip(CircleShape).background(Color(0xFF9333EA).copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF9333EA))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Extracting Readings...", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF9333EA))
                        Text(
                            if (processingTranscript.isEmpty()) "Structuring health data with AI..." else "\"$processingTranscript\"",
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = Color.Gray, maxLines = 1
                        )
                    }
                    Icon(Icons.Default.AutoAwesome, null, tint = Color(0xFF9333EA), modifier = Modifier.size(16.dp))
                }
            }
            isListening -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.08f))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MicroVoiceWaveVisualizer(barColor = Color(0xFF2563EB))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = transcript.ifEmpty { "Listening for more readings..." },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                        .clickable { onStartListening() }
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(Color(0xFF2563EB).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Mic, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add More Readings", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("Tap to speak additional vitals, fluids, or medicines", fontSize = 11.sp, color = Color.Gray)
                    }
                    Icon(Icons.Default.GraphicEq, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ── Section Card container ────────────────────────────────────────────────────

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), content = content)
    }
}

@Composable
private fun SectionHeader(
    icon: @Composable () -> Unit,
    title: String,
    badgeText: String,
    accentColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(6.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = accentColor, modifier = Modifier.weight(1f))
        Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.12f)) {
            Text(
                badgeText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
        }
    }
}

// ── Anomaly Alerts Section ────────────────────────────────────────────────────

@Composable
private fun AnomalyAlertsSection(alerts: List<VitalAlert>) {
    val criticalColor = Color(0xFFEF4444)
    val warningColor  = Color(0xFFF97316)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        alerts.forEach { alert ->
            val isCritical = alert.severity == AnomalySeverity.CRITICAL
            val alertColor = if (isCritical) criticalColor else warningColor

            Card(
                shape  = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = alertColor.copy(alpha = 0.08f)),
                elevation = CardDefaults.cardElevation(0.dp),
                modifier  = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(alert.emoji, fontSize = 20.sp, modifier = Modifier.padding(end = 10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                alert.vitalLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize   = 13.sp,
                                color      = alertColor
                            )
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = alertColor.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    if (isCritical) "CRITICAL" else "WARNING",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = alertColor,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            "${alert.value} — ${alert.message}",
                            fontSize = 12.sp,
                            color    = alertColor.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

// ── Vitals Section ────────────────────────────────────────────────────────────

@Composable
private fun VitalsSection(
    vitals: Map<String, String>,
    vitalTrends: Map<String, VitalTrend> = emptyMap(),
    onRemoveVital: (List<String>) -> Unit
) {
    val blue = Color(0xFF2563EB)
    SectionCard {
        SectionHeader(
            icon      = { Icon(Icons.Default.MonitorHeart, null, tint = blue, modifier = Modifier.size(14.dp)) },
            title     = "Vitals",
            badgeText = "${vitals.size} Recorded",
            accentColor = blue
        )
        Spacer(Modifier.height(8.dp))

        val sys  = vitals["vmValueBPSys"]
        val dias = vitals["vmValueBPDias"]
        if (sys != null && dias != null) {
            VitalPreviewRow("❤️", Color(0xFFEF4444), "Blood Pressure", "$sys/$dias mmHg",
                trend = vitalTrends["vmValueBPSys"]) {
                onRemoveVital(listOf("vmValueBPSys", "vmValueBPDias"))
            }
        } else {
            sys?.let  { VitalPreviewRow("❤️", Color(0xFFEF4444), "BP Systolic",  "$it mmHg",  trend = vitalTrends["vmValueBPSys"])  { onRemoveVital(listOf("vmValueBPSys")) } }
            dias?.let { VitalPreviewRow("❤️", Color(0xFFEF4444), "BP Diastolic", "$it mmHg",  trend = vitalTrends["vmValueBPDias"]) { onRemoveVital(listOf("vmValueBPDias")) } }
        }
        vitals["vmValuePulse"]?.let           { VitalPreviewRow("〰️", Color(0xFFEC4899), "Pulse Rate",        "$it bpm",    trend = vitalTrends["vmValuePulse"])           { onRemoveVital(listOf("vmValuePulse")) } }
        vitals["vmValueSPO2"]?.let            { VitalPreviewRow("🫁", Color(0xFF06B6D4), "SpO2 Level",        "$it%",       trend = vitalTrends["vmValueSPO2"])             { onRemoveVital(listOf("vmValueSPO2")) } }
        vitals["vmValueTemperature"]?.let     { VitalPreviewRow("🌡️", Color(0xFFF97316), "Temperature",       "$it °F",     trend = vitalTrends["vmValueTemperature"])      { onRemoveVital(listOf("vmValueTemperature")) } }
        vitals["vmValueHeartRate"]?.let       { VitalPreviewRow("💓", Color(0xFFEF4444), "Heart Rate",        "$it bpm",    trend = vitalTrends["vmValueHeartRate"])         { onRemoveVital(listOf("vmValueHeartRate")) } }
        vitals["vmValueRespiratoryRate"]?.let { VitalPreviewRow("🌬️", Color(0xFF06B6D4), "Respiratory Rate",  "$it /min",   trend = vitalTrends["vmValueRespiratoryRate"])  { onRemoveVital(listOf("vmValueRespiratoryRate")) } }
        vitals["vmValueRbs"]?.let             { VitalPreviewRow("🩸", Color(0xFF8B5CF6), "Blood Sugar (RBS)", "$it mg/dL",  trend = vitalTrends["vmValueRbs"])              { onRemoveVital(listOf("vmValueRbs")) } }
        vitals["weight"]?.let                 { VitalPreviewRow("⚖️", Color(0xFF8B5CF6), "Body Weight",       "$it kg",     trend = vitalTrends["weight"])                  { onRemoveVital(listOf("weight")) } }
    }
}

// ── Symptoms Section ──────────────────────────────────────────────────────────

@Composable
private fun SymptomsSection(
    symptoms: List<VoiceSymptomItem>,
    onRemoveSymptom: (String, String) -> Unit
) {
    val orange = Color(0xFFF97316)
    SectionCard {
        SectionHeader(
            icon      = { Icon(Icons.Default.LocalHospital, null, tint = orange, modifier = Modifier.size(14.dp)) },
            title     = "Symptoms",
            badgeText = "${symptoms.size} Detected",
            accentColor = orange
        )
        Spacer(Modifier.height(8.dp))

        symptoms.forEach { symptom ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HealthAndSafety, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    symptom.details,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Surface(shape = CircleShape, color = orange.copy(alpha = 0.15f)) {
                    Text(
                        "Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = orange,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = { onRemoveSymptom(symptom.detailID, symptom.details) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

// ── Fluid Intake Section ──────────────────────────────────────────────────────

@Composable
private fun FluidIntakesSection(
    intakes: List<VoiceFluidIntakeItem>,
    onRemove: (String) -> Unit
) {
    val cyan = Color(0xFF06B6D4)
    SectionCard {
        SectionHeader(
            icon      = { Icon(Icons.Default.WaterDrop, null, tint = cyan, modifier = Modifier.size(14.dp)) },
            title     = "Fluid Intake",
            badgeText = "${intakes.size} Beverages",
            accentColor = cyan
        )
        Spacer(Modifier.height(8.dp))

        intakes.forEach { item ->
            val (emoji, color) = when (item.name.lowercase()) {
                "water"  -> Pair("💧", cyan)
                "juice"  -> Pair("🧃", Color(0xFFF97316))
                "milk"   -> Pair("🥛", Color.Gray)
                "tea"    -> Pair("🍵", Color(0xFFC8956C))
                "coffee" -> Pair("☕", Color(0xFF78350F))
                else     -> Pair("💧", cyan)
            }
            VitalPreviewRow(emoji, color, item.name, "${item.quantity} ml") { onRemove(item.id) }
        }
    }
}

// ── Fluid Output Section ──────────────────────────────────────────────────────

@Composable
private fun FluidOutputSection(
    output: VoiceFluidOutputItem,
    onRemove: () -> Unit
) {
    val yellow = Color(0xFFEAB308)
    SectionCard {
        SectionHeader(
            icon      = { Icon(Icons.Default.Science, null, tint = yellow, modifier = Modifier.size(14.dp)) },
            title     = "Fluid Output",
            badgeText = "1 Entry",
            accentColor = yellow
        )
        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🔶", fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Urine Output", fontSize = 14.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(8.dp).clip(CircleShape).background(parseHexColor(output.colourHex))
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(output.colourName, fontSize = 12.sp, color = Color.Gray)
                }
            }
            Text("${output.quantity} ml", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(6.dp))
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ── Medicines Section ─────────────────────────────────────────────────────────

@Composable
private fun MedicinesSection(
    medicines: List<VoiceMedicineItem>,
    onRemove: (String) -> Unit
) {
    val purple = Color(0xFF8B5CF6)
    SectionCard {
        SectionHeader(
            icon      = { Icon(Icons.Default.Medication, null, tint = purple, modifier = Modifier.size(14.dp)) },
            title     = "Medication Intake",
            badgeText = "${medicines.size} Pending",
            accentColor = purple
        )
        Spacer(Modifier.height(8.dp))

        medicines.forEach { med ->
            Card(
                shape  = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F5FF)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(11.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(purple.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Medication, null, tint = purple, modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatMedicineName(med.medicineName), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            if (med.instructions.trim().isNotEmpty()) {
                                Text(med.instructions.trim(), fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { onRemove(med.id) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (med.frequency.trim().isNotEmpty()) {
                            Surface(shape = RoundedCornerShape(50), color = purple.copy(alpha = 0.12f)) {
                                Text(med.frequency.trim(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = purple,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.5.dp))
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        val timeStr = formatMedicineTime(med.scheduledDateTime)
                        if (timeStr != null) {
                            Icon(Icons.Default.AccessTime, null, modifier = Modifier.size(10.dp), tint = Color.Gray)
                            Spacer(Modifier.width(2.dp))
                            Text(timeStr, fontSize = 11.sp, color = Color.Gray)
                            Spacer(Modifier.width(6.dp))
                        }
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF22C55E).copy(alpha = 0.12f)) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF22C55E), modifier = Modifier.size(10.dp))
                                Spacer(Modifier.width(3.dp))
                                Text("Take Now", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF22C55E))
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Common row composable ─────────────────────────────────────────────────────

@Composable
fun VitalPreviewRow(
    emoji: String,
    color: Color,
    title: String,
    value: String,
    trend: VitalTrend? = null,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 14.sp, modifier = Modifier.width(24.dp))
        Text(title, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold)

        // Trend delta badge
        trend?.let { t ->
            Spacer(Modifier.width(4.dp))
            val (arrow, trendColor) = when (t.direction) {
                TrendDirection.UP     -> Pair("↑", Color(0xFF22C55E))
                TrendDirection.DOWN   -> Pair("↓", Color(0xFFEF4444))
                TrendDirection.STABLE -> Pair("→", Color.Gray)
            }
            val deltaStr = if (kotlin.math.abs(t.delta) >= 1.0)
                "${arrow}${kotlin.math.abs(t.delta).toInt()}"
            else arrow
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = trendColor.copy(alpha = 0.12f)
            ) {
                Text(
                    deltaStr,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = trendColor,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
        }

        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        }
    }
}

// ── Floating Mic FAB (matches iOS FloatingVoiceButton) ───────────────────────

@Composable
fun FloatingVoiceFab(
    isListening: Boolean,
    showOverlay: Boolean,
    onClick: () -> Unit
) {
    if (!isListening && !showOverlay) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF1E40AF)),
                        start = Offset(0f, 0f),
                        end   = Offset(68f, 68f)
                    )
                )
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = "Open voice assistant",
                tint  = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun formatMedicineName(name: String): String {
    val trimmed = name.trim()
    return if (trimmed == trimmed.uppercase() && trimmed.length > 3)
        trimmed.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    else trimmed
}

private fun formatMedicineTime(dtStr: String): String? {
    if (dtStr.isEmpty()) return null
    val formats = listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss.SSS")
    for (fmt in formats) {
        try {
            val date = SimpleDateFormat(fmt, Locale.getDefault()).parse(dtStr) ?: continue
            return SimpleDateFormat("hh:mm a", Locale.getDefault()).format(date)
        } catch (_: Exception) {}
    }
    return null
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.trimStart('#')
        val rgb = cleaned.toLong(16)
        when (cleaned.length) {
            6    -> Color((0xFF000000L or rgb).toInt())
            8    -> Color(rgb.toInt())
            else -> Color(0xFFFFF9CC)
        }
    } catch (_: Exception) { Color(0xFFFFF9CC) }
}

private fun lerpColor(a: Color, b: Color, t: Float): Color = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
