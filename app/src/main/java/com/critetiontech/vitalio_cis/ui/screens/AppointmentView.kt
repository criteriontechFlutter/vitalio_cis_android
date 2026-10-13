package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.AppointmentHistoryItem
import com.critetiontech.ctvitalio.model.UpcomingAppointmentItem
import com.critetiontech.ctvitalio.ui.adaptive.LocalWindowSizeClass
import com.critetiontech.ctvitalio.ui.adaptive.WindowWidthSizeClass
import com.critetiontech.ctvitalio.ui.adaptive.adaptiveGridItems
import com.critetiontech.ctvitalio.ui.adaptive.rememberResponsiveDimens
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.AppointmentViewModel
import com.example.vitalio_cis.ui.screens.shimmerBrush

@Composable
fun FindDoctorsTopSection(vm: AppointmentViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val upcoming by vm.upcoming.collectAsState()
    val history by vm.history.collectAsState()
    val loading by vm.loading.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) { vm.fetchAll() }

    CommonAppBar(title = "Appointments") {
        val appointDimens = rememberResponsiveDimens()
        val gridCols = if (LocalWindowSizeClass.current != WindowWidthSizeClass.Compact) 2 else 1

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(appointDimens.screenPadding)
        ) {
            AppointmentToggle(selected = selectedTab, onSelect = { selectedTab = it })
            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(appointDimens.cardSpacing)
                ) {
                    items(3) {
                        AppointmentShimmerCard()
                    }
                }
            } else if (selectedTab == 0) {
                if (upcoming.isEmpty()) {
                    EmptyState("No upcoming appointments")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(appointDimens.cardSpacing)
                    ) {
                        adaptiveGridItems(
                            items = upcoming,
                            columns = gridCols,
                            spacing = appointDimens.cardSpacing
                        ) { item ->
                            UpcomingAppointmentCard(item)
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    EmptyState("No appointment history")
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(appointDimens.cardSpacing)
                    ) {
                        adaptiveGridItems(
                            items = history,
                            columns = gridCols,
                            spacing = appointDimens.cardSpacing
                        ) { item ->
                            HistoryAppointmentCard(item)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingAppointmentCard(item: UpcomingAppointmentItem) {
    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Blue accent header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Color(0xFF2E5BFF), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Status Badges & Visit No Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppointmentStatusBadge(isUpcoming = true)
                    if (item.paymentStatus.isNotBlank()) {
                        PaymentBadge(item.paymentStatus)
                    }
                    if (item.patientType.isNotBlank()) {
                        SmallChip(
                            text = item.patientType,
                            bgColor = Color(0xFFF0F4FF),
                            textColor = Color(0xFF2E5BFF)
                        )
                    }
                    if (item.tokenNumber > 0) {
                        SmallChip(
                            text = "Token #${item.tokenNumber}",
                            bgColor = Color(0xFFF3E8FF),
                            textColor = Color(0xFF7E22CE)
                        )
                    }
                }

                if (item.visitNo.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFEEF2FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.visitNo.takeLast(6),
                            fontSize = 11.sp,
                            color = Color(0xFF2E5BFF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Doctor info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEEF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.imageUrl.replace("\\", "/"),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.img),
                        error = painterResource(R.drawable.img),
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.doctorName.trim().ifEmpty { "Doctor" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.uhID.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "UHID: ${item.uhID}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    val patientInfo = listOfNotNull(
                        item.genderName.takeIf { it.isNotBlank() },
                        item.age.takeIf { it.isNotBlank() }?.let { "Age: $it" }
                    ).joinToString(" • ")
                    if (patientInfo.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = patientInfo,
                            fontSize = 12.sp,
                            color = Color(0xFF6B7280)
                        )
                    }
                }
            }

            HorizontalDivider(Modifier, thickness = 1.dp, color = Color(0xFFEEEEEE))

            // Date / time / location
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF2E5BFF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = formatVisitDate(item.visitDate),
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A2E),
                        fontWeight = FontWeight.Medium
                    )

                    val formattedSlot = formatSlotTime(item.slotTime)
                    if (formattedSlot.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF2E5BFF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedSlot,
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF2E5BFF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.address,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAppointmentCard(item: AppointmentHistoryItem) {
    val colors = LocalMyColorScheme.current
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Accent strip (red if cancelled, purple otherwise)
            val stripColor = if (item.isCancelAppointment) Color(0xFFEF4444) else Color(0xFF6C63FF)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(stripColor, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Status Badges & Visit No Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppointmentStatusBadge(
                        isUpcoming = false,
                        isCancelled = item.isCancelAppointment
                    )
                    if (item.prescriptionStatus.isNotBlank()) {
                        PrescriptionBadge(item.prescriptionStatus)
                    }
                    if (item.tokenNumber > 0) {
                        SmallChip(
                            text = "Token #${item.tokenNumber}",
                            bgColor = Color(0xFFF3E8FF),
                            textColor = Color(0xFF7E22CE)
                        )
                    }
                    if (item.shiftName.isNotBlank()) {
                        SmallChip(
                            text = item.shiftName,
                            bgColor = Color(0xFFF0EDFF),
                            textColor = Color(0xFF6C63FF)
                        )
                    }
                }

                if (item.visitNo.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFF0EDFF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = item.visitNo.takeLast(6),
                            fontSize = 11.sp,
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Doctor info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF0EDFF)),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = item.profileUrl.replace("\\", "/"),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        placeholder = painterResource(R.drawable.img),
                        error = painterResource(R.drawable.img),
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.doctorName.trim().ifEmpty { "Doctor" },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val dept = item.departmentName?.takeIf { it.isNotBlank() } ?: item.designation.takeIf { it.isNotBlank() }
                    if (!dept.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dept,
                            fontSize = 12.sp,
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!item.qualification.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = item.qualification, fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }

            HorizontalDivider(Modifier, thickness = 1.dp, color = Color(0xFFEEEEEE))

            // Date / time / location
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFFF0EDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = null,
                            tint = Color(0xFF6C63FF),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    val dateStr = if (item.formattedDate.isNotBlank()) {
                        "${item.formattedDate}${if (item.dayName.isNotBlank()) " (${item.dayName})" else ""}"
                    } else {
                        formatIsoDate(item.appointmentDate.ifEmpty { item.visitDate })
                    }
                    Text(
                        text = dateStr,
                        fontSize = 13.sp,
                        color = Color(0xFF1A1A2E),
                        fontWeight = FontWeight.Medium
                    )

                    val formattedSlot = formatSlotTime(item.slotTime)
                    if (formattedSlot.isNotBlank()) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF0EDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF6C63FF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = formattedSlot,
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (item.shiftTime.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF0EDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF6C63FF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Shift: ${item.shiftTime}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                if (item.address.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF0EDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF6C63FF),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = item.address,
                            fontSize = 12.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentBadge(status: String) {
    if (status.isBlank()) return
    val isPaid = status.equals("Paid", ignoreCase = true)
    val isPending = status.equals("Pending", ignoreCase = true)
    val bgColor = when {
        isPaid -> Color(0xFFE8F5E9)
        isPending -> Color(0xFFFFF3E0)
        else -> Color(0xFFFFEBEE)
    }
    val textColor = when {
        isPaid -> Color(0xFF2E7D32)
        isPending -> Color(0xFFE65100)
        else -> Color(0xFFC62828)
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = status, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun AppointmentStatusBadge(
    isUpcoming: Boolean = false,
    isCancelled: Boolean = false
) {
    val (bgColor, textColor, label) = when {
        isUpcoming -> Triple(Color(0xFFEEF2FF), Color(0xFF2E5BFF), "Upcoming")
        isCancelled -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "Cancelled")
        else -> Triple(Color(0xFFE0F2F1), Color(0xFF00695C), "Completed")
    }
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun PrescriptionBadge(status: String) {
    if (status.isBlank()) return
    val isPrescribed = status.equals("Prescribed", ignoreCase = true)
    val bgColor = if (isPrescribed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
    val textColor = if (isPrescribed) Color(0xFF2E7D32) else Color(0xFFE65100)
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = status, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun SmallChip(
    text: String,
    bgColor: Color = Color(0xFFF3F4F6),
    textColor: Color = Color(0xFF4B5563)
) {
    if (text.isBlank()) return
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = text, color = textColor, fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = AppTextStyles.style12GCN())
    }
}

private fun formatSlotTime(raw: String?): String {
    if (raw.isNullOrBlank() || raw.startsWith("00:00:00")) return ""
    return try {
        val parts = raw.split(":")
        if (parts.size >= 2) {
            val hour = parts[0].toIntOrNull() ?: return "${parts[0]}:${parts[1]}"
            val minute = parts[1]
            val amPm = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            String.format("%02d:%s %s", displayHour, minute, amPm)
        } else raw
    } catch (e: Exception) { raw }
}

private fun formatVisitDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val datePart = raw.substringBefore(" ").trim()
        val parts = datePart.split("-")
        if (parts.size == 3) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIdx = parts[1].toIntOrNull() ?: 0
            if (monthIdx in 1..12) {
                "${parts[0]} ${months[monthIdx]} ${parts[2]}"
            } else {
                datePart
            }
        } else {
            datePart
        }
    } catch (e: Exception) {
        raw.substringBefore(" ")
    }
}

private fun formatIsoDate(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return try {
        val datePart = raw.substringBefore("T").trim()
        val parts = datePart.split("-")
        if (parts.size == 3 && parts[0].length == 4) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthIdx = parts[1].toIntOrNull() ?: 0
            if (monthIdx in 1..12) {
                "${parts[2]} ${months[monthIdx]} ${parts[0]}"
            } else {
                "${parts[2]}-${parts[1]}-${parts[0]}"
            }
        } else {
            formatVisitDate(raw)
        }
    } catch (e: Exception) { raw }
}

@Composable
private fun AppointmentShimmerCard() {
    val colors = LocalMyColorScheme.current
    val brush = shimmerBrush()
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(brush, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Box(
                        modifier = Modifier
                            .width(150.dp)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .width(100.dp)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(brush)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .width(70.dp)
                            .height(20.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(brush)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFEEEEEE))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(brush)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .width(160.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(brush)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppointmentToggle(selected: Int = 0, onSelect: (Int) -> Unit = {}) {
    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.dashboardContainerColor, RoundedCornerShape(12.dp))
            .padding(4.dp)
    ) {
        Row {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected == 0) Color(0xFF2E5BFF) else Color.Transparent)
                    .clickable { onSelect(0) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Upcoming",
                    color = if (selected == 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected == 1) Color(0xFF2E5BFF) else Color.Transparent)
                    .clickable { onSelect(1) }
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