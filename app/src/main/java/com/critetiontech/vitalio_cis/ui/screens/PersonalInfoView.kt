package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.text.font.FontWeight
import com.critetiontech.ctvitalio.ui.components.ProfileRing
import com.critetiontech.ctvitalio.utils.bloodGroupNameFor
import com.critetiontech.ctvitalio.utils.genderNameFor
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.viewmodel.PersonalInfoViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme



@Composable
fun PersonalInfoScreen(viewModel: PersonalInfoViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current

    // What login (VerifyOTP) and Update Profile saved; re-read each time this screen is shown
    val patient by viewModel.patient.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    val navController = LocalNavController.current
    val p = patient

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

                // Profile photo in the same turning ring as the dashboard and drawer
                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    ProfileRing(size = 116.dp, ringWidth = 3.dp, gap = 3.dp) {
                        AsyncImage(
                            model = p?.imageUrl?.takeIf { it.isNotEmpty() },
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                            error = painterResource(id = R.drawable.img),
                            placeholder = painterResource(id = R.drawable.img),
                            fallback = painterResource(id = R.drawable.img)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        listOfNotNull(p?.firstName, p?.lastName).joinToString(" ").trim(),
                        color = colors.textDarkColor, fontSize = 18.sp, fontWeight = FontWeight.SemiBold
                    )
                    p?.uhId?.takeIf { it.isNotBlank() }?.let {
                        Text("UHID: $it", color = colors.textGreyColor, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    InfoCard {
                        ProfileInfoRow("First Name", p?.firstName.orEmpty())
                        ProfileInfoRow("Last Name", p?.lastName.orEmpty())
                        ProfileInfoRow("Sex", p?.genderName?.ifBlank { null } ?: p?.genderId?.takeIf { it > 0 }?.let(::genderNameFor).orEmpty())
                        ProfileInfoRow("Date of Birth", displayDob(p?.dob.orEmpty()))
                        ProfileInfoRow("Age", p?.age.orEmpty())
                    }
                    InfoCard {
                        ProfileInfoRow("Blood Group", p?.bloodGroupName?.ifBlank { null } ?: p?.bloodGroupId?.let(::bloodGroupNameFor).orEmpty())
                        ProfileInfoRow("Height", displayMeasure(p?.height))
                        ProfileInfoRow("Weight", displayMeasure(p?.weight))
                    }
                    InfoCard {
                        ProfileInfoRow("Mobile No.", p?.mobileNo.orEmpty())
                        ProfileInfoRow("Email", p?.emailAddress.orEmpty())
                    }
                    InfoCard {
                        ProfileInfoRow("Street Address", p?.address.orEmpty())
                        ProfileInfoRow("Zip Code", p?.zip.orEmpty())
                        ProfileInfoRow("City", p?.cityName.orEmpty())
                        ProfileInfoRow("State", p?.stateName.orEmpty())
                        ProfileInfoRow("Country", p?.countryName.orEmpty())
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Bottom button → Edit Profile, which saves through the Update Profile API
            HapticButton(
                onClick = { navController.navigate(com.critetiontech.ctvitalio.Routes.EDITPROFILE) },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
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

/** "2000-03-01T00:00:00" → "01 Mar 2000"; anything unparseable is shown as is. */
private fun displayDob(raw: String): String {
    if (raw.isBlank()) return ""
    return runCatching {
        java.time.LocalDate.parse(raw.take(10)).format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy", java.util.Locale.getDefault()))
    }.getOrDefault(raw)
}

/** "176.00/cm" → "176 cm", "76.50/kg" → "76.5 kg". */
private fun displayMeasure(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val (value, unit) = raw.split("/", limit = 2).let { it[0].trim() to it.getOrNull(1)?.trim().orEmpty() }
    val number = value.toDoubleOrNull() ?: return raw
    if (number <= 0) return ""
    val shown = if (number % 1.0 == 0.0) number.toLong().toString() else number.toString()
    return "$shown $unit".trim()
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