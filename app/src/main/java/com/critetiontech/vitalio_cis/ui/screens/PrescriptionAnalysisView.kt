package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.model.PrescriptionAdvice
import com.critetiontech.ctvitalio.model.PrescriptionMedicine
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.MyColorScheme
import com.critetiontech.ctvitalio.viewmodel.UploadPrescriptionViewModel

private val WarningColor = Color(0xFFD97706)
private val TealColor    = Color(0xFF0E9384)
private val PurpleColor  = Color(0xFF7C3AED)
private val OrangeColor  = Color(0xFFEA580C)
private val IndigoColor  = Color(0xFF6366F1)
private val SuccessColor = Color(0xFF16A34A)

// ─────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────

@Composable
fun PrescriptionAnalysisScreen(viewModel: UploadPrescriptionViewModel) {
    val context       = LocalContext.current
    val navController = LocalNavController.current
    val colors        = LocalMyColorScheme.current

    val result           by viewModel.result.collectAsStateWithLifecycle()
    val selectedIds      by viewModel.selectedMedicineIds.collectAsStateWithLifecycle()
    val isSaving         by viewModel.isSavingMedicines.collectAsStateWithLifecycle()
    val savedOk          by viewModel.medicinesSavedSuccessfully.collectAsStateWithLifecycle()
    val showSessionAlert by viewModel.showSessionAlert.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { showToast(context, it) }
    }

    val patient      = result?.patient
    val prescription = result?.prescription
    val medicines    = prescription?.medicines.orEmpty()
    val adviceList   = prescription?.advice.orEmpty()
    val insertable   = medicines.filter { it.isInsertable }
    val unmapped     = medicines.size - insertable.size
    val toInsert     = remember(medicines, selectedIds) { viewModel.resolvedMedicinesToInsert(medicines) }
    val allSelected  = insertable.isNotEmpty() && insertable.all { it.localId in selectedIds }

    if (showSessionAlert) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSessionAlert() },
            title = { Text("Session Expired") },
            text = { Text("Patient profile or session data is missing. Please log in again or reselect your profile.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissSessionAlert()
                    navController.popBackStack()
                }) { Text("OK") }
            }
        )
    }

    CommonAppBar(title = "Prescription Analysis") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Patient & date ───────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.Default.Person, colors.primaryBlueColor, 36)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text(patient?.name ?: "Patient Record", style = AppTextStyles.style16BCB())
                            Text("Prescription Details", style = AppTextStyles.style12GCN())
                        }
                        prescription?.date?.let {
                            DetailBadge(Icons.Default.CalendarToday, viewModel.formattedDate(it), colors.primaryBlueColor)
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 14.dp),
                        color = colors.borderGreyLightColor
                    )
                    Row {
                        patient?.age?.takeIf { it.isNotEmpty() }?.let {
                            InfoPill("Age", "$it Years", Icons.Default.Accessibility, colors, Modifier.weight(1f))
                        }
                        patient?.gender?.takeIf { it.isNotEmpty() }?.let {
                            val g = when (it.uppercase()) { "F" -> "Female"; "M" -> "Male"; else -> it }
                            InfoPill("Gender", g, Icons.Default.AccountCircle, colors, Modifier.weight(1f))
                        }
                        InfoPill("Medicines", "${medicines.size} Prescribed", Icons.Default.Medication, colors, Modifier.weight(1f))
                    }
                }
            }

            // ── Medicines ────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Prescribed Medicines", style = AppTextStyles.style16BCB(), modifier = Modifier.weight(1f))
                    if (insertable.isNotEmpty()) {
                        Text(
                            if (allSelected) "Deselect All" else "Select All",
                            style = AppTextStyles.style12GCB(),
                            color = colors.primaryBlueColor,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colors.primaryBlueColor.copy(alpha = 0.10f))
                                .clickable {
                                    if (allSelected) viewModel.deselectAllMedicines()
                                    else viewModel.selectAllMedicines(medicines)
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("${medicines.size} items", style = AppTextStyles.style12GCN())
                }

                if (unmapped > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(WarningColor.copy(alpha = 0.12f))
                            .border(1.dp, WarningColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = WarningColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("$unmapped of ${medicines.size} medicines require manual addition", style = AppTextStyles.style14BCB())
                            Spacer(Modifier.height(3.dp))
                            Text(
                                "These medicines could not be automatically matched to the drug database. You will need to add them manually in Pill Reminders.",
                                style = AppTextStyles.style12GCN()
                            )
                        }
                    }
                }

                if (medicines.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Medication, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("No medicines extracted from this prescription", style = AppTextStyles.style12GCN())
                    }
                } else {
                    medicines.forEach { med ->
                        PrescriptionMedicineCard(
                            med = med,
                            isSelected = med.localId in selectedIds,
                            colors = colors,
                            viewModel = viewModel
                        ) { med.localId?.let { viewModel.toggleMedicineSelection(it) } }
                    }
                }
            }

            // ── Advice ───────────────────────────────────
            if (adviceList.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconCircle(Icons.Default.MedicalServices, TealColor, 32)
                        Spacer(Modifier.width(8.dp))
                        Text("Doctor's Advice & Instructions", style = AppTextStyles.style16BCB(), modifier = Modifier.weight(1f))
                        Text("${adviceList.size} ${if (adviceList.size == 1) "note" else "notes"}", style = AppTextStyles.style12GCN())
                    }
                    adviceList.forEach { PrescriptionAdviceCard(it, colors, viewModel) }
                }
            }

            // ── Bottom actions ───────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (medicines.isNotEmpty()) {
                    val enabled = !isSaving && !savedOk && insertable.isNotEmpty()
                    Button(
                        onClick = { viewModel.insertPrescriptionMedicines(toInsert) },
                        enabled = enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primaryBlueColor,
                            disabledContainerColor = if (savedOk) SuccessColor else Color.Gray,
                            disabledContentColor = Color.White
                        )
                    ) {
                        when {
                            isSaving -> {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Adding to Reminders...", style = AppTextStyles.style16WCB())
                            }
                            savedOk -> {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Medicines Added to Reminders", style = AppTextStyles.style16WCB())
                            }
                            insertable.isEmpty() -> {
                                Icon(Icons.Default.Warning, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("All Medicines Require Manual Addition", style = AppTextStyles.style16WCB())
                            }
                            else -> {
                                Icon(Icons.Default.AddCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (selectedIds.isNotEmpty()) "Add Selected (${toInsert.size}) to Reminders"
                                    else "Add All (${insertable.size}) to Medicine Reminders",
                                    style = AppTextStyles.style16WCB()
                                )
                            }
                        }
                    }
                }

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", style = AppTextStyles.style14BCB())
                }
            }

            Spacer(Modifier.height(10.dp))
        }
    }
}

