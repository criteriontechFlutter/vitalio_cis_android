package com.critetiontech.ctvitalio.ui.components

import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
 import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.ThemeViewModel

@Composable
fun MyButton(
    text: String,
    onClick:   () -> Unit,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(74.dp)
        .padding(top = 24.dp),
    enabled: Boolean = true,
    shape: RoundedCornerShape = RoundedCornerShape(14.dp),
    textStyle: TextStyle? = null,
    themeViewModel: ThemeViewModel = viewModel()
) {
    val colors by themeViewModel.colorScheme.collectAsState()
    val defaultTextStyle = textStyle ?: AppTextStyles(themeViewModel)
        .style14GCB(colors.text_color_white)

    Button(
        onClick =  onClick,
        modifier = modifier,
        shape = shape,
        enabled = enabled,
        colors = dynamicButtonColors(themeViewModel)
    ) {
        Text(
            text = text,
            style = defaultTextStyle
        )
    }
}


    @Composable
fun dynamicButtonColors(themeViewModel: ThemeViewModel = viewModel()): ButtonColors {
    val colors by themeViewModel.colorScheme.collectAsState()
    return ButtonDefaults.buttonColors(
        containerColor = colors.primary,       // dynamic background
        contentColor = colors.text_color_white // dynamic text/icon
    )
}