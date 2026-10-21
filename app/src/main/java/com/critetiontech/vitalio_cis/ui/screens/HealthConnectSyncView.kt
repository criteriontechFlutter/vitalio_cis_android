package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.healthconnect.VitalMetric
import com.critetiontech.ctvitalio.healthconnect.VitalReading
import com.critetiontech.ctvitalio.healthconnect.WearableData
import com.critetiontech.ctvitalio.healthconnect.WearableReader
import com.critetiontech.ctvitalio.healthconnect.WearableSnapshot
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import com.critetiontech.ctvitalio.healthconnect.Workout
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR, NOT_INSTALLED, UPDATE_REQUIRED, PERMISSIONS_DENIED }

// ─── ViewModel ────────────────────────────────────────────────────────────────

class HealthConnectViewModel : ViewModel() {

    private val context = MyApplication.appContext

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _availabilityMessage = MutableStateFlow("Checking Health Connect…")
    val availabilityMessage: StateFlow<String> = _availabilityMessage

    private val _snapshot = MutableStateFlow<WearableSnapshot?>(null)
    val snapshot: StateFlow<WearableSnapshot?> = _snapshot

    /** Vitals and workouts saved to the server in the last sync. */
    private val _savedCount = MutableStateFlow(0 to 0)
    val savedCount: StateFlow<Pair<Int, Int>> = _savedCount

    private val _missingPermissions = MutableStateFlow<Set<String>>(emptySet())
    val missingPermissions: StateFlow<Set<String>> = _missingPermissions

    private val _autoSync = MutableStateFlow(WearableSync.isAutoSyncEnabled(context))
    val autoSync: StateFlow<Boolean> = _autoSync

    private val _backgroundSync = MutableStateFlow(false)
    val backgroundSync: StateFlow<Boolean> = _backgroundSync

