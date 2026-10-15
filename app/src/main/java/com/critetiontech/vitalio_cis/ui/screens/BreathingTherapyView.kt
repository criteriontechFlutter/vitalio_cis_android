package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import android.Manifest
import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BreathingTherapyScreen() {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val micPermission = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    CommonAppBar(
        title = "Breathing Therapy",
        onBack = { navController.popBackStack() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0FDF4), Color(0xFFE0F2FE)))),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(30.dp))
            
            Text(
                text = "Digital Biofeedback",
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0F172A),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "Activate your parasympathetic nervous system to quickly lower stress and blood pressure.",
                fontSize = 15.sp,
                color = Color(0xFF475569),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
            )

            if (!micPermission.status.isGranted) {
                Spacer(modifier = Modifier.weight(1f))
                HapticButton(
                    onClick = { micPermission.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text("Allow Microphone to Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                BreathingBiofeedbackVisualizer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
fun BreathingBiofeedbackVisualizer(modifier: Modifier = Modifier) {
    var breathPower by remember { mutableFloatStateOf(0f) }
    var repetitions by remember { mutableIntStateOf(0) }
    var hasReachedPeak by remember { mutableStateOf(false) }

    // Idle pulse animation for a "breathing" feel when not blowing
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val idlePulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "idlePulse"
    )

    // Animated scale combining breath stretch and idle pulse
    val targetScale = 1f + (breathPower * 3.5f)
    val animatedScale by animateFloatAsState(
        targetValue = targetScale * if (breathPower < 0.05f) idlePulse else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bubbleScale"
    )

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val sampleRate = 16000
            val minBuf = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                minBuf * 2
            )

            try {
                if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                    android.media.audiofx.NoiseSuppressor.create(audioRecord.audioSessionId)?.enabled = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                audioRecord.startRecording()
                val buffer = ShortArray(minBuf)
                var currentVolume = 0f

                while (isActive) {
                    val read = audioRecord.read(buffer, 0, buffer.size)
                    if (read > 0) {
                        var energySum = 0.0f
                        for (i in 0 until read) {
                            val f = buffer[i] / 32768.0f
                            energySum += f * f
                        }
                        val rms = sqrt(energySum / read)

                        // Raised threshold to 0.04f to ignore background noise and AC hum
                        if (rms > 0.04f) { 
                            currentVolume += rms * 0.15f // Inflate a bit faster
                        } else {
                            currentVolume -= 0.03f // Deflate much faster when blowing stops
                        }
                        
                        currentVolume = currentVolume.coerceIn(0f, 1f)
                        breathPower = currentVolume

                        // Repetition Counting Logic
                        if (currentVolume > 0.90f && !hasReachedPeak) {
                            hasReachedPeak = true
                        } else if (currentVolume < 0.10f && hasReachedPeak) {
                            hasReachedPeak = false
                            repetitions++
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                audioRecord.stop()
                audioRecord.release()
            }
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Repetition Counter Badge
        Surface(
            shape = CircleShape,
            color = Color(0xFFDBEAFE), // Light blue background
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(
                    Icons.Default.Favorite, 
                    contentDescription = null, 
                    tint = Color(0xFF1E40AF), 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Breaths Completed: $repetitions",
                    color = Color(0xFF1E40AF),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Outer aura
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(animatedScale * 1.6f)
                    .clip(CircleShape)
                    .background(Color(0xFF60A5FA).copy(alpha = 0.15f))
            )
            // Middle aura
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(animatedScale * 1.3f)
                    .clip(CircleShape)
                    .background(Color(0xFF3B82F6).copy(alpha = 0.25f))
            )
            // Core glowing bubble
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .scale(animatedScale)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFF93C5FD), Color(0xFF2563EB))))
            )
        }
        
        Text(
            text = when {
                breathPower > 0.95f -> "Excellent.\nNow relax and exhale."
                breathPower > 0.1f -> "Keep blowing smoothly..."
                else -> "Take a deep breath,\nthen blow slowly into the mic."
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = Color(0xFF1E293B),
            modifier = Modifier.padding(bottom = 60.dp)
        )
    }
}
