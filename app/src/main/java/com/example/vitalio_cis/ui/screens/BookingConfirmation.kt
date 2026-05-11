package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.viewmodel.DoctorDetailsViewModel


data class BookingDetails(
    val dID: String,
    val pName: String,
    val qly: String,
    val atHospital: String,
    val onDate: String,
    val onTime: String?,
    val free: String?,
)

@Composable
fun BookingConfirmationScreen(
    bookingDetails: BookingDetails?,

      viewModel: DoctorDetailsViewModel = viewModel()
 ) {

    val navController = LocalNavController.current

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.getDoctorProfile(context,)
    }
    val doctor by viewModel.doctor.collectAsState()
    val colors = LocalMyColorScheme.current
    CommonAppBar(
        title = "Booking Confirmation",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {

            // Top Bar


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {

                // Doctor
                Text(
                    "Doctor:",
                    style = AppTextStyles.style12GCN(),
                )

                Text(
                    bookingDetails?.pName.toString(),
                    style = AppTextStyles.style16BCB(),
                )

                Text(
                    bookingDetails?.qly.toString(),
                    style = AppTextStyles.style12GCN().copy(fontSize = 13.sp),
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Location
                Text("At :",
                    style = AppTextStyles.style12GCN(),
                )

                Text(
                    bookingDetails?.atHospital.toString(),
                    style = AppTextStyles.style16BCB(),
                )

                Text(
                    "1st Floor, Aluva Tower, Bank Junction, Aluva, Kochi...",
                    style = AppTextStyles.style12GCN().copy(fontSize = 13.sp),
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Date
                Text("On Date :",
                    style = AppTextStyles.style12GCN(),
                )


                Text(
                    bookingDetails?.onDate.toString(),
                    style = AppTextStyles.style16BCB(),
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Time
                Text("Time & Duration :",
                    style = AppTextStyles.style12GCN(),
                )

                Row {
                    Text(
                        bookingDetails?.onTime.toString(),
                        style = AppTextStyles.style16BCB(),
                    )


                }

                Spacer(modifier = Modifier.weight(1f))
                // Checkbox
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    var checked by remember { mutableStateOf(true) }

                    Checkbox(
                        checked = checked,
                        onCheckedChange = { checked = it }
                    )

                    Text(
                        "I consent to my current prescription details being shared with the clinic and doctor for care coordination.",

                        style = AppTextStyles.style12GCN().copy(fontSize = 13.sp),
                    )
                }


                // Fee
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Fee to be paid :", color = Color.Gray)

                    Text(
                        doctor?.consultationFee.toString(),
                        style = AppTextStyles.style16BCB(),
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))





                CommonButton(text = "Continue",
                    onClick = {

//                        navController.navigate(Routes.BOOKINGDETAILS)
//
                        viewModel.bookAppointment(context,
                            sTime = bookingDetails?.onTime.toString(),
                            did = bookingDetails?.dID.toString(),
                            appointmentDate =  bookingDetails?.onDate.toString(),
                            navController = navController,
                            bookingDetails =bookingDetails


                        )
                    })


            }
        }
    }
}