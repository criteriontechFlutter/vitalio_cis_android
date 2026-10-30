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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.screens.onboarding.components.ProgressCard


@Composable
fun BloodScreen() {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val bloodGroups = listOf(
        "A+", "A-", "B+", "B-",
        "AB+", "AB-", "O+", "O-"
    )

    var selected by remember { mutableStateOf<String?>(null) }

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
            title = stringResource(R.string.auth_onb_progress_one_third_title),
            subtitle = stringResource(R.string.auth_onb_progress_one_third_subtitle)
        )
        Spacer(modifier = Modifier.weight(1f))

        AsyncImage(
            model = R.drawable.bggif,
            contentDescription = null,
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )


        Spacer(modifier = Modifier.height(6.dp))
        Text(
            stringResource(R.string.auth_onb_blood_title),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            stringResource(R.string.auth_onb_dob_subtitle),
            fontSize = 16.sp,
            color = colors.textGreyColor )



        Spacer(modifier = Modifier.height(30.dp))

        // 🔥 Grid Layout
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(bloodGroups.size) { index ->

                val item = bloodGroups[index]
                val isSelected = selected == item

                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) Color(0xFF1E5CCB)
                            else softTint(colors.textGreyColor, Color(0xFFF2F4F7))
                        )
                        .clickable {
                            selected = item
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isSelected) Color.White else colors.textDarkColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))


        HapticButton(
            onClick = { navController.navigate("location") },
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