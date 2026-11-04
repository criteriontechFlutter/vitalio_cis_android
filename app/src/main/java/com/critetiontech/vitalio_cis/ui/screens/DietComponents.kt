package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.myapplication.utils.LocalNavController
import java.text.NumberFormat
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Shared pieces of the diet screens: Diet Checklist, Diet Box, plan builder, Add Food,
// Nutrition Facts, Nutrition Analytics and the Home overview card.

internal object DietColors {
    val Green = Color(0xFF16A34A)
    val GreenSoft = Color(0xFFDCFCE7)
    val Orange = Color(0xFFF59E0B)
    val Red = Color(0xFFDC2626)
    val Carbs = Color(0xFFC2570C)
    val Protein = Color(0xFF15803D)
    val Fat = Color(0xFFB91C1C)
    val CarbsSoft = Color(0xFFFFF4EC)
    val ProteinSoft = Color(0xFFE3FBEF)
    val FatSoft = Color(0xFFFFEEEE)
    val BlueSoft = Color(0xFFEAF2FE)
    val BlueBorder = Color(0xFFB9D2F8)
    val Track = Color(0xFFDDE7F8)
}

@Composable
internal fun mealLabel(meal: MealTime): String = stringResource(
    when (meal) {
        MealTime.MORNING -> R.string.diet_meal_morning
        MealTime.MID_MORNING -> R.string.diet_meal_mid_morning
        MealTime.AFTERNOON -> R.string.diet_meal_afternoon
        MealTime.EVENING -> R.string.diet_meal_evening
        MealTime.NIGHT -> R.string.diet_meal_night
    }
)

internal fun mealIcon(meal: MealTime): Int = when (meal) {
    MealTime.MORNING -> R.drawable.ic_meal_morning
    MealTime.MID_MORNING -> R.drawable.ic_meal_mid_morning
    MealTime.AFTERNOON -> R.drawable.ic_meal_afternoon
    MealTime.EVENING -> R.drawable.ic_meal_evening
    MealTime.NIGHT -> R.drawable.ic_meal_night
}

private val serverTime = DateTimeFormatter.ofPattern("HH:mm[:ss]")
private val shownTime = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH)

/** "07:00:00", "19:30:00" → "07:00 AM - 07:30 PM". */
internal fun mealTimeRange(start: String, end: String): String {
    fun f(t: String) = runCatching { LocalTime.parse(t.trim(), serverTime).format(shownTime) }.getOrDefault(t)
    return "${f(start)} - ${f(end)}"
}

internal fun kcalText(v: Number): String = NumberFormat.getIntegerInstance(Locale.US).format(Math.round(v.toDouble()))

internal fun gramsText(v: Double): String =
    if (v >= 100 || v % 1.0 == 0.0) Math.round(v).toString() else String.format(Locale.US, "%.1f", v)

enum class MealStatus { DONE, PARTIAL, PENDING }

/** Top bar of the diet screens: back arrow, title (+ optional subtitle), [actions] on the right. */
@Composable
internal fun DietTopBar(
    title: String,
    subtitle: String? = null,
    centered: Boolean = false,
    onBack: (() -> Unit)? = null,
    titleColor: Color? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val colors = LocalMyColorScheme.current
    val nav = LocalNavController.current
    Box(
        Modifier
            .fillMaxWidth()
            .background(colors.dashboardBackgroundColor)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(40.dp)
                .clip(CircleShape)
                .background(if (centered) colors.dashboardContainerColor else Color.Transparent)
                .bounceClick { onBack?.invoke() ?: nav.popBackStack() },
        ) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null, tint = colors.textDarkColor, modifier = Modifier.size(22.dp))
        }
        Column(
            horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
            modifier = Modifier
                .align(if (centered) Alignment.Center else Alignment.CenterStart)
                .padding(start = if (centered) 56.dp else 52.dp, end = 56.dp),
        ) {
            Text(
                title, color = titleColor ?: colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = if (centered) TextAlign.Center else TextAlign.Start,
            )
            if (subtitle != null) Text(subtitle, color = colors.textGreyColor, fontSize = 12.sp)
        }
        Row(Modifier.align(Alignment.CenterEnd), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

/** A white rounded card, the building block of every diet screen. */
@Composable
internal fun DietCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardContainerColor)
            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(padding),
        content = content,
    )
}

/**
 * One meal slot: icon, name, status, time and a chevron. Expanded, it turns light blue and shows
 * [content] (the foods) under a divider.
 */
@Composable
internal fun MealSectionCard(
    meal: MealTime,
    timeRange: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    status: MealStatus? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val bg by animateColorAsState(
        if (expanded) softTint(colors.primaryBlueColor, DietColors.BlueSoft) else colors.dashboardContainerColor, label = "mealBg",
    )
    val border = if (expanded) colors.primaryBlueColor.copy(alpha = if (isAppDark) 0.5f else 0.3f) else colors.borderGreyLightColor.copy(alpha = 0.25f)
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, tween(220), label = "chevron")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(pressedScale = 0.99f, onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        ) {
            Image(painterResource(mealIcon(meal)), contentDescription = null, modifier = Modifier.size(width = 50.dp, height = 40.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mealLabel(meal), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    if (status != null) {
                        Spacer(Modifier.width(8.dp))
                        MealStatusIcon(status)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = colors.textDarkColor.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timeRange, color = colors.textDarkColor.copy(alpha = 0.7f), fontSize = 11.sp)
                }
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = colors.textGreyColor,
                modifier = Modifier.size(24.dp).rotate(chevron),
            )
        }
        AnimatedVisibility(expanded, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp)) {
                HorizontalDivider(color = colors.primaryBlueColor.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 10.dp))
                content()
            }
        }
    }
}

