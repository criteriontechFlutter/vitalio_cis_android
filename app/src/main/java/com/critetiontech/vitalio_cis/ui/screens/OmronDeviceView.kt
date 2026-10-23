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

    // Bluetooth permission → Bluetooth on → action
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.bluetoothOn()) pendingAction?.invoke()
        else viewModel.showError("Bluetooth is off. Turn it on to connect your Omron device.")
        pendingAction = null
    }
    val nearbyDevices = rememberPermissionRequest(
        AppPermission.NearbyDevices,
        "to find your Omron device and read its measurements over Bluetooth",
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

    CommonAppBar(title = "Omron ${model?.code ?: kind?.label.orEmpty()}") {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor).verticalScroll(rememberScrollState())
        ) {
            if (kind == null) {
                Text("This device isn't supported.", modifier = Modifier.padding(24.dp), color = colors.textGreyColor)
                return@Column
            }
            val busy = phase == OmronPhase.Pairing || phase == OmronPhase.Syncing
            val saving = (phase as? OmronPhase.Synced)?.saving == true
            val d = device

            AnimatedEnter(index = 0) {
                DeviceHeader(
                    icon = if (kind == OmronKind.BP) Icons.Filled.MonitorHeart else Icons.Filled.MonitorWeight,
                    eyebrow = if (model != null) "Omron · ${model.label}" else "Omron · Any supported model",
                    title = model?.code ?: kind.label,
                    status = when {
                        phase == OmronPhase.Pairing -> "Searching…"
                        phase == OmronPhase.Syncing -> "Reading…"
                        d == null -> "Not paired"
                        d.lastSyncAt == 0L -> "Paired · User ${d.user}"
                        else -> "User ${d.user} · synced ${syncedAgo(d.lastSyncAt)}"
                    },
                    live = d != null,
                    busy = busy,
                    action = {
                        if (d == null) HeaderActionPill(if (busy) "Pairing" else "Pair", Icons.Filled.Bluetooth, enabled = !busy, onClick = pair)
                        else HeaderActionPill(
                            if (busy) "Syncing" else "Sync", Icons.Filled.Sync,
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
                        "Searching for your Omron ${kind.label.lowercase()}",
                        "Keep \"P\" blinking on the device and accept the Bluetooth request if asked. We'll recognise the model.",
                        onCancel = viewModel::cancel
                    )
                    OmronPhase.Syncing -> RadarBusyCard("Reading measurements", "Keep the device close to your phone.", onCancel = viewModel::cancel)
                    OmronPhase.JustPaired -> StatusCard(
                        true, "${model?.code ?: "Device"} paired", "Take a measurement on the device, then sync to see it here.",
                        action = "Sync now", actionIcon = Icons.Filled.Sync, onAction = sync
                    )
                    is OmronPhase.Failed -> StatusCard(
                        false, "Couldn't connect", p.message,
                        action = "Try again", actionIcon = Icons.Filled.Refresh, onAction = if (d == null) pair else sync
                    )
                    is OmronPhase.Synced -> if (p.latest == null) StatusCard(
                        false, "No readings found",
                        "Take a measurement first. If you just did, press the device's Bluetooth button and sync again.",
                        icon = Icons.Filled.Info, tint = DeviceAmber,
                        action = "Sync again", actionIcon = Icons.Filled.Sync, onAction = sync
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
                        "How to pair",
                        listOf(
                            Icons.Filled.BatteryChargingFull to "Put batteries in and set the date & time on the device (see its manual).",
                            Icons.Filled.Bluetooth to "Press and hold the Bluetooth / transfer button until \"P\" blinks on the screen.",
                            Icons.Filled.TouchApp to "Keep the device next to your phone and tap Pair above."
                        )
                    )
                }
                AnimatedEnter(index = 3) { SupportedDevicesCard(kind) }
            } else {
                AnimatedEnter(index = 1) {
                    StepsCard(
                        "How to sync",
                        if (kind == OmronKind.BP) listOf(
                            Icons.Filled.MonitorHeart to "Take your blood pressure as User ${d.user} on the device.",
                            Icons.Filled.Bluetooth to "Press the Bluetooth / transfer button once (some models send automatically).",
                            Icons.Filled.CloudUpload to "Tap Sync above, check your latest reading and tap Save."
                        ) else listOf(
                            Icons.Filled.MonitorWeight to "Step on the scale and wait for the reading.",
                            Icons.Filled.Bluetooth to "Keep the scale on; it sends readings over Bluetooth.",
                            Icons.Filled.Sync to "Tap Sync above."
                        )
                    )
                }
                AnimatedEnter(index = 2) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                        HapticTextButton(onClick = viewModel::remove, enabled = !busy && !saving) {
                            Icon(Icons.Filled.LinkOff, null, tint = DeviceRed, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Remove device", color = DeviceRed)
                        }
                        Text(
                            "After removing, also forget the device in your phone's Bluetooth settings before pairing again.",
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
            Caption("Supported devices")
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "No need to pick your model: the app recognises it while pairing. These Omron models work:",
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
        Caption("Device user")
        Spacer(Modifier.height(4.dp))
        Text("Pick the user you measure as on the device. Readings are stored per user.", fontSize = 12.sp, color = colors.textDarkColor, lineHeight = 17.sp)
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
                    Text("User $user", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = if (on) Color.White else colors.textDarkColor)
                }
            }
        }
    }
}

