package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.graphicsLayer
import com.critetiontech.ctvitalio.ui.components.*
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.AppMotion
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.healthconnect.VitalMetric
import com.critetiontech.ctvitalio.healthconnect.VitalReading
import com.critetiontech.ctvitalio.healthconnect.WearableData
import com.critetiontech.ctvitalio.healthconnect.WearableSnapshot
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import com.critetiontech.ctvitalio.healthconnect.Workout
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.critetiontech.ctvitalio.viewmodel.HealthConnectViewModel
import com.critetiontech.ctvitalio.viewmodel.SyncState

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
    val sync: () -> Unit = sync@{
        val client = healthConnectClient
        if (client == null) {
            Toast.makeText(context, "Health Connect is not available on this device.", Toast.LENGTH_LONG).show()
            return@sync
        }
        if (missing.size == WearableData.READ_PERMISSIONS.size) requestPermissions()
        else viewModel.syncNow(client)
    }

    val unavailable = syncState == SyncState.NOT_INSTALLED || syncState == SyncState.UPDATE_REQUIRED
    val syncing = syncState == SyncState.SYNCING

    CommonAppBar(title = "Wearable Sync") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
        ) {
            AnimatedEnter(index = 0) {
                DeviceHeader(
                    icon = Icons.Filled.Watch,
                    eyebrow = "Health Connect",
                    title = "Watches & Health Apps",
                    status = when {
                        syncing -> "Syncing…"
                        unavailable -> availMsg
                        snapshot?.sources?.isNotEmpty() == true -> "Connected · ${snapshot!!.sources.size} source(s)"
                        else -> "Galaxy Watch, Fitbit, Pixel, Garmin & more"
                    },
                    live = snapshot != null && !unavailable,
                    busy = syncing,
                    action = if (unavailable) null else {
                        {
                            HeaderActionPill(
                                if (syncing) "Syncing" else if (snapshot == null) "Connect" else "Sync",
                                Icons.Filled.Sync, enabled = !syncing, spinning = syncing, onClick = sync
                            )
                        }
                    }
                )
            }

            when {
                syncState == SyncState.NOT_INSTALLED -> {
                    AnimatedEnter(index = 1) {
                        StatusCard(
                            false, "Health Connect not installed",
                            "Google Health Connect is needed to sync vitals from Galaxy Watch, Fitbit, Wear OS and other wearable apps. " +
                                "It's built into Android 14+; on Android 9–13 install it from the Play Store.",
                            icon = Icons.Filled.HealthAndSafety, tint = DeviceAmber,
                            action = "Install Health Connect", actionIcon = Icons.Filled.Download, onAction = openPlayStore
                        )
                    }
                    AnimatedEnter(index = 2) { HowToConnectCard() }
                }
                syncState == SyncState.UPDATE_REQUIRED -> AnimatedEnter(index = 1) {
                    StatusCard(
                        false, "Health Connect needs an update", "Please update Health Connect in the Play Store to continue.",
                        icon = Icons.Filled.SystemUpdate, tint = DeviceAmber,
                        action = "Update Health Connect", actionIcon = Icons.Filled.Download, onAction = openPlayStore
                    )
                }
                else -> {
                    // Sync status swaps with a fade + slide
                    AnimatedContent(
                        targetState = syncState,
                        transitionSpec = {
                            (fadeIn(tween(AppMotion.MEDIUM)) + slideInVertically(tween(AppMotion.MEDIUM)) { it / 6 }) togetherWith
                                fadeOut(tween(AppMotion.SHORT)) using SizeTransform(clip = false)
                        },
                        label = "hcState"
                    ) { state ->
                        when (state) {
                            SyncState.SYNCING -> RadarBusyCard("Syncing health data", "Reading from Health Connect.", icon = Icons.Filled.Watch)
                            SyncState.SUCCESS -> StatusCard(
                                true,
                                if (savedCount.first + savedCount.second == 0) "Already up to date" else "Synced",
                                if (savedCount.first + savedCount.second == 0) "No new readings since the last sync."
                                else listOfNotNull(
                                    "${savedCount.first} vital(s)".takeIf { savedCount.first > 0 },
                                    "${savedCount.second} workout(s)".takeIf { savedCount.second > 0 }
                                ).joinToString(" and ") + " saved to your health record."
                            )
                            SyncState.ERROR -> StatusCard(
                                false, "Sync failed", errorMessage,
                                action = "Try again", actionIcon = Icons.Filled.Refresh, onAction = sync
                            )
                            SyncState.PERMISSIONS_DENIED -> PermissionsDeniedCard(missing, requestPermissions, openHealthConnectSettings)
                            else -> Spacer(Modifier.height(0.dp))
                        }
                    }

                    snapshot?.sources?.takeIf { it.isNotEmpty() }?.let { AnimatedEnter(index = 1) { SourcesRow(it) } }

                    AnimatedEnter(index = 2) {
                        AutoSyncCard(
                            enabled = autoSync,
                            background = background,
                            onToggle = { on ->
                                viewModel.setAutoSync(on)
                                if (on && !background) backgroundLauncher.launch(setOf(WearableData.BACKGROUND_PERMISSION))
                            }
                        )
                    }

                    if (missing.isNotEmpty() && missing.size < WearableData.READ_PERMISSIONS.size && syncState != SyncState.PERMISSIONS_DENIED) {
                        MissingPermissionsNote(missing.size, onAllow = requestPermissions)
                    }

                    snapshot?.let { data ->
                        if (data.vitals.isNotEmpty()) {
                            SectionTitle("Vitals", "Saved to your health record")
                            data.vitals.forEachIndexed { i, v ->
                                AnimatedEnter(index = 3 + i) { VitalCard(v, WearableSync.isSaved(context, v)) }
                            }
                        }
                        TodayCard(data)
                        if (data.workouts.isNotEmpty()) {
                            SectionTitle("Today's workouts", null)
                            data.workouts.forEachIndexed { i, w ->
                                AnimatedEnter(index = 3 + i) { WorkoutCard(w, WearableSync.isSaved(context, w)) }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
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
    VitalMetric.BLOOD_PRESSURE   -> Icons.Filled.MonitorHeart to Color(0xFF1E88E5)
    VitalMetric.SPO2             -> Icons.Filled.Air to Color(0xFF00A896)
    VitalMetric.RESPIRATORY_RATE -> Icons.Filled.Waves to Color(0xFF8E59D9)
    VitalMetric.BODY_TEMPERATURE -> Icons.Filled.Thermostat to Color(0xFFF57C00)
    VitalMetric.BLOOD_GLUCOSE    -> Icons.Filled.WaterDrop to Color(0xFF43A047)
    VitalMetric.WEIGHT           -> Icons.Filled.MonitorWeight to Color(0xFF8D6E63)
    VitalMetric.HEIGHT           -> Icons.Filled.Height to Color(0xFF78909C)
}

@Composable
private fun SectionTitle(title: String, subtitle: String?) {
    val colors = LocalMyColorScheme.current
    Column(modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp)) {
        Caption(title)
        subtitle?.let { Text(it, fontSize = 12.sp, color = colors.textGreyColor) }
    }
}

@Composable
private fun SourcesRow(sources: Set<String>) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Caption("Data from")
        sources.sorted().forEach { source ->
            Row(
                modifier = Modifier.clip(RoundedCornerShape(50)).background(tintFill(p)).padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Watch, null, tint = p, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(source, fontSize = 11.sp, color = p, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun AutoSyncCard(enabled: Boolean, background: Boolean, onToggle: (Boolean) -> Unit) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    val spin = rememberInfiniteTransition(label = "autoSpin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "autoAngle")
    FlatCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(tintFill(p)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Autorenew, null, tint = p, modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = if (enabled) angle else 0f })
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Auto Sync", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textDarkColor)
                Text(
                    when {
                        !enabled -> "Sync only when you tap Sync"
                        background -> "Every hour in the background and whenever you open the app"
                        else -> "Whenever you open the app (background sync isn't supported on this phone)"
                    },
                    fontSize = 12.sp, color = colors.textGreyColor, lineHeight = 16.sp
                )
            }
            Switch(
                checked = enabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(checkedTrackColor = p, checkedThumbColor = Color.White)
            )
        }
    }
}

@Composable
private fun VitalCard(reading: VitalReading, saved: Boolean) {
    val colors = LocalMyColorScheme.current
    val (icon, tint) = reading.metric.icon()
    FlatCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tintFill(tint)),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(reading.metric.label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textDarkColor)
                Text("${reading.time.display()} · ${reading.source}", fontSize = 11.sp, color = colors.textGreyColor, maxLines = 1)
            }
            Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("${reading.value}", fontSize = 18.sp, color = colors.textDarkColor, fontWeight = FontWeight.Bold)
                    Text(" ${reading.metric.unit}", fontSize = 11.sp, color = colors.textGreyColor, modifier = Modifier.padding(bottom = 2.dp))
                }
                SavedChip(
                    when {
                        !reading.metric.savedToServer -> "App only"
                        saved -> "Saved"
                        else -> "Not synced"
                    },
                    saved
                )
            }
        }
    }
}

