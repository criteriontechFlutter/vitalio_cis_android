package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme


@Composable
fun FeedbackScreen() {

    var email by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var selectedRating by remember { mutableIntStateOf(3) }
    var expanded by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf("") }

    val modules = listOf(
        stringResource(R.string.profile_feedback_module_app_ui),
        stringResource(R.string.profile_feedback_module_performance),
        stringResource(R.string.profile_feedback_module_bugs),
        stringResource(R.string.profile_feedback_module_other),
    )

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title =  stringResource(R.string.profile_menu_feedback),)
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            // 🔹 Email
            Text(stringResource(R.string.profile_feedback_email_label),
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            MyTextField(
                value = email,
                onValueChange = { email = it },
                placeholderText = stringResource(R.string.profile_feedback_email_hint)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Rating
            Text(stringResource(R.string.profile_feedback_rating_label),
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val emojis = listOf("😡", "😕", "🙂", "😊", "😎")

                emojis.forEachIndexed { index, emoji ->
                    val isSelected = selectedRating == index

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (isSelected) colors.primaryBlueColor else colors.dashboardContainerColor,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedRating = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Dropdown
            Text(stringResource(R.string.profile_feedback_improve_label),
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            Box {

                MyTextField(
                    value = selectedModule,
                    onValueChange = { selectedModule = it },
                    placeholderText = stringResource(R.string.profile_feedback_select_module),
                    trailingIcon = {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    },
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    modules.forEach {
                        DropdownMenuItem(
                            text = { Text(it) },
                            onClick = {
                                selectedModule = it
                                expanded = false
                            }
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { expanded = true }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Feedback Input
            Text(stringResource(R.string.profile_feedback_label),
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            MyTextField(
                value = feedback,
                onValueChange = { feedback = it },
                placeholderText = stringResource(R.string.profile_feedback_hint),
                fixedHeight = 120
            )

            Spacer(modifier = Modifier.weight(1f))


            // 🔹 Button
            HapticButton(
                onClick = { /* Submit */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor)
            ) {
                Text(stringResource(R.string.profile_submit_feedback))
            }
        }
    }
}