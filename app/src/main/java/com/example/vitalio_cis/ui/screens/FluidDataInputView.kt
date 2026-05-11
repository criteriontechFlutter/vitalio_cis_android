package com.example.vitalio_cis.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.viewmodel.IntakeOutputViewModel

enum class FluidType(val foodId: Int, val label: String, val emoji: String) {
    WATER(1, "Water", "💧"),
    MILK(2, "Milk", "🥛"),
    JUICE(3, "Juice", "🧃"),
    TEA(4, "Tea", "☕"),
    COFFEE(5, "Coffee", "☕"),
    BEVERAGE(6, "Beverage ", "🍲")
}
 @Composable
fun FluidDataInputScreen(
     viewModel: IntakeOutputViewModel = viewModel(),) {

     var selectedFluid by remember { mutableStateOf(FluidType.WATER) }
    var toggle by remember { mutableStateOf(0) }

     val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current

     val context = LocalContext.current

     LaunchedEffect(Unit) {
         viewModel.fetchIntake(context) // 🔥 page load pe call
     }

    CommonAppBar(
        title = "Fluid Data Input",
        actions = {
            Text(
                "History",
                color = Color(0xFF2563EB),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(){
                    if(toggle==0)
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


            ToggleSection(
                selected = toggle,
                onChange = { toggle = it }
            )

            if(toggle==0){
                Spacer(Modifier.height(16.dp))

                ProgressSection()

                Spacer(Modifier.height(8.dp))

                LegendSection()

                Spacer(Modifier.height(16.dp))

                SlideableWaterGlass(selectedFluid)

                Spacer(Modifier.height(20.dp))

                FluidTypeGrid(
                    selectedFluid = selectedFluid,
                    onSelect = { selectedFluid = it }
                )

                Spacer(Modifier.height(20.dp))


                IntakeSelector(
                    selectedFluid = selectedFluid
                )

                Spacer(Modifier.height(20.dp))
            }else{
                UrinationScreen()
            }
        }
    }
}


@Composable
fun ToggleSection(
    selected: Int,
    onChange: (Int) -> Unit
) {
    val colors = LocalMyColorScheme.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardContainerColor)
            .padding(4.dp)
    ) {

        // Intake
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected == 0)
                        Color(0xFF2563EB)
                    else Color.Transparent
                )
                .clickable { onChange(0) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Fluid Intake",
                color = if (selected == 0) Color.White else Color.Gray
            )
        }

        // Output
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected == 1)
                        Color(0xFF2563EB)
                    else Color.Transparent
                )
                .clickable { onChange(1) }
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Fluid Output",
                color = if (selected == 1) Color.White else Color.Gray
            )
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

                Box(
                    Modifier
                        .weight(0.4f)
                        .fillMaxHeight()
                        .background(Color(0xFF22C55E))
                )

                Box(
                    Modifier
                        .weight(0.35f)
                        .fillMaxHeight()
                        .background(Color(0xFFF59E0B))
                )

                Box(
                    Modifier
                        .weight(0.25f)
                        .fillMaxHeight()
                        .background(Color(0xFFEF4444))
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            "Remaining: 300 ml",
            modifier = Modifier.align(Alignment.End),
            color = Color.Gray
        )
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
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text, fontSize = 12.sp, color = Color.Gray)
    }
}
@Composable
fun ScaleItem(
    text: String,
    active: Boolean
) {


        Text(
            text,
            fontSize = 12.sp,
            color = if (active) Color(0xFF2563EB) else Color.Gray
        )

}
@Composable
fun Scaleline(
    active: Boolean
) {
    Box(
        modifier = Modifier
            .width(18.dp)
            .height(1.5.dp)
            .background(
                if (active) Color(0xFF2563EB)
                else Color.LightGray
            )
    )
}
@Composable
fun SlideableWaterGlass(
    fluidType: FluidType
) {

    var level by remember { mutableStateOf(0.7f) }

    val maxMl = 150

    val gradient = when (fluidType) {
        FluidType.WATER -> listOf(Color(0xFFBFD3F2), Color(0xFF8FB3E8))
        FluidType.JUICE -> listOf(Color(0xFFFFD89B), Color(0xFFF59E0B))
        FluidType.MILK -> listOf(Color.White, Color(0xFFE5E7EB))
        FluidType.TEA -> listOf(Color(0xFFFCD34D), Color(0xFFFB923C))
        FluidType.COFFEE -> listOf(Color(0xFFB45309), Color(0xFF78350F))
        FluidType.BEVERAGE -> listOf(Color(0xFFA7F3D0), Color(0xFF10B981))
    }

    val glassShape = GenericShape { size, _ ->

        val topInset = size.width * 0.05f     // top margin
        val bottomInset = size.width * 0.18f  // bottom smaller

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

        // SCALE
        Column(
            modifier = Modifier.height(260.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.End
        ) {
            ScaleItem("150 ml", level >= 1f)
            Scaleline(level >= .85f)

            ScaleItem("100 ml", level >= .66f)
            Scaleline(level >= .50f)

            ScaleItem("50 ml", level >= .33f)
            Scaleline(level >= .15f)

            ScaleItem("0 ml", true)
        }

        Spacer(Modifier.width(8.dp))

        // GLASS
        Box(
            modifier = Modifier
                .height(260.dp)
                .width(200.dp)
                .clip(glassShape)
                .background(Color(0xFFEAF1FB))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        level = (level - dragAmount / size.height)
                            .coerceIn(0f, 1f)
                    }
                }
        ) {

            // liquid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(level)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(gradient)
                    )
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
    selectedFluid: FluidType,
    onSelect: (FluidType) -> Unit
) {

    val items = FluidType.values()

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),   // IMPORTANT FIX
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(items) { item ->
            FluidItem(
                title = item,
                selected = selectedFluid == item,
                onClick = { onSelect(item) }
            )
        }
    }
}

