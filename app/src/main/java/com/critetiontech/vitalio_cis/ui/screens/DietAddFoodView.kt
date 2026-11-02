package com.critetiontech.ctvitalio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.model.RecentFood
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.DietFoodViewModel
import com.critetiontech.ctvitalio.viewmodel.DietFoodViewModel.Companion.formatQty
import com.critetiontech.ctvitalio.viewmodel.DietFoodViewModel.Companion.isGrams
import com.critetiontech.myapplication.utils.LocalNavController
import com.google.gson.Gson
import kotlin.math.roundToInt

private val TRENDING = listOf("Oatmeal", "Greek Yogurt", "Boiled Egg", "Paneer Tikka", "Banana", "Chicken Breast")

/**
 * Add Food for one meal slot: search → Nutrition Facts → the chosen food goes back to the screen
 * that opened this one (plan builder or Diet Checklist) through [PICKED_FOOD_KEY].
 */
@Composable
fun DietAddFoodScreen(mealTimeId: Int, viewModel: DietFoodViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val nav = LocalNavController.current
    val selected by viewModel.selected.collectAsState()
    val meal = mealLabelOf(mealTimeId)

    fun finish(food: PickedFood?) {
        food ?: return
        nav.previousBackStackEntry?.savedStateHandle?.set(PICKED_FOOD_KEY, Gson().toJson(food))
        nav.popBackStack()
    }

    BackHandler(enabled = selected != null) { viewModel.clearSelection() }

    Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        DietTopBar(
            title = stringResource(if (selected == null) R.string.diet_add_food else R.string.diet_nutrition_facts),
            subtitle = if (selected == null) meal else null,
            centered = true,
            onBack = if (selected != null) ({ viewModel.clearSelection() }) else null,
        )
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            contentKey = { it?.foodId },
            label = "addFoodPage",
            modifier = Modifier.weight(1f),
        ) { food ->
            if (food == null) {
                SearchPage(
                    viewModel,
                    onQuickAdd = { finish(viewModel.quickPick(mealTimeId, it)) },
                    onQuickAddRecent = { finish(viewModel.quickPickRecent(mealTimeId, it)) },
                )
            } else {
                FactsPage(food, meal, viewModel) { finish(viewModel.pickSelected(mealTimeId)) }
            }
        }
    }
}

// ── Search ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchPage(
    viewModel: DietFoodViewModel,
    onQuickAdd: (NutriFood) -> Unit,
    onQuickAddRecent: (RecentFood) -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val error by viewModel.searchError.collectAsState()
    val previews by viewModel.previews.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val sort by viewModel.sort.collectAsState()
    val recent by viewModel.recent.collectAsState()
    val typed = query.trim().length >= DietFoodViewModel.MIN_CHARS

    Column(Modifier.fillMaxSize()) {
        SearchField(query, searching, viewModel::onQueryChange)

        if (!typed) {
            var showAll by remember { mutableStateOf(false) }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    SectionHeader(Icons.AutoMirrored.Rounded.TrendingUp, stringResource(R.string.diet_trending)) {
                        Text(stringResource(R.string.diet_popular_today), color = colors.textGreyColor, fontSize = 11.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TRENDING.forEach { t ->
                            Text(
                                t, color = colors.textDarkColor, fontSize = 12.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(colors.dashboardContainerColor)
                                    .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.5f), RoundedCornerShape(50))
                                    .bounceClick { viewModel.onQueryChange(t) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                }
                if (recent.isNotEmpty()) {
                    item {
                        SectionHeader(Icons.Rounded.StarOutline, stringResource(R.string.diet_recent)) {
                            if (recent.size > 3) Text(
                                stringResource(if (showAll) R.string.diet_show_less else R.string.diet_view_all),
                                color = colors.primaryBlueColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clip(RoundedCornerShape(6.dp)).bounceClick { showAll = !showAll }.padding(4.dp),
                            )
                        }
                    }
                    items(if (showAll) recent else recent.take(3), key = { "r${it.foodId}" }) { r ->
                        FoodResultRow(
                            name = r.foodName,
                            imageUrl = r.imageUrl,
                            serving = "${formatQty(r.quantity)} ${r.unitName}",
                            nutrition = r.nutrition,
                            onOpen = { viewModel.selectRecent(r) },
                            onAdd = { onQuickAddRecent(r) },
                        )
                    }
                }
            }
            return@Column
        }

        val shown = viewModel.visible(results, previews, filter, sort)
        if (results.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                FilterChip(stringResource(R.string.diet_filter_all, results.size), filter == DietFoodViewModel.Filter.ALL) { viewModel.setFilter(DietFoodViewModel.Filter.ALL) }
                FilterChip(stringResource(R.string.diet_filter_high_protein), filter == DietFoodViewModel.Filter.HIGH_PROTEIN) { viewModel.setFilter(DietFoodViewModel.Filter.HIGH_PROTEIN) }
                FilterChip(stringResource(R.string.diet_filter_low_cal), filter == DietFoodViewModel.Filter.LOW_CALORIE) { viewModel.setFilter(DietFoodViewModel.Filter.LOW_CALORIE) }
            }
            HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.3f))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    stringResource(R.string.diet_found_items, shown.size, query.trim()), color = colors.textGreyColor, fontSize = 12.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                SortMenu(sort, viewModel::setSort)
            }
        }
        when {
            error != null && results.isEmpty() -> DietEmptyState(error.orEmpty(), "")
            results.isEmpty() && !searching -> Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.SearchOff, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.diet_no_results, query.trim()), color = colors.textGreyColor, fontSize = 14.sp)
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(shown, key = { it.foodId }) { food ->
                    LaunchedEffect(food.foodId) { viewModel.requestPreview(food) }
                    val p = previews[food.foodId]
                    FoodResultRow(
                        name = food.displayName,
                        imageUrl = food.foodImagePath,
                        serving = p?.let { "${formatQty(it.quantity)} ${it.unit.unitName}" },
                        nutrition = p?.nutrition,
                        onOpen = { viewModel.select(food) },
                        onAdd = { if (p != null) onQuickAdd(food) else viewModel.select(food) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, searching: Boolean, onChange: (String) -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(colors.dashboardContainerColor)
            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text(stringResource(R.string.diet_search_hint), color = colors.textGreyColor.copy(alpha = 0.6f), fontSize = 15.sp)
            BasicTextField(
                value = query, onValueChange = onChange, singleLine = true,
                textStyle = TextStyle(color = colors.textDarkColor, fontSize = 15.sp),
                cursorBrush = SolidColor(colors.primaryBlueColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when {
            searching -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
            query.isNotEmpty() -> Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp).clip(CircleShape).background(colors.textGreyColor.copy(alpha = 0.15f)).bounceClick { onChange("") },
            ) { Icon(Icons.Rounded.Close, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(16.dp)) }
        }
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, trailing: @Composable () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title.uppercase(), color = colors.textGreyColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.6.sp, modifier = Modifier.weight(1f))
        trailing()
    }
}

