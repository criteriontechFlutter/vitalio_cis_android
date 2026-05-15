package com.example.vitalio_cis.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.ui.unit.sp
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard
import com.example.vitalio_cis.viewmodel.RegistrationViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
fun DobScreen() {

    val navController = LocalNavController.current

    val months = listOf(
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    )

    val viewModel = remember {
        RegistrationViewModel()
    }
    val years = (1950..2025).toList()

    var selectedMonth by remember { mutableStateOf(8) }
    var selectedYear by remember { mutableStateOf(2018) }

    val days = (1..getDaysInMonth(selectedMonth, selectedYear)).toList()
    var selectedDay by remember { mutableStateOf(16) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {

        // 🔙 Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                "Create Account",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "Skip",
                color = Color(0xFF3B82F6),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = viewModel.perc,
            title = "One-Third Complete",
            subtitle = "Nice work! You're a third of the way there!"
        )
        Spacer(modifier = Modifier.weight(1f))

        // 👋 Title

        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.dobgif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            "Your date of birth?.",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            "Your date of birth ensures personalized health insights.",
            fontSize = 16.sp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {

            WheelPickerExact(months, selectedMonth) {
                selectedMonth = it
            }

            WheelPickerExact(days.map { it.toString() }, selectedDay) {
                selectedDay = it
            }

            WheelPickerExact(years.map { it.toString() }, years.indexOf(selectedYear)) {
                selectedYear = years[it]
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "${months[selectedMonth]} ${days[selectedDay]} $selectedYear",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = { navController.navigate("blood")

                viewModel.updatePer(0.2f)},
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6)
            )
        ) {
            Text("Next", color = Color.White)
        }
    }
}

@Composable
fun WheelPickerExact(
    items: List<String>,
    startIndex: Int,
    onSelected: (Int) -> Unit
) {
    val visibleItems = 5
    val itemHeight = 40.dp

    val listState = rememberLazyListState(startIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)

    Box(
        modifier = Modifier
            .height(itemHeight * visibleItems)
            .width(100.dp)
    ) {

        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = itemHeight * (visibleItems / 2))
        ) {

            itemsIndexed(items) { index, item ->

                val centerIndex =
                    listState.firstVisibleItemIndex

                val isSelected = index == centerIndex

                Text(
                    text = item,
                    modifier = Modifier
                        .height(itemHeight)
                        .wrapContentHeight(Alignment.CenterVertically)
                        .alpha(if (isSelected) 1f else 0.4f),
                    style = if (isSelected)
                        MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    else
                        MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Center highlight
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
        )

        // Top fade
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(itemHeight * 2)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, Color.Transparent)
                    )
                )
        )

        // Bottom fade
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(itemHeight * 2)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White)
                    )
                )
        )
    }

    // 🔥 FINAL PERFECT SELECTION LOGIC
    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex
        }
            .distinctUntilChanged()
            .collectLatest { index ->
                if (index in items.indices) {
                    onSelected(index)
                }
            }
    }
}

fun getDaysInMonth(month: Int, year: Int): Int {
    return when (month) {
        1 -> if (isLeap(year)) 29 else 28
        3, 5, 8, 10 -> 30
        else -> 31
    }
}

fun isLeap(year: Int): Boolean {
    return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
}