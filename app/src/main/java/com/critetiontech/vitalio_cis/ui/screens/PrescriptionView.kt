package com.critetiontech.ctvitalio.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NoFood
import androidx.compose.material.icons.filled.Pending
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.InvestigationDetail
import com.critetiontech.ctvitalio.viewmodel.PatientVitalDetail
import com.critetiontech.ctvitalio.viewmodel.PrescriptionDetail
import com.critetiontech.ctvitalio.viewmodel.PrescriptionViewModel
import com.critetiontech.ctvitalio.viewmodel.ProblemDetail
import com.critetiontech.ctvitalio.viewmodel.VitalFrequencyDetail

// ─── Design Tokens ─────────────────────────────────────────────────────────────
private val Blue = Color(0xFF2E5BFF)
private val BlueSoft = Color(0xFFEEF2FF)
private val Green = Color(0xFF22C55E)
private val GreenSoft = Color(0xFFDCFCE7)
private val Orange = Color(0xFFF97316)
private val OrangeSoft = Color(0xFFFFF7ED)
private val Red = Color(0xFFEF4444)
private val RedSoft = Color(0xFFFEF2F2)
private val Purple = Color(0xFF8B5CF6)
private val PurpleSoft = Color(0xFFF5F3FF)
private val Teal = Color(0xFF14B8A6)
private val TealSoft = Color(0xFFF0FDFA)
private val TextDark = Color(0xFF0F172A)
private val TextMid = Color(0xFF475569)
private val TextLight = Color(0xFF94A3B8)
private val DividerColor = Color(0xFFF1F5F9)

// ─── Helper model ──────────────────────────────────────────────────────────────
data class DisplayVitalItem(
    val name: String,
    val valueString: String,
    val unit: String,
    val iconTint: Color,
    val bgColor: Color,
    val icon: ImageVector
)

