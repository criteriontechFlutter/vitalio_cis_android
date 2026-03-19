package com.example.vitalio_cis.utils

import android.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.vitalio_cis.ui.theme.AppColors

@Composable
fun CommonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isError: Boolean = false,
    modifier: Modifier = Modifier
) {

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = hint)
        },
        modifier = modifier
            .fillMaxWidth().height(50.dp)
            .border(
                width = if (isError) 1.dp else 1.dp,
                color = if (isError) AppColors.red else AppColors.grey,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = isError,

        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.grey,
            unfocusedBorderColor = AppColors.grey,
            errorBorderColor = AppColors.red,
            errorCursorColor = AppColors.red,
            focusedTextColor = AppColors.black,
            cursorColor = AppColors.grey
        )
    )
}