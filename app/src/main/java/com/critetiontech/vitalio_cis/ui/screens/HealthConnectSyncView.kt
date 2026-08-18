package com.critetiontech.vitalio_cis.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

// ─── Data Models ──────────────────────────────────────────────────────────────

data class HealthMetric(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val iconTint: Color,
    val value: String = "--",
    val unit: String = "",
    val isSelected: Boolean = false,
    val isSynced: Boolean = false
)

enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR, NOT_INSTALLED, UPDATE_REQUIRED, PERMISSIONS_DENIED }

// ─── ViewModel ────────────────────────────────────────────────────────────────

class HealthConnectViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs
    private val addVitalUseCase = AppDependencies.addVital()

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _availabilityMessage = MutableStateFlow("Checking Health Connect…")
    val availabilityMessage: StateFlow<String> = _availabilityMessage

    private val _metrics = MutableStateFlow(defaultMetrics())
    val metrics: StateFlow<List<HealthMetric>> = _metrics

    private val _syncedCount = MutableStateFlow(0)
    val syncedCount: StateFlow<Int> = _syncedCount

    private val _deniedPermissions = MutableStateFlow<Set<String>>(emptySet())
    val deniedPermissions: StateFlow<Set<String>> = _deniedPermissions

    fun setPermissionsDenied(required: Set<String>, granted: Set<String>) {
        _deniedPermissions.value = required - granted
        _syncState.value = SyncState.PERMISSIONS_DENIED
    }

    fun checkAvailability(sdkStatus: Int) {
        Log.d("HealthConnect", "SDK status = $sdkStatus")
        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> {
                _availabilityMessage.value = "Health Connect is available ✓"
                if (_syncState.value != SyncState.IDLE) _syncState.value = SyncState.IDLE
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                _availabilityMessage.value = "Health Connect needs an update"
                _syncState.value = SyncState.UPDATE_REQUIRED
            }
            else -> {
                _availabilityMessage.value = "Health Connect is not installed"
                _syncState.value = SyncState.NOT_INSTALLED
            }
        }
    }

    fun toggleMetric(id: String) {
        _metrics.value = _metrics.value.map {
            if (it.id == id) it.copy(isSelected = !it.isSelected) else it
        }
    }

    fun selectAll() { _metrics.value = _metrics.value.map { it.copy(isSelected = true) } }
    fun deselectAll() { _metrics.value = _metrics.value.map { it.copy(isSelected = false) } }

    fun syncSelectedMetrics(client: HealthConnectClient) {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            _syncedCount.value = 0
            Log.d("HealthConnect", "Starting sync…")

            val selected = _metrics.value.filter { it.isSelected }
            if (selected.isEmpty()) {
                _syncState.value = SyncState.ERROR
                _errorMessage.value = "Please select at least one metric to sync."
                return@launch
            }

            val patient = prefs.getPatient()
            if (patient == null) {
                _syncState.value = SyncState.ERROR
                _errorMessage.value = "User session not found. Please log in again."
                return@launch
            }

            try {
                val now = Instant.now()
                val since = now.minus(7, ChronoUnit.DAYS)   // Widen to 7 days for easier testing
                val filter = TimeRangeFilter.between(since, now)

                var bpSys = ""; var bpDia = ""; var heartRate = ""
                var spo2 = ""; var respRate = ""; var bodyTemp = ""; var glucose = ""
                var count = 0

                selected.forEach { metric ->
                    try {
                        when (metric.id) {
                            "heart_rate" -> {
                                val records = client.readRecords(ReadRecordsRequest(HeartRateRecord::class, filter)).records
                                Log.d("HealthConnect", "HeartRate records = ${records.size}")
                                records.lastOrNull()?.samples?.lastOrNull()?.let {
                                    heartRate = it.beatsPerMinute.toString()
                                    updateMetricValue(metric.id, heartRate, "bpm"); count++
                                }
                            }
                            "blood_pressure" -> {
                                val records = client.readRecords(ReadRecordsRequest(BloodPressureRecord::class, filter)).records
                                Log.d("HealthConnect", "BloodPressure records = ${records.size}")
                                records.lastOrNull()?.let {
                                    bpSys = it.systolic.inMillimetersOfMercury.toInt().toString()
                                    bpDia = it.diastolic.inMillimetersOfMercury.toInt().toString()
                                    updateMetricValue(metric.id, "$bpSys/$bpDia", "mmHg"); count++
                                }
                            }
                            "spo2" -> {
                                val records = client.readRecords(ReadRecordsRequest(OxygenSaturationRecord::class, filter)).records
                                Log.d("HealthConnect", "SpO2 records = ${records.size}")
                                records.lastOrNull()?.let {
                                    spo2 = it.percentage.value.toInt().toString()
                                    updateMetricValue(metric.id, spo2, "%"); count++
                                }
                            }
                            "respiratory_rate" -> {
                                val records = client.readRecords(ReadRecordsRequest(RespiratoryRateRecord::class, filter)).records
                                Log.d("HealthConnect", "RespRate records = ${records.size}")
                                records.lastOrNull()?.let {
                                    respRate = it.rate.toInt().toString()
                                    updateMetricValue(metric.id, respRate, "br/min"); count++
                                }
                            }
                            "body_temperature" -> {
                                val records = client.readRecords(ReadRecordsRequest(BodyTemperatureRecord::class, filter)).records
                                Log.d("HealthConnect", "BodyTemp records = ${records.size}")
                                records.lastOrNull()?.let {
                                    bodyTemp = String.format("%.1f", it.temperature.inCelsius)
                                    updateMetricValue(metric.id, bodyTemp, "°C"); count++
                                }
                            }
                            "blood_glucose" -> {
                                val records = client.readRecords(ReadRecordsRequest(BloodGlucoseRecord::class, filter)).records
                                Log.d("HealthConnect", "BloodGlucose records = ${records.size}")
                                records.lastOrNull()?.let {
                                    glucose = it.level.inMilligramsPerDeciliter.toInt().toString()
                                    updateMetricValue(metric.id, glucose, "mg/dL"); count++
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("HealthConnect", "Skip ${metric.id}: ${e.message}")
                    }
                }

                Log.d("HealthConnect", "Synced $count vitals, posting to server…")

                if (count > 0) {
                    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                    addVitalUseCase(
                        mapOf(
                            "vmValueBPSys" to bpSys, "vmValueBPDias" to bpDia,
                            "vmValueSPO2" to spo2, "vmValueRespiratoryRate" to respRate,
                            "vmValueHeartRate" to heartRate, "vmValuePulse" to heartRate,
                            "vmValueRbs" to glucose, "vmValueTemperature" to bodyTemp,
                            "uhid" to patient.uhId, "userId" to patient.pid.toString(),
                            "vitalDate" to currentDate, "vitalTime" to currentTime,
                            "clientId" to patient.clientId.toString(),
                            "isFromPatient" to "true", "isFromMachine" to "0", "positionId" to "0"
                        )
                    )
                }

                _syncedCount.value = count
                _syncState.value = if (count > 0) SyncState.SUCCESS else {
                    _errorMessage.value = "No health data found in the last 7 days.\nTip: Open Health Connect → Browse → add a measurement, then sync again."
                    SyncState.ERROR
                }

            } catch (e: Exception) {
                Log.e("HealthConnect", "Sync failed: ${e.message}", e)
                _syncState.value = SyncState.ERROR
                _errorMessage.value = "${e.javaClass.simpleName}: ${e.message}"
            }
        }
    }

    private fun updateMetricValue(id: String, value: String, unit: String) {
        _metrics.value = _metrics.value.map {
            if (it.id == id) it.copy(value = value, unit = unit, isSynced = true) else it
        }
    }

    fun resetState() {
        _syncState.value = SyncState.IDLE
        _errorMessage.value = ""
        _metrics.value = _metrics.value.map { it.copy(isSynced = false, value = "--") }
    }

    companion object {
        fun defaultMetrics() = listOf(
            HealthMetric("heart_rate",       "Heart Rate",       Icons.Filled.Favorite,     Color(0xFFE53935)),
            HealthMetric("blood_pressure",   "Blood Pressure",   Icons.Filled.MonitorHeart, Color(0xFF1565C0)),
            HealthMetric("spo2",             "SpO₂",             Icons.Filled.Air,           Color(0xFF00897B)),
            HealthMetric("respiratory_rate", "Respiratory Rate", Icons.Filled.Waves,         Color(0xFF6A1B9A)),
            HealthMetric("body_temperature", "Body Temperature", Icons.Filled.Thermostat,    Color(0xFFE65100)),
            HealthMetric("blood_glucose",    "Blood Glucose",    Icons.Filled.WaterDrop,     Color(0xFF2E7D32))
        )

        val REQUIRED_PERMISSIONS = setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(BloodPressureRecord::class),
            HealthPermission.getReadPermission(OxygenSaturationRecord::class),
            HealthPermission.getReadPermission(RespiratoryRateRecord::class),
            HealthPermission.getReadPermission(BodyTemperatureRecord::class),
            HealthPermission.getReadPermission(BloodGlucoseRecord::class)
        )
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthConnectSyncScreen(
    viewModel: HealthConnectViewModel = viewModel()
) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    val syncState      by viewModel.syncState.collectAsState()
    val metrics        by viewModel.metrics.collectAsState()
    val syncedCount    by viewModel.syncedCount.collectAsState()
    val errorMessage   by viewModel.errorMessage.collectAsState()
    val availMsg       by viewModel.availabilityMessage.collectAsState()
    val deniedPerms    by viewModel.deniedPermissions.collectAsState()

    // ── Detect Health Connect SDK status ───────────────────────────────
    val sdkStatus = remember {
        try { HealthConnectClient.getSdkStatus(context) }
        catch (e: Exception) { Log.e("HealthConnect", "getSdkStatus error: ${e.message}"); -1 }
    }

    val healthConnectClient = remember {
        if (sdkStatus == HealthConnectClient.SDK_AVAILABLE)
            try { HealthConnectClient.getOrCreate(context) } catch (e: Exception) { null }
        else null
    }

    LaunchedEffect(sdkStatus) {
        Log.d("HealthConnect", "LaunchedEffect sdkStatus=$sdkStatus client=$healthConnectClient")
        viewModel.checkAvailability(sdkStatus)
    }

    // ── Permission launcher ────────────────────────────────────────────
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d("HealthConnect", "Permissions result — granted: $granted")
        Log.d("HealthConnect", "Required: ${HealthConnectViewModel.REQUIRED_PERMISSIONS}")
        if (granted.containsAll(HealthConnectViewModel.REQUIRED_PERMISSIONS)) {
            healthConnectClient?.let { viewModel.syncSelectedMetrics(it) }
        } else {
            // Show the denied card with re-grant option instead of a simple Toast
            viewModel.setPermissionsDenied(HealthConnectViewModel.REQUIRED_PERMISSIONS, granted)
        }
    }

    CommonAppBar(title = "Health Connect Sync") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Gradient header ──────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF1565C0), Color(0xFF42A5F5))))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Watch, null, tint = Color.White, modifier = Modifier.size(34.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text("Wear OS & Health Apps", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Sync vitals from your wearable\nand health apps automatically",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── SDK status chip ──────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        when (syncState) {
                            SyncState.NOT_INSTALLED, SyncState.UPDATE_REQUIRED -> Color(0xFFFFF3E0)
                            else -> Color(0xFFE8F5E9)
                        }
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (syncState) {
                        SyncState.NOT_INSTALLED, SyncState.UPDATE_REQUIRED -> Icons.Filled.Warning
                        else -> Icons.Filled.CheckCircle
                    },
                    contentDescription = null,
                    tint = when (syncState) {
                        SyncState.NOT_INSTALLED, SyncState.UPDATE_REQUIRED -> Color(0xFFE65100)
                        else -> Color(0xFF4CAF50)
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(availMsg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = if (syncState == SyncState.NOT_INSTALLED || syncState == SyncState.UPDATE_REQUIRED)
                        Color(0xFFE65100) else Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── NOT INSTALLED ────────────────────────────────────────
            if (syncState == SyncState.NOT_INSTALLED) {
                NotInstalledCard {
                    val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                HowToTestCard()
            }

            // ── UPDATE REQUIRED ──────────────────────────────────────
            else if (syncState == SyncState.UPDATE_REQUIRED) {
                UpdateRequiredCard {
                    val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    } catch (e: Exception) {
                        val webUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                        context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
                    }
                }
            }

            // ── AVAILABLE — show metrics ─────────────────────────────
            else {
                // Select all / none row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Metrics to Sync", style = AppTextStyles.style14BCB())
                    Row {
                        TextButton(onClick = { viewModel.selectAll() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("All", fontSize = 12.sp, color = Color(0xFF1565C0))
                        }
                        TextButton(onClick = { viewModel.deselectAll() }, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("None", fontSize = 12.sp, color = Color(0xFF9E9E9E))
                        }
                    }
                }

                // Metric cards
                metrics.forEach { metric ->
                    HealthMetricCard(
                        metric = metric,
                        isBusy = syncState == SyncState.SYNCING,
                        onToggle = { viewModel.toggleMetric(metric.id) }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Info chip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(10.dp)).background(Color(0xFFE3F2FD)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Info, null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Reads the last 7 days of data from any app connected to Health Connect (Wear OS, Samsung Health, Google Fit, etc.)",
                        fontSize = 11.sp, color = Color(0xFF1565C0), lineHeight = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Action area
                when (syncState) {
                    SyncState.SUCCESS  -> SuccessBanner(syncedCount) { viewModel.resetState() }
                    SyncState.ERROR    -> ErrorBanner(errorMessage) { viewModel.resetState() }
                    SyncState.SYNCING  -> SyncingIndicator()
                    SyncState.PERMISSIONS_DENIED -> PermissionsDeniedCard(
                        deniedPermissions = deniedPerms,
                        onRequestAgain = {
                            permissionLauncher.launch(HealthConnectViewModel.REQUIRED_PERMISSIONS)
                        },
                        onOpenSettings = {
                            // Open the Health Connect app's app-info page in Android Settings
                            // (Health Connect permissions are managed there on Android 14+)
                            try {
                                val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS").apply {
                                    data = Uri.fromParts("package", "com.google.android.apps.healthdata", null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                // Fallback: open Health Connect directly
                                context.packageManager
                                    .getLaunchIntentForPackage("com.google.android.apps.healthdata")
                                    ?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
                                    ?.let { context.startActivity(it) }
                            }
                        }
                    )
                    else -> SyncButton(
                        enabled = metrics.any { it.isSelected },
                        onClick = {
                            val client = healthConnectClient
                            if (client == null) {
                                Toast.makeText(context, "Health Connect is not available on this device.", Toast.LENGTH_LONG).show()
                                return@SyncButton
                            }
                            viewModel.viewModelScope.launch {
                                try {
                                    val granted = client.permissionController.getGrantedPermissions()
                                    Log.d("HealthConnect", "Already granted: $granted")
                                    if (granted.containsAll(HealthConnectViewModel.REQUIRED_PERMISSIONS)) {
                                        viewModel.syncSelectedMetrics(client)
                                    } else {
                                        permissionLauncher.launch(HealthConnectViewModel.REQUIRED_PERMISSIONS)
                                    }
                                } catch (e: Exception) {
                                    Log.e("HealthConnect", "Permission check error: ${e.message}", e)
                                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                HowToTestCard()
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

@Composable
private fun HealthMetricCard(metric: HealthMetric, isBusy: Boolean, onToggle: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val scale by animateFloatAsState(
        targetValue = if (metric.isSelected) 1f else 0.98f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "cardScale"
    )
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
            .scale(scale).clickable(enabled = !isBusy) { onToggle() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (metric.isSelected) metric.iconTint.copy(alpha = 0.07f) else colors.dashboardContainerColor
        ),
        border = if (metric.isSelected) BorderStroke(1.5.dp, metric.iconTint.copy(alpha = 0.35f)) else null
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(metric.iconTint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(metric.icon, null, tint = metric.iconTint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(metric.name, style = AppTextStyles.style14BCB())
                AnimatedVisibility(visible = metric.isSynced && metric.value != "--") {
                    Text("${metric.value} ${metric.unit}", fontSize = 12.sp, color = metric.iconTint, fontWeight = FontWeight.SemiBold)
                }
                if (!metric.isSynced) Text("Last 7 days", style = AppTextStyles.style12GCN())
            }
            if (metric.isSynced) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(22.dp))
            } else {
                Checkbox(
                    checked = metric.isSelected, onCheckedChange = { if (!isBusy) onToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = metric.iconTint, uncheckedColor = Color(0xFFBBBBBB))
                )
            }
        }
    }
}

@Composable
private fun SyncButton(enabled: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick, enabled = enabled,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0), disabledContainerColor = Color(0xFFBBDEFB))
    ) {
        Icon(Icons.Filled.Sync, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sync from Health Connect", color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SyncingIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE3F2FD)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF1565C0), modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Syncing health data…", fontWeight = FontWeight.SemiBold, color = Color(0xFF1565C0))
        Text("Reading from Health Connect", fontSize = 12.sp, color = Color(0xFF546788))
    }
}

@Composable
private fun SuccessBanner(count: Int, onSyncAgain: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8F5E9)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Successfully Synced!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32))
        Text("$count vital(s) pulled from your wearable and saved.", fontSize = 13.sp, color = Color(0xFF546788), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = onSyncAgain, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
        ) { Text("Sync Again") }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFFFEBEE)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text("Sync Failed", fontWeight = FontWeight.Bold, color = Color(0xFFC62828))
        Text(message, fontSize = 12.sp, color = Color(0xFF546788), textAlign = TextAlign.Center, lineHeight = 17.sp)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
        ) { Text("Retry") }
    }
}