    fun checkAvailability(sdkStatus: Int) {
        Log.d("HealthConnect", "SDK status = $sdkStatus")
        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> _availabilityMessage.value = "Health Connect is available ✓"
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

    /** Re-reads granted permissions and shows the current wearable data without saving it. */
    fun refresh(client: HealthConnectClient) {
        viewModelScope.launch {
            val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            _backgroundSync.value = WearableSync.canReadInBackground(client)
            if (granted.none { it in WearableData.READ_PERMISSIONS }) return@launch
            _snapshot.value = WearableReader(client).read(granted)
        }
    }

    fun onPermissionsResult(client: HealthConnectClient, granted: Set<String>) {
        if (granted.none { it in WearableData.READ_PERMISSIONS }) {
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            _syncState.value = SyncState.PERMISSIONS_DENIED
        } else {
            syncNow(client)
        }
    }

    fun syncNow(client: HealthConnectClient) {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            val result = WearableSync.sync(context, client)
            _snapshot.value = result.snapshot
            _savedCount.value = result.vitalsSaved to result.workoutsSaved
            val snapshot = result.snapshot
            val nothingRead = snapshot.vitals.isEmpty() && snapshot.workouts.isEmpty() &&
                    snapshot.daily == com.critetiontech.ctvitalio.healthconnect.DailySummary()
            _syncState.value = when {
                result.error != null -> { _errorMessage.value = result.error; SyncState.ERROR }
                nothingRead -> {
                    _errorMessage.value = snapshot.readErrors.firstOrNull()?.let { "Couldn't read from Health Connect: $it" }
                        ?: "No data found from your watch or health apps yet.\nMake sure your watch app (e.g. Samsung Health) is set to sync with Health Connect."
                    SyncState.ERROR
                }
                else -> SyncState.SUCCESS
            }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        WearableSync.setAutoSync(context, enabled)
        _autoSync.value = enabled
    }

    fun resetState() {
        _syncState.value = SyncState.IDLE
        _errorMessage.value = ""
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@Composable
fun HealthConnectSyncScreen(
    viewModel: HealthConnectViewModel = viewModel()
) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    val syncState    by viewModel.syncState.collectAsState()
    val snapshot     by viewModel.snapshot.collectAsState()
    val savedCount   by viewModel.savedCount.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val availMsg     by viewModel.availabilityMessage.collectAsState()
    val missing      by viewModel.missingPermissions.collectAsState()
    val autoSync     by viewModel.autoSync.collectAsState()
    val background   by viewModel.backgroundSync.collectAsState()

    // ── Detect Health Connect SDK status ───────────────────────────────
    val sdkStatus = remember {
        try { HealthConnectClient.getSdkStatus(context) }
        catch (e: Exception) { Log.e("HealthConnect", "getSdkStatus error: ${e.message}"); -1 }
    }
    val healthConnectClient = remember { WearableSync.clientOrNull(context) }

    LaunchedEffect(sdkStatus) {
        viewModel.checkAvailability(sdkStatus)
        healthConnectClient?.let { viewModel.refresh(it) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        Log.d("HealthConnect", "Permissions result — granted: $granted")
        healthConnectClient?.let { viewModel.onPermissionsResult(it, granted) }
    }
    val backgroundLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { healthConnectClient?.let { viewModel.refresh(it) } }

    val requestPermissions = { permissionLauncher.launch(WearableData.READ_PERMISSIONS) }
    val openHealthConnectSettings = {
        try {
            context.startActivity(Intent("androidx.health.ACTION_HEALTH_CONNECT_SETTINGS").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            context.startActivity(
                Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                    .setData(Uri.fromParts("package", "com.google.android.apps.healthdata", null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
    val openPlayStore = {
        val uri = Uri.parse("market://details?id=com.google.android.apps.healthdata")
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        } catch (e: Exception) {
            val webUri = Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
            context.startActivity(Intent(Intent.ACTION_VIEW, webUri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        }
    }

    CommonAppBar(title = "Wearable Sync") {
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
                        Text("Any Watch or Health App", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            "Samsung Galaxy Watch, Fitbit, Pixel, Garmin,\nMi, Noise, boAt & more — via Health Connect",
                            fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), lineHeight = 18.sp
                        )
                    }
                }
            }

            // ── SDK status chip ──────────────────────────────────────
            val unavailable = syncState == SyncState.NOT_INSTALLED || syncState == SyncState.UPDATE_REQUIRED
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (unavailable) Color(0xFFFFF3E0) else Color(0xFFE8F5E9))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (unavailable) Icons.Filled.Warning else Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = if (unavailable) Color(0xFFE65100) else Color(0xFF4CAF50),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(availMsg, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                    color = if (unavailable) Color(0xFFE65100) else Color(0xFF2E7D32))
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                syncState == SyncState.NOT_INSTALLED -> {
                    NotInstalledCard(openPlayStore)
                    Spacer(modifier = Modifier.height(8.dp))
                    HowToConnectCard()
                }
                syncState == SyncState.UPDATE_REQUIRED -> UpdateRequiredCard(openPlayStore)
                else -> {
                    snapshot?.sources?.takeIf { it.isNotEmpty() }?.let { SourcesRow(it) }

                    AutoSyncCard(
                        enabled = autoSync,
                        background = background,
                        onToggle = { on ->
                            viewModel.setAutoSync(on)
                            if (on && !background) backgroundLauncher.launch(setOf(WearableData.BACKGROUND_PERMISSION))
                        }
                    )

                    snapshot?.let { data ->
                        if (data.vitals.isNotEmpty()) {
                            SectionTitle("Vitals", "Saved to your health record")
                            data.vitals.forEach { VitalCard(it, WearableSync.isSaved(context, it)) }
                        }
                        TodayCard(data)
                        if (data.workouts.isNotEmpty()) {
                            SectionTitle("Today's Workouts", null)
                            data.workouts.forEach { WorkoutCard(it, WearableSync.isSaved(context, it)) }
                        }
                    }

                    if (missing.isNotEmpty() && missing.size < WearableData.READ_PERMISSIONS.size && syncState != SyncState.PERMISSIONS_DENIED) {
                        MissingPermissionsNote(missing.size, onAllow = requestPermissions)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (syncState) {
                        SyncState.SUCCESS -> SuccessBanner(savedCount.first, savedCount.second) { viewModel.resetState() }
                        SyncState.ERROR -> ErrorBanner(errorMessage) { viewModel.resetState() }
                        SyncState.SYNCING -> SyncingIndicator()
                        SyncState.PERMISSIONS_DENIED -> PermissionsDeniedCard(
                            deniedPermissions = missing,
                            onRequestAgain = requestPermissions,
                            onOpenSettings = openHealthConnectSettings
                        )
                        else -> SyncButton(
                            label = if (snapshot == null) "Connect & Sync" else "Sync Now",
                            onClick = {
                                val client = healthConnectClient
                                if (client == null) {
                                    Toast.makeText(context, "Health Connect is not available on this device.", Toast.LENGTH_LONG).show()
                                    return@SyncButton
                                }
                                if (missing.size == WearableData.READ_PERMISSIONS.size) requestPermissions()
                                else viewModel.syncNow(client)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HowToConnectCard()
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────────────────────

private val timeFormat = DateTimeFormatter.ofPattern("d MMM, h:mm a", Locale.getDefault())

private fun Instant.display(): String {
    val local = atZone(ZoneId.systemDefault())
    return if (local.toLocalDate() == LocalDate.now()) "Today, " + local.format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    else local.format(timeFormat)
}

private fun VitalMetric.icon(): Pair<ImageVector, Color> = when (this) {
    VitalMetric.HEART_RATE       -> Icons.Filled.Favorite to Color(0xFFE53935)
    VitalMetric.BLOOD_PRESSURE   -> Icons.Filled.MonitorHeart to Color(0xFF1565C0)
    VitalMetric.SPO2             -> Icons.Filled.Air to Color(0xFF00897B)
    VitalMetric.RESPIRATORY_RATE -> Icons.Filled.Waves to Color(0xFF6A1B9A)
    VitalMetric.BODY_TEMPERATURE -> Icons.Filled.Thermostat to Color(0xFFE65100)
    VitalMetric.BLOOD_GLUCOSE    -> Icons.Filled.WaterDrop to Color(0xFF2E7D32)
    VitalMetric.WEIGHT           -> Icons.Filled.MonitorWeight to Color(0xFF5D4037)
    VitalMetric.HEIGHT           -> Icons.Filled.Height to Color(0xFF455A64)
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 4.dp)) {
        Text(title, style = AppTextStyles.style14BCB())
        subtitle?.let { Text(it, style = AppTextStyles.style12GCN()) }
    }
}

@Composable
private fun SourcesRow(sources: Set<String>) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Data from:", fontSize = 12.sp, color = Color(0xFF546788))
        sources.sorted().forEach { source ->
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE3F2FD))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Watch, null, tint = Color(0xFF1565C0), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(source, fontSize = 11.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AutoSyncCard(enabled: Boolean, background: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalMyColorScheme.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Autorenew, null, tint = Color(0xFF1565C0), modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Sync", style = AppTextStyles.style14BCB())
                Text(
                    when {
                        !enabled -> "Sync only when you tap Sync Now"
                        background -> "Every hour in the background and whenever you open the app"
                        else -> "Whenever you open the app (background sync isn't supported on this phone)"
                    },
                    style = AppTextStyles.style12GCN()
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}

@Composable
private fun VitalCard(reading: VitalReading, saved: Boolean) {
    val colors = LocalMyColorScheme.current
    val (icon, tint) = reading.metric.icon()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reading.metric.label, style = AppTextStyles.style14BCB())
                Text("${reading.time.display()} · ${reading.source}", style = AppTextStyles.style12GCN())
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${reading.value} ${reading.metric.unit}", fontSize = 15.sp, color = tint, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !reading.metric.savedToServer -> "App only"
                        saved -> "Saved"
                        else -> "Not synced"
                    },
                    fontSize = 11.sp,
                    color = if (saved) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
private fun TodayCard(data: WearableSnapshot) {
    val d = data.daily
    val tiles = listOfNotNull(
        d.steps?.let { Triple(Icons.Filled.DirectionsWalk, "Steps", "%,d".format(it)) },
        d.distanceKm?.let { Triple(Icons.Filled.Straighten, "Distance", String.format(Locale.US, "%.2f km", it)) },
        d.activeKcal?.let { Triple(Icons.Filled.LocalFireDepartment, "Active Calories", "${it.toInt()} kcal") },
        d.totalKcal?.let { Triple(Icons.Filled.Whatshot, "Total Calories", "${it.toInt()} kcal") },
        d.sleep?.takeIf { !it.isZero }?.let { Triple(Icons.Filled.Bedtime, "Sleep", "${it.toHours()}h ${it.toMinutes() % 60}m") },
        d.restingHeartRate?.let { Triple(Icons.Filled.FavoriteBorder, "Resting HR", "$it bpm") },
        d.hrvMs?.let { Triple(Icons.Filled.Timeline, "HRV", "${it.toInt()} ms") }
    )
    if (tiles.isEmpty()) return
    SectionTitle("Today", "Shown here only — not saved to your record yet")
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (icon, label, value) -> StatTile(icon, label, value, Modifier.weight(1f)) }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).background(colors.dashboardContainerColor).padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Color(0xFF1565C0), modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(label, style = AppTextStyles.style12GCN())
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1565C0))
    }
}

@Composable
private fun WorkoutCard(workout: Workout, saved: Boolean) {
    val colors = LocalMyColorScheme.current
    val minutes = Duration.between(workout.start, workout.end).toMinutes()
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0xFFFF7043).copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.FitnessCenter, null, tint = Color(0xFFFF7043), modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, style = AppTextStyles.style14BCB())
                Text("${workout.start.display()} · $minutes min · ${workout.source}", style = AppTextStyles.style12GCN())
            }
            Text(
                when {
                    saved -> "Saved"
                    workout.activityName == null -> "Not supported"
                    else -> "Not synced"
                },
                fontSize = 11.sp,
                color = if (saved) Color(0xFF4CAF50) else Color(0xFF9E9E9E)
            )
        }
    }
}