// ─────────────────────────────────────────────────────────
// MEDICINE CARD
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrescriptionMedicineCard(
    med: PrescriptionMedicine,
    isSelected: Boolean,
    colors: MyColorScheme,
    viewModel: UploadPrescriptionViewModel,
    onToggle: () -> Unit
) {
    val strokeColor = if (isSelected) colors.primaryBlueColor.copy(alpha = 0.5f) else colors.borderGreyLightColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(if (isSelected) 1.5.dp else 1.dp, strokeColor, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable(enabled = med.isInsertable) { onToggle() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    when {
                        !med.isInsertable -> Icons.Default.RemoveCircleOutline
                        isSelected -> Icons.Default.CheckCircle
                        else -> Icons.Default.RadioButtonUnchecked
                    },
                    contentDescription = null,
                    tint = when {
                        !med.isInsertable -> colors.textGreyColor.copy(alpha = 0.4f)
                        isSelected -> colors.primaryBlueColor
                        else -> colors.textGreyColor
                    },
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(22.dp)
                )
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(med.displayName, style = AppTextStyles.style16BCB())
                    med.brandName?.takeIf { it.isNotEmpty() && !it.equals(med.displayName, ignoreCase = true) }?.let {
                        Text(it, style = AppTextStyles.style12GCN(), maxLines = 2)
                    }
                    med.strength?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = AppTextStyles.style14BCB(), color = colors.primaryBlueColor)
                    }
                }
                val displayForm = med.dosageFormName?.takeIf { it.isNotEmpty() } ?: med.form.orEmpty()
                if (displayForm.isNotEmpty()) {
                    DetailBadge(formIcon(displayForm), displayForm, PurpleColor, RoundedCornerShape(6.dp))
                }
            }

            if (!med.isInsertable) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(WarningColor.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = WarningColor, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Manual Addition Required • Not matched in database", style = AppTextStyles.style12GCB(), color = WarningColor)
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                med.dosage?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Science, "$it Dose", TealColor) }
                med.frequency?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Repeat, it, OrangeColor) }
                med.timing?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Restaurant, viewModel.formattedTiming(it), colors.primaryBlueColor) }
                med.duration?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Schedule, it, IndigoColor) }
            }

            val slots = med.parsedTimeSlots
            if (slots.isNotEmpty()) TimeSlotsRow(slots, colors.primaryBlueColor, viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────
// ADVICE CARD
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrescriptionAdviceCard(item: PrescriptionAdvice, colors: MyColorScheme, viewModel: UploadPrescriptionViewModel) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TealColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Description, contentDescription = null, tint = TealColor, modifier = Modifier
                    .padding(top = 2.dp)
                    .size(16.dp))
                Spacer(Modifier.width(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    item.instruction?.takeIf { it.isNotEmpty() }?.let { Text(it, style = AppTextStyles.style14BCB()) }
                    item.product?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = AppTextStyles.style12GCB(), color = colors.primaryBlueColor)
                    }
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.duration?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Schedule, "Duration: $it", IndigoColor) }
                item.frequency?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Repeat, it, OrangeColor) }
                item.type?.takeIf { it.isNotEmpty() }?.let { DetailBadge(Icons.Default.Label, it, PurpleColor) }
            }

            val slots = item.parsedTimeSlots
            if (slots.isNotEmpty()) TimeSlotsRow(slots, TealColor, viewModel)
        }
    }
}

