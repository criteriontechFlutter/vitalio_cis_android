package com.critetiontech.ctvitalio.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.AppTextStyles
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Material colours in the app's blue. The app has no MaterialTheme, so Material dialogs
 * (date and time pickers) would otherwise fall back to Material's default purple.
 */
@Composable
fun AppPickerTheme(content: @Composable () -> Unit) {
    val colors = LocalMyColorScheme.current
    val blue = colors.primaryBlueColor
    val blueTint = blue.copy(alpha = 0.12f).compositeOver(Color.White)
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = blue,
            onPrimary = Color.White,
            primaryContainer = blueTint,
            onPrimaryContainer = blue,
            secondaryContainer = blueTint,
            onSecondaryContainer = blue,
            tertiaryContainer = blueTint,
            onTertiaryContainer = blue,
            surface = Color.White,
            surfaceContainerHigh = Color.White,
            surfaceContainerHighest = colors.dashboardBackgroundColor,
            onSurface = colors.textDarkColor,
            onSurfaceVariant = colors.textGreyColor,
            outline = colors.borderGreyLightColor,
            outlineVariant = colors.borderGreyLightColor,
        ),
        content = content,
    )
}

/**
 * Calendar dialog in the app's style. [isSelectable] greys out days that can't be picked,
 * e.g. future days for a past report or days before a start date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    confirmText: String = "OK",
    isSelectable: (LocalDate) -> Boolean = { true },
) {
    AppPickerTheme {
        // DatePicker works in UTC midnights.
        val state = rememberDatePickerState(
            initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    isSelectable(Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate())
            },
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                HapticTextButton(
                    onClick = {
                        state.selectedDateMillis?.let {
                            onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text(confirmText) }
            },
            dismissButton = { HapticTextButton(onClick = onDismiss) { Text("Cancel") } },
        ) {
            DatePicker(state = state)
        }
    }
}

/** Clock-face time picker dialog in the app's style. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    title: String = "Select time",
) {
    AppPickerTheme {
        val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = false)
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title, style = AppTextStyles.style16BCB()) },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = state) }
            },
            confirmButton = { HapticTextButton(onClick = { onConfirm(state.hour, state.minute) }) { Text("OK") } },
            dismissButton = { HapticTextButton(onClick = onDismiss) { Text("Cancel") } },
            containerColor = Color.White,
        )
    }
}
