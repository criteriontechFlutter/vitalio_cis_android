package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun ActivityHistoryScreen(viewModel: ActivityViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val recentList by viewModel.recentActivityList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchPatientActivity()
    }

    CommonAppBar(title = "History") {
        if (isLoading && recentList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (recentList.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                Text("No activity history yet", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(recentList.size, key = { index -> "${recentList[index].serverId}_$index" }) { index ->
                    ActivityHistoryCard(recentList[index])
                }
            }
        }
    }
}

@Composable
private fun ActivityHistoryCard(item: PatientActivityItem) {
    val colors = LocalMyColorScheme.current
    val formattedTime = remember(item) { formatTimeRange(item.resolvedStartTime, item.totalTimeInMinutes) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Top: time + edit icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.reminders),
                    contentDescription = null,
                    tint = Color(0xFF475569),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formattedTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDarkColor
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    painter = painterResource(R.drawable.fluid_edit),
                    contentDescription = "Edit",
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }

            // Divider
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = Color(0xFFF1F5F9)
            )

            // Bottom: activity name
            Text(
                text = item.activityName.ifEmpty { "Activity" },
                fontSize = 14.sp,
                color = Color(0xFF64748B),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

private fun formatTimeRange(startTime: String, duration: String?): String {
    val durationText = duration?.let { "$it min" } ?: "30 min"
    val inputFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val outputFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())
    return try {
        val date = inputFormatter.parse(startTime)
        val timeStr = outputFormatter.format(date!!)
        "$timeStr - $durationText"
    } catch (e: Exception) {
        if (startTime.isNotEmpty()) "$startTime - $durationText"
        else "8:00 AM - $durationText"
    }
}
