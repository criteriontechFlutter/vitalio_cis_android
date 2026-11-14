package com.critetiontech.ctvitalio.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Vaccines
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.viewmodel.MedicalDetailMedicine
import com.critetiontech.ctvitalio.viewmodel.MedicalDetailsValue
import com.critetiontech.ctvitalio.viewmodel.MedicalSummaryItem
import com.critetiontech.ctvitalio.viewmodel.PrescriptionDetailViewModel
import com.critetiontech.ctvitalio.viewmodel.PrescriptionHistoryViewModel
import com.critetiontech.ctvitalio.viewmodel.RxUiState
import com.critetiontech.ctvitalio.viewmodel.RxReminderState
import com.critetiontech.ctvitalio.ui.feedback.StretchPullRefreshBox
import com.critetiontech.ctvitalio.utils.PrescriptionPdf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import com.critetiontech.myapplication.utils.LocalNavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Design tokens (from the Prescriptions mockups) ───────────────────────────
private val RxBlue = Color(0xFF1765F2)
private val RxBlueText = Color(0xFF1765F2)
private val RxSubtitle = Color(0xFF4A5E8C)
private val RxIconGrey = Color(0xFFB4BCC8)
private val RxAvatarBg = Color(0xFFE6E8EC)
private val RxAvatarFg = Color(0xFFC3C7CE)
private val RxLineIcon = Color(0xFF7B8698)

private val TextDark: Color @Composable get() = LocalMyColorScheme.current.textDarkColor
private val TextMid: Color @Composable get() = LocalMyColorScheme.current.textGreyColor
private val CardColor: Color @Composable get() = LocalMyColorScheme.current.dashboardContainerColor
private val DividerColor: Color
    @Composable get() = if (isAppDark) LocalMyColorScheme.current.borderGreyLightColor else Color(0xFFEDEFF3)

// ─── Date helpers ─────────────────────────────────────────────────────────────
private fun parseVisitDate(raw: String?): Date? {
    if (raw.isNullOrBlank()) return null
    return try {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(raw.take(19))
    } catch (e: Exception) { null }
}

/** "12:30 PM, Tuesday - December 16" */
private fun listDateLabel(raw: String?): String = parseVisitDate(raw)?.let {
    SimpleDateFormat("hh:mm a, EEEE - MMMM d", Locale.getDefault()).format(it)
} ?: raw.orEmpty()

/** "Tuesday, 16 December 2025" */
private fun detailDateLabel(raw: String?): String = parseVisitDate(raw)?.let {
    SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(it)
} ?: raw.orEmpty()

/** "Dr. Name" in the app language; left as-is when the server already sent the title. */
@Composable
private fun doctorLabel(name: String?): String? {
    val n = name?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return if (n.startsWith("Dr", ignoreCase = true) || n.startsWith("डॉ")) n
    else stringResource(R.string.rx_doctor_name, n)
}

