package com.example.vitalio_cis.ui.screens.onboarding


import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.R

@Composable
fun ThankYouView() {

    val navController = LocalNavController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8F8F8))
            .padding(horizontal = 16.dp)
    ) {

        Spacer(modifier = Modifier.height(14.dp))

        /// =========================
        /// TOP BAR
        /// =========================

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {

                        navController.popBackStack()
                    },

                contentAlignment = Alignment.Center
            ) {

                Icon(
                    imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Black
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Text(
                text = "Create Account",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.height(26.dp))

        /// =========================
        /// THANK YOU IMAGE
        /// =========================

        Image(
            painter = painterResource(id = R.drawable.thankyoupng),
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),

            contentScale = ContentScale.Fit
        )

        Spacer(modifier = Modifier.height(24.dp))

        /// =========================
        /// PROGRESS CARD
        /// =========================

        Card(
            modifier = Modifier.fillMaxWidth(),

            shape = RoundedCornerShape(16.dp),

            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFF1F5F9)
            ),

            elevation = CardDefaults.cardElevation(
                defaultElevation = 0.dp
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
            ) {

                /// PROGRESS TEXT

                Text(
                    text = "81%",
                    color = Color(0xFF2563EB),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                /// PROGRESS BAR

                LinearProgressIndicator(
                    progress = { 0.81f },

                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(20.dp)),

                    color = Color(0xFF2563EB),

                    trackColor = Color(0xFFD6E4FF)
                )

                Spacer(modifier = Modifier.height(14.dp))

                /// TITLE

                Text(
                    text = "Thank you for registering with Vitalio!",

                    modifier = Modifier.fillMaxWidth(),

                    textAlign = TextAlign.Center,

                    fontSize = 16.sp,

                    fontWeight = FontWeight.Bold,

                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(6.dp))

                /// SUBTITLE

                Text(
                    text = "Your submission is now complete, and we're excited to have you on board.",

                    modifier = Modifier.fillMaxWidth(),

                    textAlign = TextAlign.Center,

                    fontSize = 13.sp,

                    lineHeight = 18.sp,

                    color = Color.Gray
                )
            }
        }


        /// =========================
        /// WHAT'S NEXT
        /// =========================

        Text(
            text = "What’s Next?",

            modifier = Modifier.fillMaxWidth(),

            textAlign = TextAlign.Center,

            fontSize = 34.sp,

            fontWeight = FontWeight.Bold,

            color = Color(0xFF2563EB)
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "To help you maintain optimal health, we need a few more details.",

            modifier = Modifier.fillMaxWidth(),

            textAlign = TextAlign.Center,

            fontSize = 13.sp,

            fontStyle = FontStyle.Italic,

            lineHeight = 20.sp,

            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(36.dp))

        /// =========================
        /// BUTTON
        /// =========================

        Button(
            onClick = {

                navController.navigate("reminderPreference")
            },

            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),

            shape = RoundedCornerShape(14.dp),

            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2563EB)
            )
        ) {

            Text(
                text = "Set preferences for a better experience",

                color = Color.White,

                fontSize = 15.sp,

                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}