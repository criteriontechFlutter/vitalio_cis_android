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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard
import com.critetiontech.ctvitalio.R

@Composable
fun FluidDetailsScreen() {

    var weight by remember {
        mutableStateOf("")
    }

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    var expanded by remember {
        mutableStateOf(false)
    }

    var selectedUnit by remember {
        mutableStateOf("kg")
    }

    val unitList = listOf("kg", "lbs")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardContainerColor)
            .padding(20.dp)
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
                stringResource(R.string.auth_onb_create_account),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                stringResource(R.string.auth_common_skip),
                color = Color(0xFF3B82F6),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = stringResource(R.string.auth_onb_progress_all_done_title),
            subtitle = stringResource(R.string.auth_onb_progress_all_done_subtitle)
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.fluiddetailsgif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.auth_onb_fluid_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.auth_onb_fluid_subtitle),
            fontSize = 16.sp,
            color = colors.textGreyColor,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.auth_onb_fluid_label),
            fontSize = 14.sp,
            color = colors.textDarkColor,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {

            OutlinedTextField(
                value = weight,
                onValueChange = {
                    weight = it
                },
                placeholder = {
                    Text(
                        text = stringResource(R.string.auth_onb_weight_hint),
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
                    unfocusedBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
                    focusedContainerColor = colors.dashboardContainerColor,
                    unfocusedContainerColor = colors.dashboardContainerColor
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number
                )
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box {

                Row(
                    modifier = Modifier
                        .clickable {
                            expanded = true
                        }
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedUnit,
                        color = Color(0xFF2563EB),
                        fontWeight = FontWeight.SemiBold
                    )

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF2563EB)
                    )
                }

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = {
                        expanded = false
                    }
                ) {

                    unitList.forEach { unit ->

                        DropdownMenuItem(
                            text = {
                                Text(text = unit)
                            },
                            onClick = {
                                selectedUnit = unit
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))




        HapticButton(
            onClick = {
                navController.navigate("registrationCompleted")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = softTint(colors.textGreyColor, Color(0xFFE5E7EB))
            )
        ) {
            Text(stringResource(R.string.auth_common_next), color = Color.White)
        }
    }
}