package com.example.vitalio_cis.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.espresso.base.Default
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.ui.theme.AppColors
import androidx.compose.ui.text.TextStyle
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CommonButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    cornerRadius: Dp = 12.dp,
    textStyle: TextStyle    = AppTextStyles.style14WCN(),
    containerColor: Color = AppColors.Primary,
    modifier: Modifier = Modifier
) {

    Button(
        onClick = { if (!isLoading) onClick() },
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .height(47.dp),
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
        )
    ) {

        if (isLoading) {

            CircularProgressIndicator(
                color = AppColors.white,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
            )

        } else {

            Text(text = text,
                style = textStyle)

        }
    }
}