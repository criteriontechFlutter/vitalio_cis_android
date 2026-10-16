package com.critetiontech.ctvitalio.ui.screens

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.HapticCheckbox
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.viewmodel.DoctorDetailsViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.google.gson.Gson
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class BookingDetails(
    val dID: String,
    val pName: String,
    val qly: String,
    val atHospital: String,
    val onDate: String,
    val onTime: String?,
    val free: String?,
    val shiftId: Int = 0,
    val queueNo: Int? = null,
    /** "Walk-in" or "Virtual", as picked on Doctor's Details. */
    val mode: String = "Walk-in",
    /** Slot length in minutes; 0 for token bookings. */
    val slotMinutes: Int = 0,
    /** Consultation fee as the server sends it, e.g. "1600". */
    val fee: String = "",
    /** Visit number from the server once booked, e.g. "VNO2604500640". */
    val visitNo: String? = null,
    /** Token bookings: the shift's name and end time ("Morning", "17:00"); [onTime] is its start. */
    val shiftName: String = "",
    val shiftEnd: String = "",
)

/** "10:00 AM - 11:10 AM" for a slot, "10:00 AM - 05:00 PM" for a token shift, or just the start. */
internal fun BookingDetails.timeRange(): String {
    val start = onTime.orEmpty()
    if (start.isEmpty()) return ""
    return when {
        slotMinutes > 0 -> "${formatSlotTime(start)} - ${slotEnd(start, slotMinutes)}"
        shiftEnd.isNotEmpty() -> "${formatSlotTime(start)} - ${formatSlotTime(shiftEnd)}"
        else -> formatSlotTime(start)
    }
}

/** "2026-10-16" or "2026/10/16" (months and days may be unpadded) as a date. */
@RequiresApi(Build.VERSION_CODES.O)
internal fun parseBookingDate(raw: String): LocalDate? = try {
    val parts = raw.replace('/', '-').split('-').map { it.trim().toInt() }
    LocalDate.of(parts[0], parts[1], parts[2])
} catch (e: Exception) {
    null
}

/** "2026-10-16" → "16/10/2026". */
@RequiresApi(Build.VERSION_CODES.O)
private fun formatBookingDate(raw: String): String =
    parseBookingDate(raw)?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: raw

/** "1600" → "₹1,600.00"; null when there is no fee to show. */
private fun formatFee(raw: String?): String? {
    val amount = raw?.replace(",", "")?.trim()?.toDoubleOrNull() ?: return null
    return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingConfirmationScreen(
    bookingDetails: BookingDetails?,
    viewModel: DoctorDetailsViewModel = viewModel()
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current

    // This screen has its own ViewModel, so load the doctor's department, booking type and consent here.
    LaunchedEffect(bookingDetails?.dID) {
        bookingDetails?.dID?.takeIf { it.isNotEmpty() }?.let { viewModel.initDoctorConfiguration(it) }
    }

    val doctor by viewModel.doctor.collectAsState()
    val bookingLoading by viewModel.bookingLoading.collectAsState()
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()
    val bookingError by viewModel.bookingError.collectAsState()
    var consent by rememberSaveable { mutableStateOf(false) }

    // Navigate on success — pop the entire booking flow and go to BookingDetails
    LaunchedEffect(bookingSuccess) {
        if (bookingSuccess) {
            viewModel.resetBookingState()
            val json = Uri.encode(Gson().toJson(bookingDetails?.copy(visitNo = viewModel.visitNo.value)))
            navController.navigate(Routes.BOOKINGDETAILS + "/$json") {
                popUpTo(Routes.FINDDOCTOR) { inclusive = true }
            }
        }
    }

    val details = bookingDetails
    val mode = details?.mode?.ifEmpty { null } ?: "Walk-in"
    val virtual = mode == "Virtual"
    val department = details?.atHospital?.ifEmpty { null } ?: doctor?.departmentName?.ifEmpty { null }
    val fee = formatFee(details?.fee?.ifEmpty { null } ?: doctor?.consultationFee)
    val time = details?.onTime.orEmpty()
    val minutes = details?.slotMinutes ?: 0

    Column(Modifier.fillMaxSize().background(colors.dashboardContainerColor)) {
        RoundBackTopBar(title = "Appointment Confirmation")

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            ConfirmRow(
                label = "Doctor:",
                primary = details?.pName.orEmpty(),
                secondary = details?.qly.orEmpty(),
            )
            ConfirmDivider()
            ConfirmRow(
                label = "$mode :",
                primary = department?.let { "$it Department" } ?: if (virtual) "Online consultation" else "In-clinic visit",
                secondary = if (virtual) "Online consultation with the doctor" else "Visit the clinic at your appointment time",
            )
            ConfirmDivider()
            ConfirmRow(label = "Appointment Date :", primary = formatBookingDate(details?.onDate.orEmpty()))
            ConfirmDivider()

            Text(if (details?.queueNo != null) "Shift :" else "Select Time Slot :", color = colors.textGreyColor, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                buildAnnotatedString {
                    append(details?.timeRange().orEmpty().ifEmpty { formatSlotTime(time) })
                    val note = if (minutes > 0) "$minutes Min" else details?.shiftName.orEmpty()
                    if (note.isNotEmpty()) {
                        withStyle(SpanStyle(color = colors.textGreyColor, fontWeight = FontWeight.Normal)) { append("   ($note)") }
                    }
                },
                color = colors.textDarkColor,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            )
            details?.queueNo?.let {
                Spacer(Modifier.height(4.dp))
                Text("Token No. $it", color = colors.primaryBlueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }

            if (bookingError != null) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFEE2E2))
                        .padding(14.dp)
                ) {
                    Text("Appointment not booked", color = Color(0xFFB91C1C), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(bookingError!!, color = Color(0xFFB91C1C), fontSize = 13.sp, lineHeight = 18.sp)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Choose another time",
                        color = Color(0xFFB91C1C),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White)
                            .clickable { navController.popBackStack() }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // Consent, fee and button pinned to the bottom.
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { consent = !consent }
                    .padding(vertical = 6.dp),
            ) {
                HapticCheckbox(
                    checked = consent,
                    onCheckedChange = { consent = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.primaryBlueColor,
                        uncheckedColor = Color(0xFFCBD5E1),
                    ),
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "I consent to my current prescription details being shared with the clinic and doctor for care coordination.",
                    color = colors.textGreyColor,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }

            if (fee != null) {
                ConfirmDivider()
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Fee to be paid :", color = colors.textGreyColor, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Text(fee, color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(16.dp))
            CommonButton(
                text = "Continue",
                isLoading = bookingLoading,
                enabled = !bookingLoading && details != null,
                cornerRadius = 14.dp,
                containerColor = colors.primaryBlueColor,
                modifier = Modifier.height(54.dp),
                onClick = {
                    viewModel.bookAppointment(
                        did = details?.dID.orEmpty(),
                        shiftId = details?.shiftId ?: 0,
                        sTime = details?.onTime.orEmpty(),
                        appointmentDate = details?.onDate.orEmpty(),
                        queueNo = details?.queueNo,
                        consentGiven = consent,
                    )
                },
            )
        }
    }
}

@Composable
private fun ConfirmRow(label: String, primary: String, secondary: String = "") {
    val colors = LocalMyColorScheme.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, color = colors.textGreyColor, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(primary, color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.Medium)
        if (secondary.isNotEmpty()) {
            Spacer(Modifier.height(2.dp))
            Text(secondary, color = colors.textGreyColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ConfirmDivider() {
    HorizontalDivider(Modifier.padding(vertical = 16.dp), thickness = 1.dp, color = Color(0xFFEEF0F3))
}
