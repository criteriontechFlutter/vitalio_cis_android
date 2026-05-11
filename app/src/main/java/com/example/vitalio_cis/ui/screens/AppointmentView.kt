package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButtonDefaults.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.vitalio_cis.R
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.ui.theme.getColorScheme
import com.example.vitalio_cis.utils.CommonTextField

@Composable
fun FindDoctorsTopSection() {


    val colors = LocalMyColorScheme.current
        CommonAppBar(
            title = "Appointments",
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .padding(12.dp)
            ) {
                AppointmentToggle()
                Spacer(modifier = Modifier.height(12.dp))
                AppointmentCard(
                    title = "Prescription for Fever",
                    doctorName = "Dr. Abdul Karim",
                    qualification = "MBBS, MS, MCh (Neurologist)",
                    time = "12:30 PM, Tuesday - December 16",
                    location = "LifeSpring Medical, 1st Floor, Aluva Tower, Bank Junction"
                )

                Spacer(modifier = Modifier.height(12.dp))

                AppointmentCard(
                    title = "Prescription for Leg Pain",
                    doctorName = "Dr. Farooque Ali",
                    qualification = "MBBS, MS (Ortho Surgeon)",
                    time = "05:00 PM, Saturday - October 11",
                    location = "LifeSpring Medical, 1st Floor, Aluva Tower, Bank Junction"
                )
            }
        }
    }

    @Composable
    fun AppointmentCard(
        title: String,
        doctorName: String,
        qualification: String,
        time: String,
        location: String
    ) {
        val colors = LocalMyColorScheme.current
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.dashboardContainerColor
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                // Header


                // Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img), // replace with your image
                        contentDescription = null,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = doctorName,
                            style = AppTextStyles.style16BCB()
                        )

                        Text(
                            text = qualification,
                            style = AppTextStyles.style12GCN()
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = Color(0xFF1E5BB8)
                    )
                }

                Divider()

                // Footer
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = time,

                            style = AppTextStyles.style12GCN())
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color.Gray)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = location,
                            style = AppTextStyles.style12GCN())
                    }
                }
            }
        }
    }

@Composable
fun AppointmentToggle() {

    var selected by remember { mutableStateOf(0) }

    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor,
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {

        Row {

            // In Clinic
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected == 0) Color(0xFF2E5BFF)
                        else Color.Transparent
                    )
                    .clickable { selected = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upcoming",
                    color = if (selected == 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Virtual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected == 1) Color(0xFF2E5BFF)
                        else Color.Transparent
                    )
                    .clickable { selected = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "History",
                    color = if (selected == 1) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}