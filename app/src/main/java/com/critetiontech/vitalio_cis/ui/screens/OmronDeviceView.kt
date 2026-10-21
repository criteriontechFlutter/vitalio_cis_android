package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.omron.OmronException
import com.critetiontech.ctvitalio.omron.OmronKind
import com.critetiontech.ctvitalio.omron.OmronManager
import com.critetiontech.ctvitalio.omron.OmronModel
import com.critetiontech.ctvitalio.omron.OmronModels
import com.critetiontech.ctvitalio.omron.OmronProfile
import com.critetiontech.ctvitalio.omron.OmronReading
import com.critetiontech.ctvitalio.omron.PairedOmronDevice
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// ─── ViewModel ────────────────────────────────────────────────────────────────

sealed interface OmronPhase {
    data object Idle : OmronPhase
    data object Pairing : OmronPhase
    data object Syncing : OmronPhase
    data object JustPaired : OmronPhase
    data class Synced(val readings: List<OmronReading>, val saved: Set<String>, val error: String?) : OmronPhase
    data class Failed(val message: String) : OmronPhase
}

class OmronDeviceViewModel : ViewModel() {

    private val context = MyApplication.appContext
    private var job: Job? = null

    var model: OmronModel? = null
        private set

    private val _device = MutableStateFlow<PairedOmronDevice?>(null)
    val device: StateFlow<PairedOmronDevice?> = _device

    private val _phase = MutableStateFlow<OmronPhase>(OmronPhase.Idle)
    val phase: StateFlow<OmronPhase> = _phase

    private val _userCount = MutableStateFlow(1)
    val userCount: StateFlow<Int> = _userCount

    private val _selectedUser = MutableStateFlow(1)
    val selectedUser: StateFlow<Int> = _selectedUser

    fun load(code: String) {
        if (model?.code == code) return
        model = OmronModels.byCode(code)
        _device.value = OmronManager.pairedDevice(context, code)
        _userCount.value = model?.let { OmronManager.userCount(OmronManager.deviceConfig(context, it)) } ?: 1
    }

    fun selectUser(user: Int) { _selectedUser.value = user }

    fun showError(message: String) { _phase.value = OmronPhase.Failed(message) }

    fun pair() = run(OmronPhase.Pairing) {
        val m = model ?: return@run
        _device.value = OmronManager.pair(context, m, _selectedUser.value, profile(m))
        _phase.value = OmronPhase.JustPaired
    }

    fun sync() = run(OmronPhase.Syncing) {
        val m = model ?: return@run
        val d = _device.value ?: return@run
        val result = OmronManager.sync(context, d, profile(m))
        _device.value = OmronManager.pairedDevice(context, d.code)
        _phase.value = OmronPhase.Synced(result.readings, result.saved, result.error)
    }

    fun cancel() {
        job?.cancel()
        _phase.value = OmronPhase.Idle
    }

    fun remove() {
        model?.let { OmronManager.removeDevice(context, it.code) }
        _device.value = null
        _phase.value = OmronPhase.Idle
    }

    fun reset() { _phase.value = OmronPhase.Idle }

    private fun run(busy: OmronPhase, block: suspend () -> Unit) {
        job?.cancel()
        _phase.value = busy
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: OmronException) {
                Log.w("OmronDevice", "error ${e.code}: ${e.message}")
                _phase.value = OmronPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            } catch (e: Exception) {
                Log.e("OmronDevice", "unexpected", e)
                _phase.value = OmronPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    /** Scales need height, gender and date of birth; height comes from the last saved vitals. */
    private suspend fun profile(model: OmronModel): OmronProfile? {
        if (model.kind != OmronKind.SCALE) return null
        val patient = AppDependencies.prefs.getPatient() ?: return null
        val height = (AppDependencies.fetchLastVital()(patient.uhId, patient.clientId.toString(), patient.pid.toString())
                as? DomainResult.Success)?.data
            ?.firstOrNull { it.vitalName.equals("Height", ignoreCase = true) }?.vitalValue?.takeIf { it > 50 }
        val dob = runCatching { LocalDate.parse(patient.dob.take(10)) }.getOrNull()
        return OmronProfile(height, dob, isMale = patient.genderName.startsWith("M", ignoreCase = true))
    }
}

// ─── Screen ───────────────────────────────────────────────────────────────────

private fun bluetoothPermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

private fun Context.hasBluetoothPermissions() = bluetoothPermissions().all {
    ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
}

private fun Context.bluetoothOn(): Boolean =
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

