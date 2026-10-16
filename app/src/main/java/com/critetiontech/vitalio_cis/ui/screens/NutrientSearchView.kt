package com.critetiontech.ctvitalio.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriNutrient
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.viewmodel.NutrientViewModel
import java.util.Locale

private val CarbColor = Color(0xFFF59E0B)
private val ProteinColor = Color(0xFF10B981)
private val FatColor = Color(0xFFEF4444)
private val EnergyColor = Color(0xFFF97316)

/** Food search → serving → nutrients, from the NutriAnalyser API. Opened from Diet Checklist. */
@Composable
fun NutrientSearchScreen(viewModel: NutrientViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val selected by viewModel.selected.collectAsState()

    // Back from a food's details returns to the search results first.
    BackHandler(enabled = selected != null) { viewModel.clearSelection() }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .imePadding()
    ) {
        RoundBackTopBar(
            title = "Nutrition Facts",
            onBack = if (selected != null) ({ viewModel.clearSelection() }) else null,
        )
        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            contentKey = { it?.foodId },
            label = "nutriPage",
        ) { food ->
            if (food == null) SearchPage(viewModel) else DetailPage(food, viewModel)
        }
    }
}

// ── Search ────────────────────────────────────────────────────────────────────

@Composable
private fun SearchPage(viewModel: NutrientViewModel) {
    val colors = LocalMyColorScheme.current
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    val searching by viewModel.searching.collectAsState()
    val error by viewModel.searchError.collectAsState()
    val typed = query.trim().length >= NutrientViewModel.MIN_CHARS

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        MyTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholderText = "Search a food, e.g. roti, dal, apple",
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    searching -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
                    query.isNotEmpty() -> Icon(
                        Icons.Rounded.Close, contentDescription = "Clear",
                        modifier = Modifier.size(32.dp).clip(CircleShape).bounceClick { viewModel.onQueryChange("") }.padding(6.dp),
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))

        when {
            !viewModel.isConfigured -> NutriMessage(
                Icons.Rounded.Restaurant,
                "Nutrition lookup isn't set up",
                "The NutriAnalyser API key is missing from this build.",
            )
            !typed -> NutriMessage(
                Icons.Rounded.Restaurant,
                "Check what's in your food",
                "Search a food to see its calories, carbs, protein, fat and more for the serving you eat.",
            )
            error != null -> NutriMessage(Icons.Rounded.SearchOff, "Search didn't work", error!!, "Try again") {
                viewModel.onQueryChange(query)
            }
            !searching && results.isEmpty() -> NutriMessage(
                Icons.Rounded.SearchOff,
                "No foods found",
                "Nothing matches \"${query.trim()}\". Try a shorter or different name.",
            )
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { "${it.foodId}_${it.displayName}" }) { food ->
                    FoodRow(food) { viewModel.select(food) }
                }
                item { Spacer(Modifier.navigationBarsPadding().height(12.dp)) }
            }
        }
    }
}

