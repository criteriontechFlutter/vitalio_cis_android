package com.example.vitalio_cis.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.components.MyButton
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme


@Composable
fun FeedbackScreen() {

    var email by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf("") }
    var selectedRating by remember { mutableStateOf(3) }
    var expanded by remember { mutableStateOf(false) }
    var selectedModule by remember { mutableStateOf("Select Module") }

    val modules = listOf("App UI", "Performance", "Bugs", "Other")

    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title =  "Feedback",)
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {


            // 🔹 Email
            Text("Your Email id*",
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            MyTextField(
                value = email,
                onValueChange = { email = it },
                placeholderText = "Enter Email"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 🔹 Rating
            Text("Your Rating*",
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
            Text("What Can We Improve",
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            Box {

                MyTextField(
                    value = selectedModule,
                    onValueChange = { selectedModule = it },
                    placeholderText = "Enter Email",
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
            Text("Your Feedback*",
                style = AppTextStyles.style14GCN())
            Spacer(modifier = Modifier.height(6.dp))

            MyTextField(
                value = feedback,
                onValueChange = { feedback = it },
                placeholderText = "Enter Email",
                fixedHeight = 120
            )

            Spacer(modifier = Modifier.weight(1f))


            // 🔹 Button
            Button(
                onClick = { /* Submit */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor)
            ) {
                Text("Save & Track Symptoms")
            }
        }
    }
}