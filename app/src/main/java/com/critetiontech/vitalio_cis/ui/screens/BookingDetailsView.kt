package com.critetiontech.vitalio_cis.ui.screens

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
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
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.CommonButton
import com.critetiontech.vitalio_cis.viewmodel.DoctorDetailsViewModel

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun BookingDetailsScreen(
    bookingDetails: BookingDetails?,
    viewModel: DoctorDetailsViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current

    CommonAppBar(title = "Booking Details") {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(16.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            DateTimeSection(
                date = bookingDetails?.onDate.orEmpty(),
                time = bookingDetails?.onTime.orEmpty()
            )

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(
                title    = bookingDetails?.pName.orEmpty(),
                subtitle = bookingDetails?.qly.orEmpty()
            )

            Divider(modifier = Modifier.padding(vertical = 12.dp))

            InfoRow(
                title    = bookingDetails?.atHospital.orEmpty(),
                subtitle = ""
            )

            Spacer(modifier = Modifier.weight(1f))

            CommonButton(
                text    = "Done",
                onClick = {
                    navController.navigate(Routes.APPOINTMENTS) {
                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                    }
                }
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
fun DateTimeSection(date: String = "", time: String = "") {
    val colors = LocalMyColorScheme.current

    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.dashboardContainerColor, RoundedCornerShape(14.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "📅", style = AppTextStyles.style18BCB())
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = date.ifEmpty { "—" }, style = AppTextStyles.style14BCN())
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .background(colors.dashboardContainerColor, RoundedCornerShape(14.dp))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "🕒", style = AppTextStyles.style18BCB())
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = time.ifEmpty { "—" }, style = AppTextStyles.style14BCN())
            }
        }
    }
}