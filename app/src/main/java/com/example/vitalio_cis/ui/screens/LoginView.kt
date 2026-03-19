package com.critetiontech.ctvitalio.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import androidx.compose.foundation.Image
 import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
 import androidx.compose.foundation.layout.Row
 import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.viewmodel.LoginViewModel
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.theme.AppColors
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.utils.CommonTextField

@Composable
fun LoginScreen() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1E63D5))
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {

            Spacer(modifier = Modifier.height(30.dp))
            TopHeader()

            DoctorImage(
                modifier = Modifier.weight(1f)
            )

            LoginCard()
        }
    }
}

@Composable
fun TopHeader() {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center
    ) {

        Text(
            text = "Our Smart App!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}


@Composable
fun DoctorImage(modifier: Modifier = Modifier) {

    Image(
        painter = painterResource(id = R.drawable.img),
        contentDescription = null,
        modifier = modifier
            .fillMaxWidth(),
        contentScale = ContentScale.Fit
    )
}

@Composable
fun LoginCard(   viewModel: LoginViewModel = viewModel()) {

    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(
            topStart = 30.dp,
            topEnd = 30.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            modifier = Modifier.padding(24.dp)
        ) {

            Spacer(modifier = Modifier.height(15.dp))
            Text(
                text = "Login/Sign Up",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold, color = AppColors.Primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Access your health records and services",
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Enter UHID/Mobile No./Email",
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))
            CommonTextField(
                value =  viewModel.mobile,
                onValueChange = {    viewModel.onMobileChange(it)  },
                hint = "Enter UHID/Mobile No./Email"
            )

            Spacer(modifier = Modifier.height(20.dp))
            CommonButton(
                text = "Send OTP",
                onClick = {
                    viewModel.sendOTP(
                        context = context
                    );
                    // action
                }
            )


            Spacer(modifier = Modifier.height(60.dp))

            Text(
                text = "By signing in you agree to our",
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Text(
                text = "Terms & Conditions and Privacy Policy",
                fontSize = 12.sp,
                color = Color(0xFF1E63D5),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}