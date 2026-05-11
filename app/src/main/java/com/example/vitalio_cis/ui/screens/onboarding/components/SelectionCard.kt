package com.example.vitalio_cis.ui.screens.onboarding.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vitalio_cis.ui.theme.AppDimens
import com.example.vitalio_cis.ui.theme.LightGray
import com.example.vitalio_cis.ui.theme.PrimaryBlue
import com.example.vitalio_cis.ui.theme.TextPrimary

@Composable
fun SelectionCard(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(AppDimens.cornerRadius),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) PrimaryBlue else LightGray
        )
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            Text(
                text,
                modifier = Modifier.padding(start = 16.dp),
                color = if (selected) Color.White else TextPrimary
            )
        }
    }
}