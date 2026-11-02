package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.LunchDining
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.RamenDining
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.NutritionAnalyticsViewModel
import com.critetiontech.ctvitalio.viewmodel.NutritionAnalyticsViewModel.Companion.FIBER_GOAL_G
import com.critetiontech.ctvitalio.viewmodel.NutritionAnalyticsViewModel.Companion.SODIUM_LIMIT_MG
import com.critetiontech.ctvitalio.viewmodel.NutritionAnalyticsViewModel.Companion.SUGAR_LIMIT_G
import com.critetiontech.ctvitalio.viewmodel.NutritionAnalyticsViewModel.Companion.WATER_GOAL_ML
import com.critetiontech.myapplication.utils.LocalNavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.IsoFields
import java.util.Locale
import kotlin.math.roundToInt

private val shortDate = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)

@Composable
fun NutritionAnalyticsScreen(viewModel: NutritionAnalyticsViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val summary by viewModel.summary.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        DietTopBar(title = stringResource(R.string.diet_nutrition_analytics), centered = true)
        val s = summary ?: return@Column
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            WeekPicker(s, onDay = viewModel::selectDate, onWeek = viewModel::shiftWeek)
            AnimatedEnter(index = 0) {
                EnergyIntakeCard(s.consumedKcal, s.goalKcal, s.percent) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(softTint(colors.primaryBlueColor, DietColors.BlueSoft)).padding(vertical = 10.dp),
                    ) {
                        FooterStat(stringResource(R.string.diet_planned), stringResource(R.string.diet_kcal_cap, kcalText(s.plannedKcal)), DietColors.Green, Modifier.weight(1f))
                        FooterStat(stringResource(R.string.diet_remaining), stringResource(R.string.diet_kcal_cap, kcalText(s.leftKcal)), colors.textDarkColor, Modifier.weight(1f))
                        FooterStat(stringResource(R.string.diet_goal), stringResource(R.string.diet_kcal_cap, kcalText(s.goalKcal)), colors.primaryBlueColor, Modifier.weight(1f))
                    }
                }
            }
            AnimatedEnter(index = 1) { WeeklyTrendCard(s) }
            AnimatedEnter(index = 2) { MacroSplitCard(s) }
            AnimatedEnter(index = 3) { MealsBreakdownCard(s) }
            AnimatedEnter(index = 4) { MicrosCard(s) }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun FooterStat(label: String, value: String, valueColor: Color, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = colors.textGreyColor, fontSize = 11.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WeekPicker(s: NutritionAnalyticsViewModel.Summary, onDay: (LocalDate) -> Unit, onWeek: (Long) -> Unit) {
    val colors = LocalMyColorScheme.current
    val today = LocalDate.now()
    val first = s.week.first().date
    val last = s.week.last().date
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RoundArrow(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, true) { onWeek(-1) }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.diet_week_label, first.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR), "${first.format(shortDate)} - ${last.format(shortDate)}"),
                    color = colors.textDarkColor, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                )
            }
            RoundArrow(Icons.AutoMirrored.Rounded.KeyboardArrowRight, last.isBefore(today)) { onWeek(1) }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.week.forEach { d ->
                val selected = d.date == s.date
                val future = d.date.isAfter(today)
                val isToday = d.date == today
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) colors.primaryBlueColor else colors.dashboardContainerColor)
                        .border(1.dp, if (isToday && !selected) colors.primaryBlueColor else Color.Transparent, RoundedCornerShape(12.dp))
                        .bounceClick(enabled = !future, pressedScale = 0.92f) { onDay(d.date) }
                        .padding(vertical = 8.dp),
                ) {
                    val fg = when { selected -> Color.White; isToday -> colors.primaryBlueColor; else -> colors.textDarkColor }
                    Text(d.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH), color = fg.copy(alpha = if (future) 0.4f else 0.85f), fontSize = 12.sp)
                    Text("${d.date.dayOfMonth}", color = fg.copy(alpha = if (future) 0.4f else 1f), fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.size(5.dp).clip(CircleShape).background(
                            when {
                                selected -> Color(0xFF7DD3FC)
                                d.logged -> DietColors.Green
                                else -> colors.borderGreyLightColor.copy(alpha = 0.5f)
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundArrow(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(34.dp).clip(CircleShape).background(colors.dashboardContainerColor).bounceClick(enabled = enabled, onClick = onClick),
    ) { Icon(icon, contentDescription = null, tint = colors.textDarkColor.copy(alpha = if (enabled) 1f else 0.3f), modifier = Modifier.size(22.dp)) }
}

@Composable
private fun WeeklyTrendCard(s: NutritionAnalyticsViewModel.Summary) {
    val colors = LocalMyColorScheme.current
    val max = maxOf(s.goalKcal.toDouble(), s.week.maxOf { it.nutrition.kcal }) * 1.15
    val dashed = if (isAppDark) Color(0xFF93C5FD) else Color(0xFF1E40AF)
    DietCard {
        Text(stringResource(R.string.diet_weekly_trend), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.diet_weekly_avg, kcalText(s.weekAvgKcal), s.adherence), color = colors.textGreyColor, fontSize = 12.sp)
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                s.week.forEach { d ->
                    val kcal = d.nutrition.kcal
                    val selected = d.date == s.date
                    val over = kcal > s.goalKcal * 1.025
                    val barColor = when {
                        selected -> colors.primaryBlueColor
                        over -> Color(0xFFFDBA8C)
                        else -> Color(0xFFBCC8F7)
                    }
                    val h by animateFloatAsState((kcal / max).toFloat(), tween(600), label = "bar")
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom) {
                        if (kcal > 0) Text(
                            kcalText(kcal), fontSize = 10.sp, maxLines = 1,
                            color = when { selected -> colors.primaryBlueColor; over -> Color(0xFFDC2626); else -> colors.textDarkColor.copy(alpha = 0.7f) },
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        )
                        Spacer(Modifier.height(3.dp))
                        Box(Modifier.fillMaxWidth().fillMaxHeight(h.coerceAtLeast(0.02f)).clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)).background(if (kcal > 0) barColor else colors.borderGreyLightColor.copy(alpha = 0.2f)))
                    }
                }
            }
            // Target line
            Canvas(Modifier.fillMaxSize()) {
                val y = size.height * (1f - (s.goalKcal / max).toFloat())
                drawLine(dashed, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            s.week.forEach { d ->
                val selected = d.date == s.date
                Text(
                    d.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale.ENGLISH),
                    color = if (selected) colors.primaryBlueColor else colors.textDarkColor.copy(alpha = 0.8f),
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(colors.dashboardBackgroundColor).padding(horizontal = 6.dp, vertical = 5.dp),
        ) {
            Text(
                stringResource(R.string.diet_target, kcalText(s.goalKcal)), color = colors.primaryBlueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(colors.dashboardContainerColor).padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.weight(1f))
            Canvas(Modifier.width(36.dp).height(2.dp)) {
                drawLine(dashed, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)))
            }
            Spacer(Modifier.width(8.dp))
        }
    }
}

