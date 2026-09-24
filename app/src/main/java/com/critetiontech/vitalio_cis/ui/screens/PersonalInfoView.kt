package com.critetiontech.vitalio_cis.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.PrefsManager



@Composable
fun PersonalInfoScreen() {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val prefs = remember { PrefsManager(context) }

    // Re-read patient fresh every time this screen is shown
    var patient by remember { mutableStateOf(prefs.getPatient()) }
    LaunchedEffect(Unit) {
        patient = prefs.getPatient()
    }

    val navController = LocalNavController.current

    CommonAppBar(
        title = "Personal Info",
        actions = {}
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile image (view-only)
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    val imageUrl = patient?.imageUrl?.takeIf { it.isNotEmpty() }
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.size(110.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.img),
                            placeholder = painterResource(id = R.drawable.img)
                        )
                    } else {
                        androidx.compose.foundation.Image(
                            painter = painterResource(id = R.drawable.img),
                            contentDescription = null,
                            modifier = Modifier.size(110.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // View-only info cards (always shown on this screen)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard {
                        ProfileInfoRow("First Name", patient?.firstName.orEmpty())
                        ProfileInfoRow("Last Name", patient?.lastName.orEmpty())
                        ProfileInfoRow("Sex", patient?.genderName.orEmpty())
                        ProfileInfoRow("Date of Birth", patient?.dob.orEmpty())
                    }
                    InfoCard {
                        ProfileInfoRow("Mobile No.", patient?.mobileNo.orEmpty())
                        ProfileInfoRow("Email", patient?.emailAddress.orEmpty())
                    }
                    InfoCard {
                        ProfileInfoRow("Street Address", patient?.address.orEmpty())
                        ProfileInfoRow("Zip Code", patient?.zip.orEmpty())
                        ProfileInfoRow("City", patient?.cityName.orEmpty())
                        ProfileInfoRow("State", patient?.stateName.orEmpty())
                        ProfileInfoRow("Country", patient?.countryName.orEmpty())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom button → navigate to Edit Profile
            Button(
                onClick = { navController.navigate(com.critetiontech.vitalio_cis.Routes.EDITPROFILE) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1557FF)),
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
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
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
    val colors = LocalMyColorScheme.current
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = colors.textGreyColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value.ifEmpty { "—" },
            color = colors.textDarkColor,
            fontSize = 14.sp,
            modifier = Modifier.weight(0.6f)
        )
    }
}