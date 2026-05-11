package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.PrefsManager

@Composable
fun PersonalInfoScreen() {

    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val prefsCache = PrefsManager(context)
    CommonAppBar(
        title = "Personal Info",
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
        ) {

            // TOP BAR


            Spacer(modifier = Modifier.height(10.dp))

            // PROFILE IMAGE
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box {
                    Image(
                        painter = painterResource(id = R.drawable.img),
                        contentDescription = null,
                        modifier = Modifier
                            .size(110.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1557FF))
                            .align(Alignment.BottomEnd),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                // CARD 1
                InfoCard {
                    ProfileInfoRow("Name", prefsCache.getPatient()?.firstName.toString())
//                    ProfileInfoRow("Last Name", prefsCache.getPatient()?.lastName.toString())
                    ProfileInfoRow("Sex",  prefsCache.getPatient()?.genderName.toString())
                    ProfileInfoRow("Date of Birth", prefsCache.getPatient()?.dob.toString())
                }

                // CARD 2
                InfoCard {
                    ProfileInfoRow("Mobile No.", prefsCache.getPatient()?.mobileNo.toString())
                    ProfileInfoRow("Email", prefsCache.getPatient()?.emailAddress.toString())
                }

                // CARD 3
                InfoCard {
                    ProfileInfoRow("Street Address", prefsCache.getPatient()?.address.toString())
                    ProfileInfoRow("Zip Code", prefsCache.getPatient()?.zip.toString())
                    ProfileInfoRow("City", prefsCache.getPatient()?.cityName.toString())
                    ProfileInfoRow("State",prefsCache.getPatient()?.stateName.toString())
                    ProfileInfoRow("Country", prefsCache.getPatient()?.countryName.toString())
                }
            }

            // BOTTOM BUTTON
            Button(
                onClick = { },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1557FF)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(55.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Personal Info")
            }
        }
    }
}

@Composable
fun InfoCard(content: @Composable ColumnScope.() -> Unit) {


    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor =colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = content
        )
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {

    Row(
        modifier = Modifier.fillMaxWidth()
    ) {

        Text(
            text = label,
            style = AppTextStyles.style14GCN(),
            modifier = Modifier.weight(0.4f)
        )

        Text(
            text = value,
            style = AppTextStyles.style14BCB(),
            modifier = Modifier.weight(0.6f)
        )
    }
}