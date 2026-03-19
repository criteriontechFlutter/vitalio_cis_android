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
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
 import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.ThemeViewModel


@Composable
fun MyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
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
    fixedHeight: Int = 56 // 🔹 default Material height for text fields
) {
    val themeViewModel: ThemeViewModel = viewModel()
    val colorScheme by themeViewModel.colorScheme.collectAsState()

    val dynamicColors = colors ?: OutlinedTextFieldDefaults.colors(
//        focusedTextColor = colorScheme.textfeild_border_color_grey,
//        unfocusedTextColor = colorScheme.textfeild_border_color_grey,
//        disabledTextColor = colorScheme.textfeild_border_color_grey,
        cursorColor = colorScheme.textfeild_border_color_grey,
        focusedBorderColor = if (isError) colorScheme.textfeild_validation_color else colorScheme.textfeild_border_color_grey,
        unfocusedBorderColor = colorScheme.textfeild_border_color_grey,
         disabledBorderColor = colorScheme.textfeild_border_color_grey,
    )

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = placeholderText,
                    style = textStyle ?: AppTextStyles(themeViewModel)
                        .style14GCB(colorScheme.textfeild_placehoder_color_grey)
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
                .height(fixedHeight.dp) // ✅ fix height
        )

        if (isError && errorMessage != null) {
            Box(modifier = Modifier.height(14.dp)) {
                Text(
                    text = errorMessage,
                    color = colorScheme.textfeild_validation_color,
                    fontSize = 12.sp,
                )
            }
        }
    }
}