package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.ManualFoodAssignItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.IntakeOutputViewModel
import com.critetiontech.ctvitalio.R
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.critetiontech.ctvitalio.utils.AppTextStyles
@Composable
private fun FluidSection(delayMillis: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(380, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "fluidAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 30f,
        animationSpec = tween(380, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "fluidY"
    )
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = offsetY }) {
        content()
    }
}

// Maps food name to the right icon drawable
private fun iconResForFood(foodName: String): Int = when {
    foodName.contains("water", ignoreCase = true) -> R.drawable.fluid_water
    foodName.contains("milk", ignoreCase = true) -> R.drawable.fluid_milk
    foodName.contains("juice", ignoreCase = true) -> R.drawable.fluid_juice
    foodName.contains("tea", ignoreCase = true) -> R.drawable.fluid_tea
    foodName.contains("coffee", ignoreCase = true) -> R.drawable.fluid_coffee
    else -> R.drawable.fluid_beverage
}

@SuppressLint("NewApi")
@Composable
fun FluidDataInputScreen(viewModel: IntakeOutputViewModel = viewModel()) {

    var selectedItem by remember { mutableStateOf<ManualFoodAssignItem?>(null) }
    var toggle by remember { mutableIntStateOf(0) }

    var glassQuantity by remember { mutableIntStateOf(105) }
    var selectedPreset by remember { mutableStateOf<Int?>(null) }
    var glassMaxMl by remember { mutableIntStateOf(150) }
    val effectiveQuantity = selectedPreset ?: glassQuantity

    val manualFoodList by viewModel.manualFoodList.collectAsState()
    val isLoading by viewModel.intakeLoading.collectAsState()

    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current

    LaunchedEffect(manualFoodList) {
        if (selectedItem == null && manualFoodList.isNotEmpty()) {
            selectedItem = manualFoodList.first()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getManualFoodAssignList()
        viewModel.fetchIntake()
    }

    val context = LocalContext.current
    val intakeSuccess by viewModel.intakeSuccess.collectAsState()
    val outputSuccess by viewModel.outputSuccess.collectAsState()

    LaunchedEffect(intakeSuccess) {
        if (intakeSuccess) {
            Toast.makeText(context, "Fluid intake added successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetIntakeSuccess()
        }
    }

    LaunchedEffect(outputSuccess) {
        if (outputSuccess) {
            Toast.makeText(context, "Output recorded successfully!", Toast.LENGTH_SHORT).show()
            viewModel.resetOutputSuccess()
        }
    }

    CommonAppBar(
        title = "Fluid Data Input",
        actions = {
            Text(
                "History",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable {
                    if (toggle == 0)
                        navController.navigate(Routes.FLUIDINPUTHISTORY)
                    else
                        navController.navigate(Routes.FLUIDOUTPUTHISTORY)
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Tab toggle slides in from top
            var headerVisible by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { headerVisible = true }
            val headerAlpha by animateFloatAsState(
                targetValue = if (headerVisible) 1f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing), label = "tabAlpha"
            )
            val headerY by animateFloatAsState(
                targetValue = if (headerVisible) 0f else -20f,
                animationSpec = tween(400, easing = FastOutSlowInEasing), label = "tabY"
            )
            Box(modifier = Modifier.graphicsLayer { alpha = headerAlpha; translationY = headerY }) {
                ToggleSection(selected = toggle, onChange = { toggle = it })
            }

            // Slide horizontally between Intake and Output tabs
            AnimatedContent(
                targetState = toggle,
                transitionSpec = {
                    val dir = if (targetState > initialState) 1 else -1
                    (fadeIn(tween(220)) + slideInHorizontally(tween(280)) { it * dir }) togetherWith
                    (fadeOut(tween(180)) + slideOutHorizontally(tween(220)) { -it * dir })
                },
                label = "tabContent"
            ) { tab ->
            if (tab == 0) {
                Column {
                Spacer(Modifier.height(16.dp))

                // Progress + legend fade in
                FluidSection(delayMillis = 80) {
                    Column {
                        ProgressSection(viewModel = viewModel)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Glass slides up
                FluidSection(delayMillis = 160) {
                    SlideableWaterGlass(
                        foodName = selectedItem?.foodName.orEmpty(),
                        quantity = effectiveQuantity,
                        maxMl = glassMaxMl,
                        onQuantityChange = {
                            glassQuantity = it
                            selectedPreset = null
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Grid slides up
                FluidSection(delayMillis = 240) {
                    FluidTypeGrid(
                        items = manualFoodList,
                        selectedFoodId = selectedItem?.foodID,
                        isLoading = isLoading && manualFoodList.isEmpty(),
                        onSelect = { selectedItem = it }
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Selector springs up last
                FluidSection(delayMillis = 320) {
                    IntakeSelector(
                        selectedItem = selectedItem,
                        effectiveQuantity = effectiveQuantity,
                        selectedPreset = selectedPreset,
                        onPresetSelect = {
                            selectedPreset = it
                            glassMaxMl = it
                            glassQuantity = it
                        },
                        onUseGlass = {
                            selectedPreset = null
                            glassMaxMl = 150
                            glassQuantity = glassQuantity.coerceAtMost(150)
                        }
                    )
                }

                Spacer(Modifier.height(20.dp))
                }
            } else {
                UrinationScreen()
            }
            } // AnimatedContent
        }
    }
}

@Composable
fun ToggleSection(selected: Int, onChange: (Int) -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardContainerColor)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected == 0) Color(0xFF2563EB) else Color.Transparent)
                .clickable { onChange(0) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Fluid Intake", color = if (selected == 0) Color.White else Color.Gray)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected == 1) Color(0xFF2563EB) else Color.Transparent)
                .clickable { onChange(1) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Fluid Output", color = if (selected == 1) Color.White else Color.Gray)
        }
    }
}

@Composable
fun ProgressSection(viewModel: IntakeOutputViewModel = viewModel()) {
    val intakeList by viewModel.intakeList.collectAsState()
    val fluidSummaryList by viewModel.fluidSummaryList.collectAsState()
    val colors = LocalMyColorScheme.current

    val recommendedLimit = if (fluidSummaryList.isNotEmpty()) {
        fluidSummaryList.maxOfOrNull { it.assignedLimit }?.takeIf { it > 0 } ?: 2000
    } else 2000

    val totalIntake = intakeList.sumOf { it.foodQuantity }
    val progressFraction = (totalIntake.toFloat() / recommendedLimit).coerceIn(0f, 1f)
    val remaining = (recommendedLimit - totalIntake).coerceAtLeast(0)

    val progressColor = when {
        progressFraction < 0.5f -> Color(0xFF4CAF50)
        progressFraction < 0.85f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    val foodLegendNames = remember(intakeList) {
        intakeList.mapNotNull { it.foodName?.takeIf { n -> n.isNotEmpty() } }.distinct().take(4)
    }

    val legendColors = listOf(
        Color(0xFF4CAF50),
        Color(0xFF2196F3),
        Color(0xFF9C27B0),
        Color(0xFFFF9800)
    )

    // Animated progress bar — fills from 0 to actual value
    var screenVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { screenVisible = true }
    val animatedProgress by animateFloatAsState(
        targetValue = if (screenVisible) progressFraction else 0f,
        animationSpec = tween(900, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "progressBar"
    )

    androidx.compose.material3.Card(
        shape = RoundedCornerShape(16.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Recommended", style = AppTextStyles.style12GCN())
                    Text(
                        "${"%,d".format(recommendedLimit)} ml",
                        style = AppTextStyles.style14BCN()
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Intake", style = AppTextStyles.style12GCN())
                    Text("$totalIntake ml", style = AppTextStyles.style14BCN())
                }
            }

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFFE8EFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .background(progressColor, RoundedCornerShape(5.dp))
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (foodLegendNames.isEmpty()) {
                        listOf("Water", "Juice", "Milk", "Tea").forEachIndexed { idx, name ->
                            LegendDot(name, legendColors.getOrElse(idx) { Color.Gray })
                        }
                    } else {
                        foodLegendNames.forEachIndexed { idx, name ->
                            LegendDot(name, legendColors.getOrElse(idx) { Color.Gray })
                        }
                    }
                }
                Text(
                    "Remaining: $remaining ml",
                    style = AppTextStyles.style12GCN()
                )
            }
        }
    }
}

@Composable
private fun LegendDot(name: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(name, style = AppTextStyles.style12GCN())
    }
}

@Composable
fun ScaleItem(text: String, active: Boolean) {
    Text(text, fontSize = 12.sp, color = if (active) Color(0xFF2563EB) else Color.Gray)
}

@Composable
fun Scaleline(active: Boolean) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .height(1.5.dp)
            .background(if (active) Color(0xFF2563EB) else Color.LightGray)
    )
}

// ─── Beverage Style ───────────────────────────────────────────────────────────
private data class BeverageStyle(
    val primaryColors: List<Color>,
    val secondaryColors: List<Color>,
    val surfaceColor: Color,
    val glowColor: Color,
    val textColor: Color,
    val isOpaque: Boolean
)

private fun beverageStyle(foodName: String): BeverageStyle = when {
    foodName.contains("water", ignoreCase = true) -> BeverageStyle(
        primaryColors = listOf(Color(0x59338FF5), Color(0xEB0D6BE0)),
        secondaryColors = listOf(Color(0x6666CCFF), Color(0x728CB9F2)),
        surfaceColor = Color(0xCCB3E5FC),
        glowColor = Color(0x592563EB),
        textColor = Color.White,
        isOpaque = false
    )
    foodName.contains("juice", ignoreCase = true) -> BeverageStyle(
        primaryColors = listOf(Color(0xEBFF9900), Color(0xF2F35100)),
        secondaryColors = listOf(Color(0x80FFCC4D), Color(0x80FF8000)),
        surfaceColor = Color(0xE5FFE57F),
        glowColor = Color(0x66FF6D00),
        textColor = Color.White,
        isOpaque = false
    )
    foodName.contains("milk", ignoreCase = true) -> BeverageStyle(
        primaryColors = listOf(Color(0xF5FAFAFA), Color(0xF9E6E8EB)),
        secondaryColors = listOf(Color(0xA6FFFFFF), Color(0xB3D9DCE0)),
        surfaceColor = Color.White,
        glowColor = Color(0x40FFFFFF),
        textColor = Color(0xFF404040),
        isOpaque = true
    )
    foodName.contains("tea", ignoreCase = true) -> BeverageStyle(
        primaryColors = listOf(Color(0xEBD08540), Color(0xF25E3214)),
        secondaryColors = listOf(Color(0x80EBAD60), Color(0x8CBF7233)),
        surfaceColor = Color(0xD9FFD199),
        glowColor = Color(0x594E342E),
        textColor = Color.White,
        isOpaque = false
    )
    foodName.contains("coffee", ignoreCase = true) -> BeverageStyle(
        primaryColors = listOf(Color(0xF2724030), Color(0xFA2E1608)),
        secondaryColors = listOf(Color(0x8CA66B47), Color(0x99331C0F)),
        surfaceColor = Color(0xE5CC9966),
        glowColor = Color(0x66724030),
        textColor = Color.White,
        isOpaque = true
    )
    else -> BeverageStyle(
        primaryColors = listOf(Color(0xE51557FF), Color(0xFA1557FF)),
        secondaryColors = listOf(Color(0x733399FF), Color(0x801557FF)),
        surfaceColor = Color(0xCCFFFFFF),
        glowColor = Color(0x4D2563EB),
        textColor = Color.White,
        isOpaque = false
    )
}

// ─── Wave Shape ───────────────────────────────────────────────────────────────
private fun wavePath(
    width: Float, height: Float,
    progress: Float, phase: Float, amplitude: Float
): androidx.compose.ui.graphics.Path {
    val path = androidx.compose.ui.graphics.Path()
    val waterLevel = height * (1f - progress)
    path.moveTo(0f, height)
    for (x in 0..width.toInt()) {
        val relX = x / width
        val y = waterLevel + kotlin.math.sin(relX * Math.PI.toFloat() * 2f + phase) * amplitude
        path.lineTo(x.toFloat(), y)
    }
    path.lineTo(width, height)
    path.close()
    return path
}

// ─── Glass Shape (trapezoid: wider top, narrower bottom) ─────────────────────
private val GlassOutlineShape = GenericShape { size, _ ->
    val topInset = size.width * 0.05f
    val bottomInset = size.width * 0.18f
    moveTo(topInset + 28f, 0f)
    lineTo(size.width - topInset - 28f, 0f)
    quadraticTo(size.width - topInset, 0f, size.width - topInset, 28f)
    lineTo(size.width - bottomInset, size.height - 28f)
    quadraticTo(size.width - bottomInset, size.height, size.width - bottomInset - 28f, size.height)
    lineTo(bottomInset + 28f, size.height)
    quadraticTo(bottomInset, size.height, bottomInset, size.height - 28f)
    lineTo(topInset, 28f)
    quadraticTo(topInset, 0f, topInset + 28f, 0f)
    close()
}

// ─── Interactive Fluid Glass (iOS-matching) ───────────────────────────────────
@Composable
fun SlideableWaterGlass(
    foodName: String,
    quantity: Int,
    maxMl: Int = 150,
    onQuantityChange: (Int) -> Unit
) {
    val style = remember(foodName) { beverageStyle(foodName) }

    var level by remember { mutableFloatStateOf((quantity.toFloat() / maxMl).coerceIn(0f, 1f)) }
    var isDragging by remember { mutableStateOf(false) }

    LaunchedEffect(quantity, maxMl) {
        level = (quantity.toFloat() / maxMl).coerceIn(0f, 1f)
    }

    val currentMaxMl by rememberUpdatedState(maxMl)
    val currentOnQuantityChange by rememberUpdatedState(onQuantityChange)

    // Animated wave phase — drives continuous sine wave animation
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "wavePhase"
    )

    // Amplitude — bigger during drag (turbulence), settles after
    val amplitudeTarget by rememberUpdatedState(if (isDragging) 10f else 4f)
    val amplitude by animateFloatAsState(
        targetValue = amplitudeTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "amplitude"
    )

    val glassWidth = 170.dp
    val glassHeight = 260.dp

    val percentage = (level * 100).toInt()
    val currentMl = (level * maxMl).toInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Scale labels on the left
        Column(
            modifier = Modifier.height(glassHeight),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            ScaleItem("$maxMl ml", level >= 1f)
            Scaleline(level >= .85f)
            ScaleItem("${maxMl * 2 / 3} ml", level >= .66f)
            Scaleline(level >= .50f)
            ScaleItem("${maxMl / 3} ml", level >= .33f)
            Scaleline(level >= .15f)
            ScaleItem("0 ml", true)
        }

        Spacer(Modifier.width(10.dp))

        // Glass container
        Box(
            modifier = Modifier
                .height(glassHeight)
                .width(glassWidth),
            contentAlignment = Alignment.Center
        ) {
            // Outer glow
            if (level > 0.05f) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clip(GlassOutlineShape)
                        .background(style.glowColor)
                        .graphicsLayer { renderEffect = null; alpha = 0.55f }
                )
            }

            // Glass background (frosted)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(GlassOutlineShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.70f), Color.White.copy(alpha = 0.30f))
                        )
                    )
            )

            // Animated liquid waves drawn on Canvas
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .clip(GlassOutlineShape)
                    .pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { _ -> isDragging = true },
                            onDragEnd = {
                                isDragging = false
                                currentOnQuantityChange(
                                    (level * currentMaxMl).toInt().coerceIn(0, currentMaxMl)
                                )
                            },
                            onDragCancel = { isDragging = false }
                        ) { _, dragAmount ->
                            level = (level - dragAmount / size.height).coerceIn(0f, 1f)
                        }
                    }
            ) {
                if (level > 0f) {
                    // Secondary back wave (depth layer)
                    val backPath = wavePath(
                        size.width, size.height,
                        level, phase + Math.PI.toFloat(), amplitude * 0.75f
                    )
                    drawPath(
                        path = backPath,
                        brush = Brush.verticalGradient(
                            style.secondaryColors,
                            startY = 0f, endY = size.height
                        )
                    )

                    // Primary front wave
                    val frontPath = wavePath(
                        size.width, size.height,
                        level, phase, amplitude
                    )
                    drawPath(
                        path = frontPath,
                        brush = Brush.verticalGradient(
                            style.primaryColors,
                            startY = 0f, endY = size.height
                        )
                    )

                    // Surface highlight line
                    drawPath(
                        path = frontPath,
                        color = style.surfaceColor,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                    )
                }
            }

            // Glass rim highlights (left reflection + top rim + bottom base)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(GlassOutlineShape)
            ) {
                // Left vertical rim reflection
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxHeight()
                        .width(14.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(0.6f), Color.White.copy(0.05f), Color.Transparent)
                            )
                        )
                )
                // Top rim capsule
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 4.dp)
                        .width(glassWidth * 0.8f)
                        .height(2.5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color.White.copy(0.8f), Color.White.copy(0.2f))
                            )
                        )
                )
                // Bottom base highlight
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 6.dp)
                        .width(glassWidth * 0.5f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(0.3f))
                )
            }

            // Glass border stroke
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(GlassOutlineShape)
                    .background(Color.Transparent)
            )

            // Frosted metrics HUD (center overlay)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center)
            ) {
                Text(
                    "$percentage%",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (style.isOpaque && level > 0.45f) style.textColor else Color(0xFF1A1A2E),
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(0.55f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "$currentMl ml of $maxMl ml",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (style.isOpaque && level > 0.45f) style.textColor.copy(0.85f) else Color(0xFF333355)
                    )
                }
            }

            // Drag-to-adjust hint (shown only while dragging)
            if (isDragging) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(0.7f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        "⇅  Drag to adjust",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun fluidShimmerBrush(): Brush {
    val shimmerColors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0))
    val transition = rememberInfiniteTransition(label = "fluidShimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "fluidShimmerX"
    )
    return Brush.linearGradient(shimmerColors, start = Offset(translateX, 0f), end = Offset(translateX + 600f, 0f))
}