// ── Permissions Denied Card ────────────────────────────────────────────────────

@Composable
private fun PermissionsDeniedCard(
    deniedPermissions: Set<String>,
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // Human-readable names for the permission strings
    fun permissionLabel(perm: String): String = when {
        perm.contains("HeartRate",       true) -> "Heart Rate"
        perm.contains("BloodPressure",   true) -> "Blood Pressure"
        perm.contains("OxygenSaturation",true) -> "SpO₂ / Oxygen Saturation"
        perm.contains("RespiratoryRate", true) -> "Respiratory Rate"
        perm.contains("BodyTemperature", true) -> "Body Temperature"
        perm.contains("BloodGlucose",    true) -> "Blood Glucose"
        else -> perm.substringAfterLast(".")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.LockOpen,
            contentDescription = null,
            tint = Color(0xFFE65100),
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "Health Permissions Denied",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = Color(0xFFE65100)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "The following permissions were not granted. Health Connect permissions can always be re-granted — tap \"Allow Again\" below.",
            fontSize = 12.sp,
            color = Color(0xFF546788),
            textAlign = TextAlign.Center,
            lineHeight = 17.sp
        )

        if (deniedPermissions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.6f))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Missing permissions:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF333333)
                )
                deniedPermissions.forEach { perm ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Cancel,
                            contentDescription = null,
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            permissionLabel(perm),
                            fontSize = 12.sp,
                            color = Color(0xFF555555)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Primary — re-request permissions (works every time with Health Connect)
        Button(
            onClick = onRequestAgain,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
        ) {
            Icon(Icons.Filled.Key, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Allow Again", color = Color.White, fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Secondary — open Health Connect settings manually
        OutlinedButton(
            onClick = onOpenSettings,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFE65100)),
            border = BorderStroke(1.dp, Color(0xFFE65100))
        ) {
            Icon(Icons.Filled.Settings, null, tint = Color(0xFFE65100), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open Health Connect Settings", color = Color(0xFFE65100))
        }

        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "💡 In Health Connect settings: go to \"App permissions\" → \"Vitalio\" → enable all toggles",
            fontSize = 11.sp,
            color = Color(0xFF777777),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun NotInstalledCard(onInstall: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF8E1)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.HealthAndSafety, null, tint = Color(0xFFE65100), modifier = Modifier.size(52.dp))
        Spacer(modifier = Modifier.height(12.dp))
        Text("Health Connect Not Installed", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE65100))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Google Health Connect is required to sync vitals from Wear OS, Samsung Health, Google Fit, and other wearable apps.\n\nIt is built into Android 14+. On Android 9–13 it needs to be installed separately.",
            fontSize = 13.sp, color = Color(0xFF546788), textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(onClick = onInstall, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) {
            Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Install Health Connect from Play Store", color = Color.White)
        }
    }
}

