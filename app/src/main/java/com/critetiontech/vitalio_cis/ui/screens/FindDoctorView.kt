package com.critetiontech.ctvitalio.ui.screens

import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EventNote
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.Doctor
import com.critetiontech.ctvitalio.model.DoctorDetails
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.ContentStateCrossfade
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.contentStateOf
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.FindDoctorViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import kotlin.math.absoluteValue

/** Orange tag on a doctor's photo, as in the design. */
private val DepartmentBadge = Color(0xFFF97316)

/** Light outline used by the search bar and day chips. */
internal val DoctorOutline = Color(0xFFE5E7EB)

// -------------------- Shared doctor pieces --------------------

/** Server sends "…/no image" when a doctor has no photo, so treat that like no URL at all. */
internal fun doctorPhotoUrl(raw: String?): String? =
    raw?.replace('\\', '/')?.trim()?.takeIf { it.isNotEmpty() && !it.endsWith("no image", ignoreCase = true) }

private val AvatarPalette = listOf(
    Color(0xFFDBEAFE) to Color(0xFF1D4ED8),
    Color(0xFFFCE7F3) to Color(0xFFBE185D),
    Color(0xFFDCFCE7) to Color(0xFF15803D),
    Color(0xFFFEF3C7) to Color(0xFFB45309),
    Color(0xFFEDE9FE) to Color(0xFF6D28D9),
    Color(0xFFCFFAFE) to Color(0xFF0E7490),
)

/**
 * The doctor's photo, or their initials on a soft colour (stable per name) while it loads or
 * when there is none.
 */