@Composable
private fun FilterChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Text(
        text,
        color = if (selected) colors.primaryBlueColor else colors.textDarkColor,
        fontSize = 12.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) softTint(colors.primaryBlueColor, DietColors.BlueSoft) else colors.dashboardContainerColor)
            .border(1.dp, if (selected) colors.primaryBlueColor else colors.borderGreyLightColor.copy(alpha = 0.5f), RoundedCornerShape(50))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun SortMenu(sort: DietFoodViewModel.Sort, onSort: (DietFoodViewModel.Sort) -> Unit) {
    val colors = LocalMyColorScheme.current
    var open by remember { mutableStateOf(false) }
    @Composable fun label(s: DietFoodViewModel.Sort) = stringResource(
        when (s) {
            DietFoodViewModel.Sort.POPULARITY -> R.string.diet_sort_popularity
            DietFoodViewModel.Sort.CALORIES -> R.string.diet_sort_calories
            DietFoodViewModel.Sort.PROTEIN -> R.string.diet_sort_protein
        }
    )
    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clip(RoundedCornerShape(6.dp)).bounceClick { open = true }.padding(4.dp),
        ) {
            Text(label(sort), color = colors.primaryBlueColor, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DietFoodViewModel.Sort.entries.forEach { s ->
                DropdownMenuItem(text = { Text(label(s)) }, onClick = { onSort(s); open = false })
            }
        }
    }
}

