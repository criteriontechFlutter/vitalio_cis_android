package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.model.MedicineItem
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.MedicineReminderViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Composable
fun MedicineReminderScreen(viewModel: MedicineReminderViewModel = viewModel()) {

    val navController = LocalNavController.current
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    val periods by viewModel.periods.collectAsState()
    val loading by viewModel.loading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchMedicineIntake(context)
    }

    CommonAppBar(
        title = "Medicine Reminder",
        actions = {
            Text(
                "View Log", color = Color(0xFF2962FF),
                modifier = Modifier.clickable {
                    navController.navigate(Routes.MANAGEMEDICAIONS)
                },
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            if (loading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (periods.isEmpty()) {
                Text(
                    "No medicines scheduled for today",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    periods.forEach { period ->
                        MedicinePeriodSection(period, viewModel, context)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun MedicinePeriodSection(period: MedicinePeriod, viewModel: MedicineReminderViewModel, context: android.content.Context) {
    val iconRes = when (period.dayPeriod.lowercase()) {
        "morning" -> R.drawable.morning_medicine
        "afternoon" -> R.drawable.afternoon_medicine
        "night" -> R.drawable.night_medicine
        else -> R.drawable.morning_medicine
    }

    Row {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.width(74.dp),
            )
            Text(
                period.dayPeriod,
                modifier = Modifier.padding(end = 4.dp),
                textAlign = TextAlign.Center,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            period.medicineData.forEach { item ->
                ApiMedicineCard(item, viewModel, context)
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
fun ApiMedicineCard(item: MedicineItem, viewModel: MedicineReminderViewModel, context: android.content.Context) {
    val colors = LocalMyColorScheme.current

    val timeFormatted = try {
        val dt = LocalDateTime.parse(item.scheduledDateTime)
        dt.format(DateTimeFormatter.ofPattern("hh:mm a"))
    } catch (e: Exception) {
        item.scheduledDateTime
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.medicineName.trim(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Text(
                    text = "${item.dosageStrength.toInt()} ${item.unit} • ${item.instructions.trim()}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "⏰ $timeFormatted",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = item.brandName.trim(),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            when {
                item.isTaken -> Text(
                    "Taken",
                    color = Color(0xFF2962FF),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                item.isSkipped -> Text(
                    "Skipped",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
                else -> Button(
                    onClick = { viewModel.markIntake(context, item.id, item.scheduledDateTime) },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA000)),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 12.dp, vertical = 4.dp
                    )
                ) {
                    Text("Intake", fontSize = 13.sp, color = Color.White)
                }
            }
        }
    }
}