// ─────────────────────────────────────────────────────────
// HELPERS
// ─────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimeSlotsRow(slots: List<String>, tint: Color, viewModel: UploadPrescriptionViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.CenterVertically)) {
            Icon(Icons.Default.Alarm, contentDescription = null, tint = tint, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text("Schedule:", style = AppTextStyles.style12GCN())
        }
        slots.forEach { slot ->
            Text(
                viewModel.formattedTimeSlot(slot),
                style = AppTextStyles.style12GCB(),
                color = tint,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@Composable
private fun DetailBadge(
    icon: ImageVector,
    text: String,
    color: Color,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.10f))
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, style = AppTextStyles.style12GCB(), color = color, maxLines = 1)
    }
}

@Composable
private fun IconCircle(icon: ImageVector, tint: Color, size: Int) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size((size / 2).dp))
    }
}

@Composable
private fun InfoPill(label: String, value: String, icon: ImageVector, colors: MyColorScheme, modifier: Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = AppTextStyles.style12GCN())
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(value, style = AppTextStyles.style12GCB(), color = colors.textDarkColor)
        }
    }
}

private fun formIcon(form: String): ImageVector {
    val lower = form.lowercase()
    return when {
        lower.contains("tab") || lower.contains("cap") -> Icons.Default.Medication
        lower.contains("syr") || lower.contains("liquid") -> Icons.Default.WaterDrop
        lower.contains("inj") -> Icons.Default.Vaccines
        else -> Icons.Default.MedicalServices
    }
}