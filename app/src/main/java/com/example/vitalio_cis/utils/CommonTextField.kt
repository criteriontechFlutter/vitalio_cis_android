package com.example.vitalio_cis.utils

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.vitalio_cis.ui.theme.AppColors

@Composable
fun CommonTextField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    isError: Boolean = false,
    leadingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = hint, color = Color(0xFFAAAAAA))
        },
        leadingIcon = leadingIcon,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        isError = isError,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AppColors.Primary,
            unfocusedBorderColor = Color(0xFFE0E0E0),
            errorBorderColor = AppColors.red,
            errorCursorColor = AppColors.red,
            focusedTextColor = AppColors.black,
            unfocusedTextColor = AppColors.black,
            cursorColor = AppColors.Primary,
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color(0xFFF9FAFB)
        )
    )
}