// ─── Main Screen ───────────────────────────────────────────────────────────────
@Composable
fun PrescriptionScreen(vm: PrescriptionViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val prescriptions by vm.prescriptions.collectAsState()
    val vitals by vm.vitals.collectAsState()
    val investigations by vm.investigations.collectAsState()
    val complaints by vm.complaints.collectAsState()
    val diagnoses by vm.diagnoses.collectAsState()
    val vitalFrequencies by vm.vitalFrequencies.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMessage by vm.errorMessage.collectAsState()

    LaunchedEffect(Unit) { vm.fetchOPDHistory() }

    CommonAppBar(title = "Prescription") {
        when {
            // ── Loading ──────────────────────────────────────────────────
            isLoading -> PrescriptionLoadingState(colors.dashboardBackgroundColor)

            // ── Error ────────────────────────────────────────────────────
            errorMessage != null -> PrescriptionErrorState(
                message = errorMessage ?: "",
                bgColor = colors.dashboardBackgroundColor,
                onRetry = { vm.fetchOPDHistory() }
            )

            // ── Empty ────────────────────────────────────────────────────
            prescriptions.isEmpty() -> PrescriptionEmptyState(
                bgColor = colors.dashboardBackgroundColor,
                onRefresh = { vm.fetchOPDHistory() }
            )

            // ── Content ──────────────────────────────────────────────────
            else -> {
                val displayVitals = getDisplayVitals(vitals)
                val clinicalSummary = complaints.firstOrNull { it.pdmId == 3 }
                val symptomItems = complaints.filter { it.pdmId in listOf(2, 56, 64) }
                val diagnosisItems = if (diagnoses.isNotEmpty()) diagnoses
                else complaints.filter { it.pdmId == 4 }
                val recommendedDiet = complaints.filter { it.pdmId == 9 }
                val avoidDiet = complaints.filter { it.pdmId == 10 }
                val followUp = complaints.firstOrNull { it.pdmId == 11 }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.dashboardBackgroundColor),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // ── Header Banner ───────────────────────────────────
                    item { PrescriptionHeaderBanner(prescriptions.size) }

                    // ── 1. Prescribed Medicines ─────────────────────────
                    item {
                        SectionWrapper {
                            SectionHeader(
                                icon = Icons.Default.MedicalServices,
                                iconBg = BlueSoft,
                                iconTint = Blue,
                                title = "Prescribed Medicines",
                                badge = "${prescriptions.size}"
                            )
                            Spacer(Modifier.height(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                prescriptions.forEach { MedicineCard(it, colors.dashboardContainerColor) }
                            }
                        }
                    }

                    // ── 2. Recorded Vitals ──────────────────────────────
                    if (displayVitals.isNotEmpty()) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.MonitorHeart,
                                    iconBg = RedSoft,
                                    iconTint = Red,
                                    title = "Recorded Vitals"
                                )
                                Spacer(Modifier.height(12.dp))
                                VitalsGrid(displayVitals, colors.dashboardContainerColor)
                            }
                        }
                    }

                    // ── 3. Investigations ───────────────────────────────
                    if (investigations.isNotEmpty()) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.Biotech,
                                    iconBg = PurpleSoft,
                                    iconTint = Purple,
                                    title = "Recommended Investigations",
                                    badge = "${investigations.size}"
                                )
                                Spacer(Modifier.height(12.dp))
                                InvestigationsCard(investigations, colors.dashboardContainerColor)
                            }
                        }
                    }

                    // ── 4. Clinical Summary ─────────────────────────────
                    if (clinicalSummary != null) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.TextSnippet,
                                    iconBg = TealSoft,
                                    iconTint = Teal,
                                    title = "Clinical Summary"
                                )
                                Spacer(Modifier.height(12.dp))
                                ClinicalSummaryCard(clinicalSummary, colors.dashboardContainerColor)
                            }
                        }
                    }

                    // ── 5. Symptoms & Diagnosis ─────────────────────────
                    if (symptomItems.isNotEmpty() || diagnosisItems.isNotEmpty()) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.LocalHospital,
                                    iconBg = RedSoft,
                                    iconTint = Red,
                                    title = "Symptoms & Diagnosis"
                                )
                                Spacer(Modifier.height(12.dp))
                                SymptomsDiagnosisCard(
                                    symptomItems, diagnosisItems, colors.dashboardContainerColor
                                )
                            }
                        }
                    }

                    // ── 6. Dietary Guidance ─────────────────────────────
                    if (recommendedDiet.isNotEmpty() || avoidDiet.isNotEmpty()) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.Restaurant,
                                    iconBg = GreenSoft,
                                    iconTint = Green,
                                    title = "Dietary Guidance"
                                )
                                Spacer(Modifier.height(12.dp))
                                DietaryCard(recommendedDiet, avoidDiet, colors.dashboardContainerColor)
                            }
                        }
                    }

                    // ── 7. Monitoring Schedule ──────────────────────────
                    if (vitalFrequencies.isNotEmpty()) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.Schedule,
                                    iconBg = BlueSoft,
                                    iconTint = Blue,
                                    title = "Monitoring Schedule"
                                )
                                Spacer(Modifier.height(12.dp))
                                MonitoringCard(vitalFrequencies, colors.dashboardContainerColor)
                            }
                        }
                    }

                    // ── 8. Follow-Up ────────────────────────────────────
                    if (followUp != null) {
                        item {
                            SectionWrapper {
                                SectionHeader(
                                    icon = Icons.Default.CalendarMonth,
                                    iconBg = OrangeSoft,
                                    iconTint = Orange,
                                    title = "Follow-up Instructions"
                                )
                                Spacer(Modifier.height(12.dp))
                                FollowUpCard(followUp, colors.dashboardContainerColor)
                            }
                        }
                    }

                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

// ─── Header Banner ─────────────────────────────────────────────────────────────
@Composable
private fun PrescriptionHeaderBanner(count: Int) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(Color(0xFF1557FF), Color(0xFF4F8AFF)))
            )
            .padding(horizontal = 20.dp, vertical = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    "OPD Prescription",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "$count medicine${if (count != 1) "s" else ""} prescribed",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// ─── Section Wrapper ───────────────────────────────────────────────────────────
@Composable
private fun SectionWrapper(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 20.dp)
    ) {
        content()
    }
}

// ─── Section Header ────────────────────────────────────────────────────────────
@Composable
private fun SectionHeader(
    icon: ImageVector,
    iconBg: Color,
    iconTint: Color,
    title: String,
    badge: String? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark,
            modifier = Modifier.weight(1f)
        )
        if (badge != null) {
            Box(
                modifier = Modifier
                    .background(iconBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(badge, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = iconTint)
            }
        }
    }
}

