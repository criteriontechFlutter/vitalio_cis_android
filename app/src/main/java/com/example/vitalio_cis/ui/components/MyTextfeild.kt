package com.critetiontech.ctvitalio.ui.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
 import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel


@Composable
fun MyTextField(
    value: String = "",
    onValueChange: (String) -> Unit = {},
    placeholderText: String = "",
    textStyle: TextStyle? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp),
    singleLine: Boolean = true,
    isError: Boolean = false,
    errorMessage: String? = null,
    shape: RoundedCornerShape = RoundedCornerShape(12.dp),
    colors: TextFieldColors? = null,
    fixedHeight: Int = 50
) {

    val themeViewModel: ThemeViewModel = viewModel()
    val themeColors by themeViewModel.colorScheme.collectAsState()

    val dynamicColors = colors ?: OutlinedTextFieldDefaults.colors(
        cursorColor = themeColors.primaryBlueColor,
        focusedBorderColor = if (isError)
            Color.Red
        else
            themeColors.btnWhiteColor,

        unfocusedBorderColor = themeColors.borderGreyLightColor,
        disabledBorderColor = themeColors.borderGreyLightColor,
        focusedTextColor = themeColors.borderGreyLightColor,
        unfocusedTextColor = themeColors.borderGreyLightColor,
        disabledTextColor = themeColors.borderGreyLightColor
    )

    Column(modifier = modifier) {

        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = textStyle ?: MaterialTheme.typography.bodyMedium,
                    color = themeColors.textGreyColor
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            singleLine = singleLine,
            shape = shape,
            colors = dynamicColors,
            isError = isError,
            modifier = Modifier
                .fillMaxWidth()
                .height(fixedHeight.dp)
        )

        if (isError && !errorMessage.isNullOrEmpty()) {
            Text(
                text = errorMessage,
                color = Color.Red,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp)
            )
        }
    }
}