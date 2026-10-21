package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.*

// ──────────────────────────────────────────────────────
//  Add Activity Screen (bottom-nav tab)
// ──────────────────────────────────────────────────────
@Composable
fun AddActivityScreen(viewModel: ActivityViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val allActivities by viewModel.allActivities.collectAsState()
    val recentActivityList by viewModel.recentActivityList.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val filteredActivities = if (viewModel.searchText.isEmpty()) allActivities
    else allActivities.filter { it.activityName.contains(viewModel.searchText, ignoreCase = true) }

    LaunchedEffect(Unit) {
        viewModel.getAllActivities()
        viewModel.fetchPatientActivity()
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showBottomSheet = false
            viewModel.resetSaveSuccess()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .padding(horizontal = 16.dp)
    ) {
        // ── Top bar with "Activity History" button ──
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Add Activity", style = AppTextStyles.style18BCB())
            Spacer(Modifier.weight(1f))
            Text(
                text = "Activity History",
                color = Color(0xFF2E5BFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable {
                    navController.navigate(Routes.ACTIVITYHISTORY)
                }
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Search bar ──
        OutlinedTextField(
            value = viewModel.searchText,
            onValueChange = { viewModel.onSearchChange(it) },
            placeholder = { Text("Search activity...", color = Color.Gray.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (viewModel.searchText.isNotEmpty()) {
                    HapticIconButton(onClick = { viewModel.onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.15f),
                focusedBorderColor = Color(0xFF2E5BFF),
                unfocusedContainerColor = colors.dashboardContainerColor,
                focusedContainerColor = colors.dashboardContainerColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Content ──
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Recent Activities ──
            if (recentActivityList.isNotEmpty()) {
                item {
                    Text("Recent", style = AppTextStyles.style16BCB())
                    Spacer(Modifier.height(10.dp))
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentActivityList.forEach { item ->
                            RecentActivityChip(
                                title = item.activityName,
                                isSelected = viewModel.selectedActivity == item.activityName,
                                onSelect = {
                                    viewModel.selectActivity(item.activityName, item.activityId.toString())
                                    showBottomSheet = true
                                },
                                onDelete = { viewModel.removeRecentActivity(item) }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── All Activities ──
            item {
                Text("All Activities", style = AppTextStyles.style16BCB())
                Spacer(Modifier.height(10.dp))
            }

            item {
                if (isLoading && filteredActivities.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredActivities.forEach { activity ->
                            ActivityChipItem(
                                title = activity.activityName,
                                isSelected = viewModel.selectedActivity == activity.activityName,
                                onClick = {
                                    viewModel.selectActivity(activity.activityName, activity.id.toString())
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Bottom Sheet ──
    if (showBottomSheet) {
        ActivityBottomSheet(
            viewModel = viewModel,
            onDismiss = { showBottomSheet = false },
            onSave = { viewModel.insertActivity() }
        )
    }
}

// ──────────────────────────────────────────────────────
//  Activity Bottom Sheet
// ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityBottomSheet(
    viewModel: ActivityViewModel,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val isSaving by viewModel.isSaving.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val timeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }

    com.critetiontech.ctvitalio.ui.components.AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // ── Header with icon + duration badge ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFFF9800).copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.activity),
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = viewModel.selectedActivity.ifEmpty { "Activity" },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDarkColor
                )
                Spacer(Modifier.weight(1f))

                // Duration badge
                Row(
                    modifier = Modifier
                        .background(Color(0xFF2E5BFF).copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.reminders),
                        contentDescription = null,
                        tint = Color(0xFF2E5BFF),
                        modifier = Modifier.size(12.dp)
                    )
                    Text(viewModel.formattedDuration, color = Color(0xFF2E5BFF), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Time pickers row ──
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                // Start time
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select Start Time", fontSize = 13.sp, color = Color(0xFF737B8C))
                    Spacer(Modifier.height(8.dp))
                    TimePickerCard(
                        text = timeFormat.format(viewModel.startTime),
                        onClick = {
                            val cal = Calendar.getInstance().apply { time = viewModel.startTime }
                            TimePickerDialog(context, { _, h, m ->
                                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                                viewModel.updateStartTime(c.time)
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                        }
                    )
                }
                // End time
                Column(modifier = Modifier.weight(1f)) {
                    Text("Select End Time", fontSize = 13.sp, color = Color(0xFF737B8C))
                    Spacer(Modifier.height(8.dp))
                    TimePickerCard(
                        text = timeFormat.format(viewModel.endTime),
                        onClick = {
                            val cal = Calendar.getInstance().apply { time = viewModel.endTime }
                            TimePickerDialog(context, { _, h, m ->
                                val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                                viewModel.updateEndTime(c.time)
                            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), false).show()
                        }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // ── Save button ──
            HapticButton(
                onClick = onSave,
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5BFF)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Save", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimePickerCard(text: String, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.dashboardBackgroundColor)
            .border(1.dp, colors.borderGreyLightColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colors.textDarkColor)
        Spacer(Modifier.weight(1f))
        Icon(
            painter = painterResource(R.drawable.reminders),
            contentDescription = null,
            tint = Color(0xFF2E5BFF).copy(alpha = 0.8f),
            modifier = Modifier.size(15.dp)
        )
    }
}

// ──────────────────────────────────────────────────────
//  Chip Components
// ──────────────────────────────────────────────────────
@Composable
fun RecentActivityChip(
    title: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Row(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF2E5BFF).copy(alpha = 0.12f)
                else colors.dashboardContainerColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = if (isSelected) Color(0xFF2E5BFF) else colors.textDarkColor
        )
        Icon(
            Icons.Default.Close,
            contentDescription = "Remove",
            tint = Color(0xFFF25959),
            modifier = Modifier.size(16.dp).clickable { onDelete() }
        )
    }
}

@Composable
fun ActivityChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF2E5BFF).copy(alpha = 0.12f)
                else colors.dashboardContainerColor,
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            color = if (isSelected) Color(0xFF2E5BFF) else colors.textDarkColor
        )
    }
}
