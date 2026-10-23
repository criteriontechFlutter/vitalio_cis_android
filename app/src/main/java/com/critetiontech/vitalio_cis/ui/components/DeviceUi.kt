package com.critetiontech.ctvitalio.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import java.util.Locale

// Shared look for the device screens (device list, Omron, wearables): app theme colours, flat
// cards, and motion that tells the user what's happening. Works in light and dark themes.

val DeviceGreen = Color(0xFF2E9E5B)
val DeviceRed = Color(0xFFD64545)
val DeviceAmber = Color(0xFFE08A00)

/** Brand gradient built from the theme's primary colour. */
@Composable
fun brandBrush(): Brush {
    val p = LocalMyColorScheme.current.primaryBlueColor
    return Brush.linearGradient(listOf(lerp(p, Color.Black, 0.45f), p, lerp(p, Color.White, 0.22f)))
}

/** Light accent derived from the primary colour, for highlights on the dark gradient. */
@Composable
fun brandGlow(): Color = lerp(LocalMyColorScheme.current.primaryBlueColor, Color.White, 0.65f)

/** Tinted fill that reads on both light and dark cards. */
@Composable
fun tintFill(tint: Color): Color = tint.copy(alpha = if (isAppDark) 0.18f else 0.09f)

/** Small uppercase, letter-spaced label. */
@Composable
fun Caption(text: String, color: Color = LocalMyColorScheme.current.textGreyColor, modifier: Modifier = Modifier) =
    Text(text.uppercase(Locale.getDefault()), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = color, modifier = modifier)

/** Flat card: no elevation and no border, only the theme's card colour. */
@Composable
fun FlatCard(
    modifier: Modifier = Modifier,
    centered: Boolean = false,
    padding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.dashboardContainerColor)
            .padding(padding),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        content = content
    )
}

/**
 * Hero header: brand gradient, dot grid, a pulsing ring round the icon while [live], a scan beam
 * while [busy], and an optional compact [action] pill (e.g. Sync) so the main action sits in the
 * header instead of a full-width button.
 */
@Composable
fun DeviceHeader(
    icon: ImageVector,
    eyebrow: String,
    title: String,
    status: String,
    live: Boolean,
    busy: Boolean,
    action: (@Composable () -> Unit)? = null,
) {
    val glow = brandGlow()
    val motion = rememberInfiniteTransition(label = "header")
    val ring by motion.animateFloat(0f, 1f, infiniteRepeatable(tween(if (busy) 1200 else 2400, easing = LinearEasing)), label = "ring")
    val scan by motion.animateFloat(-0.2f, 1.2f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "scan")
    Box(
        modifier = Modifier.fillMaxWidth().padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(brandBrush())
            .drawBehind {
                val step = 18.dp.toPx()
                var x = step / 2
                while (x < size.width) {
                    var y = step / 2
                    while (y < size.height) {
                        drawCircle(Color.White.copy(alpha = 0.07f), radius = 1.2.dp.toPx(), center = Offset(x, y))
                        y += step
                    }
                    x += step
                }
                if (busy) {
                    val cx = size.width * scan
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, glow.copy(alpha = 0.28f), Color.Transparent),
                            startX = cx - 60.dp.toPx(), endX = cx + 60.dp.toPx()
                        )
                    )
                }
            }
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(72.dp)) {
                    if (live || busy) {
                        Box(
                            Modifier.size(72.dp).graphicsLayer {
                                scaleX = 0.75f + ring * 0.35f; scaleY = scaleX; alpha = (1f - ring) * 0.8f
                            }.border(1.5.dp, glow, CircleShape)
                        )
                    }
                    Box(
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(28.dp)) }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Caption(eyebrow, glow)
                    Text(title, fontSize = 21.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.3.sp, lineHeight = 25.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(
                    modifier = Modifier.weight(1f, fill = false).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(7.dp).graphicsLayer { alpha = if (live) 0.45f + 0.55f * (1f - ring) else 1f }
                            .clip(CircleShape).background(if (live) Color(0xFF69F0AE) else Color.White.copy(alpha = 0.5f))
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(status, fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (action != null) {
                    Spacer(Modifier.width(8.dp))
                    action()
                }
            }
        }
    }
}

/** Compact white pill for the header's main action. */
@Composable
fun HeaderActionPill(label: String, icon: ImageVector, enabled: Boolean, spinning: Boolean = false, onClick: () -> Unit) {
    val p = LocalMyColorScheme.current.primaryBlueColor
    val spin = rememberInfiniteTransition(label = "pillSpin")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "angle")
    Row(
        modifier = Modifier.height(38.dp).clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = if (enabled) 1f else 0.55f))
            .bounceClick(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = p, modifier = Modifier.size(17.dp).graphicsLayer { rotationZ = if (spinning) angle else 0f })
        Spacer(Modifier.width(6.dp))
        Text(label, color = p, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/** Numbered steps joined by a thin line. */
@Composable
fun StepsCard(title: String, steps: List<Pair<ImageVector, String>>, footnote: String? = null) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    FlatCard {
        Caption(title)
        Spacer(Modifier.height(12.dp))
        steps.forEachIndexed { i, (icon, text) ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(tintFill(p)),
                        contentAlignment = Alignment.Center
                    ) { Icon(icon, null, tint = p, modifier = Modifier.size(17.dp)) }
                    if (i < steps.lastIndex) Box(Modifier.width(1.5.dp).height(18.dp).background(p.copy(alpha = 0.25f)))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f).padding(top = 1.dp)) {
                    Text(String.format(Locale.US, "%02d", i + 1), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = p, letterSpacing = 1.sp)
                    Text(text, fontSize = 12.sp, color = colors.textDarkColor, lineHeight = 17.sp)
                }
            }
        }
        footnote?.let {
            Spacer(Modifier.height(10.dp))
            Text(it, fontSize = 11.sp, color = colors.textGreyColor, lineHeight = 16.sp)
        }
    }
}

