package com.critetiontech.ctvitalio.utils

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
import com.critetiontech.ctvitalio.ui.theme.AppColors
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark

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
    val colors = LocalMyColorScheme.current
    val dark = isAppDark
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(text = hint, color = if (dark) colors.textGreyColor else Color(0xFFAAAAAA))
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
            unfocusedBorderColor = if (dark) colors.borderGreyLightColor else Color(0xFFE0E0E0),
            errorBorderColor = AppColors.red,
            errorCursorColor = AppColors.red,
            focusedTextColor = if (dark) colors.textDarkColor else AppColors.black,
            unfocusedTextColor = if (dark) colors.textDarkColor else AppColors.black,
            cursorColor = AppColors.Primary,
            // Light mode keeps the original white/off-white field; dark mode follows the theme
            focusedContainerColor = if (dark) colors.dashboardBackgroundColor else Color.White,
            unfocusedContainerColor = if (dark) colors.dashboardBackgroundColor else Color(0xFFF9FAFB)
        )
    )
}
