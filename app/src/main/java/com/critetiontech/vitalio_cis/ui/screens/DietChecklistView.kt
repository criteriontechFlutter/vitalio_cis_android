package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.DietChecklistViewModel
import com.critetiontech.ctvitalio.viewmodel.DietFoodViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

internal const val PICKED_FOOD_KEY = "diet_picked_food"

/** Receives the food chosen on Add Food → Nutrition Facts, once, on the screen that opened it. */
@Composable
internal fun PickedFoodListener(onPicked: (PickedFood) -> Unit) {
    val nav = LocalNavController.current
    val entry = remember { nav.currentBackStackEntry } ?: return
    val json by entry.savedStateHandle.getStateFlow<String?>(PICKED_FOOD_KEY, null).collectAsState()
    LaunchedEffect(json) {
        val value = json ?: return@LaunchedEffect
        entry.savedStateHandle[PICKED_FOOD_KEY] = null
        runCatching { Gson().fromJson(value, PickedFood::class.java) }.getOrNull()?.let(onPicked)
    }
}

private val dayFormat = DateTimeFormatter.ofPattern("EEE dd MMM", Locale.ENGLISH)

@Composable
fun DietChecklistScreen(viewModel: DietChecklistViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val nav = LocalNavController.current
    val date by viewModel.date.collectAsState()
    val plan by viewModel.plan.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val logs by viewModel.logs.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val expanded by viewModel.expanded.collectAsState()

    // Reload on every return (a plan may have been created or made active in Diet Box).
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    PickedFoodListener { viewModel.addOutsideFood(it) }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        DietTopBar(title = stringResource(R.string.diet_checklist_title)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .bounceClick { nav.navigate(Routes.DIET_BOX) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.diet_box), color = colors.primaryBlueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Inventory2, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
            }
        }
        DateStrip(date, onShift = viewModel::shiftDate)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = colors.primaryBlueColor)

        AnimatedContent(
            targetState = date,
            transitionSpec = {
                val dir = if (targetState.isAfter(initialState)) 1 else -1
                (slideInHorizontally { it / 4 * dir } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 * dir } + fadeOut())
            },
            label = "dietDay",
            modifier = Modifier.weight(1f),
        ) { day ->
            if (plan == null && !loading) {
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                    DietEmptyState(stringResource(R.string.diet_no_plan_title), stringResource(R.string.diet_no_plan_msg))
                    DietPrimaryButton(
                        stringResource(R.string.diet_create_plan),
                        modifier = Modifier.padding(horizontal = 40.dp),
                    ) { nav.navigate(Routes.DIET_PLAN_BUILDER) }
                }
                return@AnimatedContent
            }
            val meals = viewModel.mealsFor(plan, day)
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 12.dp, bottom = 24.dp),
            ) {
                items(meals, key = { it.mealTime.id }) { meal ->
                    val extras = logs.filter { !it.fromPlan && it.mealTimeId == meal.mealTime.id }
                    val planned = meal.foods.size
                    val ticked = meal.foods.count { f -> logs.any { it.key == viewModel.planKey(f) } }
                    val status = when {
                        planned == 0 && extras.isEmpty() -> MealStatus.PENDING
                        ticked == planned -> MealStatus.DONE
                        ticked > 0 || extras.isNotEmpty() -> MealStatus.PARTIAL
                        else -> MealStatus.PENDING
                    }
                    MealSectionCard(
                        meal = meal.mealTime,
                        timeRange = mealTimeRange(meal.startTime, meal.endTime),
                        expanded = expanded == meal.mealTime,
                        onToggle = { viewModel.toggleExpanded(meal.mealTime) },
                        status = status,
                    ) {
                        if (meal.foods.isEmpty() && extras.isEmpty()) {
                            Text(
                                stringResource(R.string.diet_nothing_planned), color = colors.textGreyColor, fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                            )
                        }
                        meal.foods.forEach { food ->
                            val key = viewModel.planKey(food)
                            MealFoodRow(
                                name = food.foodName,
                                serving = "${DietFoodViewModel.formatQty(food.quantity)} ${food.unitName}",
                                kcal = stringResource(R.string.diet_kcal_minus, kcalText(food.calories)),
                                imageUrl = food.imageUrl,
                            ) {
                                TickBox(
                                    checked = logs.any { it.key == key },
                                    busy = key in saving,
                                    enabled = viewModel.canTick,
                                ) { viewModel.togglePlanFood(meal, food) }
                            }
                        }
                        extras.forEach { entry -> OutsideFoodRow(entry) { viewModel.removeOutsideFood(entry.key) } }
                        if (viewModel.canTick) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .bounceClick { nav.navigate(Routes.dietAddFood(meal.mealTime.id)) }
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.diet_food_outside_plan), color = colors.primaryBlueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Text(
                                stringResource(R.string.diet_future_hint), color = colors.textGreyColor, fontSize = 11.sp,
                                modifier = Modifier.align(Alignment.End).padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
        }

        DietPrimaryButton(
            stringResource(R.string.diet_nutrition_analytics),
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        ) { nav.navigate(Routes.NUTRITION_ANALYTICS) }
    }
}