@Composable
private fun MacroSplitCard(s: NutritionAnalyticsViewModel.Summary) {
    val colors = LocalMyColorScheme.current
    val t = s.totals
    val c = t.carbs * 4; val p = t.protein * 4; val f = t.fat * 9
    val total = (c + p + f).takeIf { it > 0 }
    DietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.diet_macro_split), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.diet_budget, s.date.format(shortDate)), color = colors.textGreyColor, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        if (total != null) MacroSplitBar(c / total, p / total, f / total, Triple(DietColors.Carbs, DietColors.Protein, DietColors.Fat))
        else MacroSplitBar(0.0, 0.0, 0.0)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroGoalTile(stringResource(R.string.diet_carbs), t.carbs, s.goalCarbs, c, total, DietColors.Carbs, DietColors.CarbsSoft, Modifier.weight(1f))
            MacroGoalTile(stringResource(R.string.diet_protein), t.protein, s.goalProtein, p, total, DietColors.Protein, DietColors.ProteinSoft, Modifier.weight(1f))
            MacroGoalTile(stringResource(R.string.diet_fat), t.fat, s.goalFat, f, total, DietColors.Fat, DietColors.FatSoft, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        val good = s.proteinGoalDays >= 4
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(softTint(colors.primaryBlueColor, DietColors.BlueSoft)).padding(12.dp),
        ) {
            Box(Modifier.size(36.dp).clip(CircleShape).background(Color(0xFF6EE7B7)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.MilitaryTech, contentDescription = null, tint = Color(0xFF065F46), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(10.dp))
            Text(
                buildString {
                    append(stringResource(if (good) R.string.diet_consistency_title else R.string.diet_keep_going_title)); append(' ')
                    append(stringResource(R.string.diet_protein_days_msg, s.proteinGoalDays))
                },
                color = colors.textDarkColor, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun MacroGoalTile(label: String, grams: Double, goal: Double, kcal: Double, totalKcal: Double?, accent: Color, bg: Color, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(softTint(accent, bg))
            .border(1.dp, accent.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(4.dp))
            Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(gramsText(grams).substringBefore('.'), color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("g", color = colors.textGreyColor, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
        Text("/ ${goal.roundToInt()}g", color = colors.textGreyColor, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        SlimBar((grams / goal).toFloat(), accent, track = Color.White)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.diet_kcal_cap, kcalText(kcal)), color = accent, fontSize = 11.sp)
        Text("(${if (totalKcal == null) 0 else (kcal * 100 / totalKcal).roundToInt()}%)", color = accent, fontSize = 11.sp)
    }
}

/** Meal slots grouped as the analytics design shows them. */
private enum class MealGroup(val slots: List<MealTime>, val icon: ImageVector) {
    BREAKFAST(listOf(MealTime.MORNING), Icons.Rounded.WbSunny),
    LUNCH(listOf(MealTime.AFTERNOON), Icons.Rounded.LunchDining),
    DINNER(listOf(MealTime.NIGHT), Icons.Rounded.RamenDining),
    SNACKS(listOf(MealTime.MID_MORNING, MealTime.EVENING), Icons.Rounded.Cookie),
}

@Composable
private fun MealsBreakdownCard(s: NutritionAnalyticsViewModel.Summary) {
    val colors = LocalMyColorScheme.current
    val total = s.totals.kcal.takeIf { it > 0 } ?: 1.0
    val loggedGroups = MealGroup.entries.count { g -> g.slots.any { s.meals[it.id].orEmpty().isNotEmpty() } }
    DietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.diet_meals_breakdown), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.diet_logged, loggedGroups), color = colors.primaryBlueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MealGroup.entries.forEach { g ->
                val entries: List<DietLogEntry> = g.slots.flatMap { s.meals[it.id].orEmpty() }
                val kcal = entries.sumOf { it.nutrition.kcal }
                val label = stringResource(
                    when (g) {
                        MealGroup.BREAKFAST -> R.string.diet_breakfast
                        MealGroup.LUNCH -> R.string.diet_lunch
                        MealGroup.DINNER -> R.string.diet_dinner
                        MealGroup.SNACKS -> R.string.diet_snacks
                    }
                )
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(softTint(colors.primaryBlueColor, Color(0xFFF1F5FD))).padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(8.dp)).background(softTint(colors.primaryBlueColor, Color(0xFFDCE7FB))), contentAlignment = Alignment.Center) {
                            Icon(g.icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(label, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            Text(
                                entries.takeIf { it.isNotEmpty() }?.joinToString(", ") { it.foodName } ?: stringResource(R.string.diet_nothing_logged),
                                color = colors.textGreyColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.diet_kcal_cap, kcalText(kcal)), color = colors.textDarkColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text("${(kcal * 100 / total).roundToInt()}%", color = colors.textGreyColor, fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    SlimBar((kcal / total).toFloat(), colors.primaryBlueColor, height = 4.dp)
                }
            }
        }
    }
}