// ═════════════════════════════════════════════════════════════════════════════
// Prescriptions list — PatientHistoryByUHID/PatientMedicalSummary
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun PrescriptionScreen(vm: PrescriptionHistoryViewModel = viewModel()) {
    val navController = LocalNavController.current
    val state by vm.state.collectAsState()
    val refreshing by vm.refreshing.collectAsState()

    CommonAppBar(
        title = stringResource(R.string.rx_prescriptions_title),
        actions = {
            UploadPrescriptionChip { navController.navigate(Routes.UPLOADPRESCRIPTION) }
        }
    ) {
        Box(Modifier.fillMaxSize().background(LocalMyColorScheme.current.dashboardBackgroundColor)) {
            when (val s = state) {
                RxUiState.Loading -> RxLoading(RxBlue)
                is RxUiState.Error -> RxError(s.message, onRetry = vm::load)
                is RxUiState.Success -> if (s.data.isEmpty()) {
                    RxEmpty(onRefresh = vm::load)
                } else StretchPullRefreshBox(
                    isRefreshing = refreshing,
                    onRefresh = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        itemsIndexed(s.data, key = { _, item -> item.pmId }) { index, item ->
                            AnimatedEnter(index = index) {
                                PrescriptionListCard(item) {
                                    navController.navigate(
                                        Routes.prescriptionDetail(
                                            pmId = item.pmId,
                                            doctor = item.doctorName,
                                            department = item.departmentName,
                                            date = item.visitDate,
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UploadPrescriptionChip(onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(CardColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(Icons.Outlined.AddCircleOutline, null, tint = RxBlueText, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.rx_upload_prescription),
            color = RxBlueText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun PrescriptionListCard(item: MedicalSummaryItem, onClick: () -> Unit) {
    val doctor = doctorLabel(item.doctorName)
    val department = item.departmentName?.takeIf { it.isNotBlank() }
    val title = doctor ?: department ?: stringResource(R.string.rx_opd_visit)
    val subtitle = when {
        doctor != null && department != null -> department
        !item.crNo.isNullOrBlank() -> stringResource(R.string.rx_visit_no, item.crNo)
        else -> null
    }
    val openLabel = stringResource(R.string.rx_open_prescription, title)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .clickable(onClickLabel = openLabel, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(RxAvatarBg),
                contentAlignment = Alignment.BottomCenter
            ) {
                Icon(Icons.Default.Person, null, tint = RxAvatarFg, modifier = Modifier.size(42.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextDark,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle != null) {
                    Text(
                        subtitle,
                        fontSize = 11.sp,
                        color = if (isAppDark) TextMid else RxSubtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp, bottom = 10.dp),
            thickness = 1.dp,
            color = DividerColor
        )

        RxInfoLine(Icons.Default.CalendarMonth, listDateLabel(item.visitDate))
        item.diagnosis?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(6.dp))
            RxInfoLine(Icons.Outlined.LocalHospital, it)
        }
    }
}

@Composable
private fun RxInfoLine(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RxIconGrey, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            fontSize = 12.sp,
            color = TextDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ═════════════════════════════════════════════════════════════════════════════
// Prescription details — PatientHistoryByUHID/PatientMedicalDetails
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun PrescriptionDetailScreen(
    pmId: Int,
    doctorName: String?,
    departmentName: String?,
    visitDate: String?,
    vm: PrescriptionDetailViewModel = viewModel(key = "rx_detail_$pmId"),
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val resources = androidx.compose.ui.platform.LocalResources.current
    val state by vm.state.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val medicineEnabled = com.critetiontech.ctvitalio.config.rememberFeatureFlags()[com.critetiontech.ctvitalio.config.AppFeature.MEDICINE] != false

    LaunchedEffect(pmId) { vm.load(pmId) }

    // Result of "Add to medicine reminders"
    LaunchedEffect(reminders) {
        when (val r = reminders) {
            is RxReminderState.Added -> android.widget.Toast.makeText(
                context, resources.getQuantityString(R.plurals.rx_reminders_added, r.count, r.count), android.widget.Toast.LENGTH_LONG
            ).show()
            RxReminderState.Failed -> {
                android.widget.Toast.makeText(context, resources.getString(R.string.rx_reminders_failed), android.widget.Toast.LENGTH_LONG).show()
                vm.reminderMessageShown()
            }
            else -> Unit
        }
    }

    val doctor = doctorLabel(doctorName)
    val heading = departmentName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.rx_opd_visit)
    val dateText = detailDateLabel(visitDate)

    val createdByLabel = stringResource(R.string.rx_created_by)
    val dateLabel = stringResource(R.string.rx_date)
    val notesLabel = stringResource(R.string.rx_notes)
    val diagnosisLabel = stringResource(R.string.rx_diagnosis)
    val investigationsLabel = stringResource(R.string.rx_investigations)
    val medicinesLabel = stringResource(R.string.rx_prescribed_medicines)
    val pdfFooter = stringResource(R.string.rx_pdf_footer)
    val shareFailed = stringResource(R.string.rx_share_failed)
    val shareTitle = stringResource(R.string.rx_share)
    val forDaysTemplate = stringResource(R.string.rx_for_days)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(RxBlue)
            .statusBarsPadding()
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        Row(
            // 48dp touch targets around 30dp circles: 7 + 9 = the design's 16dp edge, 1 + 9 = its 10dp
            modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RxCircleButton(
                icon = Icons.AutoMirrored.Filled.ArrowBackIos,
                contentDescription = stringResource(R.string.rx_back),
                iconOffsetX = 3.dp,
                onClick = { navController.popBackStack() }
            )
            Text(
                stringResource(R.string.rx_details_title),
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            RxCircleButton(
                icon = Icons.AutoMirrored.Filled.Notes,
                contentDescription = shareTitle,
                enabled = state is RxUiState.Success,
                onClick = {
                    val data = (state as? RxUiState.Success)?.data ?: return@RxCircleButton
                    val content = PrescriptionPdf.Content(
                        heading = heading,
                        lines = listOfNotNull(
                            doctor?.let { "$createdByLabel $it" },
                            dateText.takeIf { it.isNotBlank() }?.let { "$dateLabel $it" },
                        ),
                        medicinesTitle = medicinesLabel,
                        medicines = data.prescription.orEmpty().map {
                            PrescriptionPdf.Medicine(it.drugName.orEmpty(), medicineInstruction(it, forDaysTemplate))
                        },
                        sections = listOfNotNull(
                            data.diagnosis?.let { diagnosisLabel to it },
                            data.investigation.orEmpty().mapNotNull { it.investigationName?.takeIf(String::isNotBlank) }
                                .takeIf { it.isNotEmpty() }?.let { list -> investigationsLabel to list.joinToString("\n") { "• $it" } },
                            data.notes?.let { notesLabel to it },
                        ),
                        footer = pdfFooter,
                    )
                    val ok = PrescriptionPdf.share(context, content, "prescription-$pmId.pdf", shareTitle)
                    if (!ok) android.widget.Toast.makeText(context, shareFailed, android.widget.Toast.LENGTH_SHORT).show()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // ── Header ───────────────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Text(heading, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            if (doctor != null) HeaderLine(createdByLabel, doctor)
            if (dateText.isNotBlank()) HeaderLine(dateLabel, dateText)
            Spacer(Modifier.height(18.dp))

            when (val s = state) {
                RxUiState.Loading -> Box(
                    Modifier.fillMaxWidth().padding(top = 60.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator(color = Color.White, strokeWidth = 3.dp) }

                is RxUiState.Error -> RxWhiteCard {
                    Text(s.message, fontSize = 13.sp, color = TextMid)
                    Spacer(Modifier.height(12.dp))
                    HapticButton(
                        onClick = { vm.load(pmId, force = true) },
                        colors = ButtonDefaults.buttonColors(containerColor = RxBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.report_try_again), fontWeight = FontWeight.SemiBold)
                    }
                }

                is RxUiState.Success -> DetailContent(
                    data = s.data,
                    forDaysTemplate = forDaysTemplate,
                    reminders = reminders,
                    showReminderAction = medicineEnabled,
                    onAddReminders = vm::addToReminders,
                )
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailContent(
    data: MedicalDetailsValue,
    forDaysTemplate: String,
    reminders: RxReminderState,
    showReminderAction: Boolean,
    onAddReminders: (List<MedicalDetailMedicine>) -> Unit,
) {
    val medicines = data.prescription.orEmpty()
    val schedulable = medicines.filter { it.drugId > 0 }
    var confirm by remember { mutableStateOf(false) }

    // ── Medicines ────────────────────────────────────────────────────────────
    RxWhiteCard {
        if (medicines.isEmpty()) {
            Text(stringResource(R.string.rx_no_medicines), fontSize = 13.sp, color = TextMid)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                medicines.forEach { MedicineRow(it, forDaysTemplate) }
            }
        }
    }

    // ── Add to medicine reminders ────────────────────────────────────────────
    if (showReminderAction && schedulable.isNotEmpty()) {
        Spacer(Modifier.height(12.dp))
        val saving = reminders == RxReminderState.Saving
        val added = reminders is RxReminderState.Added
        HapticButton(
            onClick = { confirm = true },
            enabled = !saving && !added,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = RxBlue,
                disabledContainerColor = Color.White.copy(alpha = 0.7f),
                disabledContentColor = RxBlue.copy(alpha = 0.7f),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(color = RxBlue, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.rx_adding_reminders), fontWeight = FontWeight.SemiBold)
            } else {
                Icon(
                    if (added) Icons.Default.CheckCircle else Icons.Default.NotificationsActive,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (added) pluralStringResource(R.plurals.rx_reminders_added, (reminders as RxReminderState.Added).count, reminders.count)
                    else stringResource(R.string.rx_add_reminders),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(pluralStringResource(R.plurals.rx_reminders_confirm_title, schedulable.size, schedulable.size)) },
            text = { Text(stringResource(R.string.rx_reminders_confirm_body)) },
            confirmButton = {
                TextButton(onClick = { confirm = false; onAddReminders(schedulable) }) {
                    Text(stringResource(R.string.rx_reminders_confirm_add))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.rx_cancel)) }
            },
        )
    }

    // ── Diagnosis ────────────────────────────────────────────────────────────
    data.diagnosis?.let {
        Spacer(Modifier.height(24.dp))
        RxTextCard(stringResource(R.string.rx_diagnosis), it)
    }

    // ── Investigations ──────────────────────────────────────────────────────
    val investigations = data.investigation.orEmpty().mapNotNull { it.investigationName?.takeIf(String::isNotBlank) }
    if (investigations.isNotEmpty()) {
        Spacer(Modifier.height(24.dp))
        RxTextCard(stringResource(R.string.rx_investigations), investigations.joinToString("\n") { "• $it" })
    }

    // ── Notes (otherAdvice) ──────────────────────────────────────────────────
    data.notes?.let {
        Spacer(Modifier.height(24.dp))
        RxTextCard(stringResource(R.string.rx_notes), it)
    }
}

@Composable
private fun MedicineRow(med: MedicalDetailMedicine, forDaysTemplate: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            medicineIcon(med.dosageForm),
            contentDescription = null,
            tint = RxLineIcon,
            modifier = Modifier.padding(top = 2.dp).size(36.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                med.drugName.orEmpty(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )
            val instruction = medicineInstruction(med, forDaysTemplate)
            if (instruction.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(instruction, fontSize = 13.sp, color = TextMid, lineHeight = 17.sp)
            }
        }
    }
}

private fun medicineInstruction(med: MedicalDetailMedicine, forDaysTemplate: String): String {
    val form = med.dosageForm?.trim()?.takeIf { it.isNotEmpty() }?.replaceFirstChar { it.uppercase() }
    val freq = med.doseFrequency?.trim()?.takeIf { it.isNotEmpty() }
    val days = med.duration?.trim()?.takeIf { it.isNotEmpty() && it != "0" }
    val base = listOfNotNull(form, freq).joinToString(" · ")
    val withDuration = when {
        days != null && base.isNotEmpty() -> String.format(forDaysTemplate, base, days)
        days != null -> String.format(forDaysTemplate, "", days).trim()
        else -> base
    }
    val remark = med.remark?.trim()?.takeIf { it.isNotEmpty() && it != "0" }
    return listOfNotNull(withDuration.takeIf { it.isNotEmpty() }, remark).joinToString(", ")
}

private fun medicineIcon(dosageForm: String?): ImageVector {
    val f = dosageForm.orEmpty().lowercase()
    return when {
        "inj" in f -> Icons.Outlined.Vaccines
        "syrup" in f || "liquid" in f || "suspension" in f || "solution" in f -> Icons.Outlined.LocalDrink
        "drop" in f -> Icons.Outlined.WaterDrop
        else -> Icons.Outlined.Medication
    }
}


@Composable
private fun HeaderLine(label: String, value: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) { append(label) }
            append(" ")
            withStyle(SpanStyle(color = Color.White)) { append(value) }
        },
        fontSize = 14.sp
    )
}

@Composable
private fun RxWhiteCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardColor)
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) { content() }
}

@Composable
private fun RxTextCard(title: String, body: String) {
    RxWhiteCard {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextDark)
        Spacer(Modifier.height(6.dp))
        Text(body, fontSize = 12.sp, color = TextMid, lineHeight = 17.sp)
    }
}

@Composable
private fun RxCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    iconOffsetX: androidx.compose.ui.unit.Dp = 0.dp,
) {
    // 30dp circle as in the design, inside a 48dp touch target (accessibility minimum)
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = androidx.compose.ui.semantics.Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (enabled) Color.White else Color.White.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = contentDescription,
                tint = RxBlue,
                modifier = Modifier.padding(start = iconOffsetX).size(16.dp)
            )
        }
    }
}

// ─── Shared states ────────────────────────────────────────────────────────────
@Composable
private fun RxLoading(color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = color, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.report_fetching_prescription), fontSize = 14.sp, color = TextMid)
        }
    }
}

@Composable
private fun RxError(message: String, onRetry: () -> Unit) {
    RxCenteredMessage(
        icon = Icons.Default.ErrorOutline,
        tint = Color(0xFFEF4444),
        title = stringResource(R.string.report_something_went_wrong),
        body = message.take(160),
        button = stringResource(R.string.report_try_again),
        onClick = onRetry
    )
}

@Composable
private fun RxEmpty(onRefresh: () -> Unit) {
    RxCenteredMessage(
        icon = Icons.Default.MedicalServices,
        tint = RxBlue,
        title = stringResource(R.string.report_no_prescriptions_found),
        body = stringResource(R.string.report_no_opd_history),
        button = stringResource(R.string.report_refresh),
        onClick = onRefresh
    )
}

@Composable
private fun RxCenteredMessage(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
    button: String,
    onClick: () -> Unit,
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(84.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape)
                    .border(1.dp, tint.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = tint, modifier = Modifier.size(40.dp)) }
            Spacer(Modifier.height(18.dp))
            Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(6.dp))
            Text(
                body,
                fontSize = 13.sp,
                color = TextMid,
                lineHeight = 19.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            HapticButton(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(containerColor = RxBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(button, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}