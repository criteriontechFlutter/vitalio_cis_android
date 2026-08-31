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
import kotlin.math.*

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceListeningOverlay
//  Full-screen voice assistant overlay – Android equivalent of iOS
//  VoiceListeningOverlay + UnifiedVoiceConfirmationCardView.
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

// ── Animated AI Orb Mesh (matches iOS AIOrbMesh) ─────────────────────────────

@Composable
private fun AIOrbMesh(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "orbMesh")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = (2 * PI * 100).toFloat(), // large range to avoid wrap artefacts
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbMeshTime"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val t  = time / 100.0 // normalised time in radians

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

            // Gradient stroke: blue → purple → cyan
            val fraction = layer / 14f
            val strokeColor = lerp(Color(0xFF3B82F6), Color(0xFF06B6D4), fraction)
                .copy(alpha = 0.85f)

            drawPath(
                path   = path,
                color  = strokeColor,
                style  = androidx.compose.ui.graphics.drawscope.Stroke()
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
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onSaveAll: () -> Unit,
    onRemoveVital: (List<String>) -> Unit,
    onRemoveSymptom: (String, String) -> Unit
) {
    AnimatedVisibility(
        visible = showListeningOverlay,
        enter   = fadeIn(tween(300)),
        exit    = fadeOut(tween(300))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = 0.78f))
                .clickable(enabled = !showConfirmationCard) { onStop() },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.weight(1f))

                if (showConfirmationCard && pendingData != null) {
                    // Show the confirmation card
                    UnifiedVoiceConfirmationCard(
                        data         = pendingData,
                        isListening  = isListening,
                        transcript   = transcript,
                        isSavingData = isSavingData,
                        onCancel     = onCancel,
                        onSaveAll    = onSaveAll,
                        onRemoveVital    = onRemoveVital,
                        onRemoveSymptom  = onRemoveSymptom
                    )
                } else {
                    // Orb + waveform + transcript
                    Box(contentAlignment = Alignment.Center) {
                        AIOrbMesh(modifier = Modifier.size(400.dp))
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

                    // Transcript / status text
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
                        Text(
                            "Tap to cancel",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
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

                // Cancel / stop button
                if (!showConfirmationCard) {
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

// ── Unified Confirmation Card (matches iOS UnifiedVoiceConfirmationCardView) ──

@Composable
fun UnifiedVoiceConfirmationCard(
    data: VoiceExtractedData,
    isListening: Boolean,
    transcript: String,
    isSavingData: Boolean,
    onCancel: () -> Unit,
    onSaveAll: () -> Unit,
    onRemoveVital: (List<String>) -> Unit,
    onRemoveSymptom: (String, String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2235)),
        elevation = CardDefaults.cardElevation(defaultElevation = 20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = false) {} // prevent tap-through to overlay
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint  = Color(0xFF06B6D4),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Confirm Command",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }

                // Mic status badge
                Surface(
                    shape = CircleShape,
                    color = if (isListening) Color(0xFF22C55E).copy(alpha = 0.18f)
                            else Color.Gray.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(if (isListening) Color(0xFF22C55E) else Color.Red)
                        )
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                            contentDescription = null,
                            tint = if (isListening) Color(0xFF22C55E) else Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            if (isListening) "Listening..." else "Mic Off",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isListening) Color(0xFF22C55E) else Color.Gray
                        )
                    }
                }
            }

            // ── Live transcript strip ─────────────────────────────────────────
            if (isListening || transcript.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF3B82F6).copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = null,
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (transcript.isEmpty()) "Listening..." else transcript,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White,
                        maxLines = 2,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // ── Vitals Section ────────────────────────────────────────────────
            data.vitals?.takeIf { it.isNotEmpty() }?.let { vitals ->
                VitalsSection(vitals = vitals, onRemoveVital = onRemoveVital)
                Spacer(Modifier.height(10.dp))
            }

            // ── Symptoms Section ──────────────────────────────────────────────
            data.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                SymptomsSection(symptoms = symptoms, onRemoveSymptom = onRemoveSymptom)
                Spacer(Modifier.height(10.dp))
            }

            Spacer(Modifier.height(4.dp))

            // ── Action Buttons ────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cancel
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }

                // Save All
                Button(
                    onClick = onSaveAll,
                    enabled = !isSavingData,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(listOf(Color(0xFF2563EB), Color(0xFF06B6D4))),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSavingData) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save All", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                            }
                        }
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
    onRemoveVital: (List<String>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF3B82F6).copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Favorite, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Vitals", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF3B82F6))
        }
        Spacer(Modifier.height(8.dp))

        val sys  = vitals["vmValueBPSys"]
        val dias = vitals["vmValueBPDias"]
        if (sys != null && dias != null) {
            VitalPreviewRow("❤️", Color(0xFFEF4444), "Blood Pressure", "$sys/$dias mmHg") {
                onRemoveVital(listOf("vmValueBPSys", "vmValueBPDias"))
            }
        } else {
            sys?.let  { VitalPreviewRow("❤️", Color(0xFFEF4444), "BP Systolic",  "$it mmHg") { onRemoveVital(listOf("vmValueBPSys")) } }
            dias?.let { VitalPreviewRow("❤️", Color(0xFFEF4444), "BP Diastolic", "$it mmHg") { onRemoveVital(listOf("vmValueBPDias")) } }
        }
        vitals["vmValuePulse"]?.let       { VitalPreviewRow("〰️", Color(0xFFEC4899), "Pulse Rate",       "$it bpm") { onRemoveVital(listOf("vmValuePulse")) } }
        vitals["vmValueSPO2"]?.let        { VitalPreviewRow("🫁", Color(0xFF06B6D4), "SpO2 Level",       "$it%") { onRemoveVital(listOf("vmValueSPO2")) } }
        vitals["vmValueTemperature"]?.let { VitalPreviewRow("🌡️", Color(0xFFF97316), "Temperature",      "$it °F") { onRemoveVital(listOf("vmValueTemperature")) } }
        vitals["vmValueHeartRate"]?.let   { VitalPreviewRow("💓", Color(0xFFEF4444), "Heart Rate",       "$it bpm") { onRemoveVital(listOf("vmValueHeartRate")) } }
        vitals["weight"]?.let             { VitalPreviewRow("⚖️", Color(0xFF8B5CF6), "Body Weight",      "$it kg") { onRemoveVital(listOf("weight")) } }
        vitals["vmValueRbs"]?.let         { VitalPreviewRow("🩸", Color(0xFFEF4444), "Blood Sugar",      "$it mg/dL") { onRemoveVital(listOf("vmValueRbs")) } }
        vitals["vmValueRespiratoryRate"]?.let { VitalPreviewRow("🌬️", Color(0xFF06B6D4), "Respiratory Rate", "$it bpm") { onRemoveVital(listOf("vmValueRespiratoryRate")) } }
    }
}