@Composable
private fun FluidTypeGridShimmer() {
    val brush = fluidShimmerBrush()
    val colors = LocalMyColorScheme.current
    val placeholders = remember { List(6) { it } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier.fillMaxWidth().height(190.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(placeholders) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.dashboardContainerColor)
                    .padding(vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth(0.65f).height(10.dp).clip(RoundedCornerShape(3.dp)).background(brush))
            }
        }
    }
}

@Composable
fun FluidTypeGrid(
    items: List<ManualFoodAssignItem>,
    selectedFoodId: Int?,
    isLoading: Boolean,
    onSelect: (ManualFoodAssignItem) -> Unit
) {
    if (isLoading) {
        FluidTypeGridShimmer()
        return
    }

    if (items.isEmpty()) return

    // Calculate grid height: 3 columns, each row ~90dp + 10dp gap
    val rows = (items.size + 2) / 3
    val gridHeight = (rows * 90 + (rows - 1) * 10).dp

    var gridVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { gridVisible = true }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        itemsIndexed(items) { index, item ->
            val itemAlpha by animateFloatAsState(
                targetValue = if (gridVisible) 1f else 0f,
                animationSpec = tween(300, delayMillis = index * 60, easing = FastOutSlowInEasing),
                label = "drinkAlpha_$index"
            )
            val itemScale by animateFloatAsState(
                targetValue = if (gridVisible) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "drinkScale_$index"
            )
            Box(modifier = Modifier.graphicsLayer {
                alpha = itemAlpha
                scaleX = itemScale
                scaleY = itemScale
            }) {
                FluidItem(
                    item = item,
                    selected = item.foodID == selectedFoodId,
                    onClick = { onSelect(item) }
                )
            }
        }
    }
}

