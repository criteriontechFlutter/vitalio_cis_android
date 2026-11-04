package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RiceBowl
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.ui.components.AppBottomSheet
import com.critetiontech.ctvitalio.ui.components.AppDialog
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.DietFoodViewModel
import com.critetiontech.ctvitalio.viewmodel.DietPlanBuilderViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import kotlin.math.roundToInt

private val DAY_SHORT = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** Create a plan for the days picked, or view or remove a saved one ([planId] set). */
@Composable
fun DietPlanBuilderScreen(planId: Int?, viewModel: DietPlanBuilderViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val nav = LocalNavController.current
    val readOnly by viewModel.readOnly.collectAsState()
    val name by viewModel.name.collectAsState()
    val day by viewModel.day.collectAsState()
    val selectedDays by viewModel.selectedDays.collectAsState()
    val foods by viewModel.foods.collectAsState()
    val expanded by viewModel.expanded.collectAsState()
    val required by viewModel.requiredKcal.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val error by viewModel.error.collectAsState()
    val saved by viewModel.saved.collectAsState()
    val loadingDays by viewModel.loadingDays.collectAsState()
    val failedDays by viewModel.failedDays.collectAsState()
    val foodsUnavailable by viewModel.foodsUnavailable.collectAsState()

    LaunchedEffect(planId) { viewModel.start(planId) }
    PickedFoodListener { viewModel.addFood(it) }

    var renaming by remember { mutableStateOf(false) }
    var editingRequired by remember { mutableStateOf(false) }
    var needFood by remember { mutableStateOf(false) }
    var confirmRemove by remember { mutableStateOf(false) }

    val dayFoods = if (readOnly) foods[day].orEmpty() else viewModel.selectedFoods(foods, selectedDays)
    // Selected days can differ, so show the fullest one: this is a per-day figure.
    val shownDays = if (readOnly) setOf(day) else selectedDays
    val scheduled = shownDays.maxOf { d -> foods[d].orEmpty().values.sumOf { list -> list.sumOf { it.calories } } }.roundToInt()

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        DietTopBar(title = name, titleColor = if (readOnly) null else colors.textGreyColor.copy(alpha = 0.7f)) {
            if (!readOnly) Icon(
                Icons.Rounded.Edit, contentDescription = stringResource(R.string.diet_rename_plan), tint = colors.textDarkColor,
                modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { renaming = true }.padding(9.dp),
            ) else Icon(
                Icons.Rounded.Delete, contentDescription = stringResource(R.string.diet_remove), tint = DietColors.Red,
                modifier = Modifier.size(40.dp).clip(CircleShape).bounceClick { confirmRemove = true }.padding(9.dp),
            )
        }
        if (readOnly && (day in loadingDays || saving)) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = colors.primaryBlueColor)
        // Day picker
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            DAY_SHORT.forEachIndexed { i, label ->
                val id = i + 1
                val selected = if (readOnly) id == day else id in selectedDays
                val hasFood = foods[id].orEmpty().values.any { it.isNotEmpty() }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(if (selected) colors.primaryBlueColor else Color.Transparent)
                            .bounceClick(pressedScale = 0.9f) { viewModel.selectDay(id) },
                    ) {
                        Text(label, color = if (selected) Color.White else colors.textDarkColor.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                    Box(Modifier.padding(top = 3.dp).size(4.dp).clip(CircleShape).background(if (hasFood && !selected) DietColors.Green else Color.Transparent))
                }
            }
        }
        if (!readOnly && selectedDays.size > 1) {
            Text(
                stringResource(R.string.diet_adding_to_days, selectedDays.size), color = colors.primaryBlueColor, fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        // Scheduled vs required
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(stringResource(R.string.diet_scheduled) + " ", color = colors.textGreyColor, fontSize = 13.sp)
            Text(kcalText(scheduled), color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(" " + stringResource(R.string.diet_kcal_day), color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clip(RoundedCornerShape(6.dp)).bounceClick(enabled = !readOnly) { editingRequired = true }.padding(2.dp),
            ) {
                Text(stringResource(R.string.diet_required) + " ", color = colors.textGreyColor, fontSize = 13.sp)
                Text(required?.let { kcalText(it) } ?: "—", color = colors.primaryBlueColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(" " + stringResource(R.string.diet_kcal_day), color = colors.textGreyColor, fontSize = 13.sp)
                if (!readOnly) Icon(Icons.Rounded.Edit, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.padding(start = 3.dp).size(14.dp))
            }
        }

        if (foodsUnavailable) Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            DietEmptyState(stringResource(R.string.diet_foods_unavailable_title), stringResource(R.string.diet_foods_unavailable_msg))
        } else LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(MealTime.entries, key = { it.id }) { meal ->
                val list = dayFoods[meal].orEmpty()
                MealSectionCard(
                    meal = meal,
                    timeRange = mealTimeRange(meal.defaultStart, meal.defaultEnd),
                    expanded = expanded == meal,
                    onToggle = { viewModel.toggleExpanded(meal) },
                ) {
                    if (list.isEmpty() && readOnly && day !in loadingDays) {
                        Text(stringResource(if (day in failedDays) R.string.diet_load_failed else R.string.diet_nothing_planned), color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(4.dp))
                    }
                    list.forEachIndexed { index, food ->
                        MealFoodRow(
                            name = food.foodName,
                            serving = "${DietFoodViewModel.formatQty(food.quantity)} ${food.unitName}",
                            kcal = stringResource(R.string.diet_kcal_value, kcalText(food.calories)),
                            imageUrl = food.imageUrl,
                        ) {
                            if (!readOnly) Icon(
                                Icons.Rounded.Close, contentDescription = stringResource(R.string.diet_remove), tint = DietColors.Red,
                                modifier = Modifier.size(22.dp).clip(CircleShape).bounceClick { viewModel.removeFood(meal, food) }.padding(2.dp),
                            )
                        }
                    }
                    if (!readOnly) {
                        Text(
                            stringResource(R.string.diet_add_food), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(top = 8.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.primaryBlueColor)
                                .bounceClick { nav.navigate(Routes.dietAddFood(meal.id)) }
                                .padding(horizontal = 18.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (!readOnly) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.navigationBarsPadding().padding(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.dashboardContainerColor)
                        .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                        .bounceClick { nav.popBackStack() },
                ) { Text(stringResource(R.string.diet_cancel), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Medium) }
                DietPrimaryButton(stringResource(R.string.diet_save_plan), modifier = Modifier.weight(1f), loading = saving) {
                    if (viewModel.hasFood) viewModel.save() else needFood = true
                }
            }
        }
    }

    if (renaming) {
        var text by remember { mutableStateOf(name) }
        AppDialog(
            title = stringResource(R.string.diet_rename_plan),
            onDismiss = { renaming = false },
            icon = Icons.Rounded.Edit,
            confirmText = stringResource(R.string.diet_save),
            dismissText = stringResource(R.string.diet_cancel),
            confirmEnabled = text.isNotBlank(),
            onConfirm = { viewModel.rename(text); renaming = false },
        ) {
            MyTextField(value = text, onValueChange = { text = it.take(60) }, placeholderText = stringResource(R.string.diet_rename_plan))
        }
    }
    if (editingRequired) {
        var text by remember { mutableStateOf(required?.toString().orEmpty()) }
        AppDialog(
            title = stringResource(R.string.diet_required_title),
            message = stringResource(R.string.diet_required_msg),
            onDismiss = { editingRequired = false },
            confirmText = stringResource(R.string.diet_save),
            dismissText = stringResource(R.string.diet_cancel),
            confirmEnabled = (text.toIntOrNull() ?: 0) in 800..6000,
            onConfirm = { text.toIntOrNull()?.let(viewModel::setRequiredKcal); editingRequired = false },
        ) {
            MyTextField(
                value = text, onValueChange = { v -> text = v.filter { it.isDigit() }.take(4) },
                placeholderText = "2000",
            )
        }
    }
    if (confirmRemove) {
        AppDialog(
            title = stringResource(R.string.diet_remove_plan_title),
            message = stringResource(R.string.diet_remove_plan_msg),
            onDismiss = { confirmRemove = false },
            confirmText = stringResource(R.string.diet_remove),
            dismissText = stringResource(R.string.diet_cancel),
            destructive = true,
            onConfirm = { confirmRemove = false; viewModel.removePlan { nav.popBackStack() } },
        )
    }
    if (needFood) {
        AppDialog(title = stringResource(R.string.diet_add_some_food), onDismiss = { needFood = false }, icon = Icons.Rounded.RiceBowl)
    }
    error?.let { msg ->
        AppDialog(
            title = stringResource(R.string.diet_save_failed), message = msg, destructive = true,
            onDismiss = viewModel::clearError,
        )
    }
    saved?.let { plan ->
        PlanCreatedSheet(
            planName = plan.name,
            daysPlanned = plan.plannedDays,
            meals = plan.mealCount,
            kcalPerDay = plan.avgCaloriesPerDay.roundToInt(),
            onActive = { viewModel.markSavedActive { nav.popBackStack() } },
            onLater = { viewModel.leaveSavedInactive { nav.popBackStack() } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanCreatedSheet(planName: String, daysPlanned: Int, meals: Int, kcalPerDay: Int, onActive: () -> Unit, onLater: () -> Unit) {
    val colors = LocalMyColorScheme.current
    AppBottomSheet(onDismissRequest = onLater, containerColor = colors.dashboardBackgroundColor) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth()) {
                Icon(
                    Icons.Rounded.Close, contentDescription = null, tint = colors.textDarkColor,
                    modifier = Modifier.align(Alignment.CenterEnd).size(32.dp).clip(CircleShape).background(colors.dashboardContainerColor).bounceClick(onClick = onLater).padding(7.dp),
                )
            }
            SuccessSpinner()
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.diet_plan_created), color = colors.textDarkColor, fontSize = 20.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.diet_plan_saved_msg, planName), color = colors.textGreyColor, fontSize = 13.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(18.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(softTint(colors.primaryBlueColor, DietColors.BlueSoft)).padding(vertical = 16.dp),
            ) {
                StatCell(Icons.Rounded.CalendarMonth, "$daysPlanned", "/7", stringResource(R.string.diet_days_planned), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(56.dp).background(colors.borderGreyLightColor.copy(alpha = 0.4f)))
                StatCell(Icons.Rounded.RiceBowl, "$meals", null, stringResource(R.string.diet_meal_count), Modifier.weight(1f))
                Box(Modifier.width(1.dp).height(56.dp).background(colors.borderGreyLightColor.copy(alpha = 0.4f)))
                StatCell(Icons.Rounded.WaterDrop, kcalText(kcalPerDay), null, stringResource(R.string.diet_kcal_per_day), Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            DietPrimaryButton(stringResource(R.string.diet_mark_active), onClick = onActive)
            Text(
                stringResource(R.string.diet_later), color = colors.textDarkColor, fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 14.dp).clip(RoundedCornerShape(8.dp)).bounceClick(onClick = onLater).padding(horizontal = 20.dp, vertical = 6.dp),
            )
        }
    }
}

/** The spinning green arc at the top of the success sheet. */
@Composable
private fun SuccessSpinner() {
    val angle = remember { Animatable(0f) }
    LaunchedEffect(Unit) { angle.animateTo(360f, infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart)) }
    Canvas(Modifier.size(100.dp)) {
        drawArc(Color(0xFF34D399), angle.value - 90f, 110f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun StatCell(icon: ImageVector, value: String, suffix: String?, label: String, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(colors.dashboardContainerColor), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            if (suffix != null) Text(suffix, color = colors.textGreyColor, fontSize = 12.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
        Text(label, color = colors.textGreyColor, fontSize = 11.sp)
    }
}