/** A search/recent row: photo, name, serving and the kcal · protein · fat line, + and chevron. */
@Composable
private fun FoodResultRow(
    name: String,
    imageUrl: String?,
    serving: String?,
    nutrition: FoodNutrition?,
    onOpen: () -> Unit,
    onAdd: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val highProtein = (nutrition?.protein ?: 0.0) >= 10.0
    val heavy = (nutrition?.kcal ?: 0.0) >= 300.0
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.dashboardContainerColor)
            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
            .bounceClick(pressedScale = 0.98f, onClick = onOpen)
            .padding(10.dp),
    ) {
        FoodThumb(imageUrl, size = 52.dp, corner = 10.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (highProtein) {
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.diet_protein).uppercase(), color = DietColors.Green, fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp,
                        modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(softTint(DietColors.Green, DietColors.GreenSoft)).border(1.dp, DietColors.Green.copy(alpha = 0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(serving ?: "…", color = colors.textGreyColor, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(3.dp))
            if (nutrition != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.diet_kcal_value, kcalText(nutrition.kcal)),
                        color = if (heavy) Color(0xFFD97706) else colors.primaryBlueColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Dot()
                    Text(stringResource(R.string.diet_protein_g, gramsText(nutrition.protein)), color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 11.sp)
                    Dot()
                    if (nutrition.fat >= nutrition.carbs / 3) Text(stringResource(R.string.diet_fat_g, gramsText(nutrition.fat)), color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 11.sp)
                    else Text(stringResource(R.string.diet_carbs_g, gramsText(nutrition.carbs)), color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            } else {
                Box(Modifier.width(140.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(colors.textGreyColor.copy(alpha = 0.12f)))
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(CircleShape).background(softTint(colors.primaryBlueColor, DietColors.BlueSoft)).bounceClick(pressedScale = 0.85f, onClick = onAdd),
        ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp)) }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = colors.textGreyColor.copy(alpha = 0.6f), modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun Dot() {
    Box(Modifier.padding(horizontal = 7.dp).size(3.dp).clip(CircleShape).background(LocalMyColorScheme.current.textGreyColor.copy(alpha = 0.5f)))
}

// ── Nutrition Facts ───────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FactsPage(food: NutriFood, meal: String, viewModel: DietFoodViewModel, onAdd: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val units by viewModel.units.collectAsState()
    val unit by viewModel.unit.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val nutrition by viewModel.nutrition.collectAsState()
    val loading by viewModel.detailLoading.collectAsState()
    val error by viewModel.detailError.collectAsState()
    val dailyKcal = 2000

    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Header
            AnimatedEnter(index = 0) {
                DietCard(padding = 12.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodThumb(food.foodImagePath, size = 64.dp, corner = 12.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(food.displayName, color = colors.textDarkColor, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(5.dp).clip(CircleShape).background(colors.primaryBlueColor))
                                Spacer(Modifier.width(5.dp))
                                Text(stringResource(R.string.diet_current) + " ", color = colors.textGreyColor, fontSize = 12.sp)
                                Text(
                                    unit?.let { "${formatQty(quantity)} × ${it.unitName}" } ?: "…",
                                    color = colors.textDarkColor, fontSize = 12.sp, fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
            // Serving + quantity
            AnimatedEnter(index = 1) {
                DietCard {
                    Text(stringResource(R.string.diet_serving_unit), color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        units.forEach { u -> FilterChip(u.unitName.trim(), u == unit) { viewModel.setUnit(u) } }
                    }
                    HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.diet_quantity), color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(stringResource(R.string.diet_quantity_hint), color = colors.textGreyColor, fontSize = 11.sp)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.dashboardBackgroundColor)
                                .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(4.dp),
                        ) {
                            StepButton(Icons.Rounded.Remove, quantity > (if (unit?.isGrams == true) 10.0 else DietFoodViewModel.MIN_QTY), highlighted = false) { viewModel.stepQuantity(false) }
                            Text(formatQty(quantity), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(64.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            StepButton(Icons.Rounded.Add, quantity < DietFoodViewModel.MAX_QTY, highlighted = true) { viewModel.stepQuantity(true) }
                        }
                    }
                }
            }

            val n = nutrition
            when {
                error != null && n == null -> DietCard {
                    Text(error.orEmpty(), color = colors.textGreyColor, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.diet_retry), color = colors.primaryBlueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).bounceClick { viewModel.retryDetail() }.padding(4.dp),
                    )
                }
                n == null -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.primaryBlueColor)
                }
                // Dimmed while a new serving's numbers load.
                else -> Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.alpha(if (loading) 0.5f else 1f),
                ) {
                    AnimatedEnter(index = 2) { EnergyCard(n, dailyKcal) }
                    AnimatedEnter(index = 3) { FoodMacroCard(n) }
                    AnimatedEnter(index = 4) { AllNutrientsCard(n) }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        DietPrimaryButton(
            stringResource(R.string.diet_add_to_meal, meal),
            enabled = nutrition != null && !loading,
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            onClick = onAdd,
        )
    }
}

