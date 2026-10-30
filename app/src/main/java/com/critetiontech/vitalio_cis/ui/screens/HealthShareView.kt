package com.critetiontech.ctvitalio.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.HealthShareCreated
import com.critetiontech.ctvitalio.model.HealthShareLink
import com.critetiontech.ctvitalio.model.healthShareLocalTime
import com.critetiontech.ctvitalio.ui.components.AddPillButton
import com.critetiontech.ctvitalio.ui.components.AppBottomSheet
import com.critetiontech.ctvitalio.ui.components.AppDialog
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.utils.CommonTextField
import com.critetiontech.ctvitalio.viewmodel.HealthShareViewModel
import java.time.format.DateTimeFormatter

private val ActiveGreen = Color(0xFF16A34A)
private val DangerRed = Color(0xFFDC2626)

@Composable
fun HealthShareScreen(viewModel: HealthShareViewModel = viewModel()) {

    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val links by viewModel.links.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val created by viewModel.created.collectAsState()
    val message by viewModel.message.collectAsState()

    var sharing by rememberSaveable { mutableStateOf(false) }
    var revoking by remember { mutableStateOf<HealthShareLink?>(null) }

    LaunchedEffect(message) {
        message?.let { showToast(context, it); viewModel.messageShown() }
    }

    val active = links.filter { it.active }
    val revoked = links.filterNot { it.active }

    CommonAppBar(title = stringResource(R.string.share_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) {
                item { IntroCard() }
                when {
                    loading && links.isEmpty() -> item {
                        Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = colors.primaryBlueColor)
                        }
                    }
                    loadError != null && links.isEmpty() -> item {
                        ShareMessage(
                            title = stringResource(R.string.share_load_failed_title),
                            text = loadError!!,
                            actionText = stringResource(R.string.profile_try_again),
                            onAction = viewModel::loadLinks,
                        )
                    }
                    links.isEmpty() -> item {
                        ShareMessage(
                            title = stringResource(R.string.share_empty_title),
                            text = stringResource(R.string.share_empty_text),
                        )
                    }
                    else -> {
                        if (active.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.share_section_active, active.size)) }
                            items(active, key = { it.id }) { link -> ShareRow(link, onRevoke = { revoking = link }) }
                        }
                        if (revoked.isNotEmpty()) {
                            item { SectionHeader(stringResource(R.string.share_section_revoked, revoked.size)) }
                            items(revoked, key = { it.id }) { link -> ShareRow(link, onRevoke = null) }
                        }
                    }
                }
            }

            AddPillButton(
                text = stringResource(R.string.share_new_button),
                enabled = loadError == null || links.isNotEmpty(),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                onClick = { sharing = true },
            )
        }
    }

    if (sharing) {
        ShareFormSheet(
            saving = saving,
            onDismiss = { if (!saving) sharing = false },
            onSend = { email -> viewModel.share(email) { sharing = false } },
        )
    }

    created?.let { share -> SentDialog(share, onDismiss = viewModel::createdShown) }

    revoking?.let { link ->
        AppDialog(
            title = stringResource(R.string.share_revoke_title),
            message = stringResource(R.string.share_revoke_message, link.recipientEmail?.trim().orEmpty().ifEmpty { stringResource(R.string.share_this_person) }),
            icon = Icons.Rounded.LinkOff,
            confirmText = stringResource(R.string.share_revoke),
            dismissText = stringResource(R.string.profile_cancel),
            destructive = true,
            loading = saving,
            onConfirm = { viewModel.revoke(link) { revoking = null } },
            onDismiss = { if (!saving) revoking = null },
        )
    }
}

@Composable
private fun IntroCard() {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(colors.primaryBlueColor.copy(alpha = 0.12f), CircleShape),
        ) {
            Icon(Icons.Rounded.Shield, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.share_intro_title), color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.share_intro_text), color = colors.textGreyColor, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text,
        color = LocalMyColorScheme.current.textGreyColor,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
    )
}

