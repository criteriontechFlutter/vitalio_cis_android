package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.domain.model.Disease
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.model.FamilyRelation
import com.critetiontech.ctvitalio.ui.components.AddPillButton
import com.critetiontech.ctvitalio.ui.components.AppBottomSheet
import com.critetiontech.ctvitalio.ui.components.AppDialog
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.FamilyHealthHistoryViewModel

private val FamilyRelation.tagColor: Color
    get() = when (this) {
        FamilyRelation.MOTHER -> Color(0xFFD9468F)
        FamilyRelation.FATHER -> Color(0xFF4A90D9)
        FamilyRelation.BROTHER -> Color(0xFFE8A317)
        FamilyRelation.SISTER -> Color(0xFF8E5CD9)
        FamilyRelation.SPOUSE -> Color(0xFF2EA58A)
        FamilyRelation.SON -> Color(0xFF3F6FD8)
        FamilyRelation.DAUGHTER -> Color(0xFFE5674A)
        FamilyRelation.OTHER -> Color(0xFF7D8794)
    }

/* ================= MAIN SCREEN ================= */

@Composable
fun FamilyHealthScreen(viewModel: FamilyHealthHistoryViewModel = viewModel()) {

    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    var adding by rememberSaveable { mutableStateOf(false) }
    var removing by remember { mutableStateOf<FamilyHealthCondition?>(null) }

    LaunchedEffect(Unit) { viewModel.message.collect { showToast(context, it) } }
    LaunchedEffect(Unit) { viewModel.done.collect { adding = false; removing = null } }

    CommonAppBar(
        title = "Family's Health History",
    )
    {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading && state.conditions.isEmpty() ->
                        CircularProgressIndicator(color = colors.primaryBlueColor, modifier = Modifier.align(Alignment.Center))
                    state.error != null && state.conditions.isEmpty() -> FamilyHistoryMessage(
                        icon = Icons.Rounded.WarningAmber,
                        title = "Couldn't load your family's health history",
                        text = state.error!!,
                        actionText = "Try again",
                        onAction = viewModel::load,
                    )
                    state.conditions.isEmpty() -> FamilyHistoryMessage(
                        icon = Icons.Rounded.FamilyRestroom,
                        title = "No family health history yet",
                        text = "Add conditions that run in your family, like diabetes or hypertension.",
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.conditions, key = { it.diseaseId }) { condition ->
                            HealthCard(condition = condition, onRemove = { removing = condition })
                        }
                    }
                }
            }

            AddPillButton(
                text = "+ Add Family's Health History",
                onClick = { viewModel.loadDiseases(); adding = true },
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }

    if (adding) {
        AddFamilyHistorySheet(
            viewModel = viewModel,
            saving = state.saving,
            onDismiss = { if (!state.saving) adding = false },
        )
    }

    removing?.let { condition ->
        AppDialog(
            title = "Remove ${condition.diseaseName}?",
            message = "It will no longer be listed in your family's health history.",
            icon = Icons.Rounded.DeleteOutline,
            confirmText = "Remove",
            dismissText = "Cancel",
            destructive = true,
            loading = state.saving,
            onConfirm = { viewModel.remove(condition) },
            onDismiss = { if (!state.saving) removing = null },
        )
    }
}

/* ================= CARD ================= */

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HealthCard(condition: FamilyHealthCondition, onRemove: () -> Unit) {
    val colors = LocalMyColorScheme.current
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(14.dp))
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                condition.diseaseName,
                color = colors.textDarkColor,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f).padding(top = 2.dp),
            )
            Box {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = Color.Gray,
                    modifier = Modifier.clip(CircleShape).clickable { expanded = true }.padding(4.dp).size(18.dp),
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Remove") },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = { expanded = false; onRemove() },
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(end = 10.dp),
        ) {
            condition.relations.forEach { TagChip(text = it.label, color = it.tagColor) }
        }
    }
}

/* ================= CHIP ================= */

