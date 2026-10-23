package com.critetiontech.ctvitalio.ui.components

import android.security.NetworkSecurityPolicy
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.config.ApiConfig
import com.critetiontech.ctvitalio.config.ApiEnvironment
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

private val TestServerColor = Color(0xFFF59E0B)

/**
 * Tapping this element 7 times quickly calls [onUnlock]: the hidden door to the server switcher.
 * Does nothing in builds where switching isn't allowed.
 */
fun Modifier.secretTaps(onUnlock: () -> Unit): Modifier = composed {
    if (!ApiConfig.canSwitch) return@composed this
    var taps by remember { mutableIntStateOf(0) }
    var lastTap by remember { mutableLongStateOf(0L) }
    clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
        val now = System.currentTimeMillis()
        taps = if (now - lastTap < 1_000) taps + 1 else 1
        lastTap = now
        if (taps >= 7) { taps = 0; onUnlock() }
    }
}

/** Small pill telling testers they're not on production. Hidden on production. */
@Composable
fun EnvironmentBadge(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val env = ApiConfig.environment
    if (!ApiConfig.canSwitch || env == ApiEnvironment.PRODUCTION) return
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(TestServerColor, RoundedCornerShape(50))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Rounded.Dns, null, tint = Color.White, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text("${env.label.uppercase()} SERVER", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

/** Lists the servers; choosing another one signs out and restarts the app on it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnvironmentSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    var selected by remember { mutableStateOf(ApiConfig.environment) }
    var localInput by remember { mutableStateOf(ApiConfig.localHost.removePrefix("http://")) }
    val localHost = ApiConfig.normalizeHost(localInput)
    val localError = when {
        localHost == null -> "Enter an address like 192.168.1.10, without a port"
        // network_security_config allows plain HTTP only in debug builds and to a few known servers
        localHost.startsWith("http://") &&
            !NetworkSecurityPolicy.getInstance().isCleartextTrafficPermitted(localHost.substringAfter("://")) ->
            "This APK blocks plain HTTP to this server. Use https:// or a debug APK."
        else -> null
    }
    val changed = selected != ApiConfig.environment ||
        (selected == ApiEnvironment.LOCAL && localHost != ApiConfig.localHost)
    val canSwitch = changed && (selected != ApiEnvironment.LOCAL || localError == null)

    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = "Server",
        subtitle = "For testing. Pick which backend this app talks to.",
        icon = Icons.Rounded.Dns,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ApiEnvironment.entries.forEach { env ->
                val isSelected = env == selected
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isSelected) colors.primaryBlueColor.copy(alpha = 0.08f) else colors.dashboardBackgroundColor,
                            RoundedCornerShape(16.dp),
                        )
                        .border(
                            if (isSelected) 1.5.dp else 0.dp,
                            if (isSelected) colors.primaryBlueColor else Color.Transparent,
                            RoundedCornerShape(16.dp),
                        )
                        .clickable { selected = env }
                        .padding(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .border(2.dp, if (isSelected) colors.primaryBlueColor else colors.textGreyColor, CircleShape),
                        ) {
                            if (isSelected) Box(Modifier.size(10.dp).background(colors.primaryBlueColor, CircleShape))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(env.label, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                if (env == ApiConfig.environment) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("Current", color = colors.primaryBlueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                            val shownHost = if (env == ApiEnvironment.LOCAL) ApiConfig.localHost else env.host
                            Text(shownHost.substringAfter("://"), color = colors.textGreyColor, fontSize = 12.sp)
                        }
                    }

                    // Local is whatever server the tester is running; type its address here
                    if (env == ApiEnvironment.LOCAL && isSelected) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = localInput,
                            onValueChange = { localInput = it },
                            label = { Text("Server address") },
                            placeholder = { Text("192.168.1.10") },
                            singleLine = true,
                            isError = localError != null,
                            supportingText = {
                                Text(localError ?: "IP or domain only. Ports (4082, 4084…) are added automatically.")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            if (canSwitch) {
                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TestServerColor.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .padding(12.dp),
                ) {
                    Icon(Icons.Rounded.Warning, null, tint = TestServerColor, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "You'll be signed out and the app will restart. Accounts and data are separate on each server.",
                        color = colors.textDarkColor, fontSize = 13.sp, lineHeight = 18.sp,
                    )
                }
            }

            Spacer(Modifier.height(4.dp))
            HapticButton(
                onClick = { ApiConfig.switchTo(context, selected, localHost ?: ApiConfig.localHost) },
                enabled = canSwitch,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
            ) {
                Text(if (changed) "Switch to ${selected.label} & restart" else "Choose a server", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
