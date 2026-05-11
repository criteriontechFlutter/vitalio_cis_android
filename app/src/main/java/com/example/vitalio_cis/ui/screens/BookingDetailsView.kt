package com.example.vitalio_cis.ui.screens

import android.annotation.SuppressLint
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.NavigationManager.navController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.viewmodel.DoctorDetailsViewModel

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun BookingDetailsScreen(

    bookingDetails: BookingDetails?,
    viewModel: DoctorDetailsViewModel = viewModel( )) {

    val doctor by viewModel.doctor.collectAsState()

    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    CommonAppBar(
        title = "Booking Details",
    ) {


            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .padding(16.dp)
            )
            {

                Spacer(modifier = Modifier.height(16.dp))

                DateTimeSection()

                Spacer(modifier = Modifier.height(16.dp))

                InfoRow(
                    title = bookingDetails?.pName.toString(),
                    subtitle = bookingDetails?.qly.toString()
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                InfoRow(
                    title = "Victoria General Hospital",
                    subtitle = "11620, State Route 41, West Union"
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                InfoRow(
                    title = "Payment",
                    subtitle = "••••4826"
                )

                Spacer(modifier = Modifier.weight(1f))


                CommonButton(
                    text = "Add to calendar",
                    onClick =  {

                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                CommonButton(
                    text = "Reschedule Appointment",
                    onClick =  {

                    },
                    modifier = Modifier.border(
                        width = 1.dp,
                        color = colors.primaryBlueColor,
                        shape = RoundedCornerShape(14.dp)
                    ).background(colors.btnWhiteColor)
                )

            }

    }
}

@Composable
fun InfoRow(
    title: String,
    subtitle: String
) {

    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.dashboardContainerColor, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("•",
                style = AppTextStyles.style16BCB()
                ) // replace with Icon later
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = AppTextStyles.style16BCB()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                style = AppTextStyles.style12GCN()
            )
        }
    }
}

@Composable
fun DateTimeSection() {


    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.dashboardContainerColor,
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text = "📅",
                    style = AppTextStyles.style18BCB()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "16/12/2024",
                    style = AppTextStyles.style14BCN()
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.dashboardContainerColor,
                    RoundedCornerShape(14.dp)
                )
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {

                Text(
                    text = "🕒",
                    style = AppTextStyles.style18BCB()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "09:00AM - 10:00AM",
                    style = AppTextStyles.style14BCN()
                )
            }
        }
    }
}