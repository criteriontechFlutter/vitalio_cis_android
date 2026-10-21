package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlin.math.cos
import kotlin.math.sin

/** Which illustration a vital's history page leads with. */
enum class HeroKind { BloodPressure, Heart, Oxygen, Temperature, Weight, Glucose, Generic }

fun heroKindFor(vitalName: String, isBP: Boolean): HeroKind = when {
    isBP -> HeroKind.BloodPressure
    else -> when (vitalName.trim().lowercase()) {
        "pulse", "heart rate", "heartrate" -> HeroKind.Heart
        "spo2", "oxygen saturation", "blood oxygen (spo2)" -> HeroKind.Oxygen
        "temperature" -> HeroKind.Temperature
        "weight" -> HeroKind.Weight
        "blood glucose", "rbs" -> HeroKind.Glucose
        else -> HeroKind.Generic
    }
}

/** Page title for a vital, as in the designs ("Blood Oxygen (SpO2)", "Body Temperature"…). */
fun heroTitleFor(vitalName: String): String = when (vitalName.trim().lowercase()) {
    "spo2" -> "Blood Oxygen (SpO2)"
    "temperature" -> "Body Temperature"
    "weight" -> "Body Weight"
    else -> vitalName
}

private val OutlineGrey = Color(0xFFC5CBD6)
private val BadgeBlue = Color(0xFFC9DBFF)

/**
 * The big illustration at the top of a vital's history: a soft circle for blood pressure, a heart
 * for pulse, a drop for SpO2 and glucose, a thermometer for temperature and a scale for weight.
 * [value]/[unit]/[status] describe the latest reading; [pulse] adds the blood-pressure pulse pill.
 */
@Composable
fun VitalHero(
    kind: HeroKind,
    value: String,
    unit: String,
    status: VitalStatus?,
    caption: String?,
    pulse: String? = null,
) {
    val colors = LocalMyColorScheme.current
    val statusText = when {
        status == null -> "NO DATA"
        status == VitalStatus.None -> null
        else -> status.label
    }
    val valueColor = if (status == null || status == VitalStatus.None) colors.textDarkColor else status.color

    @Composable
    fun Reading(big: Int = 40, withCaption: Boolean = true) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = valueColor, fontSize = big.sp, fontWeight = FontWeight.Medium)
                if (unit.isNotEmpty() && value != "--") {
                    Spacer(Modifier.width(4.dp))
                    Text(unit, color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
                }
            }
            statusText?.let { Text(it, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
            if (withCaption) caption?.let { Text(it, color = colors.textGreyColor, fontSize = 11.sp) }
        }
    }
    // Shapes with a badge or tight outline get the "Latest · …" line underneath instead.
    @Composable
    fun CaptionBelow() {
        caption?.let { Text(it, color = colors.textGreyColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
    }

    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        when (kind) {
            HeroKind.BloodPressure -> {
                Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) { drawBlob(Color(0xFF2F6BFF)) }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(horizontalArrangement = Arrangement.spacedBy(44.dp)) {
                            Text("SYS", color = colors.textGreyColor, fontSize = 12.sp)
                            Text("DIA", color = colors.textGreyColor, fontSize = 12.sp)
                        }
                        Reading(big = 36)
                    }
                }
                if (pulse != null) {
                    Spacer(Modifier.height(4.dp))
                    PulsePill(pulse)
                }
            }
            HeroKind.Heart -> ShapeWithBadge(
                draw = { drawPath(heartPath(size.width, size.height), OutlineGrey, style = Stroke(2.dp.toPx())) },
                badge = { EcgLine(Color.White, Modifier.size(width = 30.dp, height = 18.dp)) },
                content = { Reading(withCaption = false) },
                width = 250, height = 230, contentOffsetY = -12,
            ).also { CaptionBelow() }
            HeroKind.Oxygen, HeroKind.Glucose -> ShapeWithBadge(
                draw = { drawPath(dropPath(size.width, size.height), OutlineGrey, style = Stroke(2.dp.toPx())) },
                badge = {
                    Text(
                        if (kind == HeroKind.Oxygen) "O₂" else "mg",
                        color = if (kind == HeroKind.Oxygen) Color(0xFF1F5BE0) else Color(0xFFDC2626),
                        fontSize = if (kind == HeroKind.Oxygen) 24.sp else 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                badgeColor = if (kind == HeroKind.Oxygen) BadgeBlue else Color(0xFFFECACA),
                content = { Reading(withCaption = false) },
                width = 190, height = 240, contentOffsetY = 26,
            ).also { CaptionBelow() }
            HeroKind.Temperature -> Thermometer(value, unit, status, valueColor, statusText, caption)
            HeroKind.Weight -> Box(Modifier.size(width = 200.dp, height = 170.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawScale() }
                Box(Modifier.padding(top = 26.dp)) { Reading(big = 34, withCaption = false) }
            }.also { CaptionBelow() }
            HeroKind.Generic -> Box(Modifier.size(240.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize()) { drawBlob(Color(0xFF2F6BFF)) }
                Reading()
            }
        }
    }
}