/** AHA blood-pressure categories, used to colour the reading and place it on the gauge. */
internal data class BpCategory(val label: String, val color: Color, val index: Int)

internal val bpScale = listOf(
    "Normal" to Color(0xFF43A047),
    "Elevated" to Color(0xFFF9A825),
    "Stage 1" to Color(0xFFFB8C00),
    "Stage 2" to Color(0xFFE53935),
    "Crisis" to Color(0xFFB71C1C),
)

internal fun bpCategory(sys: Int, dia: Int): BpCategory {
    val i = when {
        sys > 180 || dia > 120 -> 4
        sys >= 140 || dia >= 90 -> 3
        sys >= 130 || dia >= 80 -> 2
        sys >= 120 -> 1
        else -> 0
    }
    val label = when (i) { 2 -> "High · Stage 1"; 3 -> "High · Stage 2"; 4 -> "Hypertensive crisis"; else -> bpScale[i].first }
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
                    name, fontSize = 9.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
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
            Caption("Latest reading")
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
                    it.label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = it.color,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(tintFill(it.color))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        if (r.isBloodPressure) {
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Caption("Sys")
                    Text("${rememberCountUp(r.systolic!!)}", fontSize = 48.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor, lineHeight = 50.sp)
                }
                Text("/", fontSize = 36.sp, color = colors.textGreyColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                Column {
                    Caption("Dia")
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
            r.bodyFatPercent?.let { Text("Body fat $it%", fontSize = 13.sp, color = colors.textGreyColor) }
        }

        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (r.isBloodPressure) {
                InfoTile("Pulse", r.pulse?.let { "$it bpm" } ?: "--", Icons.Filled.Favorite, Color(0xFFE53935), Modifier.weight(1f), heartbeat = true)
            }
            InfoTile(
                if (r.timeFromDevice) "Measured" else "Synced",
                "${local.format(readingClock)}\n${local.format(readingDay)}",
                Icons.Filled.Schedule, colors.primaryBlueColor, Modifier.weight(1f)
            )
        }
        if (!r.timeFromDevice) {
            NoteRow(Icons.Filled.Info, "The monitor's clock isn't set, so the measured time is unknown. Set the date & time on the device.", DeviceAmber, Modifier.padding(top = 10.dp))
        }
        if (r.irregularHeartbeat) {
            NoteRow(Icons.Filled.Warning, "Irregular heartbeat detected", DeviceAmber, Modifier.padding(top = 10.dp))
        }
        AnimatedVisibility(visible = result.error != null) {
            NoteRow(Icons.Filled.ErrorOutline, "Couldn't save: ${result.error.orEmpty()}", DeviceRed, Modifier.padding(top = 10.dp))
        }
        Spacer(Modifier.height(16.dp))

        val canSave = r.isBloodPressure && kind == OmronKind.BP
        AnimatedContent(
            targetState = when { !canSave -> 0; result.saved -> 2; else -> 1 },
            transitionSpec = { (fadeIn(tween(AppMotion.MEDIUM)) + scaleIn(initialScale = 0.92f)) togetherWith fadeOut(tween(AppMotion.SHORT)) },
            label = "saveState"
        ) { state ->
            when (state) {
                0 -> NoteRow(Icons.Filled.Info, "Weight is shown here only — the server doesn't store weight from devices yet.", colors.textGreyColor)
                2 -> SavedBanner()
                else -> GradientButton(
                    if (result.error != null) "Try again" else "Save to health record",
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
            Text("Saved to your health record", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = DeviceGreen)
            Text("It now shows in Vitals.", fontSize = 12.sp, color = DeviceGreen.copy(alpha = 0.8f))
        }
    }
}
