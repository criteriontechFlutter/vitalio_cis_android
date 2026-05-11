package com.example.vitalio_cis.ui.screens.onboarding.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ProgressCard(
    progress: Float, // 0f → 1f
    title: String,
    subtitle: String
) {
    val percentage = (progress * 100).toInt()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {

            Text(
                "$percentage%",
                color = Color(0xFF3B82F6),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp),
                color = Color(0xFF3B82F6),
                trackColor = Color(0xFFD1D5DB)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )

            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}