package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.feedback.HapticButton

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard
import com.critetiontech.ctvitalio.viewmodel.RegistrationViewModel

@Composable
fun GenderScreen( ) {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    var selected by remember { mutableStateOf("") }
    val viewModel = remember {
        RegistrationViewModel()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
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
            progress = viewModel.perc,
            title = stringResource(R.string.auth_onb_progress_getting_started_title),
            subtitle = stringResource(R.string.auth_onb_progress_getting_started_subtitle)
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.gendergif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(modifier = Modifier.height(24.dp))

        // 🧾 Title
        Text(
            stringResource(R.string.auth_onb_gender_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            stringResource(R.string.auth_onb_gender_subtitle, "Abhinav"),
            fontSize = 16.sp,
            color = colors.textGreyColor
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 👇 Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            GenderItem(
                title = stringResource(R.string.auth_onb_gender_male),
                icon = R.drawable.male,
                selected = selected == "Male",
                onClick = { selected = "Male" }
            )

            GenderItem(
                title = stringResource(R.string.auth_onb_gender_female),
                icon = R.drawable.female,
                selected = selected == "Female",
                onClick = { selected = "Female" }
            )

            GenderItem(
                title = stringResource(R.string.auth_onb_gender_other),
                icon = R.drawable.gender_other,
                selected = selected == "Other",
                onClick = { selected = "Other" }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 🔘 Next Button
        HapticButton(
            onClick = { navController.navigate("dob")

                viewModel.updatePer(0.2f)},
            enabled = selected.isNotEmpty(),
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
fun GenderItem(
    title: String,
    icon: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(100.dp)
            .clickable { onClick() }
    ) {

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (selected)
                    softTint(Color(0xFF2563EB), Color(0xFFDBEAFE))
                else
                    softTint(colors.textGreyColor, Color(0xFFF3F4F6))
            ),
            modifier = Modifier.size(90.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (selected) 6.dp else 2.dp
            )
        ) {

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /// 🔹 ICON AREA
                Box(
                    modifier = Modifier
                        .weight(1f) // 👈 takes upper space
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = null,
                        tint = if (selected) Color(0xFF2563EB) else Color(0xFF3B82F6),
                        modifier = Modifier.size(32.dp)
                    )
                }

                /// 🔹 TEXT AREA
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = if (selected) Color(0xFF2563EB) else colors.textDarkColor,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }
    }
}