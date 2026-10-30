package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard
import com.critetiontech.ctvitalio.R
@Composable
fun ReminderPreferenceScreen() {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardContainerColor)
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
                text = stringResource(R.string.auth_onb_set_preferences),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = stringResource(R.string.auth_common_skip),
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
            title = stringResource(R.string.auth_onb_progress_final_stretch_title),
            subtitle = stringResource(R.string.auth_onb_progress_final_stretch_subtitle)
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
            text = stringResource(R.string.auth_onb_vital_reminder_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_onb_vital_reminder_subtitle),

            fontSize = 13.sp,

            lineHeight = 20.sp,

            color = colors.textGreyColor
        )

        Spacer(modifier = Modifier.height(28.dp))

        /// =========================
        /// DROPDOWNS
        /// =========================

        ReminderDropDown(
            title = stringResource(R.string.auth_onb_vital_blood_pressure)
        )

        ReminderDropDown(
            title = stringResource(R.string.auth_onb_vital_heart_rate)
        )

        ReminderDropDown(
            title = stringResource(R.string.auth_onb_vital_spo2)
        )

        ReminderDropDown(
            title = stringResource(R.string.auth_onb_vital_respiratory_rate)
        )

        Spacer(modifier = Modifier.weight(1f))

        /// =========================
        /// BUTTON
        /// =========================

        HapticButton(
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
                text = stringResource(R.string.auth_common_next),
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

    val colors = LocalMyColorScheme.current
    val reminderList = listOf(
        stringResource(R.string.auth_onb_interval_once_day),
        stringResource(R.string.auth_onb_interval_twice_day),
        stringResource(R.string.auth_onb_interval_thrice_day),
        stringResource(R.string.auth_onb_interval_four_day),
        stringResource(R.string.auth_onb_interval_every_hour),
        stringResource(R.string.auth_onb_interval_every_2_hours)
    )

    var selectedValue by remember {
        mutableStateOf(reminderList[2])
    }

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
                        color = colors.textDarkColor
                    )

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = selectedValue,
                        fontSize = 12.sp,
                        color = colors.textGreyColor
                    )
                }

                Icon(
                    imageVector =
                        if (expanded)
                            Icons.Default.KeyboardArrowUp
                        else
                            Icons.Default.KeyboardArrowDown,

                    contentDescription = null,
                    tint = colors.textGreyColor
                )
            }

            DropdownMenu(
                expanded = expanded,

                onDismissRequest = {
                    expanded = false
                },

                modifier = Modifier.background(colors.dashboardContainerColor)
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
            color = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
            thickness = 1.dp
        )
    }
}