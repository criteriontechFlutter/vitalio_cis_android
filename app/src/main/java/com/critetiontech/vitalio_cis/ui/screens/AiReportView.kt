package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.ui.theme.MyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.UploadReportViewModel
import com.google.gson.Gson

// ─────────────────────────────────────────────────────────
// SCREEN
// ─────────────────────────────────────────────────────────

@Composable
fun AiReportScreen(viewModel: UploadReportViewModel) {

    val colors         = LocalMyColorScheme.current
    val context        = LocalContext.current
    val navController  = LocalNavController.current
    val isLoading      by viewModel.loading.collectAsStateWithLifecycle()
    val responseStr    by viewModel.responseString.collectAsStateWithLifecycle()
    val uploadSuccess  by viewModel.uploadSuccess.collectAsStateWithLifecycle()

    // Navigate back to the previous screen (LabReports) after a successful save
    LaunchedEffect(uploadSuccess) {
        if (uploadSuccess) {
            viewModel.resetUploadSuccess()
            navController.popBackStack()
        }
    }

    // ── Parse JSON ───────────────────────────────────────
    val reportData = remember(responseStr) {
        if (responseStr.isNotEmpty()) {
            try { Gson().fromJson(responseStr, ApiResponse::class.java) }
            catch (e: Exception) { ApiResponse() }
        } else ApiResponse()
    }

    val responseData    = reportData.response.firstOrNull()
    val patientDetails  = responseData?.patient_details
    val report          = responseData?.report ?: emptyList()
    val summary         = responseData?.summary ?: ""
    val status          = responseData?.status ?: ""
    val specialty       = responseData?.recommended_specialty ?: ""

    val isNormal = status.lowercase().let {
        it.contains("normal") || it.contains("healthy") || it.contains("good")
    }
    val statusColor = when {
        isNormal -> Color(0xFF16A34A)
        status.isEmpty() -> colors.textGreyColor
        else -> Color(0xFFDC2626)
    }

    // ── UI ───────────────────────────────────────────────
    CommonAppBar(
        title = "AI Report Analysis",
        actions = {
            // Save button
            val btnBg = if (isLoading) colors.borderGreyLightColor else colors.primaryBlueColor
            Button(
                onClick = { viewModel.AddMedia(context) },
                enabled = !isLoading,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = btnBg,
                    disabledContainerColor = colors.borderGreyLightColor
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                modifier = Modifier.height(38.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Saving…", style = AppTextStyles.style12WCB())
                } else {
                    Icon(Icons.Default.CloudUpload, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Report", style = AppTextStyles.style12WCB())
                }
            }
            Spacer(Modifier.width(12.dp))
        }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // ─── Patient header card ──────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF1564ED), Color(0xFF0A3FA8))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = patientDetails?.patient_name?.ifEmpty { "Patient" } ?: "Patient",
                                    color = Color.White,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (!patientDetails?.age.isNullOrEmpty()) {
                                        PatientChip(
                                            icon = Icons.Default.Cake,
                                            label = patientDetails?.age ?: ""
                                        )
                                    }
                                    if (!patientDetails?.sex.isNullOrEmpty()) {
                                        PatientChip(
                                            icon = Icons.Default.Person,
                                            label = patientDetails?.sex ?: ""
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                        Spacer(Modifier.height(14.dp))

                        // Lab info row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            HeaderInfoItem(
                                label = "Lab",
                                value = patientDetails?.lab_name?.ifEmpty { "—" } ?: "—"
                            )
                            HeaderInfoItem(
                                label = "Collected",
                                value = patientDetails?.collection_date?.ifEmpty { "—" } ?: "—"
                            )
                            HeaderInfoItem(
                                label = "Reported",
                                value = patientDetails?.reported_date?.ifEmpty { "—" } ?: "—"
                            )
                        }
                    }
                }
            }

            // ─── Status badge ─────────────────────────────
            if (status.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Overall Status", style = AppTextStyles.style16BCB())
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(statusColor.copy(alpha = 0.12f))
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = status,
                                    color = statusColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }

            // ─── Summary card ─────────────────────────────
            if (summary.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(16.dp))
                    AiSectionCard(
                        title = "Summary",
                        icon = Icons.Default.Summarize,
                        colors = colors
                    ) {
                        Text(
                            text = summary,
                            style = AppTextStyles.style14BCN(),
                            lineHeight = 22.sp
                        )
                    }
                }
            }

            // ─── Recommended specialty ────────────────────
            if (specialty.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    AiSectionCard(
                        title = "Recommended Specialty",
                        icon = Icons.Default.LocalHospital,
                        colors = colors
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.primaryBlueColor.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Icon(
                                Icons.Default.MedicalServices,
                                contentDescription = null,
                                tint = colors.primaryBlueColor,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = specialty,
                                style = AppTextStyles.style14PCB()
                            )
                        }
                    }
                }
            }

            // ─── Lab results section header ───────────────
            if (report.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.primaryBlueColor)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Test Results", style = AppTextStyles.style16BCB())
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.primaryBlueColor.copy(alpha = 0.1f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "${report.size} tests",
                                style = AppTextStyles.style12PCB()
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }

            // ─── Lab result cards ─────────────────────────
            items(report) { item ->
                ReportResultCard(item = item, colors = colors)
                Spacer(Modifier.height(10.dp))
            }

            // empty state
            if (report.isEmpty() && responseStr.isNotEmpty()) {
                item {
                    Spacer(Modifier.height(40.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.HourglassEmpty,
                            contentDescription = null,
                            tint = colors.textGreyColor.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No test results found",
                            style = AppTextStyles.style14GCN(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// REPORT RESULT CARD
// ─────────────────────────────────────────────────────────

@Composable
fun ReportResultCard(item: ReportItem, colors: MyColorScheme) {

    var editedValue by remember(item.result) { mutableStateOf(item.result) }

    // Determine if value is outside range
    val rangeColor = remember(item.result, item.normal_values) {
        parseRangeColor(item.result, item.normal_values)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Test name
                Text(
                    text = item.test_name,
                    style = AppTextStyles.style14GCN(),
                    modifier = Modifier.weight(1f)
                )
                // Status dot
                if (rangeColor != null) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(rangeColor)
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Result value (large + editable)
                OutlinedTextField(
                    value = editedValue,
                    onValueChange = { editedValue = it },
                    modifier = Modifier.width(120.dp),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = rangeColor ?: colors.textDarkColor
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = colors.primaryBlueColor,
                        unfocusedBorderColor = colors.borderGreyLightColor,
                        focusedContainerColor   = colors.dashboardBackgroundColor,
                        unfocusedContainerColor = colors.dashboardBackgroundColor,
                    ),
                    suffix = {
                        if (!item.unit.isNullOrEmpty()) {
                            Text(
                                item.unit,
                                fontSize = 11.sp,
                                color = colors.textGreyColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                )

                Spacer(Modifier.weight(1f))

                // Normal range
                if (!item.normal_values.isNullOrEmpty()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "Normal Range",
                            style = AppTextStyles.style12GCN(),
                            color = colors.textGreyColor.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            item.normal_values,
                            style = AppTextStyles.style12BCB(),
                            color = colors.textGreyColor
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────
// SECTION CARD WRAPPER
// ─────────────────────────────────────────────────────────

@Composable
fun AiSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colors: MyColorScheme,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primaryBlueColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = colors.primaryBlueColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(title, style = AppTextStyles.style16BCB())
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

// ─────────────────────────────────────────────────────────
// SMALL HELPERS
// ─────────────────────────────────────────────────────────

@Composable
private fun PatientChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
        Spacer(Modifier.width(4.dp))
        Text(label, color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun HeaderInfoItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
        Spacer(Modifier.height(3.dp))
        Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, maxLines = 2)
    }
}

/** Returns green/red/null based on whether result is within normal range */
fun parseRangeColor(result: String, normalValues: String?): Color? {
    if (result.isEmpty() || normalValues.isNullOrEmpty()) return null
    return try {
        val resultNum = result.trim().toDoubleOrNull() ?: return null
        // Handles "3.5-5.0" or "< 200" or "> 0.5" etc.
        when {
            normalValues.contains("-") -> {
                val parts = normalValues.split("-")
                val lo = parts[0].trim().replace(",", ".").toDoubleOrNull()
                val hi = parts[1].trim().replace(",", ".").toDoubleOrNull()
                if (lo != null && hi != null) {
                    if (resultNum in lo..hi) Color(0xFF16A34A) else Color(0xFFDC2626)
                } else null
            }
            normalValues.startsWith("<") -> {
                val hi = normalValues.removePrefix("<").trim().toDoubleOrNull()
                if (hi != null) (if (resultNum < hi) Color(0xFF16A34A) else Color(0xFFDC2626)) else null
            }
            normalValues.startsWith(">") -> {
                val lo = normalValues.removePrefix(">").trim().toDoubleOrNull()
                if (lo != null) (if (resultNum > lo) Color(0xFF16A34A) else Color(0xFFDC2626)) else null
            }
            else -> null
        }
    } catch (e: Exception) { null }
}

// ─────────────────────────────────────────────────────────
// API MODELS
// ─────────────────────────────────────────────────────────

data class ApiResponse(
    val response: List<ResponseData> = emptyList()
)

data class ResponseData(
    val patient_details: PatientDetails = PatientDetails(),
    val report: List<ReportItem> = emptyList(),
    val summary: String = "",
    val status: String = "",
    val recommended_specialty: String = ""
)

data class PatientDetails(
    val patient_name: String = "",
    val sex: String = "",
    val age: String = "",
    val lab_name: String = "",
    val collection_date: String = "",
    val reported_date: String = ""
)

data class ReportItem(
    val test_name: String = "",
    val result: String = "",
    val unit: String? = null,
    val normal_values: String? = null,
    val id: String? = null
)