@Composable
fun FluidItem(
    item: ManualFoodAssignItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val name = item.foodName.orEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (selected) colors.primaryBlueColor.copy(.08f)
                else colors.dashboardContainerColor
            )
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconResForFood(name)),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = if (selected) colors.primaryBlueColor else Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name.uppercase(),
            fontSize = 11.sp,
            color = if (selected) colors.primaryBlueColor else Color.Gray,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun IntakeSelector(
    viewModel: IntakeOutputViewModel = viewModel(),
    selectedItem: ManualFoodAssignItem?,
    effectiveQuantity: Int,
    selectedPreset: Int?,
    onPresetSelect: (Int) -> Unit,
    onUseGlass: () -> Unit
) {
    val options = listOf(150, 250, 300, 400)
    val colors = LocalMyColorScheme.current
    val isGlassMode = selectedPreset == null
    val canAdd = selectedItem != null && effectiveQuantity > 0

    Column {
        // iOS: "Select Your Glass Size" label
        Text(
            "Select Your Glass Size",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Pencil icon — blue when glass/manual mode is active
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isGlassMode) Color(0xFF2563EB) else colors.dashboardContainerColor)
                    .clickable { onUseGlass() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.fluid_edit),
                    contentDescription = null,
                    tint = if (isGlassMode) Color.White else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            options.forEach { value ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selectedPreset == value) Color(0xFF2563EB).copy(.1f)
                            else colors.dashboardContainerColor
                        )
                        .clickable { onPresetSelect(value) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "$value ml",
                        color = if (selectedPreset == value) Color(0xFF2563EB) else Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val saveLoading by viewModel.intakeLoading.collectAsState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    if (canAdd && !saveLoading)
                        Brush.verticalGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB)))
                    else
                        Brush.verticalGradient(listOf(Color(0xFFCCCCCC), Color(0xFFBBBBBB)))
                )
                .clickable(enabled = canAdd && !saveLoading) {
                    viewModel.addIntake(
                        value = effectiveQuantity.toString(),
                        fId = selectedItem!!.foodID.toString()
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (saveLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(22.dp),
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    "Submit",
                    color = if (canAdd) Color.White else Color.White.copy(0.7f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun UrinationScreen(viewModel: IntakeOutputViewModel = viewModel()) {
    var sliderValue by remember { mutableFloatStateOf(550f) }
    var selectedColour by remember { mutableStateOf("") }
    val maxValue = 1000f
    val ovalHeight = 300.dp

    val isLoading by viewModel.outputLoading.collectAsState()
    val outputList by viewModel.outputList.collectAsState()

    val today = java.time.LocalDate.now().toString()

    LaunchedEffect(Unit) {
        viewModel.fetchOutput(today)
    }

    val todayItems = remember(outputList, today) {
        outputList.filter { it.outputDateFormat == today }
    }
    val todayCount = todayItems.size
    val todayVolume = todayItems.sumOf { it.quantity }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Urination count", fontSize = 13.sp, color = Color(0xFF2F6BFF))
                Text("$todayCount times", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Box(modifier = Modifier.width(1.dp).height(36.dp).background(Color(0xFFDDDDDD)))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Urination volume", fontSize = 13.sp, color = Color(0xFF2F6BFF))
                Text("$todayVolume ml", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val scaleWidth = 68.dp
        val bubbleWidth = 52.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(ovalHeight)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        val height = size.height.toFloat()
                        val currentY = height - (sliderValue / maxValue * height)
                        val newY = (currentY + dragAmount).coerceIn(0f, height)
                        sliderValue = ((height - newY) / height * maxValue).coerceIn(0f, maxValue)
                    }
                }
        ) {
            val percentage = sliderValue / maxValue

            Row(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier.fillMaxHeight().width(scaleWidth),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    for (i in 10 downTo 0) {
                        val active = (i * 100) <= sliderValue.toInt()
                        val labelColor = if (active) Color(0xFF4A6CF7) else Color(0xFFBBCCEE)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = if (i == 0) "00 ml" else "${i * 100} ml",
                                fontSize = 9.sp,
                                color = labelColor,
                                fontWeight = if (active) FontWeight.Medium else FontWeight.Normal
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Box(modifier = Modifier.width(7.dp).height(1.5.dp).background(labelColor))
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(152.dp)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(percent = 50))
                            .background(Color(0xFFEEEEEE))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(10) {
                                HorizontalDivider(
                                    Modifier,
                                    thickness = 1.dp,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(percentage)
                                .align(Alignment.BottomCenter)
                                .background(Color(0xFFFFF8E1))
                        )
                    }
                }

                Spacer(modifier = Modifier.width(bubbleWidth))
            }

            Canvas(modifier = Modifier.matchParentSize()) {
                val y = size.height * (1f - percentage)
                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset(scaleWidth.toPx(), y),
                    end = Offset(size.width - bubbleWidth.toPx(), y),
                    strokeWidth = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .width(bubbleWidth)
                    .offset {
                        IntOffset(
                            x = 0,
                            y = (ovalHeight.toPx() * (1f - percentage)).toInt() - 20.dp.roundToPx()
                        )
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF9800), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%.1f", sliderValue / 100),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text("⇅", fontSize = 12.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val lastTimeAgo: Pair<Long, Long>? = remember(todayItems) {
            val lastItem = todayItems.maxByOrNull { it.id } ?: return@remember null
            try {
                val fmt = java.time.format.DateTimeFormatter.ofPattern(
                    "dd/MM/yyyy hh:mm a", java.util.Locale.ENGLISH
                )
                val lastTime = java.time.LocalDateTime.parse(lastItem.outputDate.trim(), fmt)
                val diff = java.time.Duration.between(lastTime, java.time.LocalDateTime.now())
                Pair(diff.toHours().coerceAtLeast(0L), (diff.toMinutes() % 60).coerceAtLeast(0L))
            } catch (_: Exception) {
                null
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("Last urination", fontSize = 13.sp, color = Color.Gray)
            Text("  •  ", fontSize = 13.sp, color = Color.Gray)
            when {
                lastTimeAgo == null -> Text("No records today", fontSize = 13.sp, color = Color.Gray)
                lastTimeAgo.first > 0 -> {
                    Text("${lastTimeAgo.first} h ", fontSize = 13.sp, color = Color(0xFF2F6BFF), fontWeight = FontWeight.Bold)
                    Text("${lastTimeAgo.second}", fontSize = 13.sp, color = Color(0xFF2F6BFF), fontWeight = FontWeight.Bold)
                    Text("m ago", fontSize = 13.sp, color = Color.Gray)
                }
                lastTimeAgo.second > 0 -> {
                    Text("${lastTimeAgo.second}", fontSize = 13.sp, color = Color(0xFF2F6BFF), fontWeight = FontWeight.Bold)
                    Text("m ago", fontSize = 13.sp, color = Color.Gray)
                }
                else -> Text("Just now", fontSize = 13.sp, color = Color(0xFF2F6BFF), fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .background(Color.White, RoundedCornerShape(16.dp))
                .padding(horizontal = 28.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(sliderValue.toInt().toString(), fontSize = 52.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2F6BFF))
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                modifier = Modifier.padding(bottom = 10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text("ml", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Text("∨", fontSize = 10.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        BpRangeIndicator(
            selectedColour = selectedColour,
            onColourSelect = { selectedColour = it },
            isLoading = isLoading,
            onUpdate = {
                viewModel.addOutput(quantity = sliderValue.toInt(), colour = selectedColour)
            }
        )
    }
}

@Composable
fun BpRangeIndicator(
    selectedColour: String,
    onColourSelect: (String) -> Unit,
    isLoading: Boolean,
    onUpdate: () -> Unit
) {
    val colorsList = listOf(
        Color(0xFFFFF9C4), Color(0xFFFFF176), Color(0xFFFFEE58),
        Color(0xFFFFA726), Color(0xFF8D6E63), Color(0xFFD84315)
    )
    val apiLabels = listOf("Light Yellow", "Yellow", "Dark Yellow", "Amber", "Brown", "Red")
    val displayLabels = listOf("Light\nYellow", "Yellow", "Dark\nYellow", "Amber", "Brown", "Red")
    val hydrationChips = listOf(
        "Hydrated" to Color(0xFF22C55E),
        "Hydrated" to Color(0xFF84CC16),
        "Mildly\nDehydrated" to Color(0xFFF59E0B),
        "Dehydrated" to Color(0xFFEF6C00),
        "Very\nDehydrated" to Color(0xFF8D4E2A),
        "Very\nDehydrated" to Color(0xFFD84315)
    )
    val selectedIndex = apiLabels.indexOf(selectedColour)

    Column(modifier = Modifier.fillMaxWidth()) {
        /* Fixed height prevents multi-line chip labels from pushing the colour bar down */
        Row(
            modifier = Modifier.fillMaxWidth().height(44.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            repeat(apiLabels.size) { i ->
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    if (i == selectedIndex) {
                        Box(
                            modifier = Modifier
                                .background(hydrationChips[i].second, RoundedCornerShape(6.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = hydrationChips[i].first,
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        Row(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50))) {
            colorsList.forEachIndexed { i, color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                        .clickable { onColourSelect(apiLabels[i]) }
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            displayLabels.forEachIndexed { i, label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    fontSize = 8.sp,
                    color = if (selectedColour == apiLabels[i]) hydrationChips[i].second else Color.Gray,
                    fontWeight = if (selectedColour == apiLabels[i]) FontWeight.Bold else FontWeight.Normal,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        HapticButton(
            onClick = { if (!isLoading) onUpdate() },
            enabled = selectedColour.isNotEmpty() && !isLoading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFFCCCCCC),
                contentColor = Color.White,
                disabledContentColor = Color.White
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text("Update", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}
