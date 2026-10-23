package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.healthconnect.WearableData
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.reminder.AlarmSetup
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.myapplication.utils.LocalNavController

private val AllowedColor = Color(0xFF16A34A)
private val BlockedColor = Color(0xFFEA580C)

/**
 * Every permission Vitalio uses, why, and whether it's on — with a way to turn each on or off.
 * Runtime permissions are only ever requested from the feature that needs them or from here.
 */
@Composable
fun PermissionsScreen() {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current

    // Re-read every status when the user comes back from Android settings
    var refresh by remember { mutableIntStateOf(0) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) refresh++ }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    var healthStatus by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refresh) {
        val client = WearableSync.clientOrNull(context)
        healthStatus = if (client == null) "Health Connect isn't installed" else {
            val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            val total = WearableData.READ_PERMISSIONS.size
            val allowed = WearableData.READ_PERMISSIONS.count { it in granted }
            if (allowed == 0) "Not connected" else "$allowed of $total data types allowed"
        }
    }
    val alarmItems = remember(refresh) {
        AlarmSetup.requirements(context).filter { it.item != AlarmSetup.Item.Notifications }
    }

    CommonAppBar(title = "Permissions & Privacy") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IntroCard()

            SectionTitle("Asked when you use a feature")
            PermissionCard {
                AppPermission.entries.filter { it.isRequired }.forEachIndexed { i, permission ->
                    key(permission) {
                        if (i > 0) RowDivider()
                        RuntimePermissionRow(permission, refresh)
                    }
                }
            }

            SectionTitle("Health data")
            PermissionCard {
                LinkRow(
                    icon = Icons.Rounded.Favorite,
                    title = "Health Connect",
                    summary = "Reads vitals, steps, sleep and workouts from your watch and health apps. Read-only — Vitalio never writes or deletes your data there.",
                    status = healthStatus,
                    ok = healthStatus?.contains(" of ") == true,
                    onClick = { navController.navigate(Routes.HEALTHCONNECTSYNC) },
                )
            }

            SectionTitle("Special access for medicine alarms")
            PermissionCard {
                alarmItems.forEachIndexed { i, req ->
                    if (i > 0) RowDivider()
                    LinkRow(
                        icon = req.item.icon,
                        title = req.title,
                        summary = req.description,
                        status = if (req.granted) "Allowed" else if (req.critical) "Needed for alarms" else "Recommended",
                        ok = req.granted,
                        onClick = { context.startActivity(AlarmSetup.settingsIntent(context, req.item)) },
                    )
                }
            }

            SectionTitle("Allowed automatically")
            PermissionCard {
                listOf(
                    Triple(Icons.Rounded.Language, "Internet", "Sync your health record securely with the Vitalio server"),
                    Triple(Icons.Rounded.RestartAlt, "Run at startup", "Put medicine alarms back after your phone restarts"),
                    Triple(Icons.Rounded.Vibration, "Vibration", "Vibrate for alarms and button taps"),
                    Triple(Icons.Rounded.PhoneAndroid, "Keep phone awake", "Keep an alarm ringing until you respond"),
                ).forEachIndexed { i, (icon, title, summary) ->
                    if (i > 0) RowDivider()
                    InfoRow(icon, title, summary)
                }
            }

            HapticOutlinedButton(
                onClick = { context.startActivity(AppPermission.appSettingsIntent(context)) },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(24.dp),
            ) {
                Icon(Icons.Rounded.Settings, null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Open Android app settings", color = colors.primaryBlueColor, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private val AlarmSetup.Item.icon: ImageVector
    get() = when (this) {
        AlarmSetup.Item.Notifications -> Icons.Rounded.Notifications
        AlarmSetup.Item.ExactAlarms -> Icons.Rounded.Alarm
        AlarmSetup.Item.FullScreen -> Icons.Rounded.Wallpaper
        AlarmSetup.Item.PopupOverApps -> Icons.Rounded.PhoneAndroid
        AlarmSetup.Item.Battery -> Icons.Rounded.BatteryChargingFull
    }

@Composable
private fun IntroCard() {
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.primaryBlueColor.copy(alpha = 0.10f))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(colors.primaryBlueColor, CircleShape),
        ) {
            Icon(Icons.Rounded.Shield, null, tint = Color.White, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("Your data, your control", color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(
                "Vitalio asks for a permission only when you use the feature that needs it, and explains why first. " +
                    "Tap any item to see how it's used. You can turn anything off at any time.",
                color = colors.textGreyColor, fontSize = 13.sp, lineHeight = 18.sp,
            )
        }
    }
}

@Composable
private fun RuntimePermissionRow(permission: AppPermission, refresh: Int) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    var expanded by remember { mutableStateOf(false) }
    var granted by remember(refresh) { mutableStateOf(permission.isGranted(context)) }
    val blocked = remember(refresh, granted) { permission.isBlocked(context) }
    val request = rememberPermissionRequest(permission, permission.defaultReason) {
        granted = true
    }

    Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBubble(permission.icon)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(permission.title, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(permission.summary, color = colors.textGreyColor, fontSize = 12.sp, lineHeight = 16.sp)
                Spacer(Modifier.height(6.dp))
                StatusChip(
                    text = when {
                        granted -> "Allowed"
                        blocked -> "Turned off in Settings"
                        else -> "Not allowed"
                    },
                    color = when {
                        granted -> AllowedColor
                        blocked -> BlockedColor
                        else -> colors.textGreyColor
                    },
                )
            }
            Icon(
                if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (expanded) "Hide details" else "Show details",
                tint = colors.textGreyColor,
            )
        }

        AnimatedVisibility(expanded) {
            Column(Modifier.padding(start = 52.dp, top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Used for", color = colors.textGreyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                permission.usedFor.forEach {
                    Text("•  $it", color = colors.textDarkColor, fontSize = 13.sp, lineHeight = 18.sp)
                }
                Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.Top) {
                    Icon(Icons.Rounded.Lock, null, tint = colors.primaryBlueColor, modifier = Modifier.size(14.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(permission.privacy, color = colors.textGreyColor, fontSize = 12.sp, lineHeight = 17.sp)
                }
                Spacer(Modifier.height(4.dp))
                if (granted) {
                    // Android only lets the user revoke from system settings
                    HapticOutlinedButton(
                        onClick = { context.startActivity(AppPermission.appSettingsIntent(context)) },
                        shape = RoundedCornerShape(20.dp),
                    ) { Text("Manage in Settings", color = colors.primaryBlueColor, fontSize = 13.sp) }
                } else {
                    HapticButton(
                        onClick = { request.launch() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
                    ) { Text(if (blocked) "Turn on in Settings" else "Allow", fontSize = 13.sp) }
                }
            }
        }
    }
}