@Composable
private fun MealStatusIcon(status: MealStatus) {
    when (status) {
        MealStatus.DONE -> Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = DietColors.Green, modifier = Modifier.size(18.dp))
        MealStatus.PARTIAL -> Canvas(Modifier.size(16.dp)) {
            val stroke = 2.dp.toPx()
            drawArc(DietColors.Orange, -90f, 260f, false, style = Stroke(stroke, cap = StrokeCap.Round))
            drawCircle(DietColors.Orange, radius = size.minDimension / 6)
        }
        MealStatus.PENDING -> Icon(Icons.Rounded.Schedule, contentDescription = null, tint = Color(0xFFA0AABB), modifier = Modifier.size(17.dp))
    }
}

/** A food row inside an expanded meal: image, name, serving, kcal and a trailing control. */
@Composable
internal fun MealFoodRow(
    name: String,
    serving: String,
    kcal: String,
    imageUrl: String? = null,
    tag: String? = null,
    trailing: @Composable () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.dashboardContainerColor)
            .padding(horizontal = 10.dp, vertical = 10.dp),
    ) {
        FoodThumb(imageUrl, size = 22.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = colors.textDarkColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                Spacer(Modifier.width(5.dp))
                Text(serving, color = colors.textGreyColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            }
            if (tag != null) Text(tag, color = colors.primaryBlueColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(8.dp))
        Text(kcal, color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 11.sp)
        Spacer(Modifier.width(10.dp))
        trailing()
    }
}

/** Food photo from NutriAnalyser, or a plate icon when there is none. */
@Composable
internal fun FoodThumb(url: String?, size: Dp, corner: Dp = size / 4) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(corner))
            .background(softTint(DietColors.Orange, Color(0xFFFFF3E0))),
    ) {
        Icon(Icons.Rounded.Restaurant, contentDescription = null, tint = DietColors.Orange, modifier = Modifier.size(size * 0.55f))
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url.replace(" ", "%20"), contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(RoundedCornerShape(corner)),
            )
        }
    }
}

/** Blue progress ring with the percentage in the middle. */
@Composable
internal fun CalorieRing(percent: Int, size: Dp = 76.dp, stroke: Dp = 7.dp) {
    val colors = LocalMyColorScheme.current
    val sweep by animateFloatAsState((percent.coerceIn(0, 100) / 100f) * 360f, tween(700), label = "ring")
    val ringColor = if (percent > 110) DietColors.Orange else colors.primaryBlueColor
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Canvas(Modifier.size(size)) {
            val s = stroke.toPx()
            drawArc(ringColor.copy(alpha = 0.15f), 0f, 360f, false, style = Stroke(s))
            drawArc(ringColor, -90f, sweep, false, style = Stroke(s, cap = StrokeCap.Round))
        }
        Text("$percent%", color = ringColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

/** The "Total Energy Intake" card used on Home and Nutrition Analytics. */
@Composable
internal fun EnergyIntakeCard(
    consumed: Int,
    goal: Int,
    percent: Int,
    modifier: Modifier = Modifier,
    footer: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = LocalMyColorScheme.current
    DietCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.diet_total_energy), color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            val over = consumed > goal
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(softTint(if (over) DietColors.Orange else DietColors.Green, if (over) Color(0xFFFFF3E0) else DietColors.GreenSoft))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Box(Modifier.size(6.dp).clip(CircleShape).background(if (over) DietColors.Orange else DietColors.Green))
                Spacer(Modifier.width(5.dp))
                Text(
                    stringResource(if (over) R.string.diet_kcal_over else R.string.diet_kcal_left, kcalText(kotlin.math.abs(goal - consumed))),
                    color = if (over) Color(0xFFB45309) else DietColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(kcalText(consumed), color = colors.textDarkColor, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(4.dp))
                    Text("/ ${kcalText(goal)}", color = colors.textGreyColor, fontSize = 15.sp, modifier = Modifier.padding(bottom = 5.dp))
                }
                Text(stringResource(R.string.diet_kcal_consumed, percent), color = colors.textGreyColor, fontSize = 11.sp)
            }
            CalorieRing(percent, stroke = 10.dp)
        }
        if (footer != null) {
            Spacer(Modifier.height(14.dp))
            footer()
        }
    }
}

/** A thin rounded progress bar. */
@Composable
internal fun SlimBar(progress: Float, color: Color, modifier: Modifier = Modifier, track: Color = DietColors.Track, height: Dp = 5.dp) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(if (isAppDark) track.copy(alpha = 0.15f) else track)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(CircleShape).background(color))
    }
}

/** Full-width blue pill button pinned at the bottom of a screen. */
@Composable
internal fun DietPrimaryButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) colors.primaryBlueColor else colors.primaryBlueColor.copy(alpha = 0.4f))
            .bounceClick(enabled = enabled && !loading, onClick = onClick),
    ) {
        if (loading) androidx.compose.material3.CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Centered empty state for diet lists. */
@Composable
internal fun DietEmptyState(title: String, message: String, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    Column(modifier.fillMaxWidth().padding(vertical = 40.dp, horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Image(painterResource(R.drawable.ic_meal_mid_morning), contentDescription = null, modifier = Modifier.size(width = 84.dp, height = 66.dp))
        Spacer(Modifier.height(14.dp))
        Text(title, color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(message, color = colors.textGreyColor, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}
