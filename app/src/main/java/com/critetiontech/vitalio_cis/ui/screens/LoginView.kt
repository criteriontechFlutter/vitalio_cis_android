package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.ui.theme.AppColors
import com.critetiontech.vitalio_cis.utils.CommonButton
import com.critetiontech.vitalio_cis.utils.CommonTextField
import com.critetiontech.vitalio_cis.viewmodel.LoginViewModel

@Composable
fun LoginScreen() {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val imageAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(800, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "imageAlpha"
    )
    val imageScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = tween(800, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "imageScale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 200),
        label = "cardAlpha"
    )
    val cardY by animateFloatAsState(
        targetValue = if (visible) 0f else 90f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "cardY"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E63D5))
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Spacer(modifier = Modifier.height(30.dp))

            Image(
                painter = painterResource(id = R.drawable.img),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        alpha = imageAlpha
                        scaleX = imageScale
                        scaleY = imageScale
                    },
                contentScale = ContentScale.Fit,
                alignment = Alignment.BottomCenter
            )

            Box(modifier = Modifier.graphicsLayer {
                alpha = cardAlpha
                translationY = cardY
            }) {
                LoginCard()
            }
        }
    }
}

@Composable
fun LoginCard(viewModel: LoginViewModel = viewModel()) {
    val navController = LocalNavController.current

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { route -> navController.navigate(route) }
    }

    val titleAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "titleAlpha"
    )
    val titleY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(400, delayMillis = 350, easing = FastOutSlowInEasing),
        label = "titleY"
    )
    val fieldAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "fieldAlpha"
    )
    val fieldY by animateFloatAsState(
        targetValue = if (visible) 0f else 18f,
        animationSpec = tween(400, delayMillis = 500, easing = FastOutSlowInEasing),
        label = "fieldY"
    )
    val buttonAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(400, delayMillis = 650),
        label = "buttonAlpha"
    )
    val buttonScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.88f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Box(modifier = Modifier.graphicsLayer {
                alpha = titleAlpha
                translationY = titleY
            }) {
                Column {
                    Text(
                        text = "Login/Sign Up",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.Primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Access your health records and services",
                        color = Color(0xFF888888),
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(modifier = Modifier.graphicsLayer {
                alpha = fieldAlpha
                translationY = fieldY
            }) {
                Column {
                    Text(
                        text = "Enter UHID/Mobile No",
                        fontSize = 13.sp,
                        color = Color(0xFF444444)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    CommonTextField(
                        value = viewModel.mobile,
                        onValueChange = { viewModel.onMobileChange(it) },
                        hint = "Enter UHID/Mobile No.",
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Badge,
                                contentDescription = null,
                                tint = Color(0xFFAAAAAA),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.sendOTP()
                            }
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.graphicsLayer {
                alpha = buttonAlpha
                scaleX = buttonScale
                scaleY = buttonScale
            }) {
                CommonButton(
                    text = "Send OTP",
                    onClick = {
                        viewModel.sendOTP()
                    }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "By signing in you agree to our",
                fontSize = 12.sp,
                color = Color(0xFF888888),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer { alpha = titleAlpha }
            )
            Text(
                text = "Terms & Conditions and Privacy Policy",
                fontSize = 12.sp,
                color = AppColors.Primary,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .graphicsLayer { alpha = titleAlpha }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