@Composable
private fun FoodRow(food: NutriFood, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.dashboardContainerColor)
            .bounceClick(onClick = onClick)
            .padding(10.dp),
    ) {
        FoodImage(food, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)))
        Spacer(Modifier.width(12.dp))
        Text(
            food.displayName,
            color = colors.textDarkColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null,
            tint = colors.textGreyColor, modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun FoodImage(food: NutriFood, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Box(modifier.background(softTint(EnergyColor, Color(0xFFFFF1E6))), contentAlignment = Alignment.Center) {
        Text(
            food.displayName.take(1).uppercase(),
            color = EnergyColor, fontWeight = FontWeight.Bold, fontSize = 18.sp,
        )
        val url = food.foodImagePath?.trim()?.takeIf { it.startsWith("http") }
        if (url != null) {
            AsyncImage(
                model = url, contentDescription = food.displayName,
                contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize(),
            )
        }
    }
}

// ── Detail ────────────────────────────────────────────────────────────────────

@Composable
private fun DetailPage(food: NutriFood, viewModel: NutrientViewModel) {
    val colors = LocalMyColorScheme.current
    val units by viewModel.units.collectAsState()
    val unit by viewModel.unit.collectAsState()
    val quantity by viewModel.quantity.collectAsState()
    val nutrients by viewModel.nutrients.collectAsState()
    val loading by viewModel.detailLoading.collectAsState()
    val error by viewModel.detailError.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .navigationBarsPadding()
            .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Food
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.dashboardContainerColor)
                .padding(12.dp),
        ) {
            FoodImage(food, Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(food.displayName, color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    unit?.let {
                        if (it.unitName.trim().equals("gm", true)) "${formatQty(quantity)} g"
                        else "${formatQty(quantity)} × ${it.unitName.trim()}"
                    } ?: "Choose a serving",
                    color = colors.textGreyColor, fontSize = 13.sp,
                )
            }
        }

        // Serving
        if (units.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.dashboardContainerColor)
                    .padding(16.dp),
            ) {
                Text("Serving", color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    units.forEach { u -> UnitChip(u, selected = u.id == unit?.id) { viewModel.setUnit(u) } }
                }
                Spacer(Modifier.height(14.dp))
                val grams = unit?.unitName?.trim()?.equals("gm", true) == true
                val step = if (grams) 10.0 else 0.5
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Quantity", color = colors.textGreyColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    StepButton(Icons.Rounded.Remove, "Less", enabled = quantity - step >= NutrientViewModel.MIN_QTY) {
                        viewModel.setQuantity(quantity - step)
                    }
                    Text(
                        formatQty(quantity) + if (grams) " g" else "",
                        color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, modifier = Modifier.width(84.dp),
                    )
                    StepButton(Icons.Rounded.Add, "More", enabled = quantity + step <= NutrientViewModel.MAX_QTY) {
                        viewModel.setQuantity(quantity + step)
                    }
                }
            }
        }

        when {
            error != null && nutrients.isEmpty() -> NutriMessage(
                Icons.Rounded.SearchOff, "Couldn't load nutrients", error!!, "Try again",
            ) { viewModel.retryDetail() }
            nutrients.isEmpty() -> Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = colors.primaryBlueColor)
            }
            else -> NutrientSummary(nutrients, loading)
        }
    }
}

@Composable
private fun UnitChip(unit: NutriUnit, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Text(
        unit.unitName.trim(),
        color = if (selected) Color.White else colors.textDarkColor,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.primaryBlueColor else colors.dashboardBackgroundColor)
            .border(1.dp, if (selected) colors.primaryBlueColor else colors.borderGreyLightColor.copy(alpha = 0.4f), RoundedCornerShape(50))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun StepButton(icon: ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (enabled) softTint(colors.primaryBlueColor, Color(0xFFE8F0FE)) else colors.dashboardBackgroundColor)
            .bounceClick(enabled = enabled, onClick = onClick),
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) colors.primaryBlueColor else colors.textGreyColor, modifier = Modifier.size(20.dp))
    }
}

private fun List<NutriNutrient>.find(vararg names: String): NutriNutrient? =
    firstOrNull { n -> names.any { n.nutrientName.trim().equals(it, ignoreCase = true) } }
        ?: firstOrNull { n -> names.any { n.nutrientName.contains(it, ignoreCase = true) } }

