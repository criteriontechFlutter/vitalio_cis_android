package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.AppMotion
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.critetiontech.ctvitalio.ui.components.*
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.omron.OmronKind
import com.critetiontech.ctvitalio.omron.OmronModel
import com.critetiontech.ctvitalio.omron.OmronModels
import com.critetiontech.ctvitalio.omron.OmronReading
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.critetiontech.ctvitalio.viewmodel.OmronDeviceViewModel
import com.critetiontech.ctvitalio.viewmodel.OmronPhase

// ─── Screen ───────────────────────────────────────────────────────────────────

internal fun Context.bluetoothOn(): Boolean =
    getSystemService(BluetoothManager::class.java)?.adapter?.isEnabled == true

@Composable
fun OmronDeviceScreen(modelCode: String, viewModel: OmronDeviceViewModel = viewModel()) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    LaunchedEffect(modelCode) { viewModel.load(modelCode) }

    val device by viewModel.device.collectAsState()
    val phase by viewModel.phase.collectAsState()
    val userCount by viewModel.userCount.collectAsState()
    val selectedUser by viewModel.selectedUser.collectAsState()
    val model = viewModel.model ?: OmronModels.byCode(modelCode)
    val kind = viewModel.kind ?: OmronModels.autoKind(modelCode) ?: model?.kind
    val kindLabel = kind?.label?.let { deviceKindLabel(it) }.orEmpty()

    // Bluetooth permission → Bluetooth on → action
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.bluetoothOn()) pendingAction?.invoke()
        else viewModel.showError(context.getString(R.string.device_omron_bluetooth_off))
        pendingAction = null
    }
    val nearbyDevices = rememberPermissionRequest(
        AppPermission.NearbyDevices,
        stringResource(R.string.device_omron_permission_reason),
        onDenied = { pendingAction = null },
    ) {
        if (context.bluetoothOn()) { pendingAction?.invoke(); pendingAction = null }
        else enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
    fun withBluetooth(action: () -> Unit) {
        pendingAction = action
        nearbyDevices.launch()
    }
    val sync = { withBluetooth(viewModel::sync) }
    val pair = { withBluetooth(viewModel::pair) }

    CommonAppBar(title = "Omron ${model?.code ?: kindLabel}") {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor).verticalScroll(rememberScrollState())
        ) {
            if (kind == null) {
                Text(stringResource(R.string.device_not_supported), modifier = Modifier.padding(24.dp), color = colors.textGreyColor)
                return@Column
            }
            val busy = phase == OmronPhase.Pairing || phase == OmronPhase.Syncing
            val saving = (phase as? OmronPhase.Synced)?.saving == true
            val d = device

            AnimatedEnter(index = 0) {
                DeviceHeader(
                    icon = if (kind == OmronKind.BP) Icons.Filled.MonitorHeart else Icons.Filled.MonitorWeight,
                    eyebrow = if (model != null) "Omron · ${deviceKindLabel(model.label)}" else "Omron · ${stringResource(R.string.device_any_supported_model)}",
                    title = model?.code ?: kindLabel,
                    status = when {
                        phase == OmronPhase.Pairing -> stringResource(R.string.device_status_searching)
                        phase == OmronPhase.Syncing -> stringResource(R.string.device_status_reading)
                        d == null -> stringResource(R.string.device_status_not_paired)
                        d.lastSyncAt == 0L -> stringResource(R.string.device_paired_user, d.user)
                        else -> stringResource(R.string.device_user_synced, d.user, syncedAgo(d.lastSyncAt))
                    },
                    live = d != null,
                    busy = busy,
                    action = {
                        if (d == null) HeaderActionPill(if (busy) stringResource(R.string.device_action_pairing) else stringResource(R.string.device_action_pair), Icons.Filled.Bluetooth, enabled = !busy, onClick = pair)
                        else HeaderActionPill(
                            if (busy) stringResource(R.string.device_action_syncing) else stringResource(R.string.device_action_sync), Icons.Filled.Sync,
                            enabled = !busy && !saving, spinning = phase == OmronPhase.Syncing, onClick = sync
                        )
                    }
                )
            }

            // Phase card swaps with a fade + slide so status changes feel continuous
            AnimatedContent(
                targetState = phase,
                contentKey = { it::class },
                transitionSpec = {
                    (fadeIn(tween(AppMotion.MEDIUM)) + slideInVertically(tween(AppMotion.MEDIUM)) { it / 6 }) togetherWith
                        fadeOut(tween(AppMotion.SHORT)) using SizeTransform(clip = false)
                },
                label = "omronPhase"
            ) { p ->
                when (p) {
                    OmronPhase.Pairing -> RadarBusyCard(
                        stringResource(R.string.device_omron_searching_for, kindLabel.lowercase()),
                        stringResource(R.string.device_omron_searching_hint),
                        onCancel = viewModel::cancel
                    )
                    OmronPhase.Syncing -> RadarBusyCard(stringResource(R.string.device_omron_reading_measurements), stringResource(R.string.device_omron_keep_device_close), onCancel = viewModel::cancel)
                    OmronPhase.JustPaired -> StatusCard(
                        true, stringResource(R.string.device_model_paired, model?.code ?: stringResource(R.string.device_generic_device)), stringResource(R.string.device_omron_just_paired_message),
                        action = stringResource(R.string.device_action_sync_now), actionIcon = Icons.Filled.Sync, onAction = sync
                    )
                    is OmronPhase.Failed -> StatusCard(
                        false, stringResource(R.string.device_couldnt_connect), p.message,
                        action = stringResource(R.string.device_action_try_again), actionIcon = Icons.Filled.Refresh, onAction = if (d == null) pair else sync
                    )
                    is OmronPhase.Synced -> if (p.latest == null) StatusCard(
                        false, stringResource(R.string.device_omron_no_readings),
                        stringResource(R.string.device_omron_no_readings_message),
                        icon = Icons.Filled.Info, tint = DeviceAmber,
                        action = stringResource(R.string.device_action_sync_again), actionIcon = Icons.Filled.Sync, onAction = sync
                    ) else ReadingCard(p, p.latest, kind, onSave = viewModel::save)
                    OmronPhase.Idle -> Spacer(Modifier.height(0.dp))
                }
            }

            if (d == null) {
                if (userCount > 1) AnimatedEnter(index = 1) {
                    UserPicker(userCount, selectedUser, enabled = !busy, onSelect = viewModel::selectUser)
                }
                AnimatedEnter(index = 2) {
                    StepsCard(
                        stringResource(R.string.device_omron_how_to_pair),
                        listOf(
                            Icons.Filled.BatteryChargingFull to stringResource(R.string.device_omron_pair_step_1),
                            Icons.Filled.Bluetooth to stringResource(R.string.device_omron_pair_step_2),
                            Icons.Filled.TouchApp to stringResource(R.string.device_omron_pair_step_3)
                        )
                    )
                }
                AnimatedEnter(index = 3) { SupportedDevicesCard(kind) }
            } else {
                AnimatedEnter(index = 1) {
                    StepsCard(
                        stringResource(R.string.device_omron_how_to_sync),
                        if (kind == OmronKind.BP) listOf(
                            Icons.Filled.MonitorHeart to stringResource(R.string.device_omron_sync_bp_step_1, d.user),
                            Icons.Filled.Bluetooth to stringResource(R.string.device_omron_sync_bp_step_2),
                            Icons.Filled.CloudUpload to stringResource(R.string.device_omron_sync_bp_step_3)
                        ) else listOf(
                            Icons.Filled.MonitorWeight to stringResource(R.string.device_omron_sync_scale_step_1),
                            Icons.Filled.Bluetooth to stringResource(R.string.device_omron_sync_scale_step_2),
                            Icons.Filled.Sync to stringResource(R.string.device_omron_sync_scale_step_3)
                        )
                    )
                }
                AnimatedEnter(index = 2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        HapticTextButton(onClick = viewModel::remove, enabled = !busy && !saving) {
                            Icon(Icons.Filled.LinkOff, null, tint = DeviceRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.device_remove_device), color = DeviceRed)
                        }
                        Text(
                            stringResource(R.string.device_omron_remove_hint),
                            fontSize = 11.sp, color = colors.textGreyColor, textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

internal val readingDay = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
internal val readingClock = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())

/** The models that pair, so people can check theirs; pairing detects which one it is. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SupportedDevicesCard(kind: OmronKind) {
    val colors = LocalMyColorScheme.current
    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Caption(stringResource(R.string.device_supported_devices))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.device_omron_supported_models_intro),
            fontSize = 12.sp, color = colors.textDarkColor, lineHeight = 17.sp
        )
        Spacer(Modifier.height(10.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OmronModels.ofKind(kind).forEach { m ->
                Text(
                    m.code, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = colors.primaryBlueColor,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(tintFill(colors.primaryBlueColor))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun UserPicker(count: Int, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    FlatCard {
        Caption(stringResource(R.string.device_device_user))
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.device_omron_pick_user), fontSize = 12.sp, color = colors.textDarkColor, lineHeight = 17.sp)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            (1..count).forEach { user ->
                val on = user == selected
                val bg by animateColorAsState(if (on) p else tintFill(p), label = "userBg")
                Row(
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(bg)
                        .clickable(enabled = enabled) { onSelect(user) }
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Person, null, tint = if (on) Color.White else p, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.device_user_number, user), fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (on) Color.White else colors.textDarkColor)
                }
            }
        }
    }
}

/** AHA blood-pressure categories, used to colour the reading and place it on the gauge. */
internal data class BpCategory(@androidx.annotation.StringRes val label: Int, val color: Color, val index: Int)

internal val bpScale = listOf(
    R.string.device_bp_normal to Color(0xFF43A047),
    R.string.device_bp_elevated to Color(0xFFF9A825),
    R.string.device_bp_stage_1 to Color(0xFFFB8C00),
    R.string.device_bp_stage_2 to Color(0xFFE53935),
    R.string.device_bp_crisis to Color(0xFFB71C1C),
)

internal fun bpCategory(sys: Int, dia: Int): BpCategory {
    val i = when {
        sys > 180 || dia > 120 -> 4
        sys >= 140 || dia >= 90 -> 3
        sys >= 130 || dia >= 80 -> 2
        sys >= 120 -> 1
        else -> 0
    }
    val label = when (i) { 2 -> R.string.device_bp_high_stage_1; 3 -> R.string.device_bp_high_stage_2; 4 -> R.string.device_bp_hypertensive_crisis; else -> bpScale[i].first }
    return BpCategory(label, bpScale[i].second, i)
}

/** Five-band AHA bar with a marker that glides to the reading's band. */
@Composable
internal fun BpGauge(category: BpCategory) {
    val colors = LocalMyColorScheme.current
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { placed = true }
    val position by animateFloatAsState(
        if (placed) (category.index + 0.5f) / bpScale.size else 0f,
        spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessVeryLow), label = "gauge"
    )
    Column {
        BoxWithConstraints(Modifier.fillMaxWidth().height(18.dp)) {
            Row(Modifier.fillMaxWidth().height(6.dp).align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                bpScale.forEachIndexed { i, (_, c) ->
                    Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(c.copy(alpha = if (i == category.index) 1f else 0.3f)))
                }
            }
            Box(
                Modifier.offset(x = maxWidth * position - 9.dp).size(18.dp).clip(CircleShape)
                    .background(colors.dashboardContainerColor).border(3.dp, category.color, CircleShape)
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth()) {
            bpScale.forEachIndexed { i, (name, _) ->
                Text(
                    stringResource(name), fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    color = if (i == category.index) category.color else colors.textGreyColor,
                    fontWeight = if (i == category.index) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun ReadingCard(result: OmronPhase.Synced, r: OmronReading, kind: OmronKind, onSave: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val local = r.time.atZone(ZoneId.systemDefault())
    val category = if (r.isBloodPressure) bpCategory(r.systolic!!, r.diastolic!!) else null
    val accent = category?.color ?: colors.primaryBlueColor
    val blink = rememberInfiniteTransition(label = "live")
    val liveAlpha by blink.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "liveAlpha")

    FlatCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(7.dp).graphicsLayer { alpha = liveAlpha }.clip(CircleShape).background(accent))
            Spacer(Modifier.width(6.dp))
            Caption(stringResource(R.string.device_latest_reading))
            r.sequence?.let {
                Spacer(Modifier.width(6.dp))
                Text(
                    "#$it", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.textGreyColor,
                    modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(tintFill(colors.textGreyColor))
                        .padding(horizontal = 6.dp, vertical = 1.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            category?.let {
                Text(
                    stringResource(it.label), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = it.color,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(tintFill(it.color))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (r.isBloodPressure) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Caption(stringResource(R.string.device_field_sys))
                    Text("${rememberCountUp(r.systolic!!)}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 50.sp)
                }
                Text("/", fontSize = 36.sp, color = colors.textGreyColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Column {
                    Caption(stringResource(R.string.device_field_dia))
                    Text("${rememberCountUp(r.diastolic!!)}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 50.sp)
                }
                Text("mmHg", fontSize = 13.sp, color = colors.textGreyColor, modifier = Modifier.padding(start = 8.dp, bottom = 10.dp))
            }
            Spacer(Modifier.height(14.dp))
            BpGauge(category!!)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${r.weightKg}", fontSize = 44.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor)
                Text(" kg", fontSize = 14.sp, color = colors.textGreyColor, modifier = Modifier.padding(bottom = 8.dp))
            }
            r.bodyFatPercent?.let { Text(stringResource(R.string.device_body_fat_percent, it), fontSize = 13.sp, color = colors.textGreyColor) }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (r.isBloodPressure) {
                InfoTile(stringResource(R.string.device_field_pulse), r.pulse?.let { "$it bpm" } ?: "--", Icons.Filled.Favorite, Color(0xFFE53935), Modifier.weight(1f), heartbeat = true)
            }
            InfoTile(
                if (r.timeFromDevice) stringResource(R.string.device_measured) else stringResource(R.string.device_synced),
                "${local.format(readingClock)}\n${local.format(readingDay)}",
                Icons.Filled.Schedule, colors.primaryBlueColor, Modifier.weight(1f)
            )
        }
        if (!r.timeFromDevice) {
            NoteRow(Icons.Filled.Info, stringResource(R.string.device_omron_clock_not_set), DeviceAmber, Modifier.padding(top = 10.dp))
        }
        if (r.irregularHeartbeat) {
            NoteRow(Icons.Filled.Warning, stringResource(R.string.device_irregular_heartbeat), DeviceAmber, Modifier.padding(top = 10.dp))
        }
        AnimatedVisibility(visible = result.error != null) {
            NoteRow(Icons.Filled.ErrorOutline, stringResource(R.string.device_couldnt_save_reason, result.error.orEmpty()), DeviceRed, Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(16.dp))

        val canSave = r.isBloodPressure && kind == OmronKind.BP
        AnimatedContent(
            targetState = when { !canSave -> 0; result.saved -> 2; else -> 1 },
            transitionSpec = { (fadeIn(tween(AppMotion.MEDIUM)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(AppMotion.SHORT)) },
            label = "saveState"
        ) { state ->
            when (state) {
                0 -> NoteRow(Icons.Filled.Info, stringResource(R.string.device_omron_weight_not_stored), colors.textGreyColor)
                2 -> SavedBanner()
                else -> GradientButton(
                    if (result.error != null) stringResource(R.string.device_action_try_again) else stringResource(R.string.device_save_to_health_record),
                    if (result.error != null) Icons.Filled.Refresh else Icons.Filled.CloudUpload,
                    colorsOverride = listOf(Color(0xFF1B7A43), DeviceGreen, Color(0xFF4CC17F)),
                    loading = result.saving,
                    onClick = onSave
                )
            }
        }
    }
}

@Composable
internal fun InfoTile(
    label: String, value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color,
    modifier: Modifier = Modifier, heartbeat: Boolean = false
) {
    val colors = LocalMyColorScheme.current
    val beat = rememberInfiniteTransition(label = "beat")
    val scale by beat.animateFloat(
        1f, if (heartbeat) 1.25f else 1f,
        infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "beatScale"
    )
    Row(
        modifier.clip(RoundedCornerShape(14.dp)).background(tintFill(colors.primaryBlueColor)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tintFill(tint)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp).graphicsLayer { scaleX = scale; scaleY = scale }) }
        Spacer(Modifier.width(10.dp))
        Column {
            Caption(label)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = colors.textDarkColor, lineHeight = 16.sp)
        }
    }
}

@Composable
internal fun SavedBanner() {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val checkScale by animateFloatAsState(
        if (shown) 1f else 0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "check"
    )
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tintFill(DeviceGreen)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(34.dp).graphicsLayer { scaleX = checkScale; scaleY = checkScale }.clip(CircleShape).background(DeviceGreen),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.device_saved_to_health_record), fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DeviceGreen)
            Text(stringResource(R.string.device_now_shows_in_vitals), fontSize = 12.sp, color = DeviceGreen.copy(alpha = 0.8f))
        }
    }
}
