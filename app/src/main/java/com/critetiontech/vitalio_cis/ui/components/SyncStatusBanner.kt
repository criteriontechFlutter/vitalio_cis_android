package com.critetiontech.ctvitalio.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SyncBlue = Color(0xFF2F7BF5)
private val SyncGreen = Color(0xFF2DBE7E)
private val SyncAmber = Color(0xFFF59E0B)
private val SyncGrey = Color(0xFF64748B)

/** How long "All synced" stays up after the queue empties. */
private const val SYNCED_NOTE_MS = 4_000L

/**
 * One line about offline logging: offline (and how many logs will upload), syncing, logs the
 * server refused (with Retry/Discard), or a brief "All synced". Renders nothing otherwise.
 */
@Composable
fun SyncStatusBanner(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val online by AppDependencies.networkMonitor.isOnline.collectAsState()
    val entries by OfflineOutbox.entries.collectAsState()
    val syncing by OfflineOutbox.syncing.collectAsState()
    val lastSyncedAt by OfflineOutbox.lastSyncedAt.collectAsState()

    var showSynced by remember { mutableStateOf(false) }
    LaunchedEffect(lastSyncedAt) {
        if (lastSyncedAt == 0L) return@LaunchedEffect
        showSynced = true
        delay(SYNCED_NOTE_MS)
        showSynced = false
    }
    // Back online with logs waiting: start the upload now rather than when WorkManager gets to it.
    LaunchedEffect(online) { if (online) OfflineOutbox.requestSync(context) }

    val waiting = entries.count { !it.failed }
    val failed = entries.count { it.failed }

    val state: BannerState? = when {
        failed > 0 -> BannerState(
            Icons.Rounded.ErrorOutline, SyncAmber,
            pluralStringResource(R.plurals.offline_failed, failed, failed),
            stringResource(R.string.offline_failed_desc),
            showActions = true,
        )
        !online && waiting > 0 -> BannerState(
            Icons.Rounded.CloudOff, SyncGrey,
            stringResource(R.string.offline_title),
            pluralStringResource(R.plurals.offline_waiting, waiting, waiting),
        )
        !online -> BannerState(
            Icons.Rounded.CloudOff, SyncGrey,
            stringResource(R.string.offline_title),
            stringResource(R.string.offline_showing_saved),
        )
        waiting > 0 -> BannerState(
            Icons.Rounded.CloudUpload, SyncBlue,
            pluralStringResource(R.plurals.offline_syncing, waiting, waiting),
            null,
            spinner = syncing,
        )
        showSynced -> BannerState(Icons.Rounded.CloudDone, SyncGreen, stringResource(R.string.offline_all_synced), null)
        else -> null
    }

    var confirmDiscard by remember { mutableStateOf(false) }
    if (confirmDiscard) {
        AppDialog(
            title = stringResource(R.string.offline_discard_title),
            message = pluralStringResource(R.plurals.offline_discard_message, failed, failed),
            icon = Icons.Rounded.ErrorOutline,
            destructive = true,
            confirmText = stringResource(R.string.offline_discard),
            dismissText = stringResource(R.string.profile_cancel),
            onDismiss = { confirmDiscard = false },
            onConfirm = { confirmDiscard = false; scope.launch { OfflineOutbox.discardFailed() } },
        )
    }

    // Keeps the last state while the banner animates out.
    var shown by remember { mutableStateOf(state) }
    if (state != null) shown = state

    AnimatedVisibility(
        visible = state != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier,
    ) {
        shown?.let { s ->
            BannerContent(
                state = s,
                onRetry = { scope.launch { OfflineOutbox.retryFailed(context) } },
                onDiscard = { confirmDiscard = true },
            )
        }
    }
}

private data class BannerState(
    val icon: ImageVector,
    val tint: Color,
    val title: String,
    val subtitle: String?,
    val spinner: Boolean = false,
    val showActions: Boolean = false,
)

@Composable
private fun BannerContent(state: BannerState, onRetry: () -> Unit, onDiscard: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(state.tint.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.spinner) {
                CircularProgressIndicator(color = state.tint, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            } else {
                Icon(state.icon, null, tint = state.tint, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(state.title, color = colors.textDarkColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                state.subtitle?.let { Text(it, color = colors.textGreyColor, fontSize = 12.sp) }
            }
        }
        if (state.showActions) {
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                HapticTextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.offline_discard), color = colors.textGreyColor, fontSize = 13.sp)
                }
                HapticTextButton(onClick = onRetry) {
                    Text(stringResource(R.string.offline_retry), color = state.tint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
