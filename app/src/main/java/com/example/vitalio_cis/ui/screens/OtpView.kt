package com.example.vitalio_cis.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R
import com.example.vitalio_cis.viewmodel.OTPViewModel
import kotlinx.coroutines.delay

@Composable
fun OtpScreen(viewModel: OTPViewModel = viewModel()) {
    val navController = LocalNavController.current
    val context = LocalContext.current

    var otpValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    var resendTrigger by remember { mutableIntStateOf(0) }
    var timerSeconds by remember { mutableIntStateOf(30) }

    // Entrance animation state
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(50)
        visible = true
        delay(150)
        focusRequester.requestFocus()
    }

    val headerAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val headerY by animateFloatAsState(
        targetValue = if (visible) 0f else -36f,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "headerY"
    )
    val imageAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "imageAlpha"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.9f,
        animationSpec = tween(700, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "imageScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "cardAlpha"
    )
    val cardY by animateFloatAsState(
        targetValue = if (visible) 0f else 80f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardY"
    )
    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 380, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleY by animateFloatAsState(
        targetValue = if (visible) 0f else 16f,
        animationSpec = tween(400, delayMillis = 380, easing = FastOutSlowInEasing),
        label = "titleY"
    )

    LaunchedEffect(resendTrigger) {
        timerSeconds = 30
        while (timerSeconds > 0) {
            delay(1000)
            timerSeconds--
        }
    }

    // Auto-verify when all 6 digits are entered
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6) {
            viewModel.verifyLogInOTPForSHFCApp(
                context = context,
                otp = otpValue,
                uhid = "6307748142",
                navController = navController
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E63D5))
            .imePadding()
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
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer { alpha = headerAlpha; translationY = headerY }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer { alpha = imageAlpha; scaleX = imageScale; scaleY = imageScale },
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter
            )

            Box(modifier = Modifier.graphicsLayer { alpha = cardAlpha; translationY = cardY }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Box(modifier = Modifier.graphicsLayer { alpha = titleAlpha; translationY = titleY }) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Hidden field — owns the keyboard and captures SMS autofill
                    BasicTextField(
                        value = otpValue,
                        onValueChange = { new ->
                            val filtered = new.filter { it.isDigit() }.take(6)
                            otpValue = filtered
                        },
                        modifier = Modifier
                            .size(1.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                if (otpValue.length == 6) {
                                    viewModel.verifyLogInOTPForSHFCApp(
                                        context = context,
                                        otp = otpValue,
                                        uhid = "6307748142",
                                        navController = navController
                                    )
                                }
                            }
                        )
                    )

                    // Visual OTP digit boxes — tapping any box restores keyboard focus
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { focusRequester.requestFocus() }
                    ) {
                        repeat(6) { index ->
                            val boxScale by animateFloatAsState(
                                targetValue = if (visible) 1f else 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium,
                                    visibilityThreshold = 0.001f
                                ).let {
                                    tween(340, delayMillis = 420 + index * 65, easing = FastOutSlowInEasing)
                                },
                                label = "boxScale_$index"
                            )
                            OtpDigitBox(
                                digit = otpValue.getOrNull(index)?.toString() ?: "",
                                isActive = index == otpValue.length && otpValue.length < 6,
                                modifier = Modifier
                                    .weight(1f)
                                    .graphicsLayer { scaleX = boxScale; scaleY = boxScale; alpha = boxScale }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.verifyLogInOTPForSHFCApp(
                                context = context,
                                otp = otpValue,
                                uhid = "6307748142",
                                navController = navController
                            )
                        },
                        enabled = otpValue.length == 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E63D5),
                            disabledContainerColor = Color(0xFF1E63D5).copy(alpha = 0.4f)
                        )
                    ) {
                        Text(text = "Verify", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Didn't receive the Code?",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (timerSeconds > 0) {
                        Text(
                            text = "Resend OTP in ${timerSeconds}s",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    } else {
                        Text(
                            text = "Resend OTP",
                            fontSize = 14.sp,
                            color = Color(0xFF1E63D5),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { resendTrigger++ }
                        )
                    }

                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
            } // Box(cardAlpha)
        }
    }
}

@Composable
private fun OtpDigitBox(
    digit: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor by animateColorAsState(
        targetValue = when {
            isActive -> Color(0xFF1E63D5)
            digit.isNotEmpty() -> Color(0xFF1E63D5).copy(alpha = 0.5f)
            else -> Color(0xFFCCCCCC)
        },
        label = "otpBorder"
    )
    val borderWidth by animateDpAsState(
        targetValue = if (isActive) 2.dp else 1.dp,
        label = "otpBorderWidth"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(borderWidth, borderColor, RoundedCornerShape(8.dp))
            .background(
                color = if (digit.isNotEmpty()) Color(0xFFF0F4FF) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (digit.isNotEmpty()) {
            Text(
                text = digit,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E63D5),
                textAlign = TextAlign.Center
            )
        } else if (isActive) {
            val infiniteTransition = rememberInfiniteTransition(label = "cursor")
            val cursorAlpha by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(500),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "cursorAlpha"
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(22.dp)
                    .background(Color(0xFF1E63D5).copy(alpha = cursorAlpha))
            )
        }
    }
}