@Composable
fun DoctorPhoto(name: String, url: String?, modifier: Modifier = Modifier, initialsSize: TextUnit = 40.sp) {
    val (bg, fg) = AvatarPalette[name.hashCode().absoluteValue % AvatarPalette.size]
    val initials = remember(name) {
        name.replace(Regex("^(dr\\.?)\\s*", RegexOption.IGNORE_CASE), "")
            .split(' ').filter { it.isNotBlank() }.take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "Dr" }
    }
    Box(modifier.background(Brush.linearGradient(listOf(bg, bg.copy(alpha = 0.55f)))), contentAlignment = Alignment.Center) {
        Text(initials, color = fg, fontSize = initialsSize, fontWeight = FontWeight.Bold)
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** Outlined "📅 Tue, Fri, Sun" chip. */
@Composable
fun DoctorDaysChip(days: String, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(6.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .border(1.dp, DoctorOutline, shape)
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Icon(Icons.Rounded.CalendarMonth, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(5.dp))
        Text(days, color = colors.textGreyColor, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** "Mon, Tue, Fri" from the doctor's schedule; "Daily" when they work all week. */
internal fun Doctor.shortDays(): String {
    val days = when {
        scheduleDays.isNotEmpty() -> scheduleDays.split(",").map { it.trim().take(3) }.filter { it.isNotEmpty() }
        workingDays.isNotEmpty() -> workingDays.map { it.dayName.take(3) }
        else -> emptyList()
    }
    return when {
        days.isEmpty() -> "N/A"
        days.distinct().size == 7 -> "Daily"
        else -> days.joinToString(", ")
    }
}

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
private fun ShimmerLine(width: Float, height: Dp, brush: Brush) {
    Box(Modifier.fillMaxWidth(width).height(height).clip(RoundedCornerShape(4.dp)).background(brush))
}

@Composable
private fun DoctorCardShimmer() {
    val brush = doctorShimmerBrush()
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(10.dp)).background(brush))
        Spacer(Modifier.height(10.dp))
        ShimmerLine(0.75f, 15.dp, brush)
        Spacer(Modifier.height(7.dp))
        ShimmerLine(0.45f, 12.dp, brush)
        Spacer(Modifier.height(8.dp))
        ShimmerLine(0.55f, 20.dp, brush)
    }
}

// -------------------- Doctor Card --------------------
@Composable
fun DoctorCard(doctor: Doctor, profile: DoctorDetails? = null) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val shortDays = doctor.shortDays()

    val displayName = profile?.name?.trim()?.ifEmpty { null }
        ?: doctor.doctorName?.trim()?.ifEmpty { null }
        ?: "Doctor ${doctor.assignedUserId}"
    val displayQualification = profile?.highestQualificationName?.ifEmpty { null }
        ?: doctor.qualification
        ?: ""
    val displayDepartment = profile?.departmentName?.ifEmpty { null }
        ?: doctor.departmentName
        ?: ""
    val photo = doctorPhotoUrl(profile?.profilePath) ?: doctorPhotoUrl(doctor.profileUrl)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bounceClick {
                val idToPass = if (doctor.assignedUserId != 0) doctor.assignedUserId else doctor.doctorId
                navController.navigate(
                    Routes.DOCTORDETAILS + "/$idToPass/${Uri.encode(shortDays)}"
                )
            }
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
        ) {
            DoctorPhoto(displayName, photo, Modifier.fillMaxSize())
            if (displayDepartment.isNotEmpty()) {
                Text(
                    displayDepartment,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .widthIn(max = 130.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(DepartmentBadge)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            displayName,
            color = colors.textDarkColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (displayQualification.isNotEmpty()) {
            Text(
                displayQualification,
                color = colors.textGreyColor,
                fontSize = 13.sp,
                fontStyle = FontStyle.Italic,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(6.dp))
        DoctorDaysChip(shortDays)
    }
}

// -------------------- Search --------------------
@Composable
private fun DoctorSearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(8.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.dashboardContainerColor)
            .border(1.dp, DoctorOutline, shape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = colors.textGreyColor.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text(
                    "Search doctor by name or role",
                    color = colors.textGreyColor.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    fontStyle = FontStyle.Italic,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = TextStyle(color = colors.textDarkColor, fontSize = 14.sp),
                cursorBrush = SolidColor(colors.primaryBlueColor),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (query.isNotEmpty()) {
            HapticIconButton(onClick = { onQueryChange("") }, modifier = Modifier.size(22.dp)) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search", tint = colors.textGreyColor, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Grey "Appointments 📅" pill in the top bar. */
@Composable
private fun AppointmentsPill(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.dashboardBackgroundColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    ) {
        Text("Appointments", color = colors.textDarkColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.EventNote, contentDescription = null, tint = colors.textDarkColor, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun DepartmentChip(name: String, selected: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(50)
    Text(
        name,
        color = if (selected) Color.White else colors.textGreyColor,
        fontSize = 13.sp,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) colors.primaryBlueColor else colors.dashboardBackgroundColor)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

// -------------------- Main Screen --------------------
@Composable
fun FindDoctorsScreen(
    doctors: List<Doctor>,
    doctorProfiles: Map<Int, DoctorDetails> = emptyMap(),
    viewModel: FindDoctorViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val isLoading by viewModel.loading.collectAsState()
    val departments by viewModel.departmentList.collectAsState()
    val selectedDept = viewModel.selectedDepartmentId

    val filteredDoctors = doctors.filter {
        val profile = doctorProfiles[it.assignedUserId]
        val name = profile?.name?.ifEmpty { null } ?: it.doctorName ?: ""
        val dept = profile?.departmentName?.ifEmpty { null } ?: it.departmentName ?: ""
        val qualification = profile?.highestQualificationName?.ifEmpty { null } ?: it.qualification ?: ""
        listOf(name, dept, qualification).any { field -> field.contains(viewModel.searchText.trim(), ignoreCase = true) }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        DoctorSearchField(
            query = viewModel.searchText,
            onQueryChange = { viewModel.onSearchChange(it) },
            modifier = Modifier.padding(top = 8.dp),
        )

        // Department filter — only shown when the clinic has departments to pick from.
        if (departments.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                item {
                    DepartmentChip("All", selectedDept == null) {
                        if (selectedDept != null) haptic(HapticEvent.Selection)
                        viewModel.onDepartmentSelected(null)
                    }
                }
                items(departments) { dept ->
                    DepartmentChip(dept.departmentName, selectedDept == dept.id) {
                        if (selectedDept != dept.id) haptic(HapticEvent.Selection)
                        viewModel.onDepartmentSelected(dept.id)
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        ContentStateCrossfade(
            state = contentStateOf(loading = isLoading, empty = filteredDoctors.isEmpty()),
            loading = {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(4) { DoctorCardShimmer() }
                }
            },
            empty = {
                Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                    Text(
                        if (viewModel.searchText.isNotBlank()) "No doctors match \"${viewModel.searchText.trim()}\"" else "No doctors found",
                        color = colors.textGreyColor,
                    )
                }
            },
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                gridItemsIndexed(filteredDoctors) { i, doctor ->
                    AnimatedEnter(index = i) {
                        DoctorCard(doctor, doctorProfiles[doctor.assignedUserId])
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) { Spacer(Modifier.navigationBarsPadding()) }
            }
        }
    }
}

// -------------------- Screen entry --------------------
@Composable
fun PreviewFindDoctors(viewModel: FindDoctorViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val doctors by viewModel.doctorList.collectAsState()
    val doctorProfiles by viewModel.doctorProfiles.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchDepartments()
        viewModel.fetchDoctorsAvalability(viewModel.selectedDepartmentId ?: 0)
    }

    Column(Modifier.fillMaxSize().background(colors.dashboardContainerColor)) {
        RoundBackTopBar(
            title = "Find Doctors",
            // Return to My Appointments if we came from there, instead of stacking another copy.
            actions = {
                AppointmentsPill {
                    navController.navigate(Routes.APPOINTMENTS) {
                        popUpTo(Routes.APPOINTMENTS)
                        launchSingleTop = true
                    }
                }
            },
        )
        FindDoctorsScreen(doctors = doctors, doctorProfiles = doctorProfiles, viewModel = viewModel)
    }
}
