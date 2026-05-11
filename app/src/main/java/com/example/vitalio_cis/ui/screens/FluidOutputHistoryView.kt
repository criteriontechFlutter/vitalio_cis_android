package com.example.vitalio_cis.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun FluidOutputHistoryScreen() {

    var selectedTab by remember { mutableStateOf(0) }
    var currentDate by remember { mutableStateOf(LocalDate.now()) }

    val colors = LocalMyColorScheme.current
    val mode = when (selectedTab) {
        0 -> DateMode.DAILY
        1 -> DateMode.WEEKLY
        else -> DateMode.MONTHLY
    }
    CommonAppBar(
        title = "Fluid Output History",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            outputTabSection(
                selected = selectedTab,
                onTabChange = { selectedTab = it }
            )

            Spacer(Modifier.height(16.dp))

            outputDateRow(
                currentDate = currentDate,
                mode = mode,
                onPrevious = {
                    currentDate = changeDate(currentDate, mode, false)
                },
                onNext = {
                    currentDate = changeDate(currentDate, mode, true)
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
            ) {

                Column(modifier = Modifier.padding(16.dp)) {

                    // title
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Fluid Output Log", style = AppTextStyles.style16BCN()
                        )

                        Row {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Check, contentDescription = null)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FluidOutputItem("Yellow", "02:58 PM", "180 ml", Color(0xFFE9DB8A))
                    FluidOutputItem("Light Yellow", "01:08 AM", "160 ml", Color(0xFFF4F1C9))
                    FluidOutputItem("Yellow", "11:40 AM", "210 ml", Color(0xFFE9DB8A))
                    FluidOutputItem("Dark Yellow", "10:50 AM", "150 ml", Color(0xFFF4C400))
                    FluidOutputItem("Light Yellow", "10:00 AM", "135 ml", Color(0xFFF4F1C9))
                    FluidOutputItem("Yellow", "06:17 AM", "175 ml", Color(0xFFE9DB8A))
                    FluidOutputItem("Dark Yellow", "05:55 AM", "190 ml", Color(0xFFF4C400))
                    FluidOutputItem("Amber", "04:20 AM", "240 ml", Color(0xFFFFA000))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row {
                Text("Total Output ",
                    style = AppTextStyles.style14GCN())
                Text(
                    "1450 ml",
                    style = AppTextStyles.style14GCN())

            }
        }
    }
}


@Composable
fun outputTabSection(selected: Int, onTabChange: (Int) -> Unit) {

    val tabs = listOf("Daily", "Weekly", "Monthly")

    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {

        tabs.forEachIndexed { i, text ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (selected == i) Color(0xFF2F6BFF)
                        else Color.Transparent
                    )
                    .clickable { onTabChange(i) }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    color = if (selected == i) Color.White else Color.Gray
                )
            }
        }
    }
}

/* ================= DATE ROW ================= */

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun outputDateRow(
    currentDate: LocalDate,
    mode: DateMode,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {

    val colors = LocalMyColorScheme.current
    val text = when (mode) {

        DateMode.DAILY -> {
            currentDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        }

        DateMode.WEEKLY -> {
            val start = currentDate.with(DayOfWeek.MONDAY)
            val end = start.plusDays(6)

            "${start.dayOfMonth} - ${end.dayOfMonth} ${end.month}"
        }

        DateMode.MONTHLY -> {
            currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(
            onClick = { onPrevious() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = colors.btnDarkColor
            )
        ) {
            Icon(Icons.Default.KeyboardArrowLeft, contentDescription = null)
        }

        Text(text, style = AppTextStyles.style14GCN())
        IconButton(
            onClick = { onNext() },
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = colors.btnDarkColor
            )
        ) {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }

    }
}

@Composable
fun FluidOutputItem(
    title: String,
    time: String,
    value: String,
    color: Color
) {

    Column {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(color)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(title,
                    style = AppTextStyles.style14BCN())
                Text(time,
                    style = AppTextStyles.style12GCN())
            }

            Text(
                value,
                style = AppTextStyles.style14BCN())
        }

        Divider(color = Color(0xFFE0E0E0), thickness = 0.7.dp)
    }
}
@Composable
fun RowScope.ToggleOutputItem(text: String, selected: Boolean) {

    Box(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(30.dp))
            .background(
                if (selected) Color(0xFF2F6BFF)
                else Color.Transparent
            )
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 14.sp
        )
    }
}