@Composable
private fun UpdateRequiredCard(onUpdate: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp)).background(Color(0xFFFFF3E0)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.SystemUpdate, null, tint = Color(0xFFE65100), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("Health Connect Needs Update", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFFE65100))
        Text("Please update Health Connect in Play Store to continue.", fontSize = 13.sp, color = Color(0xFF546788), textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onUpdate, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) { Text("Update Health Connect", color = Color.White) }
    }
}

// ── How-to-test guide card ────────────────────────────────────────────────────

@Composable
private fun HowToTestCard() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Science, null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("How to Test Without a Watch", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1565C0))
        }
        Spacer(modifier = Modifier.height(10.dp))
        TestStep("1", "Install \"Health Connect\" from Play Store (or update on Android 14+)")
        TestStep("2", "Open Health Connect app → Browse → pick any metric (e.g. Heart rate)")
        TestStep("3", "Tap \"+\" → Add entry → enter a value → Save")
        TestStep("4", "Come back here → select that metric → tap Sync")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "💡 Tip: Apps like Google Fit, Samsung Health, Polar Beat, or any Wear OS watch automatically write data to Health Connect in the background.",
            fontSize = 11.sp, color = Color(0xFF546788), lineHeight = 16.sp
        )
    }
}

@Composable
private fun TestStep(number: String, text: String) {
    Row(modifier = Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier.size(20.dp).clip(CircleShape).background(Color(0xFF1565C0)),
            contentAlignment = Alignment.Center
        ) { Text(number, fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = Color(0xFF333333), lineHeight = 17.sp, modifier = Modifier.weight(1f))
    }
}