@Composable
private fun MicrosCard(s: NutritionAnalyticsViewModel.Summary) {
    val colors = LocalMyColorScheme.current
    val t = s.totals
    val fiberPct = (t.fiber / FIBER_GOAL_G * 100).roundToInt()
    val waterL = s.waterMl / 1000
    val waterPct = (s.waterMl / WATER_GOAL_ML * 100).roundToInt()
    val sugarOk = t.sugar <= SUGAR_LIMIT_G
    val sodiumOk = t.sodium <= SODIUM_LIMIT_MG
    // Each part scores up to 25: fiber and water by progress, sugar and sodium by staying under the limit.
    val score = (minOf(fiberPct, 100) + minOf(waterPct, 100) + (if (sugarOk) 100 else 40) + (if (sodiumOk) 100 else 40)) / 4
    DietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.MonitorHeart, contentDescription = null, tint = DietColors.Protein, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.diet_micros), color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(softTint(DietColors.Green, Color(0xFFBBF7D0))).padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(stringResource(R.string.diet_score), color = DietColors.Protein, fontSize = 10.sp)
                Text("$score/100", color = DietColors.Protein, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MicroTile(stringResource(R.string.diet_dietary_fiber), "$fiberPct%", DietColors.Protein, gramsText(t.fiber).substringBefore('.'), "/ ${FIBER_GOAL_G.toInt()}g", (t.fiber / FIBER_GOAL_G).toFloat(), DietColors.Protein, Modifier.weight(1f))
            MicroTile(stringResource(R.string.diet_hydration), "$waterPct%", colors.primaryBlueColor, String.format(Locale.US, "%.1f", waterL), "/ ${String.format(Locale.US, "%.1f", WATER_GOAL_ML / 1000)} L", (s.waterMl / WATER_GOAL_ML).toFloat(), colors.primaryBlueColor, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MicroTile(
                stringResource(R.string.diet_added_sugar), stringResource(if (sugarOk) R.string.diet_within_limit else R.string.diet_over_limit),
                if (sugarOk) DietColors.Protein else DietColors.Red, gramsText(t.sugar).substringBefore('.'), "/ <${SUGAR_LIMIT_G.toInt()}g",
                (t.sugar / SUGAR_LIMIT_G).toFloat(), if (sugarOk) DietColors.Protein else DietColors.Red, Modifier.weight(1f),
            )
            MicroTile(
                stringResource(R.string.diet_sodium), stringResource(if (sodiumOk) R.string.diet_optimal else R.string.diet_high),
                if (sodiumOk) colors.primaryBlueColor else DietColors.Red, kcalText(t.sodium), "/ ${kcalText(SODIUM_LIMIT_MG)}mg",
                (t.sodium / SODIUM_LIMIT_MG).toFloat(), if (sodiumOk) colors.primaryBlueColor else DietColors.Red, Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MicroTile(label: String, badge: String, badgeColor: Color, value: String, of: String, progress: Float, barColor: Color, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(softTint(colors.primaryBlueColor, Color(0xFFF1F5FD))).padding(12.dp)) {
        Row(verticalAlignment = Alignment.Top) {
            Text(label, color = colors.textGreyColor, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text(badge, color = badgeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(64.dp))
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = colors.textDarkColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Text(of, color = colors.textGreyColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 3.dp))
        }
        Spacer(Modifier.height(6.dp))
        SlimBar(progress, barColor, height = 6.dp)
    }
}

/** "Daily Diet Overview" on Home: today's calories against the goal; opens the Diet Checklist. */
@Composable
fun DailyDietOverview(viewModel: NutritionAnalyticsViewModel = viewModel(key = "homeDietOverview")) {
    val nav = LocalNavController.current
    val summary by viewModel.summary.collectAsState()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.selectDate(LocalDate.now()) }
    val s = summary ?: return
    Column {
        Text(stringResource(R.string.diet_daily_overview), style = com.critetiontech.ctvitalio.utils.AppTextStyles.style18BCB())
        Spacer(Modifier.height(12.dp))
        Box(Modifier.clip(RoundedCornerShape(18.dp)).bounceClick(pressedScale = 0.98f) { nav.navigate(Routes.DIETCHECKLIST) }) {
            EnergyIntakeCard(s.consumedKcal, s.goalKcal, s.percent)
        }
    }
}
