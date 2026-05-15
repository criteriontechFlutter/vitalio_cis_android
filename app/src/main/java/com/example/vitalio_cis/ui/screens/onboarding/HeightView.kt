package com.example.vitalio_cis.ui.screens.onboarding
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
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.screens.onboarding.components.ProgressCard


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
    val sheetState = rememberModalBottomSheetState()

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
            title = "Moving Ahead",
            subtitle = "You're past halfway-great job so far!"
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
            text = "What is your height?",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Your height helps us offer more accurate health\nand fitness insights.",
            fontSize = 16.sp,
            color = Color.Gray,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "Height",
            fontSize = 14.sp,
            color = Color.Gray
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
                Text("Select your height")
            },
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledBorderColor = Color(0xFFE5E7EB),
                disabledTextColor = Color.Black,
                disabledContainerColor = Color.White
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                navController.navigate("chronicDisease")
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

    /// BOTTOM SHEET

    if (showBottomSheet) {

        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = sheetState,
            containerColor = Color.White
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
            text = "Select Your Height",
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
                    text = "Feet",
                    fontSize = 12.sp,
                    color = Color.Gray
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
                                    Color.Gray
                        )
                    }
                }
            }

            /// INCH

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Inch",
                    fontSize = 12.sp,
                    color = Color.Gray
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
                                    Color.Gray
                        )
                    }
                }
            }

            /// UNIT

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "Unit",
                    fontSize = 12.sp,
                    color = Color.Gray
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
                    color = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Submit")
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}