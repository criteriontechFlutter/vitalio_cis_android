package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import com.critetiontech.ctvitalio.ui.feedback.HapticSwitch
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.critetiontech.ctvitalio.reminder.AlarmSetup
import com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler
import com.critetiontech.ctvitalio.reminder.ReminderSettings
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.AppTextStyles
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ReminderBlue = Color(0xFF2F7BF5)
private val ReminderGrey = Color(0xFF8A90A0)
private val WarningAmber = Color(0xFFF59E0B)
private val ReminderGreen = Color(0xFF2DBE7E)

private val offsetOptions = listOf(0, 5, 10, 15, 30)
private val snoozeOptions = listOf(5, 10, 15)

@Composable
fun MedicineReminderSettingsScreen() {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val settings = remember { ReminderSettings(context) }

    var enabled by remember { mutableStateOf(settings.enabled) }
    var offset by remember { mutableIntStateOf(settings.offsetMinutes) }
    var sound by remember { mutableStateOf(settings.soundEnabled) }
    var snooze by remember { mutableIntStateOf(settings.snoozeMinutes) }
    var muted by remember { mutableStateOf(settings.mutedMedicines) }
    var doses by remember { mutableStateOf(settings.doses) }
    var syncing by remember { mutableStateOf(false) }

    var setup by remember { mutableStateOf(AlarmSetup.requirements(context)) }

    fun applyChange(block: ReminderSettings.() -> Unit) {
        settings.block()
        MedicineReminderScheduler.reschedule(context)
    }

    LifecycleResumeEffect(Unit) {
        setup = AlarmSetup.requirements(context)
        // Alarms registered before the exact-alarm grant were inexact; set them again.
        MedicineReminderScheduler.reschedule(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(Unit) {
        syncing = true
        MedicineReminderScheduler.sync(context)
        doses = settings.doses
        syncing = false
    }

    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // Once the user has said no twice the dialog stops showing; send them to settings.
        if (!granted) context.startActivity(AlarmSetup.settingsIntent(context, AlarmSetup.Item.Notifications))
        setup = AlarmSetup.requirements(context)
    }

    CommonAppBar(title = "Medicine Reminders") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── What this Android version needs for the alarm to work ──────────
            AlarmSetupCard(
                requirements = setup,
                onFix = { item ->
                    if (item == AlarmSetup.Item.Notifications && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(AlarmSetup.settingsIntent(context, item))
                    }
                },
            )

            // ── Master switch ───────────────────────────────────────────────────
            SettingsCard {
                SwitchRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Medicine alarms",
                    subtitle = "Full screen alarm with sound at dose time",
                    checked = enabled,
                    onCheckedChange = { enabled = it; applyChange { this.enabled = it } },
                )
            }

            // ── Timing & sound ──────────────────────────────────────────────────
            SettingsCard {
                RowHeader(Icons.Rounded.Alarm, "Remind me")
                Spacer(Modifier.height(10.dp))
                ChoiceChips(
                    options = offsetOptions,
                    selected = offset,
                    label = { if (it == 0) "On time" else "$it min before" },
                    enabled = enabled,
                    onSelect = { offset = it; applyChange { offsetMinutes = it } },
                )

                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = colors.borderGreyLightColor.copy(alpha = 0.4f))

                SwitchRow(
                    icon = Icons.Rounded.VolumeUp,
                    title = "Alarm sound",
                    subtitle = if (sound) "Rings until you respond (up to 2 min)" else "Vibrate only",
                    checked = sound,
                    enabled = enabled,
                    onCheckedChange = { sound = it; applyChange { soundEnabled = it } },
                )

                HorizontalDivider(Modifier.padding(vertical = 14.dp), color = colors.borderGreyLightColor.copy(alpha = 0.4f))

                RowHeader(Icons.Rounded.Snooze, "Snooze for")
                Spacer(Modifier.height(10.dp))
                ChoiceChips(
                    options = snoozeOptions,
                    selected = snooze,
                    label = { "$it min" },
                    enabled = enabled,
                    onSelect = { snooze = it; applyChange { snoozeMinutes = it } },
                )

                Spacer(Modifier.height(16.dp))
                HapticOutlinedButton(
                    onClick = {
                        MedicineReminderScheduler.scheduleTestAlarm(context)
                        android.widget.Toast.makeText(context, "Test alarm will ring in 5 seconds", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    enabled = enabled,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Test alarm", color = if (enabled) ReminderBlue else ReminderGrey)
                }
            }

            // ── Scheduled alerts ────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Upcoming alerts", style = AppTextStyles.style16BCB(), modifier = Modifier.weight(1f))
                if (syncing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            }

            val groups = doses.groupBy { it.medicineName.trim() }
            if (groups.isEmpty() && !syncing) {
                SettingsCard {
                    Text(
                        "No upcoming doses. Alarms are set automatically for medicines you add.",
                        color = ReminderGrey, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                }
            }
            groups.forEach { (name, medDoses) ->
                val isMuted = name.lowercase() in muted
                MedicineAlertCard(
                    name = name,
                    dose = medDoses.first().dose,
                    alertTimes = medDoses.mapNotNull { MedicineReminderScheduler.alertTimeMillis(it, offset) },
                    active = enabled && !isMuted,
                    checked = !isMuted,
                    switchEnabled = enabled,
                    onCheckedChange = { on ->
                        settings.setMuted(name, !on)
                        muted = settings.mutedMedicines
                        MedicineReminderScheduler.reschedule(context)
                    },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardContainerColor)
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun RowHeader(icon: ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Text(title, style = AppTextStyles.style14BCB())
    }
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color = ReminderBlue) {
    Box(
        modifier = Modifier.size(34.dp).background(tint.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconBadge(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = AppTextStyles.style14BCB())
            Text(subtitle, color = ReminderGrey, fontSize = 12.sp)
        }
        HapticSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = ReminderBlue),
        )
    }
}

@Composable
private fun ChoiceChips(
    options: List<Int>,
    selected: Int,
    label: (Int) -> String,
    enabled: Boolean,
    onSelect: (Int) -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState()),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                enabled = enabled,
                onClick = { onSelect(option) },
                label = { Text(label(option), fontSize = 13.sp) },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = ReminderBlue,
                    selectedLabelColor = Color.White,
                ),
            )
        }
    }
}