// ─── Medicine Card ──────────────────────────────────────────────────────────────
@Composable
private fun MedicineCard(med: PrescriptionDetail, containerColor: Color) {
    val isOngoing = med.medicineStatus.lowercase() == "ongoing"
    val statusColor = if (isOngoing) Green else TextLight
    val statusBg = if (isOngoing) GreenSoft else Color(0xFFF8FAFC)

    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Column {
            // Top accent bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(listOf(Blue, Color(0xFF4F8AFF))),
                        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Icon circle
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .background(BlueSoft, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getMedicineIcon(med.dosageForm),
                        contentDescription = null,
                        tint = Blue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    // Name + status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = med.drugName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(statusBg, RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = med.medicineStatus.replaceFirstChar { it.uppercase() },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    // Dosage form pill
                    if (med.dosageForm.isNotBlank()) {
                        MiniPill(
                            icon = Icons.Default.Vaccines,
                            text = med.dosageForm.replaceFirstChar { it.uppercase() },
                            color = Blue
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    // Frequency & Duration row
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (med.doseFrequency.isNotBlank()) {
                            MiniPill(
                                icon = Icons.Default.Schedule,
                                text = med.doseFrequency,
                                color = Purple
                            )
                        }
                        if (med.duration.isNotBlank() && med.duration != "0") {
                            MiniPill(
                                icon = Icons.Default.CalendarMonth,
                                text = "${med.duration} Days",
                                color = Teal
                            )
                        }
                    }
                    // Remark
                    if (med.remark.isNotEmpty() && med.remark != "0") {
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(OrangeSoft, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp)
                        ) {
                            Icon(
                                Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = Orange,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = med.remark,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Orange
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniPill(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, fontSize = 11.sp, color = color, fontWeight = FontWeight.Medium)
    }
}

// ─── Vitals Grid ───────────────────────────────────────────────────────────────
@Composable
private fun VitalsGrid(vitals: List<DisplayVitalItem>, containerColor: Color) {
    val rows = vitals.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowItems.forEach { vital ->
                    VitalCard(vital, containerColor, Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun VitalCard(vital: DisplayVitalItem, containerColor: Color, modifier: Modifier) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = modifier.border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(vital.bgColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    vital.icon,
                    contentDescription = null,
                    tint = vital.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    vital.name,
                    fontSize = 11.sp,
                    color = TextLight,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        vital.valueString,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                    if (vital.unit.isNotBlank()) {
                        Spacer(Modifier.width(3.dp))
                        Text(
                            vital.unit,
                            fontSize = 11.sp,
                            color = TextLight,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Investigations Card ────────────────────────────────────────────────────────
@Composable
private fun InvestigationsCard(investigations: List<InvestigationDetail>, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Column {
            investigations.forEachIndexed { index, inv ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (inv.isDone) GreenSoft else OrangeSoft,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (inv.isDone) Icons.Default.CheckCircle else Icons.Default.Pending,
                            contentDescription = null,
                            tint = if (inv.isDone) Green else Orange,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        getInvestigationName(inv.itemId),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusBadge(
                        label = if (inv.isDone) "Done" else "Pending",
                        color = if (inv.isDone) Green else Orange,
                        bg = if (inv.isDone) GreenSoft else OrangeSoft
                    )
                }
                if (index < investigations.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                }
            }
        }
    }
}

// ─── Clinical Summary Card ──────────────────────────────────────────────────────
@Composable
private fun ClinicalSummaryCard(summary: ProblemDetail, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(TealSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.TextSnippet,
                    contentDescription = null,
                    tint = Teal,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                summary.problemName,
                fontSize = 14.sp,
                color = TextMid,
                lineHeight = 22.sp
            )
        }
    }
}

// ─── Symptoms & Diagnosis Card ─────────────────────────────────────────────────
@Composable
private fun SymptomsDiagnosisCard(
    symptoms: List<ProblemDetail>,
    diagnoses: List<ProblemDetail>,
    containerColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Diagnosis block
            if (diagnoses.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedSoft, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.LocalHospital,
                        contentDescription = null,
                        tint = Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "DIAGNOSIS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red
                    )
                }
                Spacer(Modifier.height(10.dp))
                diagnoses.forEach { diag ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Red, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            diag.problemName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                    }
                }
                if (symptoms.isNotEmpty()) {
                    Spacer(Modifier.height(14.dp))
                    HorizontalDivider(thickness = 1.dp, color = DividerColor)
                    Spacer(Modifier.height(14.dp))
                }
            }
            // Symptoms block
            if (symptoms.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(OrangeSoft, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.SentimentVeryDissatisfied,
                        contentDescription = null,
                        tint = Orange,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "REPORTED SYMPTOMS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Orange
                    )
                }
                Spacer(Modifier.height(10.dp))
                symptoms.forEach { sym ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Orange, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            sym.problemName,
                            fontSize = 14.sp,
                            color = TextDark
                        )
                    }
                }
            }
        }
    }
}

