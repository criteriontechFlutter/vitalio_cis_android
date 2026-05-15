package com.example.vitalio_cis.ui.screens.onboarding


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard

@Composable
fun ReminderPreferenceScreen() {

    val navController = LocalNavController.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {

        /// =========================
        /// TOP BAR
        /// =========================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = "Set Preferences",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "Skip",
                color = Color(0xFF2563EB),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        /// =========================
        /// PROGRESS CARD
        /// =========================

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = "Final Stretch in Sight",
            subtitle = "You're making great progress-just a little more to go!"
        )

        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.vitalremindergif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )


        /// =========================
        /// ICON
        /// =========================


        Spacer(modifier = Modifier.height(18.dp))

        /// =========================
        /// TITLE
        /// =========================

        Text(
            text = "Set Vital Reminder",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your primary details are completed. Please select the interval for your vital reminders.",

            fontSize = 13.sp,

            lineHeight = 20.sp,

            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(28.dp))

        /// =========================
        /// DROPDOWNS
        /// =========================

        ReminderDropDown(
            title = "Blood Pressure"
        )

        ReminderDropDown(
            title = "Heart Rate"
        )

        ReminderDropDown(
            title = "Blood Oxygen (SpO2)"
        )

        ReminderDropDown(
            title = "Respiratory Rate"
        )

        Spacer(modifier = Modifier.weight(1f))

        /// =========================
        /// BUTTON
        /// =========================

        Button(
            onClick = {

                navController.navigate("fluidDetails")
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB)
            )
        ) {

            Text(
                text = "Next",
                fontSize = 16.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ReminderDropDown(
    title: String
) {

    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedValue by remember {
        mutableStateOf("3 Times A Day")
    }

    val reminderList = listOf(
        "Once A Day",
        "2 Times A Day",
        "3 Times A Day",
        "4 Times A Day",
        "Every Hour",
        "Every 2 Hour"
    )

    Column {

        Box {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        expanded = true
                    }
                    .padding(vertical = 14.dp),

                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {

                    Text(
                        text = title,
                        fontSize = 14.sp,
                        color = Color.Black
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = selectedValue,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector =
                        if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,

                    contentDescription = null,
                    tint = Color.Gray
                )
            }

            DropdownMenu(
                expanded = expanded,

                onDismissRequest = {
                    expanded = false
                },

                modifier = Modifier.background(Color.White)
            ) {

                reminderList.forEach { item ->

                    DropdownMenuItem(

                        text = {

                            Text(
                                text = item,
                                fontSize = 14.sp
                            )
                        },

                        onClick = {

                            selectedValue = item

                            expanded = false
                        }
                    )
                }
            }
        }

        HorizontalDivider(
            color = Color(0xFFE5E7EB),
            thickness = 1.dp
        )
    }
}