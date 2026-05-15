package com.example.vitalio_cis.ui.screens.onboarding


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
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard


@Composable
fun FluidDetailsScreen() {

    var weight by remember {
        mutableStateOf("")
    }

    val navController = LocalNavController.current
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
            .background(Color.White)
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
                "Create Account",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )

            Text(
                "Skip",
                color = Color(0xFF3B82F6),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 📊 Progress Card
        ProgressCard(
            progress = 0f,
            title = "All Done!",
            subtitle = "You're just one step away from completing the process."
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
            text = "Fluid Intake Details",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB),
            lineHeight = 34.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "We've got the time to remind you for your vitals. Let us know about your fluid intake.",
            fontSize = 16.sp,
            color = Color.Gray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Fluid Intake",
            fontSize = 14.sp,
            color = Color.DarkGray,
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
                        text = "Enter your weight",
                        color = Color.LightGray,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFFE5E7EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
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




        Button(
            onClick = {
                navController.navigate("registrationCompleted")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF3B82F6),
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {
            Text("Next", color = Color.White)
        }
    }
}