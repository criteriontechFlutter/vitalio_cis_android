package com.critetiontech.vitalio_cis.ui.screens

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.CommonButton
import com.critetiontech.vitalio_cis.viewmodel.DoctorDetailsViewModel
import com.google.gson.Gson

data class BookingDetails(
    val dID: String,
    val pName: String,
    val qly: String,
    val atHospital: String,
    val onDate: String,
    val onTime: String?,
    val free: String?,
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingConfirmationScreen(
    bookingDetails: BookingDetails?,
    viewModel: DoctorDetailsViewModel = viewModel()
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    LaunchedEffect(Unit) {
        viewModel.getDoctorProfile(context)
    }

    val doctor by viewModel.doctor.collectAsState()
    val bookingLoading by viewModel.bookingLoading.collectAsState()
    val bookingSuccess by viewModel.bookingSuccess.collectAsState()
    val bookingError by viewModel.bookingError.collectAsState()

    // Navigate on success — pop the entire booking flow and go to BookingDetails
    LaunchedEffect(bookingSuccess) {
        if (bookingSuccess) {
            viewModel.resetBookingState()
            val json = Uri.encode(Gson().toJson(bookingDetails))
            navController.navigate(Routes.BOOKINGDETAILS + "/$json") {
                popUpTo(Routes.FINDDOCTOR) { inclusive = true }
            }
        }
    }

    CommonAppBar(title = "Booking Confirmation") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // Doctor card
                BookingInfoCard(
                    icon     = Icons.Rounded.Person,
                    label    = "Doctor",
                    primary  = bookingDetails?.pName.orEmpty(),
                    secondary = bookingDetails?.qly.orEmpty()
                )

                // Location card
                BookingInfoCard(
                    icon     = Icons.Rounded.LocationOn,
                    label    = "At",
                    primary  = bookingDetails?.atHospital.orEmpty(),
                    secondary = ""
                )

                // Date card
                BookingInfoCard(
                    icon     = Icons.Rounded.DateRange,
                    label    = "Date",
                    primary  = bookingDetails?.onDate.orEmpty(),
                    secondary = ""
                )

                // Time card
                BookingInfoCard(
                    icon     = Icons.Rounded.Schedule,
                    label    = "Time",
                    primary  = bookingDetails?.onTime.orEmpty(),
                    secondary = ""
                )

                // Consultation fee row
                if (doctor?.consultationFee != null) {
                    Surface(
                        color  = colors.dashboardContainerColor,
                        shape  = RoundedCornerShape(14.dp),
                        shadowElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fee to be paid", style = AppTextStyles.style14GCN())
                            Text(
                                "₹ ${doctor!!.consultationFee}",
                                style = AppTextStyles.style16BCB()
                            )
                        }
                    }
                }

                // Error message
                if (bookingError != null) {
                    Text(
                        bookingError!!,
                        color    = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // Consent + button pinned to bottom
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.dashboardBackgroundColor)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                var checked by remember { mutableStateOf(true) }

                Row(
                    verticalAlignment = Alignment.Top,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "I consent to my current prescription details being shared with the clinic and doctor for care coordination.",
                        style = AppTextStyles.style12GCN(),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(Modifier.height(4.dp))

                CommonButton(
                    text      = "Confirm Appointment",
                    isLoading = bookingLoading,
                    enabled   = checked && !bookingLoading,
                    onClick   = {
                        viewModel.bookAppointment(
                            context          = context,
                            did              = bookingDetails?.dID.orEmpty(),
                            sTime            = bookingDetails?.onTime.orEmpty(),
                            appointmentDate  = bookingDetails?.onDate.orEmpty()
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BookingInfoCard(
    icon: ImageVector,
    label: String,
    primary: String,
    secondary: String
) {
    val colors = LocalMyColorScheme.current

    Surface(
        color           = colors.dashboardContainerColor,
        shape           = RoundedCornerShape(14.dp),
        shadowElevation = 1.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier          = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color(0xFFEAF1FF), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFF2962FF), modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(label, style = AppTextStyles.style12GCN())
                Spacer(Modifier.height(2.dp))
                Text(primary, style = AppTextStyles.style14BCN(), fontWeight = FontWeight.SemiBold)
                if (secondary.isNotEmpty()) {
                    Text(secondary, style = AppTextStyles.style12GCN())
                }
            }
        }
    }
}