@Composable
private fun MissingPermissionsNote(count: Int, onAllow: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF8E1)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Info, null, tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("$count data type(s) not allowed yet", fontSize = 12.sp, color = Color(0xFFE65100), modifier = Modifier.weight(1f))
        HapticTextButton(onClick = onAllow, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Allow", fontSize = 12.sp, color = Color(0xFF1565C0), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun SyncButton(label: String, onClick: () -> Unit) {
    HapticButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(54.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
    ) {
        Icon(Icons.Filled.Sync, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold)
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
private fun SuccessBanner(vitals: Int, workouts: Int, onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFE8F5E9)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(44.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            if (vitals + workouts == 0) "Already Up to Date" else "Successfully Synced!",
            fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF2E7D32)
        )
        Text(
            if (vitals + workouts == 0) "No new readings since the last sync."
            else listOfNotNull(
                "$vitals vital(s)".takeIf { vitals > 0 },
                "$workouts workout(s)".takeIf { workouts > 0 }
            ).joinToString(" and ") + " saved to your health record.",
            fontSize = 13.sp, color = Color(0xFF546788), textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        HapticOutlinedButton(onClick = onDone, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32))
        ) { Text("Done") }
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
        HapticOutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF3E0))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.LockOpen, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(10.dp))
        Text("Health Permissions Denied", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFE65100))
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Allow at least one data type so Vitalio can read it from your watch. Health Connect permissions can always be re-granted — tap \"Allow Again\" below.",
            fontSize = 12.sp, color = Color(0xFF546788), textAlign = TextAlign.Center, lineHeight = 17.sp
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
                Text("Missing permissions:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF333333))
                deniedPermissions.sorted().forEach { perm ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Cancel, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(WearableData.permissionLabel(perm), fontSize = 12.sp, color = Color(0xFF555555))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        HapticButton(
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

        HapticOutlinedButton(
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
            fontSize = 11.sp, color = Color(0xFF777777), textAlign = TextAlign.Center, lineHeight = 16.sp
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
            "Google Health Connect is required to sync vitals from Galaxy Watch, Fitbit, Wear OS and other wearable apps.\n\nIt is built into Android 14+. On Android 9–13 it needs to be installed separately.",
            fontSize = 13.sp, color = Color(0xFF546788), textAlign = TextAlign.Center, lineHeight = 19.sp
        )
        Spacer(modifier = Modifier.height(20.dp))
        HapticButton(onClick = onInstall, shape = RoundedCornerShape(12.dp),
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
        HapticButton(onClick = onUpdate, shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
        ) { Text("Update Health Connect", color = Color.White) }
    }
}

// ── How-to-connect guide card ─────────────────────────────────────────────────

@Composable
private fun HowToConnectCard() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp)).background(Color(0xFFF3F4F6)).padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Watch, null, tint = Color(0xFF1565C0), modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("How to Connect Your Watch", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1565C0))
        }
        Spacer(modifier = Modifier.height(10.dp))
        TestStep("1", "Pair your watch with its app (Samsung Health, Fitbit, Garmin Connect, Mi Fitness, NoiseFit…)")
        TestStep("2", "In that app, turn on Health Connect sync — e.g. Samsung Health → Settings → Health Connect")
        TestStep("3", "Come back here → tap Sync Now and allow the data types")
        TestStep("4", "Turn on Auto Sync to keep your record updated automatically")
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "💡 Your watch app must write to Health Connect. Most popular watch apps support it; check the app's settings if no data shows up.",
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
