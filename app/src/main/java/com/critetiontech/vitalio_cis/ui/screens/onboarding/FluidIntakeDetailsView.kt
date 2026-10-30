package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
 import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.R
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidIntakeDetailsScreen() {
    val colors = LocalMyColorScheme.current

    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedUnit by remember {
        mutableStateOf("Ml")
    }

    var fluidGoal by remember {
        mutableStateOf("")
    }

    val unitList = listOf(
        "Ml",
        "Litre",
        "Glass"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardContainerColor)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        /// =========================
        /// TOP BAR
        /// =========================

        Text(
            text = stringResource(R.string.auth_onb_create_account),
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = colors.textDarkColor
        )

        Spacer(modifier = Modifier.height(18.dp))

        /// =========================
        /// PROGRESS CARD
        /// =========================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(
                containerColor = if (isAppDark) colors.dashboardBackgroundColor else Color(0xFFF5F7FB)
            )
        ) {

            Column(
                modifier = Modifier.padding(14.dp)
            ) {

                Text(
                    text = "81%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2563EB)
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { 0.81f },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),

                    color = Color(0xFF2563EB),

                    trackColor = softTint(Color(0xFF2563EB), Color(0xFFD9E5FF))
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(R.string.auth_onb_progress_one_step_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.auth_onb_progress_one_step_subtitle),
                    fontSize = 12.sp,
                    color = colors.textGreyColor
                )
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        /// =========================
        /// IMAGE
        /// =========================

        Image(
            painter = painterResource(id = R.drawable.dobgif),

            contentDescription = null,

            modifier = Modifier
                .size(120.dp)
                .align(Alignment.CenterHorizontally),

            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(18.dp))

        /// =========================
        /// TITLE
        /// =========================

        Text(
            text = stringResource(R.string.auth_onb_fluid_title),

            modifier = Modifier.align(Alignment.CenterHorizontally),

            fontSize = 26.sp,

            fontWeight = FontWeight.Bold,

            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_onb_fluid_goal_subtitle),

            modifier = Modifier.align(Alignment.CenterHorizontally),

            fontSize = 13.sp,

            lineHeight = 20.sp,

            color = colors.textGreyColor
        )

        Spacer(modifier = Modifier.height(30.dp))

        /// =========================
        /// LABEL
        /// =========================

        Text(
            text = stringResource(R.string.auth_onb_fluid_goal_label),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(10.dp))

        /// =========================
        /// INPUT + DROPDOWN
        /// =========================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            /// TEXTFIELD

            OutlinedTextField(
                value = fluidGoal,

                onValueChange = {
                    fluidGoal = it
                },

                modifier = Modifier.weight(1f),

                placeholder = {
                    Text(stringResource(R.string.auth_onb_fluid_goal_hint))
                },

                singleLine = true,

                shape = RoundedCornerShape(10.dp),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB))
                )
            )

            Spacer(modifier = Modifier.width(10.dp))

            /// DROPDOWN

            ExposedDropdownMenuBox(
                expanded = expanded,

                onExpandedChange = {
                    expanded = !expanded
                }
            ) {

                OutlinedTextField(
                    value = selectedUnit,

                    onValueChange = {},

                    readOnly = true,

                    modifier = Modifier
                        .width(110.dp)
                        .menuAnchor(),

                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = expanded
                        )
                    },

                    shape = RoundedCornerShape(10.dp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB))
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,

                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    unitList.forEach { item ->

                        DropdownMenuItem(

                            text = {
                                Text(item)
                            },

                            onClick = {

                                selectedUnit = item

                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        /// =========================
        /// BUTTON
        /// =========================

        HapticButton(
            onClick = {

            },

            enabled = fluidGoal.isNotEmpty(),

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = softTint(colors.textGreyColor, Color(0xFFE5E7EB))
            )
        ) {

            Text(
                text = stringResource(R.string.auth_common_next),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}