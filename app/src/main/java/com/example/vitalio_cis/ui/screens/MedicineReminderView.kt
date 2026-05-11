package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel

@Composable
fun MedicineReminderScreen() {


    val navController = LocalNavController.current



    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Medicine Reminder",
        actions = {
            Text(
                "View Log", color = Color(0xFF2962FF),
                modifier = Modifier.clickable() {

                    navController.navigate(Routes.MANAGEMEDICAIONS)
                },
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {

            // 🔹 Header


            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Sections
            MedicineSection(
                "Morning", listOf(
                    Medicine("1 cup tea", "112 mg • Before meals", "08:00 AM", true)
                ), id = R.drawable.morning_medicine
            )

            MedicineSection(
                "Afternoon", listOf(
                    Medicine("Vitamin D3", "1000 IU • After lunch", "02:00 PM"),
                    Medicine("Aspirin", "75 mg • After lunch", "02:00 PM")
                ), id = R.drawable.afternoon_medicine
            )

            MedicineSection(
                "Night", listOf(
                    Medicine("Atorvastatin", "20 mg • After dinner", "09:00 PM")
                ), id = R.drawable.night_medicine
            )
        }
    }
}

data class Medicine(
    val name: String,
    val desc: String,
    val time: String,
    val isTaken: Boolean = false
)

@Composable
fun MedicineSection(title: String, medicines: List<Medicine>,id:Int) {

    Row {
        Spacer(modifier = Modifier.height(16.dp))
        Column {
            Image(
                painter = painterResource(id = id),
                contentDescription = null,
                modifier = Modifier
                    .width(74.dp),
            )
            Text(
                title,
                modifier = Modifier.padding(end = 4.dp),
                textAlign = TextAlign.End,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        medicines.forEach {
            MedicineCard(it)
        }
    }
}


@Composable
fun MedicineCard(medicine: Medicine) {


    val colors = LocalMyColorScheme.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = colors.dashboardContainerColor,
        )
    ) {

        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(modifier = Modifier.weight(1f)) {

                Text(
                    text = medicine.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )

                Text(
                    text = medicine.desc,
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ ", fontSize = 12.sp)
                    Text(
                        text = medicine.time,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (medicine.isTaken) {
                Button(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2962FF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Taken")
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
        }
    }
}