@Composable
private fun VitalPreviewRow(
    emoji: String,
    color: Color,
    title: String,
    value: String,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 14.sp, modifier = Modifier.width(24.dp))
        Text(title, fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), modifier = Modifier.weight(1f))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
            Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
        }
    }
}

// ── Symptoms Section ──────────────────────────────────────────────────────────

@Composable
private fun SymptomsSection(
    symptoms: List<VoiceSymptomItem>,
    onRemoveSymptom: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF97316).copy(alpha = 0.12f))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalHospital, null, tint = Color(0xFFF97316), modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(6.dp))
            Text("Symptoms", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF97316))
        }
        Spacer(Modifier.height(8.dp))

        symptoms.forEach { symptom ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.HealthAndSafety, null, tint = Color(0xFF3B82F6), modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    symptom.details,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF97316).copy(alpha = 0.15f)
                ) {
                    Text(
                        "Active",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF97316),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(
                    onClick = { onRemoveSymptom(symptom.detailID, symptom.details) },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444).copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
                }
            }
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

// ── lerp helper (Color linear interpolation) ─────────────────────────────────

private fun lerp(a: Color, b: Color, t: Float): Color = Color(
    red   = a.red   + (b.red   - a.red)   * t,
    green = a.green + (b.green - a.green) * t,
    blue  = a.blue  + (b.blue  - a.blue)  * t,
    alpha = a.alpha + (b.alpha - a.alpha) * t
)
