package com.critetiontech.ctvitalio.ui.screens.onboarding

import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.softTint
import com.critetiontech.ctvitalio.ui.feedback.HapticButton

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard


@Composable
fun RegistrationCompletedView() {

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
        }

        Spacer(modifier = Modifier.weight(1f))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = stringResource(R.string.auth_onb_progress_completed_title),
            subtitle = stringResource(R.string.auth_onb_progress_completed_subtitle)
        )



        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.auth_onb_dashboard_loading),
            fontSize = 12.sp,
            color = colors.textGreyColor,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))



        Spacer(modifier = Modifier.weight(1f))




        HapticButton(
            onClick = {
                navController.navigate("height")
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