    // Bluetooth permission → Bluetooth on → action
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val enableBluetooth = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (context.bluetoothOn()) pendingAction?.invoke()
        else viewModel.showError("Bluetooth is off. Turn it on to connect your Omron device.")
        pendingAction = null
    }
    val requestPermissions = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            if (context.bluetoothOn()) { pendingAction?.invoke(); pendingAction = null }
            else enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        } else {
            pendingAction = null
            viewModel.showError("Allow \"Nearby devices\" permission so Vitalio can find your Omron device.")
        }
    }
    fun withBluetooth(action: () -> Unit) {
        pendingAction = action
        when {
            !context.hasBluetoothPermissions() -> requestPermissions.launch(bluetoothPermissions())
            !context.bluetoothOn() -> enableBluetooth.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else -> { pendingAction = null; action() }
        }
    }

    CommonAppBar(title = "Omron ${model?.code.orEmpty()}") {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor).verticalScroll(rememberScrollState())
        ) {
            if (model == null) {
                Text("This device isn't supported.", modifier = Modifier.padding(24.dp), color = colors.textGreyColor)
                return@Column
            }
            OmronHeader(model, device)

            val busy = phase == OmronPhase.Pairing || phase == OmronPhase.Syncing
            when (val p = phase) {
                OmronPhase.Pairing -> BusyCard("Searching for your ${model.code}…", "Keep \"P\" blinking on the device and accept the Bluetooth request if asked.", viewModel::cancel)
                OmronPhase.Syncing -> BusyCard("Reading measurements…", "Keep the device close to your phone.", viewModel::cancel)
                OmronPhase.JustPaired -> MessageCard(true, "Device paired!", "Take a measurement, then tap Sync Readings to save it to your health record.", "OK", viewModel::reset)
                is OmronPhase.Failed -> MessageCard(false, "Couldn't connect", p.message, "OK", viewModel::reset)
                is OmronPhase.Synced -> SyncResultCard(p, model, onDone = viewModel::reset)
                OmronPhase.Idle -> Unit
            }

            if (device == null) {
                StepsCard(
                    "How to pair",
                    listOf(
                        "Put batteries in and set the date & time on the device (see its manual).",
                        "Press and hold the Bluetooth / transfer button until \"P\" blinks on the screen.",
                        "Keep the device next to your phone and tap Pair Device."
                    )
                )
                if (userCount > 1) UserPicker(userCount, selectedUser, enabled = !busy, onSelect = viewModel::selectUser)
                PrimaryButton(if (busy) "Pairing…" else "Pair Device", Icons.Filled.Bluetooth, enabled = !busy) {
                    withBluetooth(viewModel::pair)
                }
            } else {
                StepsCard(
                    "How to sync",
                    if (model.kind == OmronKind.BP) listOf(
                        "Take your blood pressure as usual.",
                        "Press the Bluetooth / transfer button once (some models send automatically).",
                        "Tap Sync Readings — new readings are saved to your health record."
                    ) else listOf(
                        "Step on the scale and wait for the reading.",
                        "Keep the scale on; it sends readings over Bluetooth.",
                        "Tap Sync Readings."
                    )
                )
                PrimaryButton(if (busy) "Syncing…" else "Sync Readings", Icons.Filled.Sync, enabled = !busy) {
                    withBluetooth(viewModel::sync)
                }
                HapticTextButton(
                    onClick = viewModel::remove, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                ) { Text("Remove device", color = Color(0xFFC62828)) }
                Text(
                    "After removing, also forget the device in your phone's Bluetooth settings before pairing again.",
                    fontSize = 11.sp, color = colors.textGreyColor, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

private val readingTime = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())

@Composable
private fun OmronHeader(model: OmronModel, device: PairedOmronDevice?) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5)))).padding(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (model.kind == OmronKind.BP) Icons.Filled.MonitorHeart else Icons.Filled.MonitorWeight,
                    null, tint = Color.White, modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Omron ${model.code}", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text(model.label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (device != null) Icons.Filled.BluetoothConnected else Icons.Filled.BluetoothDisabled,
                        null, tint = Color.White, modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        when {
                            device == null -> "Not paired"
                            device.lastSyncAt == 0L -> "Paired · User ${device.user}"
                            else -> "Paired · User ${device.user} · synced " +
                                android.text.format.DateUtils.getRelativeTimeSpanString(device.lastSyncAt)
                        },
                        fontSize = 11.sp, color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun StepsCard(title: String, steps: List<String>) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).padding(16.dp)
    ) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1565C0))
        Spacer(Modifier.height(10.dp))
        steps.forEachIndexed { i, text ->
            Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF1565C0)),
                    contentAlignment = Alignment.Center
                ) { Text("${i + 1}", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                Spacer(Modifier.width(8.dp))
                Text(text, fontSize = 12.sp, color = Color(0xFF333333), lineHeight = 17.sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun UserPicker(count: Int, selected: Int, enabled: Boolean, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Text("Which user are you on the device?", style = AppTextStyles.style14BCB())
        Text("Readings are stored per user on the monitor.", style = AppTextStyles.style12GCN())
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            (1..count).forEach { user ->
                FilterChip(
                    selected = user == selected, enabled = enabled, onClick = { onSelect(user) },
                    label = { Text("User $user") }
                )
            }
        }
    }
}

@Composable
private fun PrimaryButton(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, onClick: () -> Unit) {
    HapticButton(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp).height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0), disabledContainerColor = Color(0xFFBBDEFB))
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun BusyCard(title: String, subtitle: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE3F2FD)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF1565C0), modifier = Modifier.size(36.dp))
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0), textAlign = TextAlign.Center)
        Text(subtitle, fontSize = 12.sp, color = Color(0xFF546788), textAlign = TextAlign.Center)
        HapticTextButton(onClick = onCancel) { Text("Cancel", color = Color(0xFF1565C0)) }
    }
}

