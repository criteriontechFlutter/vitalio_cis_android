package com.critetiontech.ctvitalio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import kotlinx.coroutines.launch

/**
 * The app's bottom sheet, matching [AppDialog]: theme container colour, 28dp top corners, a slim
 * handle and an optional header (tinted icon, title, subtitle, close button). Screens put their own
 * body in [content].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color? = null,
    showClose: Boolean = title != null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val scope = rememberCoroutineScope()
    val accent = iconTint ?: colors.primaryBlueColor

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = colors.dashboardContainerColor,
        contentColor = colors.textDarkColor,
        tonalElevation = 0.dp,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colors.borderGreyLightColor.copy(alpha = 0.6f))
            )
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            if (title != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 16.dp, top = 6.dp, bottom = 14.dp),
                ) {
                    if (icon != null) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.12f)),
                        ) {
                            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            color = colors.textDarkColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!subtitle.isNullOrBlank()) {
                            Spacer(Modifier.height(2.dp))
                            Text(subtitle, color = colors.textGreyColor, fontSize = 13.sp, lineHeight = 18.sp)
                        }
                    }
                    if (showClose) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.5f), CircleShape)
                                .bounceClick {
                                    scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
                                },
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = colors.textGreyColor, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            content()
        }
    }
}
