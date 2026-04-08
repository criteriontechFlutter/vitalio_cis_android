package com.example.vitalio_cis.ui.screens

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.viewmodel.LoginViewModel
import com.example.myapplication.utils.LocalNavController
import kotlinx.coroutines.delay

import com.example.vitalio_cis.R
import com.example.vitalio_cis.viewmodel.OTPViewModel

@Composable
fun OtpScreen(  viewModel: OTPViewModel = viewModel()) {

    val navController = LocalNavController.current
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }
    val focusRequesters = List(6) { FocusRequester() }

    val context = LocalContext.current
    // 👉 Auto focus first field
    LaunchedEffect(Unit) {
        delay(200)
        focusRequesters[0].requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E63D5))
    ) {

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(40.dp))

            Text(
                text = "Empower Your Health with\nOur Smart App!",
                color = Color.White,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth(),
                contentScale = ContentScale.Fit
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        text = "Verify your UHID!",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1E63D5)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Enter 6 digit verification code sent to your number",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        otpValues.forEachIndexed { index, value ->

                            OutlinedTextField(
                                value = value,
                                onValueChange = {
                                    if (it.length <= 1) {
                                        otpValues[index] = it

                                        // 👉 Move to next field
                                        if (it.isNotEmpty() && index < 5) {
                                            focusRequesters[index + 1].requestFocus()
                                        }

                                        // 👉 Move back on delete
                                        if (it.isEmpty() && index > 0) {
                                            focusRequesters[index - 1].requestFocus()
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(56.dp)
                                    .focusRequester(focusRequesters[index]),
                                textStyle = TextStyle(
                                    fontSize = 20.sp,
                                    textAlign = TextAlign.Center
                                ),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number
                                ),
                                keyboardActions = KeyboardActions(
                                    onNext = {
                                        if (index < 5) {
                                            focusRequesters[index + 1].requestFocus()
                                        }
                                    }
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            val otp = otpValues.joinToString("")
                            println("OTP Entered: $otp")

                            viewModel.verifyLogInOTPForSHFCApp(
                                context = context,
                                otp =otp,
                                uhid = "6307748142",
                                navController = navController
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E63D5)
                        )
                    ) {
                        Text("Verify")
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Didn't receive the Code?",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "Resend OTP",
                        fontSize = 14.sp,
                        color = Color(0xFF1E63D5),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { }
                    )
                }
            }
        }
    }
}