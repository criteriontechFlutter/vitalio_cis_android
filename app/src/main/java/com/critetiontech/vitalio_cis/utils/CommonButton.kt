package com.critetiontech.ctvitalio.utils

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.theme.AppColors
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
    /** Shown next to the spinner while [isLoading], e.g. "Sending OTP". Spinner only when null. */
    loadingText: String? = null,
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
            // Keep the brand colour while loading instead of the greyed-out disabled look
            disabledContainerColor = if (isLoading) containerColor else ButtonDefaults.buttonColors().disabledContainerColor,
            disabledContentColor = if (isLoading) AppColors.white else ButtonDefaults.buttonColors().disabledContentColor,
        )
    ) {

        if (isLoading) {

            if (loadingText != null) {
                ButtonLoadingContent(text = loadingText, textStyle = textStyle)
            } else {
                CircularProgressIndicator(
                    color = AppColors.white,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            }

        } else {

            Text(text = text,
                style = textStyle)

        }
    }
}
/** Spinner + label with animated trailing dots ("Verifying", "Verifying.", …) for in-button loading. */
@Composable
fun ButtonLoadingContent(
    text: String,
    textStyle: TextStyle = AppTextStyles.style14WCN(),
    color: Color = AppColors.white
) {
    val dots by rememberInfiniteTransition(label = "loadingDots").animateValue(
        initialValue = 0,
        targetValue = 4,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing)),
        label = "dots"
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            color = color,
            strokeWidth = 2.dp,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(10.dp))
        Box {
            // Invisible full-width text reserves space so the row doesn't jitter as dots change
            Text(text = "$text...", style = textStyle, color = Color.Transparent)
            Text(text = text + ".".repeat(dots.coerceAtMost(3)), style = textStyle, color = color)
        }
    }
}
