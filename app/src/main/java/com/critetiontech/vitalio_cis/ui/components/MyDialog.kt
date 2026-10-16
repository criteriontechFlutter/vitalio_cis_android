package com.critetiontech.ctvitalio.ui.components

import androidx.compose.runtime.Composable

/** Simple message popup; drawn with [AppDialog]. */
@Composable
fun MyDialog(
    title: String,
    message: String,
    confirmText: String = "OK",
    onConfirm: () -> Unit,
    onDismiss: (() -> Unit)? = null,
    dismissText: String? = null
) {
    AppDialog(
        title = title,
        message = message,
        confirmText = confirmText,
        onConfirm = onConfirm,
        onDismiss = { onDismiss?.invoke() },
        dismissText = dismissText,
    )
}