@Composable
private fun SavedChip(label: String, saved: Boolean) {
    val colors = LocalMyColorScheme.current
    val tint = if (saved) DeviceGreen else colors.textGreyColor
    Row(
        Modifier.padding(top = 3.dp).clip(RoundedCornerShape(50)).background(tintFill(tint)).padding(horizontal = 7.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (saved) {
            Icon(Icons.Filled.Check, null, tint = tint, modifier = Modifier.size(10.dp))
            Spacer(Modifier.width(3.dp))
        }
        Text(label, fontSize = 10.sp, color = tint, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TodayCard(data: WearableSnapshot) {
    val d = data.daily
    val tiles = listOfNotNull(
        d.steps?.let { Triple(Icons.Filled.DirectionsWalk, "Steps", "%,d".format(it)) },
        d.distanceKm?.let { Triple(Icons.Filled.Straighten, "Distance", String.format(Locale.US, "%.2f km", it)) },
        d.activeKcal?.let { Triple(Icons.Filled.LocalFireDepartment, "Active calories", "${it.toInt()} kcal") },
        d.totalKcal?.let { Triple(Icons.Filled.Whatshot, "Total calories", "${it.toInt()} kcal") },
        d.sleep?.takeIf { !it.isZero }?.let { Triple(Icons.Filled.Bedtime, "Sleep", "${it.toHours()}h ${it.toMinutes() % 60}m") },
        d.restingHeartRate?.let { Triple(Icons.Filled.FavoriteBorder, "Resting HR", "$it bpm") },
        d.hrvMs?.let { Triple(Icons.Filled.Timeline, "HRV", "${it.toInt()} ms") }
    )
    if (tiles.isEmpty()) return
    SectionTitle("Today", "Shown here only — not saved to your record yet")
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.chunked(2).forEachIndexed { r, row ->
            AnimatedEnter(index = r) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (icon, label, value) -> StatTile(icon, label, value, Modifier.weight(1f)) }
                    if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    Column(
        modifier = modifier.clip(RoundedCornerShape(16.dp)).background(colors.dashboardContainerColor).padding(14.dp)
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(tintFill(p)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = p, modifier = Modifier.size(16.dp)) }
        Spacer(modifier = Modifier.height(8.dp))
        Caption(label)
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = colors.textDarkColor)
    }
}

@Composable
private fun WorkoutCard(workout: Workout, saved: Boolean) {
    val colors = LocalMyColorScheme.current
    val minutes = Duration.between(workout.start, workout.end).toMinutes()
    val tint = Color(0xFFFF7043)
    FlatCard(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tintFill(tint)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.FitnessCenter, null, tint = tint, modifier = Modifier.size(22.dp)) }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(workout.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = colors.textDarkColor)
                Text("${workout.start.display()} · $minutes min · ${workout.source}", fontSize = 11.sp, color = colors.textGreyColor, maxLines = 1)
            }
            SavedChip(
                when {
                    saved -> "Saved"
                    workout.activityName == null -> "Not supported"
                    else -> "Not synced"
                },
                saved
            )
        }
    }
}