// ─── Dietary Guidance Card ──────────────────────────────────────────────────────
@Composable
private fun DietaryCard(
    recommended: List<ProblemDetail>,
    avoid: List<ProblemDetail>,
    containerColor: Color
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (recommended.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(GreenSoft, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = Green,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "RECOMMENDED FOODS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Green
                    )
                }
                Spacer(Modifier.height(10.dp))
                recommended.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Green, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(item.problemName, fontSize = 14.sp, color = TextDark)
                    }
                }
            }
            if (recommended.isNotEmpty() && avoid.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                HorizontalDivider(thickness = 1.dp, color = DividerColor)
                Spacer(Modifier.height(14.dp))
            }
            if (avoid.isNotEmpty()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RedSoft, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        Icons.Default.NoFood,
                        contentDescription = null,
                        tint = Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "FOODS TO AVOID",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Red
                    )
                }
                Spacer(Modifier.height(10.dp))
                avoid.forEach { item ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Red, CircleShape)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(item.problemName, fontSize = 14.sp, color = TextDark)
                    }
                }
            }
        }
    }
}

// ─── Monitoring Schedule Card ───────────────────────────────────────────────────
@Composable
private fun MonitoringCard(frequencies: List<VitalFrequencyDetail>, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Column {
            frequencies.forEachIndexed { index, freq ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(BlueSoft, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            getFreqIcon(freq.parameterId, freq.parameterTypeId),
                            contentDescription = null,
                            tint = Blue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            getFreqParameterName(freq.parameterId, freq.parameterTypeId),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextDark
                        )
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                getFreqName(freq.frequencyTypeId),
                                fontSize = 12.sp,
                                color = TextLight
                            )
                            if (freq.quantity != null) {
                                Text("  ·  ", fontSize = 12.sp, color = TextLight)
                                Text(
                                    "Target: ${freq.quantity.toInt()} ml",
                                    fontSize = 12.sp,
                                    color = Blue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    Icon(
                        Icons.Default.DirectionsRun,
                        contentDescription = null,
                        tint = TextLight,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (index < frequencies.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        thickness = 1.dp,
                        color = DividerColor
                    )
                }
            }
        }
    }
}

// ─── Follow-Up Card ─────────────────────────────────────────────────────────────
@Composable
private fun FollowUpCard(followUp: ProblemDetail, containerColor: Color) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFCC80))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Orange,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Next Visit",
                    fontSize = 11.sp,
                    color = TextLight,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    followUp.problemName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )
            }
        }
    }
}

// ─── Status Badge ───────────────────────────────────────────────────────────────
@Composable
private fun StatusBadge(label: String, color: Color, bg: Color) {
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

// ─── Loading State ──────────────────────────────────────────────────────────────
@Composable
private fun PrescriptionLoadingState(bgColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Blue, strokeWidth = 3.dp)
            Spacer(Modifier.height(16.dp))
            Text("Fetching prescription...", fontSize = 14.sp, color = TextLight)
        }
    }
}

