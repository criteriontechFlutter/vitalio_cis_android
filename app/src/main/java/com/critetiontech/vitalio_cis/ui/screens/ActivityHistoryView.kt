package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.text.format.DateFormat
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.critetiontech.ctvitalio.ui.components.AppDatePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.components.showToast
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.ui.theme.isAppDark
import com.critetiontech.ctvitalio.viewmodel.ActivityViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun ActivityHistoryScreen(viewModel: ActivityViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val recentList by viewModel.recentActivityList.collectAsState()
    val isLoading by viewModel.isLoadingHistory.collectAsState()
    val message by viewModel.message.collectAsState()
    val historyDate by viewModel.historyDate.collectAsState()
    var pendingDelete by remember { mutableStateOf<PatientActivityItem?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Master list gives the right names (the history API's own activityName is wrong).
    val allActivities by viewModel.allActivities.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.getAllActivities()
        viewModel.loadHistory()
    }

    LaunchedEffect(message) {
        message?.let {
            Haptics.error()
            showToast(context, context.getString(it))
            viewModel.clearMessage()
        }
    }

    CommonAppBar(title = stringResource(R.string.track_history)) {
      Column(Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        HistoryDateBar(
            date = historyDate,
            isToday = viewModel.isHistoryToday,
            onShift = viewModel::shiftHistoryDate,
            onPick = { showDatePicker = true }
        )
        when {
            isLoading && recentList.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            recentList.isEmpty() -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(if (viewModel.isHistoryToday) R.string.track_no_activity_history else R.string.track_activity_none_on_day),
                    color = colors.textGreyColor
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp)
            ) {
                items(recentList.size, key = { index -> "${recentList[index].serverId}_$index" }) { index ->
                    AnimatedEnter(index = index) {
                        val row = recentList[index]
                        val name = remember(row, allActivities) { viewModel.displayName(row) }
                        ActivityHistoryCard(row, name, onDelete = { pendingDelete = it })
                    }
                }
            }
        }
      }
    }

    if (showDatePicker) {
        val today = LocalDate.now()
        AppDatePickerDialog(
            initial = historyDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.setHistoryDate(it)
                showDatePicker = false
            },
            isSelectable = { !it.isAfter(today) }
        )
    }

    pendingDelete?.let { item ->
        val name = viewModel.displayName(item).ifEmpty { stringResource(R.string.track_activity) }
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.track_activity_delete_title)) },
            text = { Text(stringResource(R.string.track_activity_delete_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeActivity(item)
                    pendingDelete = null
                }) { Text(stringResource(R.string.track_activity_delete), color = Color(0xFFF25959)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.track_activity_cancel)) }
            }
        )
    }
}

/** "‹   📅 Today / Mon, 16 Nov 2026   ›": arrows move a day, the middle opens a calendar. */
@Composable
private fun HistoryDateBar(date: LocalDate, isToday: Boolean, onShift: (Long) -> Unit, onPick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val label = if (isToday) stringResource(R.string.track_activity_today)
    else date.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.getDefault()))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HapticIconButton(onClick = { onShift(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.track_activity_previous_day), tint = colors.textDarkColor)
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = stringResource(R.string.track_activity_pick_date)) { onPick() }
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = Color(0xFF2E5BFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = colors.textDarkColor)
        }
        HapticIconButton(onClick = { onShift(1) }, enabled = !isToday) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                stringResource(R.string.track_activity_next_day),
                tint = if (isToday) colors.textGreyColor.copy(alpha = 0.3f) else colors.textDarkColor
            )
        }
    }
}

@Composable
private fun ActivityHistoryCard(item: PatientActivityItem, name: String, onDelete: (PatientActivityItem) -> Unit) {
    val colors = LocalMyColorScheme.current
    val is24h = DateFormat.is24HourFormat(LocalContext.current)
    val formattedTime = remember(item, is24h) {
        formatActivityTimeRange(item.resolvedStartTime, item.totalTimeInMinutes, is24h)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.reminders),
                    contentDescription = null,
                    tint = if (isAppDark) colors.textGreyColor else Color(0xFF475569),
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formattedTime,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textDarkColor,
                    modifier = Modifier.weight(1f)
                )
                if (item.serverId > 0) {
                    HapticIconButton(onClick = { onDelete(item) }) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.track_activity_delete),
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = if (isAppDark) colors.borderGreyLightColor else Color(0xFFF1F5F9)
            )

            Text(
                text = name.ifEmpty { stringResource(R.string.track_activity) },
                fontSize = 14.sp,
                color = colors.textGreyColor,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
        }
    }
}

/**
 * "7:30 AM – 8:15 AM · 45 min" from the stored start time (HH:mm[:ss]) and duration.
 * Shows only what the server actually sent — no invented times or durations.
 */
internal fun formatActivityTimeRange(startTime: String, duration: String?, is24h: Boolean = false): String {
    val minutes = duration?.trim()?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 }
    val out = SimpleDateFormat(if (is24h) "HH:mm" else "h:mm a", Locale.US)
    val raw = startTime.trim().substringAfter('T')
    val start = listOf("HH:mm:ss", "HH:mm").firstNotNullOfOrNull { pattern ->
        runCatching { SimpleDateFormat(pattern, Locale.US).apply { isLenient = false }.parse(raw) }.getOrNull()
    }
    val durationText = minutes?.let { "$it min" }
    return when {
        start != null && minutes != null -> {
            val end = Calendar.getInstance().apply { time = start; add(Calendar.MINUTE, minutes) }.time
            "${out.format(start)} – ${out.format(end)} · $durationText"
        }
        start != null -> out.format(start)
        durationText != null -> durationText
        else -> "—"
    }
}
