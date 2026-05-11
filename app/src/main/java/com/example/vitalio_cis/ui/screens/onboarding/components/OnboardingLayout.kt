package com.example.vitalio_cis.ui.screens.onboarding.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vitalio_cis.ui.theme.AppDimens
import com.example.vitalio_cis.ui.theme.PrimaryBlue

@Composable
fun OnboardingLayout(
    title: String,
    subtitle: String? = null,
    buttonText: String,
    onNext: () -> Unit,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppDimens.screenPadding)
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)

        subtitle?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color.Gray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Column(modifier = Modifier.weight(1f), content = content)

        Button(
            onClick = onNext,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(AppDimens.buttonHeight),
            shape = RoundedCornerShape(AppDimens.cornerRadius),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
        ) {
            Text(buttonText, color = Color.White)
        }
    }
}