// ─── Error State ────────────────────────────────────────────────────────────────
@Composable
private fun PrescriptionErrorState(message: String, bgColor: Color, onRetry: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(RedSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ErrorOutline, null, tint = Red, modifier = Modifier.size(40.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text("Something went wrong", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(6.dp))
            Text(
                message.take(120),
                fontSize = 13.sp,
                color = TextLight,
                lineHeight = 20.sp
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Try Again", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Empty State ────────────────────────────────────────────────────────────────
@Composable
private fun PrescriptionEmptyState(bgColor: Color, onRefresh: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(BlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.MedicalServices,
                    null,
                    tint = Blue,
                    modifier = Modifier.size(44.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("No Prescriptions Found", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextDark)
            Spacer(Modifier.height(6.dp))
            Text(
                "No OPD history is available for this patient.",
                fontSize = 13.sp,
                color = TextLight
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(containerColor = Blue),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Refresh", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Helper: Medicine Icon ──────────────────────────────────────────────────────
private fun getMedicineIcon(dosageForm: String): ImageVector {
    return when (dosageForm.lowercase()) {
        "injection", "inj" -> Icons.Default.Vaccines
        "drop", "drops" -> Icons.Default.WaterDrop
        "syrup", "liquid" -> Icons.Default.WaterDrop
        else -> Icons.Default.MedicalServices
    }
}

// ─── Helper: Investigation Name ────────────────────────────────────────────────
private fun getInvestigationName(itemId: Int): String = when (itemId) {
    71 -> "Complete Blood Count (CBC)"
    260 -> "Urinalysis / Urine Routine"
    958 -> "Brain MRI / Neurological Scan"
    else -> "Lab Test #$itemId"
}

// ─── Helper: Vital Frequency ───────────────────────────────────────────────────
private fun getFreqParameterName(parameterId: Int, parameterTypeId: Int): String = when {
    parameterId == 2 -> "Water Intake / Hydration"
    parameterId == 1 -> when (parameterTypeId) {
        4 -> "Blood Pressure Monitoring"
        7 -> "Respiratory Rate Monitoring"
        else -> "Vital Monitoring"
    }
    else -> "Parameter Monitoring"
}

private fun getFreqIcon(parameterId: Int, parameterTypeId: Int): ImageVector = when {
    parameterId == 2 -> Icons.Default.WaterDrop
    parameterId == 1 -> when (parameterTypeId) {
        4 -> Icons.Default.Favorite
        else -> Icons.Default.MonitorHeart
    }
    else -> Icons.Default.MonitorHeart
}

private fun getFreqName(frequencyTypeId: Int): String = when (frequencyTypeId) {
    2 -> "AC — Before Meals"
    5 -> "OD — Once Daily"
    else -> "Daily"
}

// ─── Helper: Display Vitals Mapping ───────────────────────────────────────────
private fun getDisplayVitals(rawVitals: List<PatientVitalDetail>): List<DisplayVitalItem> {
    val list = mutableListOf<DisplayVitalItem>()
    val sys = rawVitals.firstOrNull { it.vmId == 4 }?.vmValue
    val dia = rawVitals.firstOrNull { it.vmId == 6 }?.vmValue
    if (sys != null && dia != null) {
        list.add(DisplayVitalItem("Blood Pressure", "${sys.toInt()}/${dia.toInt()}", "mmHg", Green, GreenSoft, Icons.Default.Favorite))
    } else {
        sys?.let { list.add(DisplayVitalItem("Systolic BP", "${it.toInt()}", "mmHg", Green, GreenSoft, Icons.Default.Favorite)) }
        dia?.let { list.add(DisplayVitalItem("Diastolic BP", "${it.toInt()}", "mmHg", Green, GreenSoft, Icons.Default.Favorite)) }
    }
    rawVitals.firstOrNull { it.vmId == 7 }?.let {
        list.add(DisplayVitalItem("Temperature", String.format("%.1f", it.vmValue), "°C", Orange, OrangeSoft, Icons.Default.MonitorHeart))
    }
    rawVitals.firstOrNull { it.vmId == 5 }?.let {
        list.add(DisplayVitalItem("Pulse Rate", "${it.vmValue.toInt()}", "bpm", Red, RedSoft, Icons.Default.Favorite))
    }
    rawVitals.firstOrNull { it.vmId == 3 }?.let {
        list.add(DisplayVitalItem("Oxygen (SpO2)", "${it.vmValue.toInt()}", "%", Blue, BlueSoft, Icons.Default.WaterDrop))
    }
    rawVitals.firstOrNull { it.vmId == 56 }?.let {
        list.add(DisplayVitalItem("Heart Rate", "${it.vmValue.toInt()}", "bpm", Red, RedSoft, Icons.Default.MonitorHeart))
    }
    val skipIds = setOf(3, 4, 5, 6, 7, 56)
    rawVitals.filter { it.vmId !in skipIds }.forEach { v ->
        val valStr = if (v.vmValue == kotlin.math.floor(v.vmValue)) "${v.vmValue.toInt()}"
        else String.format("%.1f", v.vmValue)
        when (v.vmId) {
            10 -> list.add(DisplayVitalItem("RBS", valStr, "mg/dL", Red, RedSoft, Icons.Default.WaterDrop))
            2, 57 -> list.add(DisplayVitalItem("Weight", valStr, "kg", Teal, TealSoft, Icons.Default.DirectionsRun))
            1 -> list.add(DisplayVitalItem("Height", valStr, "cm", Purple, PurpleSoft, Icons.Default.DirectionsRun))
            74 -> list.add(DisplayVitalItem("Heart Rate", valStr, "bpm", Red, RedSoft, Icons.Default.MonitorHeart))
            else -> list.add(DisplayVitalItem("Vital #${v.vmId}", valStr, "", TextLight, DividerColor, Icons.Default.MonitorHeart))
        }
    }
    return list
}