@Composable
private fun ShareRow(link: HealthShareLink, onRevoke: (() -> Unit)?) {
    val colors = LocalMyColorScheme.current
    val locale = LocalConfiguration.current.locales[0]
    val formatter = remember(locale) { DateTimeFormatter.ofPattern("d MMM yyyy, h:mm a", locale) }
    val email = link.recipientEmail?.trim().orEmpty()
    val sharedAt = healthShareLocalTime(link.createdDate)?.format(formatter)
    val revokedAt = healthShareLocalTime(link.revokedAt)?.format(formatter)
    val statusColor = if (link.active) ActiveGreen else colors.textGreyColor

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(40.dp).background(statusColor.copy(alpha = 0.12f), CircleShape),
        ) {
            Text(email.take(1).uppercase().ifEmpty { "?" }, color = statusColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                email.ifEmpty { stringResource(R.string.share_unknown_recipient) },
                color = colors.textDarkColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            val detail = when {
                !link.active && revokedAt != null -> stringResource(R.string.share_revoked_on, revokedAt)
                sharedAt != null -> stringResource(R.string.share_shared_on, sharedAt)
                else -> null
            }
            detail?.let { Text(it, color = colors.textGreyColor, fontSize = 12.sp) }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(if (link.active) R.string.share_status_active else R.string.share_status_revoked),
                color = statusColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
        if (onRevoke != null) {
            TextButton(onClick = onRevoke) {
                Text(stringResource(R.string.share_revoke), color = DangerRed, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ShareMessage(title: String, text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp),
    ) {
        Icon(Icons.Rounded.Share, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(text, color = colors.textGreyColor, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (actionText != null && onAction != null) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onAction) { Text(actionText, color = colors.primaryBlueColor, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShareFormSheet(
    saving: Boolean,
    onDismiss: () -> Unit,
    onSend: (email: String) -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    var email by rememberSaveable { mutableStateOf("") }
    var showError by rememberSaveable { mutableStateOf(false) }
    val emailOk = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    val submit = {
        if (emailOk) onSend(email.trim())
        else { showError = true; haptic(HapticEvent.Error) }
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.share_sheet_title),
        subtitle = stringResource(R.string.share_sheet_subtitle),
        icon = Icons.Rounded.Share,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(stringResource(R.string.share_email_label), color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
            CommonTextField(
                value = email,
                onValueChange = { email = it.replace(" ", "").take(100); showError = false },
                hint = stringResource(R.string.share_email_hint),
                isError = showError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSend = { submit() }),
            )
            if (showError) {
                Text(stringResource(R.string.share_email_invalid), color = DangerRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.share_sheet_note), color = colors.textGreyColor, fontSize = 12.sp, lineHeight = 17.sp)
            Spacer(Modifier.height(20.dp))
            CommonButton(
                text = stringResource(R.string.share_send),
                isLoading = saving,
                loadingText = stringResource(R.string.share_sending),
                cornerRadius = 14.dp,
                containerColor = colors.primaryBlueColor,
                modifier = Modifier.height(54.dp),
                onClick = submit,
            )
        }
    }
}

/** After sharing: confirms the email went out, and offers the link for WhatsApp etc. (the code stays in the email). */
@Composable
private fun SentDialog(share: HealthShareCreated, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current
    val url = share.shareUrl?.takeIf { it.isNotBlank() }
    val copied = stringResource(R.string.share_link_copied)
    val chooserTitle = stringResource(R.string.share_link_chooser)
    val shareText = url?.let { stringResource(R.string.share_link_message, it) }

    AppDialog(
        title = stringResource(R.string.share_sent_title),
        message = stringResource(R.string.share_sent_message, share.recipientEmail.orEmpty()),
        icon = Icons.Rounded.MarkEmailRead,
        iconTint = ActiveGreen,
        confirmText = stringResource(R.string.share_done),
        onConfirm = onDismiss,
        onDismiss = onDismiss,
    ) {
        if (url != null && shareText != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Health records link", url))
                        showToast(context, copied)
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share_copy_link), color = colors.primaryBlueColor)
                }
                OutlinedButton(
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, shareText)
                        runCatching { context.startActivity(Intent.createChooser(send, chooserTitle)) }
                    },
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.share_send_link), color = colors.primaryBlueColor)
                }
            }
        }
    }
}
