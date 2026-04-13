package com.example.vitalio_cis.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.Doctor
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.viewmodel.FindDoctorViewModel
import com.example.vitalio_cis.viewmodel.SymptomTrackerViewModel

// -------------------- Data Model --------------------

// -------------------- Doctor Card --------------------
@Composable
fun DoctorCard(doctor: Doctor) {




    val themeViewModel: ThemeViewModel = viewModel()
    val colors by themeViewModel.colorScheme.collectAsState()
    val navController = LocalNavController.current
    val shortDays = doctor.scheduleDays
        .split(",") // split multiple days
        .joinToString(", ") { day ->
            day.take(3) // safe, takes first 3 letters even if shorter
        }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable() {
                navController.navigate(Routes.DOCTORDETAILS + "/" + doctor.assignedUserId.toString() + "/" + shortDays.toString(),)
            }
            .clip(RoundedCornerShape(12.dp)) // apply rounded corners
            .background(colors.dashboardContainerColor)
            .padding(3.dp)    // background color


    ) {
        Column(
        ) {
            // Doctor Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .clip(RoundedCornerShape(12.dp)) // apply rounded corners
                    .background(Color.LightGray)      // background color
            ) {
                // If you have a URL, replace with AsyncImage from Coil
                // AsyncImage(model = doctor.imageUrl, contentDescription = null, modifier = Modifier.fillMaxSize())
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Role Tag (like "Neuro")

            // Name
            Text(
                text = doctor.doctorName,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = doctor.qualification ?: "Role",
                    color = Color.LightGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )

            // Qualification
            Text(
                text = doctor.qualification ?: "",
                fontSize = 12.sp,
                color = Color.Gray
            )

            // Schedule badge


            Text(
                text = shortDays,
                fontSize = 10.sp
            )

        }
    }
}

// -------------------- Main Screen --------------------
@Composable
fun FindDoctorsScreen(
    doctors: List<Doctor>,
    clinicName: String = "LifeSpring Medical",
    clinicAddress: String = "Main Bazaar Road, Aluva, Kochi - 683101",
    selectedDate: String = "16.01.2025",
    onBack: () -> Unit = {},
    onClinicSwitch: () -> Unit = {},
            viewModel: FindDoctorViewModel = viewModel()
) {



    val themeViewModel: ThemeViewModel = viewModel()
    val colors by themeViewModel.colorScheme.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    // Filter doctors by name or role
    val filteredDoctors = doctors.filter {
        it.doctorName.contains(searchQuery, ignoreCase = true) ||
                (it.departmentName?.contains(searchQuery, ignoreCase = true) ?: false)
    }

    Column(modifier = Modifier
        .fillMaxSize().padding(16.dp)
        .background(colors.dashboardBackgroundColor)) {

        // -------------------- Top Bar --------------------


        // -------------------- Clinic Info --------------------
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClinicSwitch() }
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.LightGray, CircleShape)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(clinicName, fontWeight = FontWeight.Medium)
                    Text(clinicAddress, fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.weight(1f))
                Text("Switch Clinic", color = Color(0xFF007AFF), fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // -------------------- Search Bar --------------------


        MyTextField(
            value = viewModel.searchText,
            onValueChange = { viewModel.onSearchChange(it) },
            placeholderText = "Search doctor by name or role"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // -------------------- Doctor Grid --------------------
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredDoctors) { doctor ->
                DoctorCard(doctor)
            }
        }
    }
}

// -------------------- Sample Preview --------------------
@Composable
fun PreviewFindDoctors(viewModel: FindDoctorViewModel = viewModel()) {

    val context = LocalContext.current

    val doctors by viewModel.doctorList.collectAsState()
    // 🔹 API Call
    LaunchedEffect(Unit) {
        viewModel.fetchDoctorsAvalability(context)
    }


    CommonAppBar(
        title = "Find Doctors",
    ) {
        FindDoctorsScreen(doctors = doctors)
    }
}