@Composable
private fun TagChip(text: String, color: Color) {
    Text(
        text,
        color = Color.White,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun FamilyHistoryMessage(
    icon: ImageVector,
    title: String,
    text: String,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(44.dp))
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

/* ================= ADD SHEET ================= */

private val CheckGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddFamilyHistorySheet(
    viewModel: FamilyHealthHistoryViewModel,
    saving: Boolean,
    onDismiss: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val diseases by viewModel.diseases.collectAsState()

    var query by rememberSaveable { mutableStateOf("") }
    // Several diseases can be picked; the chosen relations are recorded for each of them.
    var selectedDiseases by remember { mutableStateOf(listOf<Disease>()) }
    var selected by remember { mutableStateOf(setOf<FamilyRelation>()) }
    val canSubmit = selectedDiseases.isNotEmpty() && selected.isNotEmpty() && !saving

    val suggestions = remember(query, diseases) {
        val q = query.trim()
        if (q.isEmpty()) emptyList()
        else diseases.asSequence()
            .filter { it.name.contains(q, ignoreCase = true) }
            .sortedBy { if (it.name.startsWith(q, ignoreCase = true)) 0 else 1 }
            .take(30)
            .toList()
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.dashboardBackgroundColor,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                "Add Family's Health History",
                color = colors.textDarkColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 22.dp),
            )

            SheetLabel("Chronic Disease")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.dashboardContainerColor)
                    .padding(horizontal = 12.dp),
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textGreyColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (query.isEmpty()) Text("Select Chronic Diseases", color = colors.textGreyColor.copy(alpha = 0.7f), fontSize = 14.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it.take(60) },
                        singleLine = true,
                        textStyle = TextStyle(color = colors.textDarkColor, fontSize = 14.sp),
                        cursorBrush = SolidColor(colors.primaryBlueColor),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Clear search",
                        tint = colors.textGreyColor,
                        modifier = Modifier.size(20.dp).clip(CircleShape).clickable { query = "" },
                    )
                }
            }

            if (suggestions.isNotEmpty()) {
                Column(
                    Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .heightIn(max = 240.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.dashboardContainerColor)
                        .verticalScroll(rememberScrollState()),
                ) {
                    suggestions.forEachIndexed { i, disease ->
                        if (i > 0) HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
                        val picked = selectedDiseases.any { it.id == disease.id }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic(HapticEvent.Selection)
                                    selectedDiseases = if (picked) selectedDiseases.filterNot { it.id == disease.id }
                                    else selectedDiseases + disease
                                    // Clear the search so the next disease can be looked up straight away.
                                    query = ""
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                        ) {
                            Text(
                                disease.name,
                                color = colors.textDarkColor,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            SquareCheck(checked = picked)
                        }
                    }
                }
            } else if (query.isNotBlank()) {
                Text(
                    if (diseases.isEmpty()) "Loading diseases..." else "No matching disease",
                    color = colors.textGreyColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                )
            }

            if (selectedDiseases.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    selectedDiseases.forEach { disease ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(colors.primaryBlueColor.copy(alpha = 0.12f))
                                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        ) {
                            Text(
                                disease.name,
                                color = colors.primaryBlueColor,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.widthIn(max = 220.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Rounded.Close,
                                contentDescription = "Remove ${disease.name}",
                                tint = colors.primaryBlueColor,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .clickable { selectedDiseases = selectedDiseases.filterNot { it.id == disease.id } },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SheetLabel("Select Relation")
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.dashboardContainerColor),
            ) {
                FamilyRelation.entries.forEachIndexed { i, relation ->
                    if (i > 0) HorizontalDivider(color = colors.borderGreyLightColor.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 12.dp))
                    val checked = relation in selected
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic(HapticEvent.Selection)
                                selected = if (checked) selected - relation else selected + relation
                            }
                            .padding(horizontal = 14.dp, vertical = 15.dp),
                    ) {
                        Text(relation.label, color = colors.textDarkColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        SquareCheck(checked = checked)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (canSubmit || saving) colors.primaryBlueColor else Color(0xFFADADAD))
                    .bounceClick(enabled = canSubmit) { viewModel.add(selectedDiseases, selected) },
            ) {
                if (saving) CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                else Text("Submit", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Small rounded square check: green with a tick when on, a light grey square when off. */
@Composable
private fun SquareCheck(checked: Boolean) {
    val colors = LocalMyColorScheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(if (checked) CheckGreen else colors.borderGreyLightColor.copy(alpha = 0.45f)),
    ) {
        if (checked) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun SheetLabel(text: String) {
    Text(
        text,
        color = LocalMyColorScheme.current.textGreyColor,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp, start = 2.dp),
    )
}
