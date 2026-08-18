package com.critetiontech.vitalio_cis.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.model.AppointmentHistoryItem
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentItem
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.AppointmentViewModel
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(12.dp)
        ) {
            AppointmentToggle(selected = selectedTab, onSelect = { selectedTab = it })
            Spacer(modifier = Modifier.height(12.dp))

            if (loading) {
                LazyColumn {
                    items(3) {
                        AppointmentShimmerCard()
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            } else if (selectedTab == 0) {
                if (upcoming.isEmpty()) {
                    EmptyState("No upcoming appointments")
                } else {
                    LazyColumn {
                        items(upcoming) { item ->
                            UpcomingAppointmentCard(item)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            } else {
                if (history.isEmpty()) {
                    EmptyState("No appointment history")
                } else {
                    LazyColumn {
                        items(history) { item ->
                            HistoryAppointmentCard(item)
                            Spacer(modifier = Modifier.height(12.dp))
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
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Blue accent header strip
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF2E5BFF), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Doctor info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                        modifier = Modifier.size(68.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.doctorName.trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    if (item.patientType.isNotEmpty()) {
                        Text(
                            text = item.patientType,
                            fontSize = 13.sp,
                            color = Color(0xFF2E5BFF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    PaymentBadge(item.paymentStatus)
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
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF2E5BFF), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = item.visitDate, fontSize = 13.sp, color = Color(0xFF1A1A2E), fontWeight = FontWeight.Medium)
                        if (item.slotTime.isNotEmpty() && item.slotTime != "00:00:00.0000000") {
                            Text(text = "Slot: ${formatSlotTime(item.slotTime)}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                if (item.address.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF2E5BFF), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = item.address, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (item.age.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = Color(0xFF2E5BFF), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = "Age: ${item.age}", fontSize = 13.sp, color = Color.Gray)
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
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Purple/teal accent strip for history
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(Color(0xFF6C63FF), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            )

            // Doctor info section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(68.dp)
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
                        modifier = Modifier.size(68.dp).clip(CircleShape)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.doctorName.trim(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color(0xFF1A1A2E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    if (item.designation.isNotEmpty()) {
                        Text(
                            text = item.designation,
                            fontSize = 13.sp,
                            color = Color(0xFF6C63FF),
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!item.qualification.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(text = item.qualification, fontSize = 12.sp, color = Color.Gray)
                    }
                }
                // Visit number chip
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF0EDFF), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                ) {
                    Text(text = item.visitNo.takeLast(6), fontSize = 11.sp, color = Color(0xFF6C63FF), fontWeight = FontWeight.Bold)
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
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0EDFF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(text = formatIsoDate(item.visitDate), fontSize = 13.sp, color = Color(0xFF1A1A2E), fontWeight = FontWeight.Medium)
                        if (item.slotTime.isNotEmpty() && item.slotTime != "00:00:00.0000000") {
                            Text(text = "Slot: ${formatSlotTime(item.slotTime)}", fontSize = 12.sp, color = Color.Gray)
                        }
                    }
                }
                if (item.address.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF0EDFF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFF6C63FF), modifier = Modifier.size(16.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(text = item.address, fontSize = 13.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentBadge(status: String) {
    if (status.isEmpty()) return
    val bgColor = if (status.equals("Pending", ignoreCase = true)) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
    val textColor = if (status.equals("Pending", ignoreCase = true)) Color(0xFFF57C00) else Color(0xFF388E3C)
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = status, color = textColor, fontWeight = FontWeight.Medium, style = AppTextStyles.style12GCN())
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, style = AppTextStyles.style12GCN())
    }
}

private fun formatSlotTime(raw: String): String {
    return try {
        val parts = raw.split(":")
        if (parts.size >= 2) "${parts[0]}:${parts[1]}" else raw
    } catch (e: Exception) { raw }
}

private fun formatIsoDate(raw: String): String {
    return try {
        raw.substringBefore("T").split("-").let { parts ->
            if (parts.size == 3) "${parts[2]}-${parts[1]}-${parts[0]}" else raw
        }
    } catch (e: Exception) { raw }
}

@Composable
private fun AppointmentShimmerCard() {
    val colors = LocalMyColorScheme.current
    val brush = shimmerBrush()
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(brush, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(68.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Box(modifier = Modifier.width(150.dp).height(17.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(modifier = Modifier.width(100.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(70.dp).height(22.dp).clip(RoundedCornerShape(6.dp)).background(brush))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFEEEEEE)))
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(2) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(brush))
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.width(160.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(brush))
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