@Composable
private fun MessageCard(success: Boolean, title: String, message: String, button: String, onClick: () -> Unit) {
    val tint = if (success) Color(0xFF2E7D32) else Color(0xFFC62828)
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(if (success) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline, null, tint = tint, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, fontWeight = FontWeight.Bold, color = tint)
        Text(message, fontSize = 12.sp, color = Color(0xFF546788), textAlign = TextAlign.Center, lineHeight = 17.sp)
        Spacer(Modifier.height(8.dp))
        HapticOutlinedButton(onClick = onClick, shape = RoundedCornerShape(12.dp)) { Text(button, color = tint) }
    }
}

@Composable
private fun SyncResultCard(result: OmronPhase.Synced, model: OmronModel, onDone: () -> Unit) {
    val savedCount = result.saved.size
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8F5E9)).padding(16.dp)
    ) {
        Text(
            when {
                result.readings.isEmpty() -> "No new readings"
                savedCount > 0 -> "$savedCount reading(s) saved"
                else -> "${result.readings.size} reading(s) received"
            },
            fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF2E7D32)
        )
        if (result.readings.isEmpty()) {
            Text(
                "Take a measurement first. If you just did, press the device's Bluetooth button and sync again.",
                fontSize = 12.sp, color = Color(0xFF546788)
            )
        }
        result.error?.let {
            Text("Some readings weren't saved: $it. They'll be retried on the next sync.", fontSize = 12.sp, color = Color(0xFFC62828))
        }
        if (model.kind == OmronKind.SCALE && result.readings.isNotEmpty()) {
            Text("Weight is shown here only — the server doesn't store weight from devices yet.", fontSize = 11.sp, color = Color(0xFF546788))
        }
        Spacer(Modifier.height(8.dp))
        result.readings.take(10).forEach { r ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (r.isBloodPressure) "${r.systolic}/${r.diastolic} mmHg" + (r.pulse?.let { " · $it bpm" } ?: "")
                        else "${r.weightKg} kg" + (r.bodyFatPercent?.let { " · fat $it%" } ?: ""),
                        fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF263238)
                    )
                    Text(
                        r.time.atZone(ZoneId.systemDefault()).format(readingTime) +
                            if (r.irregularHeartbeat) " · irregular heartbeat" else "",
                        fontSize = 11.sp, color = Color(0xFF546788)
                    )
                }
                Text(
                    when {
                        !r.isBloodPressure -> "App only"
                        r.key in result.saved -> "Saved"
                        else -> "Not saved"
                    },
                    fontSize = 11.sp,
                    color = if (r.key in result.saved) Color(0xFF2E7D32) else Color(0xFF9E9E9E)
                )
            }
        }
        HapticTextButton(onClick = onDone, modifier = Modifier.align(Alignment.End)) { Text("Done", color = Color(0xFF2E7D32)) }
    }
}
