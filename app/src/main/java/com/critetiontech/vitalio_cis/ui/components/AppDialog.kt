package com.critetiontech.ctvitalio.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme

/** Red used for destructive actions (logout, delete) in dialogs. */
val DialogDangerColor = Color(0xFFE5484D)

/**
 * The app's popup: rounded card on the theme's container colour, an icon in a tinted circle,
 * centred title and message, then full-width pill buttons (Cancel on the left, the action on the
 * right). [destructive] paints the action and icon red. Extra UI (a list, a picker) goes in [content].
 */
@Composable
fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    confirmText: String? = "OK",
    onConfirm: () -> Unit = onDismiss,
    dismissText: String? = null,
    destructive: Boolean = false,
    confirmEnabled: Boolean = true,
    loading: Boolean = false,
    showClose: Boolean = false,
    dismissible: Boolean = true,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val colors = LocalMyColorScheme.current
    val accent = when {
        destructive -> DialogDangerColor
        iconTint != null -> iconTint
        else -> colors.primaryBlueColor
    }
    val canDismiss = dismissible && !loading

    // Small spring pop when the card appears.
    val appear = remember { Animatable(0.9f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)) }

    Dialog(
        onDismissRequest = { if (canDismiss) onDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = canDismiss,
            dismissOnClickOutside = canDismiss,
        ),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = appear.value; scaleY = appear.value }
                .clip(RoundedCornerShape(24.dp))
                .background(colors.dashboardContainerColor)
                .padding(horizontal = 20.dp, vertical = 22.dp),
        ) {
            if (showClose) {
                Box(Modifier.fillMaxWidth()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.5f), CircleShape)
                            .bounceClick(enabled = canDismiss) { onDismiss() },
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.textGreyColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            if (icon != null) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.12f)),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                    ) {
                        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(24.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Text(
                title,
                color = colors.textDarkColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            if (!message.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    message,
                    color = colors.textGreyColor,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Center,
                )
            }

            if (content != null) {
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth(), content = content)
            }

            if (confirmText != null || dismissText != null) {
                Spacer(Modifier.height(22.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (dismissText != null) {
                        HapticButton(
                            onClick = onDismiss,
                            enabled = !loading,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.dashboardBackgroundColor,
                                contentColor = colors.textDarkColor,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) { Text(dismissText, fontSize = 15.sp, fontWeight = FontWeight.Medium) }
                    }
                    if (confirmText != null) {
                        HapticButton(
                            onClick = onConfirm,
                            enabled = confirmEnabled && !loading,
                            shape = RoundedCornerShape(14.dp),
                            haptic = if (destructive) HapticEvent.Warning else HapticEvent.Tap,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accent,
                                contentColor = Color.White,
                                disabledContainerColor = accent.copy(alpha = 0.35f),
                                disabledContentColor = Color.White,
                            ),
                            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                        ) {
                            if (loading) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                            else Text(confirmText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}
