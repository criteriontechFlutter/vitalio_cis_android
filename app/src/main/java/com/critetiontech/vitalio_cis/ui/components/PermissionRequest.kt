package com.critetiontech.ctvitalio.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.critetiontech.ctvitalio.permissions.AppPermission
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

/** Starts a feature that needs [AppPermission]; see [rememberPermissionRequest]. */
@Stable
class PermissionRequest internal constructor(private val start: () -> Unit) {
    fun launch() = start()
}

/**
 * Explains a permission in the user's terms before Android's dialog appears, the way most
 * well-made apps do, and runs [onGranted] once it's allowed:
 *
 * - already allowed (or not needed on this Android version) → [onGranted] straight away
 * - not yet allowed → a sheet saying why ([reason]), what it's used for and what we never do;
 *   "Continue" opens the system dialog
 * - turned off for good (declined twice) → a sheet pointing to Settings; coming back with it
 *   allowed continues the action
 *
 * [reason] finishes "Vitalio needs …" for the feature the user just tapped.
 */
@Composable
fun rememberPermissionRequest(
    permission: AppPermission,
    reason: String,
    onDenied: () -> Unit = {},
    onGranted: () -> Unit,
): PermissionRequest {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    var sheet by remember { mutableStateOf<SheetMode?>(null) }
    var waitingForSettings by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) granted() else denied()
    }

    // Continue the action when the user comes back from Settings having allowed it
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && waitingForSettings) {
                waitingForSettings = false
                if (permission.isGranted(context)) granted()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    when (sheet) {
        SheetMode.Explain -> PermissionSheet(
            permission = permission,
            reason = reason,
            blocked = false,
            onConfirm = {
                sheet = null
                permission.markAsked(context)
                launcher.launch(permission.manifestPermissions)
            },
            onDismiss = { sheet = null; denied() },
        )
        SheetMode.Blocked -> PermissionSheet(
            permission = permission,
            reason = reason,
            blocked = true,
            onConfirm = {
                sheet = null
                waitingForSettings = true
                context.startActivity(AppPermission.appSettingsIntent(context))
            },
            onDismiss = { sheet = null; denied() },
        )
        null -> Unit
    }

    return remember(permission) {
        PermissionRequest {
            when {
                !permission.isRequired || permission.isGranted(context) -> granted()
                permission.isBlocked(context) -> sheet = SheetMode.Blocked
                else -> sheet = SheetMode.Explain
            }
        }
    }
}

private enum class SheetMode { Explain, Blocked }

/** The name Android shows for this permission in Settings → Permissions. */
private val AppPermission.systemName: String
    get() = if (this == AppPermission.NearbyDevices && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) "Location" else title

private val AppPermission.askTitle: String
    get() = when (this) {
        AppPermission.Notifications -> "Turn on notifications"
        AppPermission.NearbyDevices -> "Allow nearby devices"
        else -> "Allow ${title.lowercase()} access"
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionSheet(
    permission: AppPermission,
    reason: String,
    blocked: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = if (blocked) "${permission.systemName} is turned off" else permission.askTitle,
        icon = permission.icon,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "Vitalio needs ${permission.systemName.lowercase()} access $reason.",
                color = colors.textDarkColor,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )

            if (blocked) {
                // Settings path, numbered like the screens the user will see
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.dashboardBackgroundColor, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                ) {
                    Text("To turn it on", color = colors.textGreyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    listOf("Tap Open Settings below", "Go to Permissions", "Tap ${permission.systemName} and choose Allow")
                        .forEachIndexed { i, step -> Step(i + 1, step) }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Used for", color = colors.textGreyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    permission.usedFor.forEach { Bullet(it) }
                }
            }

            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.primaryBlueColor.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
                    .padding(12.dp),
            ) {
                Icon(Icons.Rounded.Lock, null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                Spacer(Modifier.width(8.dp))
                Text(permission.privacy, color = colors.textDarkColor, fontSize = 13.sp, lineHeight = 18.sp)
            }

            Spacer(Modifier.height(2.dp))
            HapticButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
            ) {
                if (blocked) {
                    Icon(Icons.Rounded.Settings, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (blocked) "Open Settings" else "Continue", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
            HapticTextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 10.dp),
            ) {
                Text("Not now", color = colors.textGreyColor, fontSize = 15.sp)
            }
            if (!blocked) {
                Text(
                    "You can change this anytime in Menu → Permissions & Privacy.",
                    color = colors.textGreyColor,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun Bullet(text: String) {
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(top = 1.dp).size(18.dp).background(colors.primaryBlueColor.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(Icons.Rounded.Check, null, tint = colors.primaryBlueColor, modifier = Modifier.size(12.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = colors.textDarkColor, fontSize = 14.sp, lineHeight = 20.sp)
    }
}

@Composable
private fun Step(number: Int, text: String) {
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).background(colors.primaryBlueColor, CircleShape),
        ) {
            Text("$number", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = colors.textDarkColor, fontSize = 14.sp)
    }
}
