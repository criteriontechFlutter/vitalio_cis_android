package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.ui.text.font.FontWeight

import androidx.compose.ui.draw.clip

import androidx.compose.material.icons.rounded.LocalFireDepartment

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme


@Composable
fun DietChecklistScreen() {


    val colors = LocalMyColorScheme.current
    val navController = com.critetiontech.myapplication.utils.LocalNavController.current
    CommonAppBar(
        title = stringResource(R.string.track_diet_checklist),
        actions = {
            // Food search → nutrients (NutriAnalyser).
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(50))
                    .background(colors.primaryBlueColor.copy(alpha = 0.12f))
                    .clickable { navController.navigate(com.critetiontech.ctvitalio.Routes.NUTRIENTS) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Icon(
                    androidx.compose.material.icons.Icons.Rounded.LocalFireDepartment, contentDescription = null,
                    tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.track_nutrients), color = colors.primaryBlueColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        },
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
            DietSection(
                stringResource(R.string.track_morning), listOf(
                    Diet(stringResource(R.string.track_diet_cup_tea), stringResource(R.string.track_diet_before_meals, "112 mg"), "08:00 AM", true)
                ), id = R.drawable.morning_medicine
            )

            DietSection(
                stringResource(R.string.track_afternoon), listOf(
                    Diet("Vitamin D3", stringResource(R.string.track_diet_after_lunch, "1000 IU"), "02:00 PM"),
                    Diet("Aspirin", stringResource(R.string.track_diet_after_lunch, "75 mg"), "02:00 PM")
                ), id = R.drawable.afternoon_medicine
            )

            DietSection(
                stringResource(R.string.track_night), listOf(
                    Diet("Atorvastatin", stringResource(R.string.track_diet_after_dinner, "20 mg"), "09:00 PM")
                ), id = R.drawable.night_medicine
            )
        }
    }
}

data class Diet(
    val name: String,
    val desc: String,
    val time: String,
    val isTaken: Boolean = false
)

@Composable
fun DietSection(title: String, medicines: List<Diet>,id:Int) {

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
                color = LocalMyColorScheme.current.textGreyColor,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        medicines.forEach {
            DietCard(it)
        }
    }
}


@Composable
fun DietCard(medicine: Diet) {


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
                    style = AppTextStyles.style16BCB()
                )

                Text(
                    text = medicine.desc,
                    style = AppTextStyles.style12GCN()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏰ ", fontSize = 12.sp)
                    Text(
                        text = medicine.time,
                        style = AppTextStyles.style12GCN()
                    )
                }
            }

            if (medicine.isTaken) {
                HapticButton(
                    onClick = {},
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2962FF)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(stringResource(R.string.track_taken))
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = colors.textGreyColor
                )
            }
        }
    }
}