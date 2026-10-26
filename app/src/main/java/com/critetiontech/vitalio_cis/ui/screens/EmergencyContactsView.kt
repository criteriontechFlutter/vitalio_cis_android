package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.ContactPhone
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.model.EmergencyContact
import com.critetiontech.ctvitalio.ui.components.AddPillButton
import com.critetiontech.ctvitalio.ui.components.AppBottomSheet
import com.critetiontech.ctvitalio.ui.components.AppDialog
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.utils.CommonTextField
import com.critetiontech.ctvitalio.viewmodel.EmergencyContactsViewModel

private val Relationships = listOf("Father", "Mother", "Spouse", "Brother", "Sister", "Son", "Daughter", "Friend", "Other")

/** "+91 98765 43210", "9876543210": an optional +, then 7 to 15 digits. */
private fun isValidPhone(raw: String): Boolean {
    val compact = raw.trim().replace(" ", "").replace("-", "")
    return Regex("^\\+?\\d{7,15}$").matches(compact)
}

@Composable
fun EmergencyContactsScreen(viewModel: EmergencyContactsViewModel = viewModel()) {

    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val loadError by viewModel.loadError.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val message by viewModel.message.collectAsState()

    // null: closed. A contact with id 0: adding. Otherwise: editing that contact.
    var editing by remember { mutableStateOf<EmergencyContact?>(null) }
    var removing by remember { mutableStateOf<EmergencyContact?>(null) }

    LaunchedEffect(message) {
        message?.let { showToast(context, it); viewModel.messageShown() }
    }

    CommonAppBar(
        title = "Emergency Contacts",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    loading && contacts.isEmpty() ->
                        CircularProgressIndicator(color = colors.primaryBlueColor, modifier = Modifier.align(Alignment.Center))
                    loadError != null && contacts.isEmpty() -> ContactsMessage(
                        title = "Couldn't load your contacts",
                        text = loadError!!,
                        actionText = "Try again",
                        onAction = viewModel::loadContacts,
                    )
                    contacts.isEmpty() -> ContactsMessage(
                        title = "No emergency contacts yet",
                        text = "Add the people we should reach if something goes wrong.",
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            ContactItem(
                                relation = contact.relationship?.trim().orEmpty().ifEmpty { "Contact" },
                                name = contact.contactName?.trim().orEmpty(),
                                phone = contact.contactNumber?.trim().orEmpty(),
                                onEdit = { editing = contact },
                                onRemove = { removing = contact },
                            )
                        }
                    }
                }
            }

            AddPillButton(
                text = "+ Add Emergency Contact",
                enabled = !loading || contacts.isNotEmpty(),
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                onClick = { editing = EmergencyContact() },
            )
        }
    }

    editing?.let { contact ->
        ContactFormSheet(
            contact = contact,
            saving = saving,
            onDismiss = { if (!saving) editing = null },
            onSave = { name, number, relation ->
                viewModel.saveContact(contact.takeIf { it.id != 0 }, name, number, relation) { editing = null }
            },
        )
    }

    removing?.let { contact ->
        AppDialog(
            title = "Remove contact?",
            message = "${contact.contactName?.trim().orEmpty().ifEmpty { "This contact" }} will no longer be listed as an emergency contact.",
            icon = Icons.Rounded.DeleteOutline,
            confirmText = "Remove",
            dismissText = "Cancel",
            destructive = true,
            loading = saving,
            onConfirm = { viewModel.removeContact(contact) { removing = null } },
            onDismiss = { if (!saving) removing = null },
        )
    }
}

@Composable
private fun ContactsMessage(title: String, text: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Icon(Icons.Rounded.ContactPhone, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(40.dp))
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

@Composable
fun ContactItem(
    relation: String,
    name: String,
    phone: String,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {

            Column(modifier = Modifier.weight(1f)) {
                Text(relation, style = AppTextStyles.style12GCN())
                Text(name,  style = AppTextStyles.style14BCB())
                Text(phone,  style = AppTextStyles.style14PCN())
            }

            if (phone.isNotEmpty()) {
                HapticIconButton(onClick = {
                    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${phone.replace(" ", "")}"))) }
                }) {
                    Icon(Icons.Default.Call, contentDescription = "Call $name", tint = Color(0xFF2962FF))
                }
            }

            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.Gray,
                    modifier = Modifier.clip(CircleShape).clickable { expanded = true }.padding(4.dp)
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { expanded = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        onClick = { expanded = false; onRemove() }
                    )
                }
            }
        }
    }
}