@Composable
private fun NutrientSummary(nutrients: List<NutriNutrient>, refreshing: Boolean) {
    val colors = LocalMyColorScheme.current
    val energy = nutrients.find("Energy", "Calories", "Kcal")
    val carb = nutrients.find("Carb", "Carbohydrate")
    val protein = nutrients.find("Protein")
    val fat = nutrients.find("FAT", "Total Fat")
    val shown = setOfNotNull(energy, carb, protein, fat)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Calories
        AnimatedEnter(index = 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.dashboardContainerColor)
                    .padding(16.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(52.dp).clip(CircleShape).background(softTint(EnergyColor, Color(0xFFFFF1E6))),
                ) { Icon(Icons.Rounded.LocalFireDepartment, null, tint = EnergyColor, modifier = Modifier.size(28.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Energy", color = colors.textGreyColor, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            energy?.amount?.let { formatAmount(it, whole = true) } ?: "--",
                            color = colors.textDarkColor, fontSize = 32.sp, fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            energy?.unitName?.trim().orEmpty().ifEmpty { "kcal" },
                            color = colors.textGreyColor, fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                if (refreshing) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
            }
        }

        // Macros
        if (carb != null || protein != null || fat != null) {
            AnimatedEnter(index = 1) {
                MacroCard(carb, protein, fat)
            }
        }

        // Everything else the API returned
        val rest = nutrients.filter { it !in shown }
        if (rest.isNotEmpty()) {
            AnimatedEnter(index = 2) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.dashboardContainerColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        "All nutrients", color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                    rest.forEachIndexed { i, n ->
                        if (i > 0) HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.25f))
                        Row(Modifier.fillMaxWidth().padding(vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(prettyName(n.nutrientName), color = colors.textDarkColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            Text(
                                n.amount?.let { "${formatAmount(it)} ${n.unitName?.trim().orEmpty()}".trim() } ?: "—",
                                color = colors.textGreyColor, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
            }
        }

        Text(
            "Values are estimates from NutriAnalyser for the serving chosen above.",
            color = colors.textGreyColor, fontSize = 11.sp, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun MacroCard(carb: NutriNutrient?, protein: NutriNutrient?, fat: NutriNutrient?) {
    val colors = LocalMyColorScheme.current
    // Share of calories: 4 kcal per g of carb and protein, 9 per g of fat.
    val c = (carb?.amount ?: 0.0) * 4
    val p = (protein?.amount ?: 0.0) * 4
    val f = (fat?.amount ?: 0.0) * 9
    val total = (c + p + f).takeIf { it > 0 }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.dashboardContainerColor)
            .padding(16.dp),
    ) {
        Text("Macronutrients", color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        if (total != null) {
            Row(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp))) {
                listOf(c to CarbColor, p to ProteinColor, f to FatColor).forEach { (kcal, color) ->
                    val w by animateFloatAsState((kcal / total).toFloat(), tween(600), label = "macro")
                    if (w > 0f) Box(Modifier.weight(w).fillMaxSize().background(color))
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroTile("Carbs", carb, CarbColor, total?.let { c / it }, Modifier.weight(1f))
            MacroTile("Protein", protein, ProteinColor, total?.let { p / it }, Modifier.weight(1f))
            MacroTile("Fat", fat, FatColor, total?.let { f / it }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun MacroTile(label: String, n: NutriNutrient?, color: Color, share: Double?, modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(softTint(color, color.copy(alpha = 0.08f)))
            .padding(vertical = 12.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
            Text(label, color = colors.textGreyColor, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            n?.amount?.let { "${formatAmount(it)} ${n.unitName?.trim().orEmpty().ifEmpty { "g" }}" } ?: "—",
            color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Bold,
        )
        if (share != null) {
            Text("${(share * 100).toInt()}% kcal", color = colors.textGreyColor, fontSize = 11.sp)
        }
    }
}

@Composable
private fun NutriMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, start = 16.dp, end = 16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp).clip(CircleShape).background(softTint(colors.primaryBlueColor, Color(0xFFE8F0FE))),
        ) { Icon(icon, null, tint = colors.primaryBlueColor, modifier = Modifier.size(34.dp)) }
        Spacer(Modifier.height(14.dp))
        Text(title, color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, color = colors.textGreyColor, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center)
        AnimatedVisibility(action != null) {
            Text(
                action.orEmpty(),
                color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .padding(top = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.primaryBlueColor)
                    .bounceClick(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}

private fun formatQty(q: Double): String =
    if (q % 1.0 == 0.0) q.toInt().toString() else String.format(Locale.US, "%.1f", q)

private fun formatAmount(v: Double, whole: Boolean = false): String = when {
    whole || v >= 100 -> String.format(Locale.US, "%.0f", v)
    v >= 10 -> String.format(Locale.US, "%.1f", v).removeSuffix(".0")
    else -> String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
}

/** "FAT" → "Fat", "VitaminC" stays; only all-caps names are tidied. */
private fun prettyName(raw: String): String {
    val t = raw.trim()
    return if (t.length > 3 && t == t.uppercase()) t.lowercase().replaceFirstChar { it.titlecase(Locale.US) } else t
}