@Composable
private fun ShapeWithBadge(
    draw: DrawScope.() -> Unit,
    badge: @Composable () -> Unit,
    content: @Composable () -> Unit,
    width: Int,
    height: Int,
    contentOffsetY: Int = 0,
    badgeColor: Color = BadgeBlue,
) {
    Box(Modifier.size(width = width.dp, height = height.dp)) {
        Canvas(Modifier.fillMaxSize(), onDraw = draw)
        Box(Modifier.fillMaxSize().offset(y = contentOffsetY.dp), contentAlignment = Alignment.Center) { content() }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-18).dp)
                .size(58.dp)
                .clip(CircleShape)
                .background(badgeColor),
        ) { badge() }
    }
}

@Composable
private fun PulsePill(pulse: String) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardContainerColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("PULSE", color = colors.textGreyColor, fontSize = 12.sp)
        Spacer(Modifier.width(8.dp))
        EcgLine(colors.textGreyColor, Modifier.size(width = 28.dp, height = 14.dp))
        Spacer(Modifier.width(8.dp))
        Text(pulse, color = colors.textDarkColor, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(4.dp))
        Text("/min", color = colors.textGreyColor, fontSize = 12.sp)
    }
}

/** A single heartbeat trace. */
@Composable
private fun EcgLine(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val p = Path().apply {
            moveTo(0f, h / 2); lineTo(w * 0.3f, h / 2); lineTo(w * 0.42f, 0f); lineTo(w * 0.58f, h)
            lineTo(w * 0.7f, h / 2); lineTo(w, h / 2)
        }
        drawPath(p, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/* ---------------- Shapes ---------------- */

/** Four loose rings, like a hand-drawn circle. */
private fun DrawScope.drawBlob(color: Color) {
    val center = Offset(size.width / 2, size.height / 2)
    val baseRadius = size.minDimension / 2.4f
    repeat(4) { layer ->
        val path = Path()
        val points = 120
        val offset = layer * 15f
        for (i in 0..points) {
            val angle = (i * (2 * Math.PI / points)).toFloat()
            val radius = baseRadius + sin(angle * 3 + offset) * 12f + cos(angle * 2 + offset) * 8f
            val x = center.x + radius * cos(angle)
            val y = center.y + radius * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color.copy(alpha = if (layer == 0) 1f else 0.55f), style = Stroke((if (layer == 0) 2.5f else 1.5f).dp.toPx()))
    }
}

private fun heartPath(w: Float, h: Float) = Path().apply {
    moveTo(w * 0.5f, h * 0.22f)
    cubicTo(w * 0.5f, h * 0.1f, w * 0.38f, h * 0.0f, w * 0.24f, h * 0.0f)
    cubicTo(w * 0.08f, h * 0.0f, 0f, h * 0.14f, 0f, h * 0.3f)
    cubicTo(0f, h * 0.56f, w * 0.28f, h * 0.74f, w * 0.5f, h * 0.98f)
    cubicTo(w * 0.72f, h * 0.74f, w, h * 0.56f, w, h * 0.3f)
    cubicTo(w, h * 0.14f, w * 0.92f, h * 0.0f, w * 0.76f, h * 0.0f)
    cubicTo(w * 0.62f, h * 0.0f, w * 0.5f, h * 0.1f, w * 0.5f, h * 0.22f)
    close()
}

private fun dropPath(w: Float, h: Float) = Path().apply {
    moveTo(w * 0.5f, 0f)
    cubicTo(w * 0.42f, h * 0.18f, 0f, h * 0.45f, 0f, h * 0.67f)
    cubicTo(0f, h * 0.86f, w * 0.22f, h, w * 0.5f, h)
    cubicTo(w * 0.78f, h, w, h * 0.86f, w, h * 0.67f)
    cubicTo(w, h * 0.45f, w * 0.58f, h * 0.18f, w * 0.5f, 0f)
    close()
}

/** Bathroom scale: rounded body with a ruler window on top. */
private fun DrawScope.drawScale() {
    val stroke = 2.dp.toPx()
    val top = 22.dp.toPx()
    drawRoundRect(OutlineGrey, topLeft = Offset(0f, top), size = Size(size.width, size.height - top), cornerRadius = CornerRadius(26.dp.toPx()), style = Stroke(stroke))
    val winW = size.width * 0.32f
    val winH = 36.dp.toPx()
    val winX = (size.width - winW) / 2
    drawRoundRect(Color(0xFFCBD2DC), topLeft = Offset(winX, top - winH / 2), size = Size(winW, winH), cornerRadius = CornerRadius(8.dp.toPx()))
    val ticks = 9
    for (i in 0 until ticks) {
        val x = winX + winW * (i + 1) / (ticks + 1)
        val len = if (i == ticks / 2) winH * 0.55f else winH * 0.35f
        drawLine(Color.White, Offset(x, top - len / 2), Offset(x, top + len / 2), strokeWidth = 1.5.dp.toPx())
    }
}

/* ---------------- Thermometer ---------------- */

@Composable
private fun Thermometer(
    value: String,
    unit: String,
    status: VitalStatus?,
    valueColor: Color,
    statusText: String?,
    caption: String?,
) {
    val colors = LocalMyColorScheme.current
    // Mercury height: 94–108 °F (34.4–42.2 °C) maps onto the tube.
    val reading = value.toDoubleOrNull()
    val asF = reading?.let { if (it <= 45) it * 9 / 5 + 32 else it }
    val target = asF?.let { ((it - 94) / (108 - 94)).toFloat().coerceIn(0.05f, 1f) } ?: 0f
    val level by animateFloatAsState(target, tween(900), label = "mercury")
    val mercury = if (status == null || status == VitalStatus.None) Color(0xFF2F6BFF) else status.color

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = valueColor, fontSize = 38.sp, fontWeight = FontWeight.Medium)
            if (unit.isNotEmpty() && value != "--") {
                Spacer(Modifier.width(3.dp))
                Text(unit, color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        statusText?.let { Text(it, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(10.dp))
        Canvas(Modifier.size(width = 110.dp, height = 210.dp)) {
            val tubeW = 36.dp.toPx()
            val bulbR = 30.dp.toPx()
            val cx = size.width / 2
            val bulbCy = size.height - bulbR
            val tubeTop = 4.dp.toPx()
            val tubeBottom = bulbCy - bulbR * 0.6f
            val track = Color(0xFFE2E8F0)
            // Glass.
            drawRoundRect(track, topLeft = Offset(cx - tubeW / 2, tubeTop), size = Size(tubeW, bulbCy - tubeTop), cornerRadius = CornerRadius(tubeW / 2))
            drawCircle(track, radius = bulbR, center = Offset(cx, bulbCy))
            // Mercury.
            val inset = 7.dp.toPx()
            val innerW = tubeW - inset * 2
            val fillTop = tubeBottom - (tubeBottom - tubeTop - inset) * level
            drawRoundRect(mercury.copy(alpha = 0.85f), topLeft = Offset(cx - innerW / 2, fillTop), size = Size(innerW, bulbCy - fillTop), cornerRadius = CornerRadius(innerW / 2))
            drawCircle(mercury.copy(alpha = 0.85f), radius = bulbR - inset, center = Offset(cx, bulbCy))
            // Scale ticks on the right.
            val tickX = cx + tubeW / 2 + 8.dp.toPx()
            val ticks = 24
            for (i in 0..ticks) {
                val y = tubeTop + 8.dp.toPx() + (tubeBottom - tubeTop - 8.dp.toPx()) * i / ticks
                val len = if (i % 4 == 0) 12.dp.toPx() else 7.dp.toPx()
                drawLine(Color(0xFF94A3B8), Offset(tickX, y), Offset(tickX + len, y), strokeWidth = 1.dp.toPx())
            }
        }
        caption?.let { Text(it, color = colors.textGreyColor, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp)) }
    }
}