@Composable
fun FluidItem(
    title: FluidType,
    selected: Boolean,
    onClick: () -> Unit
) {

    val colors = LocalMyColorScheme.current

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
            painter = painterResource(
                id = when (title) {
                    FluidType.WATER -> R.drawable.fluid_water
                    FluidType.JUICE -> R.drawable.fluid_juice
                    FluidType.MILK -> R.drawable.fluid_milk
                    FluidType.TEA -> R.drawable.fluid_tea
                    FluidType.COFFEE -> R.drawable.fluid_coffee
                    FluidType.BEVERAGE -> R.drawable.fluid_beverage
                }
            ),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = if (selected) colors.primaryBlueColor else Color.Gray
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = title.name,
            fontSize = 12.sp,
            color = if (selected) colors.primaryBlueColor else Color.Gray
        )
    }
}



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun IntakeSelector(
    viewModel: IntakeOutputViewModel = viewModel(),

    selectedFluid: FluidType  ) {

    var selected by remember { mutableStateOf<Int?>(null) }

    val options = listOf(150, 250, 300, 400)

    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    Column {

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.dashboardContainerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.fluid_edit),
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
            }

            options.forEach { value ->

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (selected == value)
                                Color(0xFF2563EB).copy(.1f)
                            else
                                colors.dashboardContainerColor
                        )
                        .clickable { selected = value }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text(
                        "$value ml",
                        color = if (selected == value)
                            Color(0xFF2563EB)
                        else
                            Color.Gray
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
                .background(
                    if (selected == null)
                        colors.dashboardContainerColor
                    else
                        Color(0xFF2563EB)
                )
                .clickable(){
                    viewModel.addIntake(context, value = selected.toString(), fId = selectedFluid.foodId.toString())
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Add Intake",
                color = if (selected == null) Color.Gray else Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun UrinationScreen(  viewModel: IntakeOutputViewModel = viewModel()) {
    var sliderValue by remember { mutableStateOf(550f) }
    val maxValue = 1000f

    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(colors.dashboardBackgroundColor),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // 🔹 Top Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Urination count", fontSize = 14.sp, color = Color.Gray)
                Text("8 times", fontWeight = FontWeight.Bold)
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Urination volume", fontSize = 14.sp, color = Color.Gray)
                Text("1450 ml", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔥 CUSTOM OVAL SLIDER
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
                .pointerInput(Unit) {
                    detectVerticalDragGestures { change, dragAmount ->

                        val height = size.height.toFloat()
                        val currentY = height - (sliderValue / maxValue * height)
                        val newY = (currentY + dragAmount).coerceIn(0f, height)

                        sliderValue =
                            ((height - newY) / height * maxValue)
                                .coerceIn(0f, maxValue)
                    }
                }
        ) {

            val percentage = sliderValue / maxValue

            Row(modifier = Modifier.fillMaxSize()) {

                // 🔹 LEFT SCALE
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(50.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 10 downTo 0) {
                        Text("${i * 100} ml", fontSize = 10.sp, color = Color(0xFF4A6CF7))
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                // 🔥 OVAL
                Box(
                    modifier = Modifier
                        .size(width = 220.dp, height = 300.dp)
                        .clip(RoundedCornerShape(120.dp))
                        .background(Color(0xFFECE0E0))
                        .border(16.dp, Color.LightGray, RoundedCornerShape(120.dp))
                ) {

                    Column(
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        repeat(8) {
                            Divider(color = Color.Gray.copy(alpha = 0.3f))
                        }
                    }

                    // 🔹 Fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(percentage)
                            .align(Alignment.BottomCenter)
                            .background(Color(0xFFFFF3CD).copy(alpha = 0.6f))
                    )
                }
            }

            // 🔥 ✅ DRAW LINE ON TOP (IMPORTANT)
            Canvas(
                modifier = Modifier.matchParentSize()
            ) {
                val parentHeight = size.height
                val ovalHeight = 300.dp.toPx()

                val topOffset = (parentHeight - ovalHeight) / 2
                val yInsideOval = ovalHeight * (1f - percentage)

                // 🔥 👉 yahan adjust karo (increase value = aur upar)
                val extraOffset = 8.dp.toPx()

                val finalY = topOffset + yInsideOval - extraOffset

                drawLine(
                    color = Color(0xFFFF9800),
                    start = Offset(0f, finalY),
                    end = Offset(size.width, finalY),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }

            // 🔥 ✅ BUBBLE ALSO ON TOP
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset {
                        IntOffset(
                            x = 40,
                            y = (300 * (1f - percentage)).dp.roundToPx() - 20
                        )
                    }
                    .background(Color(0xFFFF9800), shape = CircleShape)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = String.format("%.1f", sliderValue / 100),
                    color = Color.Black,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 🔹 Value Display
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = sliderValue.toInt().toString(),
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2F6BFF)
            )
            Text(" ml", fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))

        BpRangeIndicator()
    }
}
@Composable
fun BpRangeIndicator() {

    val colorsList = listOf(
        Color(0xFFFFF9C4), // Light Yellow
        Color(0xFFFFF176), // Yellow
        Color(0xFFFFEE58), // Dark Yellow
        Color(0xFFFFA726), // Amber
        Color(0xFF8D6E63), // Brown
        Color(0xFFD84315)  // Red
    )

    val labels = listOf(
        "Light Yellow",
        "Yellow",
        "Dark Yellow",
        "Amber",
        "Brown",
        "Red"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        // 🔥 Color bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
        ) {
            colorsList.forEach { color ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(color)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // 🔥 Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(
                    text = it,
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // 🔥 Disabled Button (Update)
        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFE0E0E0),
                disabledContainerColor = Color(0xFFE0E0E0),
                disabledContentColor = Color.White
            )
        ) {
            Text("Update")
        }
    }
}