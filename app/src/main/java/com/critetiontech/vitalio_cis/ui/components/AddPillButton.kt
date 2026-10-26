package com.critetiontech.ctvitalio.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.feedback.pressScale
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

/**
 * The white pill "+ Add …" button at the foot of list screens (family history, allergies, emergency
 * contacts). Clipping before the click keeps the ripple inside the pill.
 */
@Composable
fun AddPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val colors = LocalMyColorScheme.current
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            // Scale before clipping so the ripple shrinks with the pill and keeps its rounded ends.
            .pressScale(interactionSource)
            .clip(RoundedCornerShape(50))
            .background(colors.dashboardContainerColor)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                enabled = enabled && !loading,
                onClick = onClick,
            ),
    ) {
        if (loading) {
            CircularProgressIndicator(color = colors.primaryBlueColor, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(
                text,
                color = colors.textDarkColor.copy(alpha = if (enabled) 1f else 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}