/** "Sun 21 Jun   Today   Tue 23 Jun": tap a side to move a day. */
@Composable
private fun DateStrip(date: LocalDate, onShift: (Long) -> Unit) {
    val colors = LocalMyColorScheme.current
    val today = LocalDate.now()
    fun label(d: LocalDate) = if (d == today) null else d.format(dayFormat).replaceFirst(" ", " ")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        val prev = date.minusDays(1)
        val next = date.plusDays(1)
        Text(
            label(prev) ?: stringResource(R.string.diet_today),
            color = colors.textGreyColor.copy(alpha = 0.55f), fontSize = 14.sp,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).bounceClick { onShift(-1) }.padding(vertical = 6.dp),
        )
        Text(
            label(date) ?: stringResource(R.string.diet_today),
            color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.weight(1f),
        )
        Text(
            label(next) ?: stringResource(R.string.diet_today),
            color = colors.textGreyColor.copy(alpha = if (next.isAfter(today.plusDays(6))) 0.25f else 0.55f), fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.End,
            modifier = Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).bounceClick { onShift(1) }.padding(vertical = 6.dp),
        )
    }
}

/** Square checkbox: green when eaten, a spinner while saving. */
@Composable
private fun TickBox(checked: Boolean, busy: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(20.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(if (checked) Color(0xFF22C55E) else Color.Transparent)
            .border(1.5.dp, if (checked) Color(0xFF22C55E) else colors.textGreyColor.copy(alpha = if (enabled) 0.6f else 0.25f), RoundedCornerShape(4.dp))
            .bounceClick(enabled = enabled && !busy, pressedScale = 0.85f, onClick = onClick),
    ) {
        when {
            busy -> CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 1.5.dp, color = colors.primaryBlueColor)
            checked -> Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
        }
    }
}

@Composable
private fun OutsideFoodRow(entry: DietLogEntry, onRemove: () -> Unit) {
    val colors = LocalMyColorScheme.current
    MealFoodRow(
        name = entry.foodName,
        serving = "${DietFoodViewModel.formatQty(entry.quantity)} ${entry.unitName}",
        kcal = stringResource(R.string.diet_kcal_minus, kcalText(entry.nutrition.kcal)),
        tag = stringResource(R.string.diet_outside_plan_tag),
    ) {
        Icon(
            Icons.Rounded.Close, contentDescription = stringResource(R.string.diet_remove), tint = colors.textGreyColor,
            modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).bounceClick(onClick = onRemove),
        )
    }
}

/** The meal slot a picked food goes into, for subtitles like "Morning". */
@Composable
internal fun mealLabelOf(mealTimeId: Int): String = mealLabel(MealTime.of(mealTimeId))
