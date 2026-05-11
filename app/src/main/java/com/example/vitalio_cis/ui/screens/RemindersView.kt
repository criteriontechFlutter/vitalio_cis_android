package com.example.vitalio_cis.ui.screens



import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.*
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme

/* ================= MODELS ================= */

sealed class TodayItem {

    data class Medicine(
        val id: Int,
        val name: String,
        val dose: String,
        val time: String,
        val isCompleted: Boolean = false,
        val showActions: Boolean = false
    ) : TodayItem()

    data class Vital(
        val id: Int,
        val title: String,
        val time: String,
        val unit: String,
        val value: String? = null
    ) : TodayItem()
}

/* ================= MAIN SCREEN ================= */

@Composable
fun RemindersScreen() {

    var list by remember {
        mutableStateOf(
            listOf(
                TodayItem.Medicine(1, "Pentaprazole", "1 Tablet, Before Lunch", "08:00 AM", true),
                TodayItem.Medicine(2, "Vitamin D, 10mg", "1 Tablet, after breakfast", "01:00 PM", false, true),
                TodayItem.Medicine(3, "Vitamin D, 10mg", "1 Tablet, After Meal", "02:00 PM"),
                TodayItem.Medicine(4, "Calcium", "1 Tablet, After Meal", "02:15 PM"),
                TodayItem.Medicine(5, "Cefixime", "1 Tablet, After Dinner", "09:15 PM"),
                TodayItem.Vital(6, "Blood Pressure", "Morning", "mm/Hg"),
                TodayItem.Vital(7, "Body Temperature", "Evening", "°F")
            )
        )
    }

    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .padding(16.dp)
    ) {

        // 🔹 Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Today’s Medicine",
                style = AppTextStyles.style18BCB())
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }

        Spacer(Modifier.height(16.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {

            items(list) { item ->

                when (item) {

                    is TodayItem.Medicine -> {
                        MedicineItem(
                            item = item,
                            onComplete = {
                                list = list.map {
                                    if (it is TodayItem.Medicine && it.id == item.id)
                                        it.copy(isCompleted = true)
                                    else it
                                }
                            }
                        )
                    }

                    is TodayItem.Vital -> {
                        VitalItem(item)
                    }
                }
            }
        }
    }
}

/* ================= MEDICINE ================= */

@Composable
fun MedicineItem(
    item: TodayItem.Medicine,
    onComplete: () -> Unit
) {


    val colors = LocalMyColorScheme.current
    if (item.showActions) {

        // 🔥 Action Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
        ) {

            Row(
                modifier = Modifier
                    .background(Color(0xFFFFB300), RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Icon(Icons.Default.Notifications, null, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Check, null, tint = Color.White)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.padding(vertical = 16.dp)) {
                Text(item.name,
                    style = AppTextStyles.style14BCN())
                Text("${item.dose} • ${item.time}",
                    style = AppTextStyles.style12GCN())
            }
        }

    } else {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Column {
                Text(item.name,
                    style = AppTextStyles.style14BCN())
                Text("${item.dose} • ${item.time}",
                    style = AppTextStyles.style12GCN())
            }

            if (item.isCompleted) {
                Box(
                    modifier = Modifier
                        .background(Color(0xFF00C853), RoundedCornerShape(12.dp))
                        .padding(8.dp)
                ) {
                    Icon(Icons.Default.Check, null, tint = Color.White)
                }
            } else {
                IconButton(onClick = onComplete) {
                    Icon(Icons.Default.MoreVert, null)
                }
            }
        }
    }
}

/* ================= VITAL ================= */

@Composable
fun VitalItem(item: TodayItem.Vital) {


    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Column {
            Text(item.title,
                style = AppTextStyles.style14BCN())

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(item.time, fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFB300), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("PENDING",
                        style = AppTextStyles.style12GCN().copy(fontSize = 10.sp))
                }
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(item.value ?: "--",
                style = AppTextStyles.style18BCN())
            Text(item.unit,
                style = AppTextStyles.style12GCN().copy(fontSize = 10.sp))
            Text("Update ⟳",
                style = AppTextStyles.style12GCN().copy(fontSize = 10.sp))
        }
    }
}