@Composable
private fun MissingPermissionsNote(count: Int, onAllow: () -> Unit) {
    NoteRow(
        Icons.Filled.Info, "$count data type(s) not allowed yet", DeviceAmber,
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        HapticTextButton(onClick = onAllow, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
            Text("Allow", fontSize = 12.sp, color = LocalMyColorScheme.current.primaryBlueColor, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Permissions Denied Card ────────────────────────────────────────────────────

@Composable
private fun PermissionsDeniedCard(
    deniedPermissions: Set<String>,
    onRequestAgain: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    StatusCard(
        false, "Health permissions denied",
        "Allow at least one data type so Vitalio can read it from your watch. You can always grant it again.",
        icon = Icons.Filled.LockOpen, tint = DeviceAmber,
        action = "Allow again", actionIcon = Icons.Filled.Key, onAction = onRequestAgain,
        extra = {
            if (deniedPermissions.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tintFill(colors.textGreyColor)).padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Caption("Missing permissions")
                    deniedPermissions.sorted().forEach { perm ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = DeviceRed, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(WearableData.permissionLabel(perm), fontSize = 12.sp, color = colors.textDarkColor)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            HapticTextButton(onClick = onOpenSettings) {
                Icon(Icons.Filled.Settings, null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Health Connect settings", color = colors.primaryBlueColor, fontSize = 13.sp)
            }
            Text(
                "In Health Connect: App permissions → Vitalio → turn on all toggles",
                fontSize = 11.sp, color = colors.textGreyColor, textAlign = TextAlign.Center, lineHeight = 16.sp
            )
        }
    )
}

// ── How-to-connect guide card ─────────────────────────────────────────────────

@Composable
private fun HowToConnectCard() {
    StepsCard(
        "How to connect your watch",
        listOf(
            Icons.Filled.Watch to "Pair your watch with its app (Samsung Health, Fitbit, Garmin Connect, Mi Fitness, NoiseFit…)",
            Icons.Filled.Link to "In that app, turn on Health Connect sync — e.g. Samsung Health → Settings → Health Connect",
            Icons.Filled.Sync to "Come back here, tap Sync above and allow the data types",
            Icons.Filled.Autorenew to "Turn on Auto Sync to keep your record updated automatically"
        ),
        footnote = "Your watch app must write to Health Connect. Most popular watch apps support it; check the app's settings if no data shows up."
    )
}
