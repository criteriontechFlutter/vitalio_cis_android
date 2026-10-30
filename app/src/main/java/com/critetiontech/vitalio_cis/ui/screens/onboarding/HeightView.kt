package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HeightScreen() {

    var showBottomSheet by remember {
        mutableStateOf(false)
    }

    var selectedFeet by remember {
        mutableStateOf("5")
    }

    var selectedInch by remember {
        mutableStateOf("07")
    }

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val sheetState = rememberModalBottomSheetState()

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
            title = stringResource(R.string.auth_onb_progress_moving_ahead_title),
            subtitle = stringResource(R.string.auth_onb_progress_moving_ahead_subtitle)
        )

        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.heightgif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )

        Text(
            text = stringResource(R.string.auth_onb_height_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.auth_onb_height_subtitle),
            fontSize = 16.sp,
            color = colors.textGreyColor,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(R.string.auth_onb_height_label),
            fontSize = 14.sp,
            color = colors.textGreyColor
        )

        Spacer(modifier = Modifier.height(8.dp))

        /// CLICKABLE FIELD

        OutlinedTextField(
            value = "$selectedFeet.$selectedInch ft",
            onValueChange = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    showBottomSheet = true
                },
            placeholder = {
                Text(stringResource(R.string.auth_onb_height_hint))
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = (if (isAppDark) colors.borderGreyLightColor else Color(0xFFE5E7EB)),
                disabledTextColor = colors.textDarkColor,
                disabledContainerColor = colors.dashboardContainerColor
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        HapticButton(
            onClick = {
                navController.navigate("chronicDisease")
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

    /// BOTTOM SHEET

    if (showBottomSheet) {

        com.critetiontech.ctvitalio.ui.components.AppBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState,
        ) {

            HeightPickerBottomSheet(
                selectedFeet = selectedFeet,
                selectedInch = selectedInch,
                onFeetChange = {
                    selectedFeet = it
                },
                onInchChange = {
                    selectedInch = it
                },
                onSubmit = {
                    showBottomSheet = false
                }
            )
        }
    }
}

@Composable
fun HeightPickerBottomSheet(
    selectedFeet: String,
    selectedInch: String,
    onFeetChange: (String) -> Unit,
    onInchChange: (String) -> Unit,
    onSubmit: () -> Unit
) {

    val colors = LocalMyColorScheme.current
    val feetList = (3..7).map { it.toString() }

    val inchList = (0..11).map {
        it.toString().padStart(2, '0')
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text(
            text = stringResource(R.string.auth_onb_height_sheet_title),
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(25.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            /// FEET

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.auth_onb_height_feet),
                    fontSize = 12.sp,
                    color = colors.textGreyColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    items(feetList) { feet ->

                        Text(
                            text = feet,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable {
                                    onFeetChange(feet)
                                },
                            fontSize =
                                if (selectedFeet == feet) 34.sp else 26.sp,
                            fontWeight =
                                if (selectedFeet == feet)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal,
                            color =
                                if (selectedFeet == feet)
                                    Color(0xFF2563EB)
                                else
                                    colors.textGreyColor
                        )
                    }
                }
            }

            /// INCH

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.auth_onb_height_inch),
                    fontSize = 12.sp,
                    color = colors.textGreyColor
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.height(150.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    items(inchList) { inch ->

                        Text(
                            text = inch,
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .clickable {
                                    onInchChange(inch)
                                },
                            fontSize =
                                if (selectedInch == inch) 34.sp else 26.sp,
                            fontWeight =
                                if (selectedInch == inch)
                                    FontWeight.Bold
                                else
                                    FontWeight.Normal,
                            color =
                                if (selectedInch == inch)
                                    Color(0xFF2563EB)
                                else
                                    colors.textGreyColor
                        )
                    }
                }
            }

            /// UNIT

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = stringResource(R.string.auth_onb_height_unit),
                    fontSize = 12.sp,
                    color = colors.textGreyColor
                )

                Spacer(modifier = Modifier.height(35.dp))

                Text(
                    text = "ft",
                    fontSize = 24.sp,
                    color = Color(0xFF2563EB),
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "cm",
                    fontSize = 18.sp,
                    color = colors.textGreyColor
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        HapticButton(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.auth_common_submit))
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}