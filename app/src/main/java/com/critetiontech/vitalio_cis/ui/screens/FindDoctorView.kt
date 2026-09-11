package com.critetiontech.vitalio_cis.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.components.MyTextField
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.FindDoctorViewModel

// -------------------- Shimmer --------------------
@Composable
private fun doctorShimmerBrush(): Brush {
    val shimmerColors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0))
    val transition = rememberInfiniteTransition(label = "docShimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "docShimmerX"
    )
    return Brush.linearGradient(shimmerColors, start = Offset(translateX, 0f), end = Offset(translateX + 600f, 0f))
}

@Composable
private fun DoctorCardShimmer() {
    val colors = LocalMyColorScheme.current
    val brush = doctorShimmerBrush()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardContainerColor)
            .padding(3.dp)
    ) {
        Column {
            // Image placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(brush)
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Name placeholder
            Box(modifier = Modifier.fillMaxWidth(0.75f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(7.dp))
            // Department placeholder
            Box(modifier = Modifier.fillMaxWidth(0.55f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(5.dp))
            // Qualification placeholder
            Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(5.dp))
            // Days placeholder
            Box(modifier = Modifier.fillMaxWidth(0.5f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
private fun DoctorGridShimmer() {
    val placeholders = remember { List(4) { it } }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
        userScrollEnabled = false
    ) {
        gridItems(placeholders) { DoctorCardShimmer() }
    }
}

// -------------------- Doctor Card --------------------
@Composable
fun DoctorCard(doctor: Doctor, profile: DoctorDetails? = null) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val shortDays = if (doctor.scheduleDays.isNotEmpty()) {
        doctor.scheduleDays.split(",").joinToString(", ") { it.trim().take(3) }
    } else if (doctor.workingDays.isNotEmpty()) {
        doctor.workingDays.joinToString(", ") { it.dayName.take(3) }
    } else {
        "N/A"
    }

    val displayName = profile?.name?.ifEmpty { null }
        ?: doctor.doctorName
        ?: "Doctor ${doctor.assignedUserId}"
    val displayQualification = profile?.highestQualificationName?.ifEmpty { null }
        ?: doctor.qualification
        ?: ""
    val displayDepartment = profile?.departmentName?.ifEmpty { null }
        ?: doctor.departmentName
        ?: ""

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val idToPass = if (doctor.assignedUserId != 0) doctor.assignedUserId else doctor.doctorId
                navController.navigate(
                    Routes.DOCTORDETAILS + "/$idToPass/${Uri.encode(shortDays)}"
                )
            }
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardContainerColor)
            .padding(3.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(182.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.LightGray)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(text = displayName, style = AppTextStyles.style14BCB())

            if (displayDepartment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = displayDepartment, style = AppTextStyles.style12GCN())
            }

            if (displayQualification.isNotEmpty()) {
                Text(text = displayQualification, style = AppTextStyles.style12GCN())
            }

            Text(
                text = shortDays,
                style = AppTextStyles.style12GCN().copy(fontSize = 10.sp)
            )
        }
    }
}

// -------------------- Main Screen --------------------
@Composable
fun FindDoctorsScreen(
    doctors: List<Doctor>,
    doctorProfiles: Map<Int, DoctorDetails> = emptyMap(),
    clinicName: String = "LifeSpring Medical",
    clinicAddress: String = "Main Bazaar Road, Aluva, Kochi - 683101",
    selectedDate: String = "16.01.2025",
    onBack: () -> Unit = {},
    onClinicSwitch: () -> Unit = {},
    viewModel: FindDoctorViewModel = viewModel()
) {



    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val isLoading by viewModel.loading.collectAsState()

    val filteredDoctors = doctors.filter {
        val profile = doctorProfiles[it.assignedUserId]
        val name = profile?.name?.ifEmpty { null } ?: it.doctorName ?: ""
        val dept = profile?.departmentName?.ifEmpty { null } ?: it.departmentName ?: ""
        name.contains(viewModel.searchText, ignoreCase = true) ||
                dept.contains(viewModel.searchText, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(colors.dashboardBackgroundColor)
    ) {
        // -------------------- Clinic Info --------------------
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .clickable { navController.navigate(Routes.SELECTCLINIC) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(40.dp).background(Color.LightGray, CircleShape))
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

        // -------------------- Category Tabs --------------------
        val departments by viewModel.departmentList.collectAsState()
        val selectedDept = viewModel.selectedDepartmentId

        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                Column(
                    modifier = Modifier.clickable { viewModel.onDepartmentSelected(null) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All",
                        fontSize = 16.sp,
                        fontWeight = if (selectedDept == null) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedDept == null) Color.Black else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (selectedDept == null) Color(0xFF00305E) else Color.Transparent)
                    ) {
                        Text(text = "All", color = Color.Transparent, fontSize = 16.sp)
                    }
                }
            }
            items(departments) { dept ->
                Column(
                    modifier = Modifier.clickable { viewModel.onDepartmentSelected(dept.id) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = dept.departmentName,
                        fontSize = 16.sp,
                        fontWeight = if (selectedDept == dept.id) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selectedDept == dept.id) Color.Black else Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .height(3.dp)
                            .fillMaxWidth()
                            .background(if (selectedDept == dept.id) Color(0xFF00305E) else Color.Transparent)
                    ) {
                        Text(text = dept.departmentName, color = Color.Transparent, fontSize = 16.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // -------------------- Doctor Grid --------------------
        when {
            isLoading && filteredDoctors.isEmpty() -> DoctorGridShimmer()
            !isLoading && filteredDoctors.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No doctors found", color = Color.Gray)
                }
            }
            else -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    gridItems(filteredDoctors) { doctor ->
                        DoctorCard(doctor, doctorProfiles[doctor.assignedUserId])
                    }
                }
            }
        }
    }
}

// -------------------- Sample Preview --------------------
@Composable
fun PreviewFindDoctors(viewModel: FindDoctorViewModel = viewModel()) {

    val doctors by viewModel.doctorList.collectAsState()
    val doctorProfiles by viewModel.doctorProfiles.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchDepartments()
        viewModel.fetchDoctorsAvalability(0)
    }

    CommonAppBar(title = "Find Doctors") {
        FindDoctorsScreen(doctors = doctors, doctorProfiles = doctorProfiles, viewModel = viewModel)
    }
}