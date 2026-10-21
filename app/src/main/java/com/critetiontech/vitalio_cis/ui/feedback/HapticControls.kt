package com.critetiontech.ctvitalio.ui.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape

// Drop-in replacements for the Material controls with the same parameters, adding the app's
// haptic on every click. Material buttons draw their own ripple and ignore LocalIndication,
// which is why they need wrapping while plain `Modifier.clickable` is covered by HapticIndication.

@Composable
fun HapticButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    haptic: HapticEvent = HapticEvent.Tap,
    content: @Composable RowScope.() -> Unit,
) {
    val perform = rememberHaptics()
    Button(
        onClick = { perform(haptic); onClick() },
        modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation,
        border = border, contentPadding = contentPadding, interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun HapticTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    haptic: HapticEvent = HapticEvent.Tap,
    content: @Composable RowScope.() -> Unit,
) {
    val perform = rememberHaptics()
    TextButton(
        onClick = { perform(haptic); onClick() },
        modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation,
        border = border, contentPadding = contentPadding, interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun HapticOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    haptic: HapticEvent = HapticEvent.Tap,
    content: @Composable RowScope.() -> Unit,
) {
    val perform = rememberHaptics()
    OutlinedButton(
        onClick = { perform(haptic); onClick() },
        modifier = modifier, enabled = enabled, shape = shape, colors = colors, elevation = elevation,
        border = border, contentPadding = contentPadding, interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun HapticIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    haptic: HapticEvent = HapticEvent.Tap,
    content: @Composable () -> Unit,
) {
    val perform = rememberHaptics()
    IconButton(
        onClick = { perform(haptic); onClick() },
        modifier = modifier, enabled = enabled, colors = colors, interactionSource = interactionSource,
        content = content,
    )
}

@Composable
fun HapticSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val perform = rememberHaptics()
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change -> { perform(HapticEvent.Toggle); change(it) } },
        modifier = modifier, thumbContent = thumbContent, enabled = enabled, colors = colors,
        interactionSource = interactionSource,
    )
}

@Composable
fun HapticCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    val perform = rememberHaptics()
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange?.let { change -> { perform(HapticEvent.Toggle); change(it) } },
        modifier = modifier, enabled = enabled, colors = colors, interactionSource = interactionSource,
    )
}