/** Add / edit sheet. [contact] with id 0 means a new contact. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContactFormSheet(
    contact: EmergencyContact,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, number: String, relationship: String) -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val isNew = contact.id == 0
    val savedRelation = contact.relationship?.trim().orEmpty()

    var name by rememberSaveable { mutableStateOf(contact.contactName?.trim().orEmpty()) }
    var number by rememberSaveable { mutableStateOf(contact.contactNumber?.trim().orEmpty()) }
    // A relationship outside the list (typed in earlier) opens as "Other" with its text kept.
    var relation by rememberSaveable {
        mutableStateOf(Relationships.firstOrNull { it.equals(savedRelation, ignoreCase = true) } ?: if (savedRelation.isEmpty()) "" else "Other")
    }
    var otherRelation by rememberSaveable {
        mutableStateOf(if (Relationships.any { it.equals(savedRelation, ignoreCase = true) }) "" else savedRelation)
    }
    var showErrors by rememberSaveable { mutableStateOf(false) }

    val finalRelation = if (relation == "Other") otherRelation.trim() else relation
    val nameOk = name.isNotBlank()
    val numberOk = isValidPhone(number)
    val relationOk = finalRelation.isNotEmpty()

    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = if (isNew) "Add Emergency Contact" else "Edit Emergency Contact",
        subtitle = "We'll reach this person in an emergency.",
        icon = Icons.Rounded.ContactPhone,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp),
        ) {
            FieldLabel("Name")
            CommonTextField(
                value = name,
                onValueChange = { name = it.take(60) },
                hint = "Contact's full name",
                isError = showErrors && !nameOk,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            )
            if (showErrors && !nameOk) FieldError("Enter the contact's name")

            Spacer(Modifier.height(14.dp))
            FieldLabel("Mobile number")
            CommonTextField(
                value = number,
                onValueChange = { v -> number = v.filter { it.isDigit() || it == '+' || it == ' ' }.take(18) },
                hint = "e.g. 9876543210",
                isError = showErrors && !numberOk,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            )
            if (showErrors && !numberOk) FieldError("Enter a valid mobile number")

            Spacer(Modifier.height(14.dp))
            FieldLabel("Relationship")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Relationships.forEach { option ->
                    val selected = relation == option
                    Text(
                        option,
                        color = if (selected) Color.White else colors.textDarkColor,
                        fontSize = 13.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) colors.primaryBlueColor else Color.Transparent)
                            .border(1.dp, if (selected) colors.primaryBlueColor else colors.borderGreyLightColor, RoundedCornerShape(50))
                            .clickable {
                                if (!selected) haptic(HapticEvent.Selection)
                                relation = option
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            if (relation == "Other") {
                Spacer(Modifier.height(10.dp))
                CommonTextField(
                    value = otherRelation,
                    onValueChange = { otherRelation = it.take(30) },
                    hint = "e.g. Neighbour, Uncle",
                    isError = showErrors && !relationOk,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            if (showErrors && !relationOk) FieldError("Choose how this person is related to you")

            Spacer(Modifier.height(20.dp))
            CommonButton(
                text = if (isNew) "Save Contact" else "Update Contact",
                isLoading = saving,
                cornerRadius = 14.dp,
                containerColor = colors.primaryBlueColor,
                modifier = Modifier.height(54.dp),
                onClick = {
                    if (nameOk && numberOk && relationOk) onSave(name, number.replace(" ", ""), finalRelation)
                    else { showErrors = true; haptic(HapticEvent.Error) }
                },
            )
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text, color = LocalMyColorScheme.current.textGreyColor, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
}

@Composable
private fun FieldError(text: String) {
    Text(text, color = Color(0xFFDC2626), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, start = 4.dp))
}