@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    summary: String,
    status: String?,
    ok: Boolean,
    onClick: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBubble(icon)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(summary, color = colors.textGreyColor, fontSize = 12.sp, lineHeight = 16.sp)
            if (status != null) {
                Spacer(Modifier.height(6.dp))
                StatusChip(status, if (ok) AllowedColor else BlockedColor)
            }
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = colors.textGreyColor)
    }
}

@Composable
private fun InfoRow(icon: ImageVector, title: String, summary: String) {
    val colors = LocalMyColorScheme.current
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, muted = true)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(summary, color = colors.textGreyColor, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, muted: Boolean = false) {
    val colors = LocalMyColorScheme.current
    val tint = if (muted) colors.textGreyColor else colors.primaryBlueColor
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(40.dp).background(tint.copy(alpha = 0.12f), CircleShape),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun StatusChip(text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.background(color.copy(alpha = 0.12f), RoundedCornerShape(50)).padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Box(Modifier.size(6.dp).background(color, CircleShape))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text.uppercase(),
        color = LocalMyColorScheme.current.textGreyColor,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.6.sp,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp),
    )
}

@Composable
private fun PermissionCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(LocalMyColorScheme.current.dashboardContainerColor)
    ) { content() }
}

@Composable
private fun RowDivider() {
    Box(
        Modifier
            .padding(start = 68.dp)
            .fillMaxWidth()
            .height(0.5.dp)
            .background(LocalMyColorScheme.current.borderGreyLightColor.copy(alpha = 0.5f))
    )
}