@Composable
private fun StepButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, highlighted: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) softTint(colors.primaryBlueColor, DietColors.BlueSoft) else colors.dashboardContainerColor)
            .border(1.dp, if (highlighted) colors.primaryBlueColor.copy(alpha = 0.4f) else colors.borderGreyLightColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .bounceClick(enabled = enabled, pressedScale = 0.85f, onClick = onClick),
    ) {
        Icon(icon, contentDescription = null, tint = if (!enabled) colors.textGreyColor.copy(alpha = 0.4f) else if (highlighted) colors.primaryBlueColor else colors.textDarkColor, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun EnergyCard(n: FoodNutrition, dailyKcal: Int, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    DietCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(softTint(Color(0xFFEA580C), Color(0xFFFFEDE3))),
            ) { Icon(Icons.Rounded.LocalFireDepartment, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(26.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.diet_energy_value), color = colors.textGreyColor, fontSize = 10.sp, letterSpacing = 1.sp)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(kcalText(n.kcal), color = colors.textDarkColor, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(6.dp))
                    Text("Kcal", color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    stringResource(R.string.diet_pct_daily, (n.kcal * 100 / dailyKcal).roundToInt()),
                    color = DietColors.Green, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(softTint(DietColors.Green, DietColors.GreenSoft)).border(1.dp, DietColors.Green.copy(alpha = 0.3f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                )
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.diet_based_on, kcalText(dailyKcal)), color = colors.textGreyColor, fontSize = 10.sp)
            }
        }
    }
}

/** Carbs/protein/fat: a split bar and three tiles with grams and share of calories. */
@Composable
private fun FoodMacroCard(n: FoodNutrition) {
    val colors = LocalMyColorScheme.current
    // Share of calories: 4 kcal per g of carb and protein, 9 per g of fat.
    val c = n.carbs * 4; val p = n.protein * 4; val f = n.fat * 9
    val total = (c + p + f).takeIf { it > 0 } ?: 1.0
    DietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.diet_macronutrients), color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.diet_total_g, gramsText(n.carbs + n.protein + n.fat)), color = colors.textGreyColor, fontSize = 11.sp)
        }
        Spacer(Modifier.height(10.dp))
        MacroSplitBar(c / total, p / total, f / total)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MacroTile(stringResource(R.string.diet_carbs), n.carbs, (c * 100 / total).roundToInt(), Color(0xFFF59E0B), Color(0xFFFFF8E6), Color(0xFFFDEBB5), Modifier.weight(1f))
            MacroTile(stringResource(R.string.diet_protein), n.protein, (p * 100 / total).roundToInt(), Color(0xFF10B981), Color(0xFFEAFBF3), Color(0xFFC4F1DD), Modifier.weight(1f))
            MacroTile(stringResource(R.string.diet_fat), n.fat, (f * 100 / total).roundToInt(), Color(0xFFEF4444), Color(0xFFFFF0F0), Color(0xFFFCD5D5), Modifier.weight(1f))
        }
    }
}

@Composable
internal fun MacroSplitBar(carbs: Double, protein: Double, fat: Double, colorsTriple: Triple<Color, Color, Color> = Triple(Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFEF4444))) {
    val colors = LocalMyColorScheme.current
    Row(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(colors.borderGreyLightColor.copy(alpha = 0.25f))) {
        if (carbs > 0) Box(Modifier.weight(carbs.toFloat()).fillMaxSize().background(colorsTriple.first))
        if (protein > 0) Box(Modifier.weight(protein.toFloat()).fillMaxSize().padding(start = 2.dp).background(colorsTriple.second))
        if (fat > 0) Box(Modifier.weight(fat.toFloat()).fillMaxSize().padding(start = 2.dp).background(colorsTriple.third))
        val rest = 1.0 - carbs - protein - fat
        if (rest > 0.01) Box(Modifier.weight(rest.toFloat()).fillMaxSize())
    }
}

@Composable
private fun MacroTile(label: String, grams: Double, pct: Int, accent: Color, bg: Color, border: Color, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(softTint(accent, bg))
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp, horizontal = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
            Spacer(Modifier.width(5.dp))
            Text(label, color = colors.textDarkColor, fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(gramsText(grams), color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("gm", color = colors.textGreyColor, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.diet_pct_kcal, pct), color = colors.textDarkColor.copy(alpha = 0.8f), fontSize = 10.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)).background(border.copy(alpha = 0.6f)).padding(vertical = 3.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun AllNutrientsCard(n: FoodNutrition) {
    val colors = LocalMyColorScheme.current
    val rows = listOf(
        stringResource(R.string.diet_fiber) to "${gramsText(n.fiber)} gm",
        stringResource(R.string.diet_sugar) to "${gramsText(n.sugar)} gm",
        stringResource(R.string.diet_sodium) to "${gramsText(n.sodium)} mg",
    )
    DietCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.diet_all_nutrients), color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.diet_per_serving), color = colors.textGreyColor, fontSize = 11.sp)
        }
        HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 10.dp))
        rows.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(colors.textGreyColor.copy(alpha = 0.4f)))
                Spacer(Modifier.width(10.dp))
                Text(label, color = colors.textGreyColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                Text(value, color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
