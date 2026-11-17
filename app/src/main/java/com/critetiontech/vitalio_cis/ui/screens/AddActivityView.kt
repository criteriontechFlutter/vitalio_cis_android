package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.app.TimePickerDialog
import android.text.format.DateFormat
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.ActivityViewModel
import java.util.*

// ──────────────────────────────────────────────────────
//  Add Activity Screen (bottom-nav tab)
// ──────────────────────────────────────────────────────
@Composable
fun AddActivityScreen(viewModel: ActivityViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val allActivities by viewModel.allActivities.collectAsState()
    val recentActivityList by viewModel.recentActivityList.collectAsState()
    val isLoadingMaster by viewModel.isLoadingMaster.collectAsState()
    val masterLoadFailed by viewModel.masterLoadFailed.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val message by viewModel.message.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    val filteredActivities = if (viewModel.searchText.isBlank()) allActivities
    else allActivities.filter { it.activityName.contains(viewModel.searchText.trim(), ignoreCase = true) }

    // Quick picks: each activity type logged today, once.
    val recentActivities = remember(recentActivityList, allActivities) {
        recentActivityList.filter { it.activityId > 0 }.distinctBy { it.activityId }
            .map { it.activityId to viewModel.displayName(it) }
            .filter { (_, name) -> name.isNotBlank() }
    }

    LaunchedEffect(Unit) {
        viewModel.getAllActivities()
        viewModel.fetchPatientActivity()
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showBottomSheet = false
            Haptics.success()
            viewModel.resetSaveSuccess()
        }
    }

    LaunchedEffect(message) {
        message?.let {
            if (it != R.string.track_activity_saved) Haptics.error()
            showToast(context, context.getString(it))
            viewModel.clearMessage()
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
            Text(stringResource(R.string.track_add_activity), style = AppTextStyles.style18BCB())
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.track_activity_history),
                color = Color(0xFF2E5BFF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clickable { navController.navigate(Routes.ACTIVITYHISTORY) }
                    .padding(vertical = 8.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        // ── Search bar ──
        OutlinedTextField(
            value = viewModel.searchText,
            onValueChange = { viewModel.onSearchChange(it) },
            placeholder = { Text(stringResource(R.string.track_search_activity), color = Color.Gray.copy(alpha = 0.6f)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray.copy(alpha = 0.6f)) },
            trailingIcon = {
                if (viewModel.searchText.isNotEmpty()) {
                    HapticIconButton(onClick = { viewModel.onSearchChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.a11y_clear), tint = Color.Gray.copy(alpha = 0.6f))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.15f),
                focusedBorderColor = Color(0xFF2E5BFF),
                unfocusedContainerColor = colors.dashboardContainerColor,
                focusedContainerColor = colors.dashboardContainerColor,
                focusedTextColor = colors.textDarkColor,
                unfocusedTextColor = colors.textDarkColor
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        // ── Content ──
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Recent Activities (logged today) ──
            if (recentActivities.isNotEmpty() && viewModel.searchText.isBlank()) {
                item {
                    Text(stringResource(R.string.track_recent), style = AppTextStyles.style16BCB())
                    Spacer(Modifier.height(10.dp))
                }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentActivities.forEach { (activityId, name) ->
                            ActivityChipItem(
                                title = name,
                                isSelected = viewModel.selectedActivityId == activityId,
                                onClick = {
                                    viewModel.openActivity(name, activityId)
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }

            // ── All Activities ──
            item {
                Text(stringResource(R.string.track_all_activities), style = AppTextStyles.style16BCB())
                Spacer(Modifier.height(10.dp))
            }

            item {
                when {
                    isLoadingMaster && allActivities.isEmpty() -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }

                    masterLoadFailed -> Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(stringResource(R.string.track_activity_load_failed), color = colors.textGreyColor, fontSize = 14.sp)
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { viewModel.getAllActivities() }) {
                            Text(stringResource(R.string.track_activity_retry), color = Color(0xFF2E5BFF), fontWeight = FontWeight.SemiBold)
                        }
                    }

                    filteredActivities.isEmpty() -> Text(
                        stringResource(R.string.track_activity_no_match),
                        color = colors.textGreyColor,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )

                    else -> FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        filteredActivities.forEach { activity ->
                            ActivityChipItem(
                                title = activity.activityName,
                                isSelected = viewModel.selectedActivityId == activity.id,
                                onClick = {
                                    viewModel.openActivity(activity.activityName, activity.id)
                                    showBottomSheet = true
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // ── Bottom Sheet ──
    if (showBottomSheet) {
        ActivityBottomSheet(
            viewModel = viewModel,
            onDismiss = {
                showBottomSheet = false
                viewModel.clearSelection()
            },
            onSave = { viewModel.insertActivity() }
        )
    }
}

@Composable
private fun durationLabel(minutes: Long): String {
    val m = minutes.coerceAtLeast(0)
    val hrs = (m / 60).toInt()
    val mins = (m % 60).toInt()
    return when {
        hrs == 0 -> stringResource(R.string.track_duration_mins, mins)
        mins == 0 -> stringResource(R.string.track_duration_hrs, hrs)
        else -> stringResource(R.string.track_duration_hrs_mins, hrs, mins)
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
    val is24h = DateFormat.is24HourFormat(context)
    val timeFormat = remember(is24h) { java.text.SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.getDefault()) }
    val valid = viewModel.isTimeRangeValid

    fun pickTime(initial: Date, onPicked: (Int, Int) -> Unit) {
        val cal = Calendar.getInstance().apply { time = initial }
        TimePickerDialog(context, { _, h, m -> onPicked(h, m) },
            cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), is24h).show()
    }

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
                    text = viewModel.selectedActivity.ifEmpty { stringResource(R.string.track_activity) },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textDarkColor,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))

                // Duration badge
                val badgeColor = if (valid) Color(0xFF2E5BFF) else Color(0xFFF25959)
                Row(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.reminders),
                        contentDescription = null,
                        tint = badgeColor,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(durationLabel(viewModel.durationMinutes), color = badgeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Time pickers row ──
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.track_select_start_time), fontSize = 13.sp, color = colors.textGreyColor)
                    Spacer(Modifier.height(8.dp))
                    TimePickerCard(text = timeFormat.format(viewModel.startTime)) {
                        pickTime(viewModel.startTime) { h, m -> viewModel.updateStartTime(h, m) }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.track_select_end_time), fontSize = 13.sp, color = colors.textGreyColor)
                    Spacer(Modifier.height(8.dp))
                    TimePickerCard(text = timeFormat.format(viewModel.endTime)) {
                        pickTime(viewModel.endTime) { h, m -> viewModel.updateEndTime(h, m) }
                    }
                }
            }

            if (!valid) {
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.track_activity_end_after_start), color = Color(0xFFF25959), fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))

            // ── Save button ──
            HapticButton(
                onClick = onSave,
                enabled = !isSaving && valid && viewModel.selectedActivityId != null,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E5BFF)),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text(stringResource(R.string.track_save), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
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
fun ActivityChipItem(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) Color(0xFF2E5BFF).copy(alpha = 0.12f)
                else colors.dashboardContainerColor
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
