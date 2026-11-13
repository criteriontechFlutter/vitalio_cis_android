package com.critetiontech.ctvitalio.wear.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import androidx.wear.remote.interactions.RemoteActivityHelper
import com.critetiontech.ctvitalio.wear.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

private const val TAG = "SignInScreen"
private const val STATUS_DISPLAY_MS = 3_000L

/**
 * Shown while no session has been synced from the phone. The watch app is a companion
 * (standalone=false), so sign-in happens on the phone and arrives via the Data Layer.
 */
@Composable
fun SignInOnPhoneScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()
    val colors = MaterialTheme.colorScheme
    var status by remember { mutableStateOf<String?>(null) }
    var opening by remember { mutableStateOf(false) }

    LaunchedEffect(status) {
        if (status != null) {
            delay(STATUS_DISPLAY_MS)
            status = null
        }
    }

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                buttonSize = EdgeButtonSize.Large,
                enabled = !opening,
                onClick = {
                    opening = true
                    context.vibrate(HAPTIC_TICK)
                    scope.launch {
                        val ok = openPhoneApp(context)
                        context.vibrate(if (ok) HAPTIC_SUCCESS else HAPTIC_FAILURE)
                        status = context.getString(if (ok) R.string.check_your_phone else R.string.phone_not_reachable)
                        opening = false
                    }
                }
            ) {
                Text(stringResource(R.string.open_on_phone), maxLines = 1)
            }
        }
    ) { contentPadding ->
        ScalingLazyColumn(
            state = listState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxWidth()
        ) {
            item { PairingIllustration(Modifier.padding(bottom = 6.dp)) }
            item {
                Text(
                    text = stringResource(R.string.sign_in_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                AnimatedContent(
                    targetState = status,
                    transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                    label = "status"
                ) { s ->
                    Text(
                        text = s ?: stringResource(R.string.sign_in_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (s != null) colors.primary else colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

/** Watch ··· phone, with dots travelling toward the phone and a gentle float. */
@Composable
private fun PairingIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val t = rememberInfiniteTransition(label = "pairing")
    val flow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "flow")
    val float by t.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(2200, easing = EmphasizedDecelerate), RepeatMode.Reverse), label = "float"
    )
    Row(
        modifier = modifier.graphicsLayer { translationY = float * 3.dp.toPx() },
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .border(1.5.dp, colors.outlineVariant, CircleShape)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_watch),
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        repeat(3) { i ->
            val f = ((flow - i / 3f) % 1f + 1f) % 1f
            val glow = sin(f * PI).toFloat()
            Box(
                Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.2f + 0.8f * glow))
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(colors.primaryContainer)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_phone),
                contentDescription = null,
                tint = colors.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Opens the Vitalio Play listing on the paired phone — it shows "Open" when the app is
 * installed and "Install" when it isn't, so no deep link is needed in the phone app.
 */
private suspend fun openPhoneApp(context: Context): Boolean = withContext(Dispatchers.IO) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(Uri.parse("market://details?id=${context.packageName}"))
        // Blocking get() is fine here: we are on Dispatchers.IO
        RemoteActivityHelper(context).startRemoteActivity(intent).get()
        true
    } catch (e: Exception) {
        Log.w(TAG, "Could not open app on phone: ${e.message}")
        false
    }
}
