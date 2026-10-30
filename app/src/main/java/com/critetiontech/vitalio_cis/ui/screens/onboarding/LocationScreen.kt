package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard

@Composable
fun LocationScreen() {

    var street by remember { mutableStateOf("") }
    var zip by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var country by remember { mutableStateOf("") }

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor)
            .padding(16.dp)
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
            title = stringResource(R.string.auth_onb_progress_near_halfway_title),
            subtitle = stringResource(R.string.auth_onb_progress_near_halfway_subtitle)
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.locationgif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            stringResource(R.string.auth_onb_location_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            stringResource(R.string.auth_onb_location_subtitle),
            fontSize = 16.sp,
            color = colors.textGreyColor )



        Spacer(modifier = Modifier.height(30.dp))
        // 🔹 Street Address
        Label(stringResource(R.string.auth_onb_street_address))
        CustomTextField(
            value = street,
            placeholder = stringResource(R.string.auth_onb_street_address),
            onValueChange = { street = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 Zip + City
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(R.string.auth_onb_zip_code))
                CustomTextField(
                    value = zip,
                    placeholder = stringResource(R.string.auth_onb_zip_code_hint),
                    onValueChange = { zip = it }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(R.string.auth_onb_city))
                CustomDropdown(
                    value = city,
                    placeholder = stringResource(R.string.auth_onb_city_hint),
                    onSelect = { city = it }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔹 State + Country
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(R.string.auth_onb_state))
                CustomDropdown(
                    value = state,
                    placeholder = stringResource(R.string.auth_onb_state_hint),
                    onSelect = { state = it }
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Label(stringResource(R.string.auth_onb_country))
                CustomDropdown(
                    value = country,
                    placeholder = stringResource(R.string.auth_onb_country_hint),
                    onSelect = { country = it }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        HapticButton(
            onClick = {
                navController.navigate("weight")
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

@Composable
fun Label(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = LocalMyColorScheme.current.textGreyColor,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun CustomTextField(
    value: String,
    placeholder: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = if (isAppDark) LocalMyColorScheme.current.borderGreyLightColor else Color(0xFFE5E7EB),
            focusedBorderColor = Color(0xFF1E5CCB)
        )
    )
}

@Composable
fun CustomDropdown(
    value: String,
    placeholder: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options = listOf("Option 1", "Option 2", "Option 3")

    Box {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            trailingIcon = {
                HapticIconButton(onClick = { expanded = true }) {
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = if (isAppDark) LocalMyColorScheme.current.borderGreyLightColor else Color(0xFFE5E7EB),
                focusedBorderColor = Color(0xFF1E5CCB)
            )
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach {
                DropdownMenuItem(
                    text = { Text(it) },
                    onClick = {
                        onSelect(it)
                        expanded = false
                    }
                )
            }
        }
    }
}