@Composable
private fun AlarmSetupCard(requirements: List<AlarmSetup.Requirement>, onFix: (AlarmSetup.Item) -> Unit) {
    val missingCritical = requirements.count { it.critical && !it.granted }
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (missingCritical > 0) WarningAmber.copy(alpha = 0.10f) else colors.dashboardContainerColor)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                if (missingCritical > 0) Icons.Rounded.WarningAmber else Icons.Rounded.CheckCircle,
                tint = if (missingCritical > 0) WarningAmber else ReminderGreen,
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Alarm setup", style = AppTextStyles.style14BCB())
                Text(
                    if (missingCritical > 0) "$missingCritical step${if (missingCritical > 1) "s" else ""} left for reliable full-screen alarms"
                    else "Alarms will ring on time and open full screen",
                    color = ReminderGrey, fontSize = 12.sp,
                )
            }
        }
        requirements.forEach { req ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (req.granted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked, null,
                    tint = when {
                        req.granted -> ReminderGreen
                        req.critical -> WarningAmber
                        else -> ReminderGrey
                    },
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(req.title + if (!req.critical) " (recommended)" else "", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    if (!req.granted) Text(req.description, color = ReminderGrey, fontSize = 12.sp)
                }
                if (!req.granted) {
                    HapticTextButton(onClick = { onFix(req.item) }) {
                        Text("Allow", color = ReminderBlue, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

private val alertTimeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

private fun formatAlertTime(millis: Long): String {
    val time = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault())
    val prefix = when (time.toLocalDate()) {
        LocalDate.now() -> ""
        LocalDate.now().plusDays(1) -> "Tmrw "
        else -> time.toLocalDate().toString() + " "
    }
    return prefix + time.format(alertTimeFormat)
}

@Composable
private fun MedicineAlertCard(
    name: String,
    dose: String,
    alertTimes: List<Long>,
    active: Boolean,
    checked: Boolean,
    switchEnabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val now = System.currentTimeMillis()
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Rounded.Medication, tint = if (active) ReminderBlue else ReminderGrey)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = AppTextStyles.style14BCB())
                if (dose.isNotBlank()) Text(dose, color = ReminderGrey, fontSize = 12.sp)
            }
            HapticSwitch(
                checked = checked,
                enabled = switchEnabled,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(checkedTrackColor = ReminderBlue),
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState()),
        ) {
            alertTimes.sorted().forEach { at ->
                // An alert whose ring time already passed (but dose is still ahead) won't ring.
                val upcoming = at > now
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (active && upcoming) ReminderBlue.copy(alpha = 0.1f) else ReminderGrey.copy(alpha = 0.1f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Icon(Icons.Rounded.Alarm, null, tint = if (active && upcoming) ReminderBlue else ReminderGrey,
                        modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(formatAlertTime(at), fontSize = 12.sp, color = if (active && upcoming) ReminderBlue else ReminderGrey)
                }
            }
        }
    }
}
