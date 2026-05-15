package com.example.vitalio_cis.ui.screens.onboarding


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
import com.example.vitalio_cis.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FluidIntakeDetailsScreen() {

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
            .background(Color.White)
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        /// =========================
        /// TOP BAR
        /// =========================

        Text(
            text = "Create Account",
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(18.dp))

        /// =========================
        /// PROGRESS CARD
        /// =========================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(14.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF5F7FB)
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

                    trackColor = Color(0xFFD9E5FF)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "One Step Away",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "You're nearly done — let's finish up!",
                    fontSize = 12.sp,
                    color = Color.Gray
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
            text = "Fluid Intake Details",

            modifier = Modifier.align(Alignment.CenterHorizontally),

            fontSize = 26.sp,

            fontWeight = FontWeight.Bold,

            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To help you stay hydrated, please specify your daily fluid intake goal.",

            modifier = Modifier.align(Alignment.CenterHorizontally),

            fontSize = 13.sp,

            lineHeight = 20.sp,

            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(30.dp))

        /// =========================
        /// LABEL
        /// =========================

        Text(
            text = "Fluid Intake Goal",
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
                    Text("Enter")
                },

                singleLine = true,

                shape = RoundedCornerShape(10.dp),

                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF2563EB),
                    unfocusedBorderColor = Color(0xFFE5E7EB)
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
                        unfocusedBorderColor = Color(0xFFE5E7EB)
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

        Button(
            onClick = {

            },

            enabled = fluidGoal.isNotEmpty(),

            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),

            shape = RoundedCornerShape(12.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB),
                disabledContainerColor = Color(0xFFE5E7EB)
            )
        ) {

            Text(
                text = "Next",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}