/** Radar animation while talking to a device or service. */
@Composable
fun RadarBusyCard(title: String, subtitle: String, icon: ImageVector = Icons.Filled.Bluetooth, onCancel: (() -> Unit)? = null) {
    val colors = LocalMyColorScheme.current
    val p = colors.primaryBlueColor
    val light = brandGlow()
    val radar = rememberInfiniteTransition(label = "radar")
    val waves = (0 until 3).map { i ->
        radar.animateFloat(
            0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing), initialStartOffset = StartOffset(i * 600)), label = "wave$i"
        )
    }
    val sweep by radar.animateFloat(0f, 360f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "sweep")
    val dots by radar.animateFloat(0f, 3.99f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "dots")
    FlatCard(centered = true) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            waves.forEach { w ->
                Box(
                    Modifier.size(120.dp).graphicsLayer {
                        scaleX = 0.3f + w.value * 0.7f; scaleY = scaleX; alpha = 1f - w.value
                    }.border(1.5.dp, p.copy(alpha = 0.6f), CircleShape)
                )
            }
            Box(
                Modifier.size(120.dp).graphicsLayer { rotationZ = sweep }.drawBehind {
                    drawArc(Brush.sweepGradient(listOf(Color.Transparent, p.copy(alpha = 0.3f))), 0f, 90f, useCenter = true)
                }
            )
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Brush.linearGradient(listOf(p, light))),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            title + ".".repeat(dots.toInt()).padEnd(3, ' '),
            fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = colors.textDarkColor, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(subtitle, fontSize = 12.sp, color = colors.textGreyColor, textAlign = TextAlign.Center, lineHeight = 17.sp)
        onCancel?.let {
            Spacer(Modifier.height(6.dp))
            Text(
                "Cancel", color = p, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).bounceClick(onClick = it).padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

/** Success / problem card with a bouncy icon and an optional next-step action. */
@Composable
fun StatusCard(
    success: Boolean,
    title: String,
    message: String,
    icon: ImageVector = if (success) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
    tint: Color = if (success) DeviceGreen else DeviceRed,
    action: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = LocalMyColorScheme.current
    var popped by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { popped = true }
    val iconScale by animateFloatAsState(
        if (popped) 1f else 0.3f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow), label = "pop"
    )
    FlatCard(centered = true) {
        Box(
            Modifier.size(64.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale }.clip(CircleShape).background(tintFill(tint)),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = tint, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textDarkColor, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(message, fontSize = 12.sp, color = colors.textGreyColor, textAlign = TextAlign.Center, lineHeight = 17.sp)
        extra?.invoke(this)
        if (action != null && onAction != null) {
            Spacer(Modifier.height(14.dp))
            GradientButton(action, actionIcon, onClick = onAction)
        }
    }
}

/** Full-width brand gradient button with a soft light sweep. */
@Composable
fun GradientButton(
    label: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colorsOverride: List<Color>? = null,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val p = LocalMyColorScheme.current.primaryBlueColor
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val x by shimmer.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "shimmerX")
    val fill = colorsOverride ?: listOf(lerp(p, Color.Black, 0.25f), p, lerp(p, Color.White, 0.2f))
    Row(
        modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(fill))
            .graphicsLayer { alpha = if (enabled && !loading) 1f else 0.6f }
            .drawBehind {
                if (enabled && !loading) {
                    val cx = size.width * x
                    drawRect(
                        Brush.horizontalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = 0.22f), Color.Transparent),
                            startX = cx - 50.dp.toPx(), endX = cx + 50.dp.toPx()
                        )
                    )
                }
            }
            .bounceClick(enabled = enabled && !loading, onClick = onClick),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            androidx.compose.material3.CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, null, tint = Color.White, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, letterSpacing = 0.3.sp)
    }
}

/** Short tinted note: info, warning or error. */
@Composable
fun NoteRow(icon: ImageVector, text: String, tint: Color, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tintFill(tint)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = tint, lineHeight = 16.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

/** Gentle count-up for a number shown on a card. */
@Composable
fun rememberCountUp(target: Int): Int {
    var started by remember(target) { mutableStateOf(false) }
    LaunchedEffect(target) { started = true }
    val value by androidx.compose.animation.core.animateIntAsState(
        if (started) target else 0, tween(900, easing = FastOutSlowInEasing), label = "count"
    )
    return value
}

/** "just now", "9 min. ago", "2 hr. ago" — short enough for a status pill. */
fun syncedAgo(millis: Long): String {
    val now = System.currentTimeMillis()
    if (now - millis < 60_000) return "just now"
    return android.text.format.DateUtils.getRelativeTimeSpanString(
        millis, now, android.text.format.DateUtils.MINUTE_IN_MILLIS, android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
    ).toString()
}
