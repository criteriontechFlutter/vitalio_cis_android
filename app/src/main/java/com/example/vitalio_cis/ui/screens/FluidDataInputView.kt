package com.example.vitalio_cis.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.ManualFoodAssignItem
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.viewmodel.IntakeOutputViewModel

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

// Maps food name to glass fill gradient
private fun gradientForFood(foodName: String): List<Color> = when {
    foodName.contains("water", ignoreCase = true) -> listOf(Color(0xFFBFD3F2), Color(0xFF8FB3E8))
    foodName.contains("juice", ignoreCase = true) -> listOf(Color(0xFFFFD89B), Color(0xFFF59E0B))
    foodName.contains("milk", ignoreCase = true) -> listOf(Color.White, Color(0xFFE5E7EB))
    foodName.contains("tea", ignoreCase = true) -> listOf(Color(0xFFFCD34D), Color(0xFFFB923C))
    foodName.contains("coffee", ignoreCase = true) -> listOf(Color(0xFFB45309), Color(0xFF78350F))
    else -> listOf(Color(0xFFA7F3D0), Color(0xFF10B981))
}

@SuppressLint("NewApi")
@Composable
fun FluidDataInputScreen(viewModel: IntakeOutputViewModel = viewModel()) {

    var selectedItem by remember { mutableStateOf<ManualFoodAssignItem?>(null) }
    var toggle by remember { mutableStateOf(0) }

    // Shared quantity state — glass drag and preset selector both read/write this
    var glassQuantity by remember { mutableStateOf(105) }   // default 70% of 150 ml
    var selectedPreset by remember { mutableStateOf<Int?>(null) }  // null = glass/manual mode
    var glassMaxMl by remember { mutableStateOf(150) }      // scale persists across mode switches
    val effectiveQuantity = selectedPreset ?: glassQuantity

    val manualFoodList by viewModel.manualFoodList.collectAsState()
    val isLoading by viewModel.intakeLoading.collectAsState()

    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val context = LocalContext.current

    // Auto-select first drink when list arrives
    LaunchedEffect(manualFoodList) {
        if (selectedItem == null && manualFoodList.isNotEmpty()) {
            selectedItem = manualFoodList.first()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getManualFoodAssignList(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            viewModel.fetchIntake(context)
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
                    ProgressSection()
                    Spacer(Modifier.height(8.dp))
                    LegendSection()
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
fun ProgressSection() {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Recommended:  2,000 ml", color = Color.Gray)
            Text("Intake:  1700 ml", color = Color.Gray)
        }
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(Color(0xFFE5E7EB))
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(Modifier.weight(0.4f).fillMaxHeight().background(Color(0xFF22C55E)))
                Box(Modifier.weight(0.35f).fillMaxHeight().background(Color(0xFFF59E0B)))
                Box(Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFFEF4444)))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Remaining: 300 ml", modifier = Modifier.align(Alignment.End), color = Color.Gray)
    }
}

@Composable
fun LegendSection() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(Color(0xFF22C55E), "Water")
        LegendDot(Color(0xFFF59E0B), "Juice")
        LegendDot(Color(0xFFFACC15), "Milk")
        LegendDot(Color(0xFFFB923C), "Tea")
    }
}

@Composable
fun LegendDot(color: Color, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 12.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
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

@Composable
fun SlideableWaterGlass(
    foodName: String,
    quantity: Int,
    maxMl: Int = 150,
    onQuantityChange: (Int) -> Unit
) {
    // Local float level — drives rendering directly so drag is never blocked by parent recomposition
    var level by remember { mutableStateOf((quantity.toFloat() / maxMl).coerceIn(0f, 1f)) }

    // Sync level when an external change arrives (preset selected, maxMl changed)
    LaunchedEffect(quantity, maxMl) {
        level = (quantity.toFloat() / maxMl).coerceIn(0f, 1f)
    }

    // rememberUpdatedState so the pointerInput lambda always reads the latest maxMl/callback
    // without needing to re-create the gesture detector (which would interrupt ongoing drags)
    val currentMaxMl by rememberUpdatedState(maxMl)
    val currentOnQuantityChange by rememberUpdatedState(onQuantityChange)

    val gradient = gradientForFood(foodName)

    val glassShape = GenericShape { size, _ ->
        val topInset = size.width * 0.05f
        val bottomInset = size.width * 0.18f
        moveTo(topInset, 0f)
        lineTo(size.width - topInset, 0f)
        lineTo(size.width - bottomInset, size.height)
        lineTo(bottomInset, size.height)
        close()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            modifier = Modifier.height(260.dp),
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

        Spacer(Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .height(260.dp)
                .width(200.dp)
                .clip(glassShape)
                .background(Color(0xFFEAF1FB))
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            // currentMaxMl always reflects the latest value via rememberUpdatedState
                            currentOnQuantityChange(
                                (level * currentMaxMl).toInt().coerceIn(0, currentMaxMl)
                            )
                        }
                    ) { _, dragAmount ->
                        level = (level - dragAmount / size.height).coerceIn(0f, 1f)
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level)
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(gradient))
            )
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${(level * 100).toInt()}%",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A5B75)
                )
                Text(
                    "${(level * maxMl).toInt()} ml of $maxMl ml",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
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
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color(0xFF2563EB), strokeWidth = 2.dp)
        }
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

@RequiresApi(Build.VERSION_CODES.O)
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
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val isGlassMode = selectedPreset == null
    val canAdd = selectedItem != null && effectiveQuantity > 0

    Column {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(if (canAdd) Color(0xFF2563EB) else colors.dashboardContainerColor)
                .clickable {
                    if (canAdd) {
                        viewModel.addIntake(
                            context,
                            value = effectiveQuantity.toString(),
                            fId = selectedItem!!.foodID.toString()
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val label = if (canAdd) {
                "Add : ${selectedItem!!.foodName.orEmpty()}, $effectiveQuantity ml"
            } else {
                "Add Intake"
            }
            Text(
                label,
                color = if (canAdd) Color.White else Color.Gray,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun UrinationScreen(viewModel: IntakeOutputViewModel = viewModel()) {
    var sliderValue by remember { mutableStateOf(550f) }
    var selectedColour by remember { mutableStateOf("") }
    val maxValue = 1000f
    val ovalHeight = 300.dp

    val context = LocalContext.current
    val isLoading by viewModel.outputLoading.collectAsState()
    val outputList by viewModel.outputList.collectAsState()

    val today = java.time.LocalDate.now().toString()

    LaunchedEffect(Unit) {
        viewModel.fetchOutput(context, today)
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
                                Divider(color = Color.White.copy(alpha = 0.85f), thickness = 1.dp)
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
            } catch (e: Exception) {
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
                viewModel.addOutput(context, quantity = sliderValue.toInt(), colour = selectedColour)
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
        Row(modifier = Modifier.fillMaxWidth()) {
            repeat(apiLabels.size) { i ->
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
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

        Button(
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
