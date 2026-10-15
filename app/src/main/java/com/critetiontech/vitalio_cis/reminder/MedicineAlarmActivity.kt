package com.critetiontech.ctvitalio.reminder

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudQueue
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private enum class DoseState { Pending, Saving, Taken, Queued }

/** Full-screen medicine alarm, shown over the lock screen or the app in use. */
class MedicineAlarmActivity : ComponentActivity() {

    private var alarm by mutableStateOf<ReminderAlarm?>(null)
    private val states = mutableStateMapOf<String, DoseState>()
    private var finishing by mutableStateOf(false)
    private lateinit var ringer: AlarmRinger

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        ringer = AlarmRinger(this)

        val incoming = ReminderAlarm.from(intent) ?: run { finish(); return }
        show(incoming)

        // Back acts like Dismiss: stop ringing, keep the reminder in the shade.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = dismiss()
        })

        // Answered from the notification while this screen was open.
        lifecycleScope.launch {
            MedicineIntakeEvents.handled.collect { code ->
                if (code == alarm?.requestCode) { ringer.stop(); finish() }
            }
        }

        setContent {
            val current = alarm ?: return@setContent
            AlarmScreen(
                alarm = current,
                states = states,
                snoozeMinutes = ReminderSettings(this).snoozeMinutes,
                allDone = finishing,
                onTake = ::take,
                onTakeAll = { pending().forEach(::take) },
                onSnooze = ::snooze,
                onDismiss = ::dismiss,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Another alarm rang while this one was open: list its doses too.
        ReminderAlarm.from(intent)?.let { show(it) }
    }

    override fun onDestroy() {
        ringer.stop()
        super.onDestroy()
    }

    private fun show(incoming: ReminderAlarm) {
        val merged = alarm?.let { old ->
            old.copy(doses = (old.doses + incoming.doses).distinctBy { it.key })
        } ?: incoming
        alarm = merged
        incoming.doses.forEach { states.putIfAbsent(it.key, DoseState.Pending) }
        finishing = false

        // This screen rings from here on; the notification stays as a quiet backup.
        MedicineAlarmNotifier.quiet(this, merged)
        val ringFor = incoming.alertAt + MedicineAlarmNotifier.RING_DURATION_MS - System.currentTimeMillis()
        ringer.start(ringFor, ReminderSettings(this).soundEnabled)
    }

    private fun pending(): List<ReminderDose> =
        alarm?.doses.orEmpty().filter { states[it.key] == DoseState.Pending }

    private fun take(dose: ReminderDose) {
        if (states[dose.key] != DoseState.Pending) return
        states[dose.key] = DoseState.Saving
        lifecycleScope.launch {
            val outcome = IntakeRecorder.markTaken(applicationContext, dose)
            states[dose.key] = if (outcome == IntakeRecorder.Outcome.Recorded) DoseState.Taken else DoseState.Queued
            onDoseHandled()
        }
    }

    private fun onDoseHandled() {
        val current = alarm ?: return
        val remaining = pending() + current.doses.filter { states[it.key] == DoseState.Saving }
        if (remaining.isNotEmpty()) {
            // Keep the shade in sync with what's still left to take.
            val left = current.copy(doses = current.doses.filter { states[it.key] == DoseState.Pending })
            if (left.doses.isNotEmpty()) MedicineAlarmNotifier.quiet(this, left)
            return
        }
        ringer.stop()
        MedicineAlarmNotifier.cancel(this, current)
        MedicineReminderScheduler.reschedule(applicationContext)
        finishing = true
        lifecycleScope.launch { delay(1_200); finish() }
    }

    private fun snooze() {
        val current = alarm ?: return
        val left = pending()
        ringer.stop()
        MedicineAlarmNotifier.cancel(this, current)
        if (left.isNotEmpty()) MedicineReminderScheduler.snooze(this, current.copy(doses = left))
        finish()
    }

    private fun dismiss() {
        val current = alarm ?: return finish()
        val left = pending()
        ringer.stop()
        if (left.isEmpty()) MedicineAlarmNotifier.cancel(this, current)
        else MedicineAlarmNotifier.quiet(this, current.copy(doses = left))
        finish()
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}

private val AlarmBlue = Color(0xFF2F7BF5)
private val AlarmGreen = Color(0xFF2DBE7E)
private val AlarmAmber = Color(0xFFF5B342)

@Composable
private fun AlarmScreen(
    alarm: ReminderAlarm,
    states: Map<String, DoseState>,
    snoozeMinutes: Int,
    allDone: Boolean,
    onTake: (ReminderDose) -> Unit,
    onTakeAll: () -> Unit,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulseScale",
    )
    val now = LocalTime.now()
    val pendingCount = alarm.doses.count { states[it.key] == DoseState.Pending }
    val busy = alarm.doses.any { states[it.key] == DoseState.Saving }
    val single = alarm.doses.size == 1

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0E2A5C), Color(0xFF1B4FA8), Color(0xFF2F7BF5))))
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp),
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Medicine reminder", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(now.format(DateTimeFormatter.ofPattern("hh:mm", Locale.getDefault())),
                    color = Color.White, fontSize = 60.sp, fontWeight = FontWeight.Light)
                Spacer(Modifier.width(6.dp))
                Text(now.format(DateTimeFormatter.ofPattern("a", Locale.getDefault())),
                    color = Color.White.copy(alpha = 0.8f), fontSize = 20.sp, modifier = Modifier.padding(bottom = 13.dp))
            }

            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier.size(if (single) 120.dp else 84.dp).scale(if (allDone) 1f else pulse)
                    .background(Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier.size(if (single) 88.dp else 62.dp)
                        .background(if (allDone) AlarmGreen else Color.White, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (allDone) Icons.Rounded.Check else Icons.Rounded.Medication, null,
                        tint = if (allDone) Color.White else AlarmBlue,
                        modifier = Modifier.size(if (single) 48.dp else 34.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                when {
                    allDone -> "All done!"
                    single -> "Time to take your medicine"
                    else -> "Time to take ${alarm.doses.size} medicines"
                },
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(alarm.doses, key = { it.key }) { dose ->
                    DoseCard(dose, states[dose.key] ?: DoseState.Pending, showTakeButton = !single, onTake = { onTake(dose) })
                }
            }

            Spacer(Modifier.height(16.dp))
            if (!allDone) {
                HapticButton(
                    onClick = onTakeAll,
                    enabled = pendingCount > 0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AlarmGreen, contentColor = Color.White,
                        disabledContainerColor = AlarmGreen.copy(alpha = 0.7f), disabledContentColor = Color.White,
                    ),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(58.dp),
                ) {
                    if (busy && pendingCount == 0) {
                        CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Icon(Icons.Rounded.Check, null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (single || pendingCount == alarm.doses.size) { if (single) "Mark as taken" else "Mark all as taken" }
                            else "Mark remaining $pendingCount as taken",
                            fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HapticOutlinedButton(
                    onClick = onSnooze,
                    enabled = pendingCount > 0 && !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    Icon(Icons.Rounded.Snooze, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Snooze $snoozeMinutes min", fontSize = 16.sp)
                }
                HapticTextButton(onClick = onDismiss, enabled = !busy) {
                    Text("Dismiss", color = Color.White.copy(alpha = 0.8f), fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun DoseCard(dose: ReminderDose, state: DoseState, showTakeButton: Boolean, onTake: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.13f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(dose.medicineName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOf(dose.dose, MedicineAlarmNotifier.formatTime(dose)).filter { it.isNotBlank() }
            if (meta.isNotEmpty()) {
                Text(meta.joinToString(" · "), color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            }
            if (dose.instructions.isNotBlank()) {
                Text(dose.instructions, color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
        Spacer(Modifier.width(10.dp))
        AnimatedContent(targetState = state, label = "doseState") { s ->
            when (s) {
                DoseState.Pending -> if (showTakeButton) {
                    HapticButton(
                        onClick = onTake,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = AlarmBlue),
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("Taken", fontWeight = FontWeight.SemiBold) }
                } else Spacer(Modifier.size(1.dp))
                DoseState.Saving -> CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(26.dp))
                DoseState.Taken -> StatusChip(Icons.Rounded.Check, "Taken", AlarmGreen)
                DoseState.Queued -> StatusChip(Icons.Rounded.CloudQueue, "Will sync", AlarmAmber)
            }
        }
    }
}

@Composable
private fun StatusChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color, RoundedCornerShape(16.dp)).padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
