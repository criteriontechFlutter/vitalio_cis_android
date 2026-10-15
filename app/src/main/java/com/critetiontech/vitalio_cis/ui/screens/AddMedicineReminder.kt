package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.components.AppDatePickerDialog
import com.critetiontech.ctvitalio.ui.components.AppTimePickerDialog
import java.time.LocalDate
import java.time.ZoneId

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticCheckbox
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.ctvitalio.model.SearchMedicineItem
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.AddMedicineReminderViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.viewmodel.TimeSlot
import java.text.SimpleDateFormat
import java.util.*

private val Blue500 = Color(0xFF4A8EF5)
private val Purple500 = Color(0xFF9C6FE4)
private val Orange500 = Color(0xFFF5A623)
private val Teal500 = Color(0xFF3DBFA8)
private val Green500 = Color(0xFF4ECB8D)

@Composable
fun AddMedicineReminderScreen(viewModel: AddMedicineReminderViewModel = viewModel()) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    val isInserting by viewModel.isInserting.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val insertSuccess by viewModel.insertSuccess.collectAsStateWithLifecycle()

    var showSuccessSheet by remember { mutableStateOf(false) }

    LaunchedEffect(insertSuccess) {
        if (insertSuccess) { showSuccessSheet = true; viewModel.clearSuccess() }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CommonAppBar(title = "Add Medicine") {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
            ) {
                // Error banner
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut()
                ) {
                    errorMessage?.let { err ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(12.dp))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, null, tint = Orange500, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(err, style = AppTextStyles.style14BCN().copy(color = Color(0xFF795548)), modifier = Modifier.weight(1f))
                            HapticIconButton(onClick = { viewModel.clearError() }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Scrollable form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Card 1: Medication Details
                    MedicineFormCard(title = "Medication Details", icon = Icons.Rounded.MedicalServices, iconColor = Blue500) {
                        MedicineSearchSection(viewModel)
                    }

                    // Card 2: Frequency & Schedule
                    MedicineFormCard(title = "Frequency & Schedule", icon = Icons.Rounded.Repeat, iconColor = Purple500) {
                        FrequencySection(viewModel)
                    }

                    // Card 3: Time Slots
                    MedicineFormCard(title = "Time Slots", icon = Icons.Rounded.Schedule, iconColor = Orange500) {
                        TimeSlotsSection(viewModel)
                    }

                    // Card 4: Reminder Duration
                    MedicineFormCard(title = "Reminder Duration", icon = Icons.Rounded.DateRange, iconColor = Blue500) {
                        DateRangeSection(viewModel)
                    }

                    // Card 5: Special Instructions
                    MedicineFormCard(title = "Special Instructions", icon = Icons.Rounded.Edit, iconColor = Teal500) {
                        OutlinedTextField(
                            value = viewModel.instructions,
                            onValueChange = { viewModel.instructions = it },
                            placeholder = { Text("e.g., Take 30 mins after meal with warm water", fontSize = 13.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = medicineFieldColors(Blue500)
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }

                // Sticky footer CTA
                HapticButton(
                    onClick = { viewModel.insertMedicineReminder() },
                    enabled = !isInserting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Blue500)
                ) {
                    if (isInserting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add Reminder", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Success bottom sheet overlay
        AnimatedVisibility(
            visible = showSuccessSheet,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(0.45f))
                    .clickable { showSuccessSheet = false; viewModel.resetFields() },
                contentAlignment = Alignment.BottomCenter
            ) {
                MedicineSuccessSheet(
                    onAddMore = { showSuccessSheet = false; viewModel.resetFields() },
                    onDone = {
                        showSuccessSheet = false
                        viewModel.resetFields()
                        navController.navigate(Routes.MEDICINE) {
                            popUpTo(Routes.MEDICINE) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}

// ── Form Card Container ───────────────────────────────────────────────────────

/** Fields inside the white form cards: tinted fill, no outline, soft [accent] ring while typing. */
@Composable
fun medicineFieldColors(accent: Color) = LocalMyColorScheme.current.let { colors ->
    OutlinedTextFieldDefaults.colors(
        unfocusedBorderColor = Color.Transparent,
        focusedBorderColor = accent.copy(alpha = 0.35f),
        unfocusedContainerColor = colors.dashboardBackgroundColor,
        focusedContainerColor = colors.dashboardBackgroundColor,
    )
}

@Composable
fun MedicineFormCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = LocalMyColorScheme.current
    Surface(
        color = colors.dashboardContainerColor,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(0.12f))
                ) {
                    Icon(icon, null, tint = iconColor, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(14.dp))
            content()
        }
    }
}

// ── Medicine Search Section ───────────────────────────────────────────────────

@Composable
fun MedicineSearchSection(viewModel: AddMedicineReminderViewModel) {
    val selected = viewModel.selectedMedicine
    if (selected != null) {
        // Selected medicine chip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Blue500.copy(0.08f), RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Medication, null, tint = Blue500, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(selected.brandName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("${selected.medicineName} • ${selected.dosageFormName} ${selected.doseStrength.toInt()} ${selected.unitName}",
                    fontSize = 12.sp, color = Color.Gray, maxLines = 1)
            }
            HapticIconButton(onClick = { viewModel.clearMedicineSelection() }) {
                Icon(Icons.Default.Cancel, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
            }
        }
    } else {
        // Search field
        OutlinedTextField(
            value = viewModel.medicineName,
            onValueChange = { viewModel.onMedicineNameChange(it) },
            placeholder = { Text("Search medicine by brand or name...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
            trailingIcon = {
                if (viewModel.medicineName.isNotEmpty() && !viewModel.isSearching) {
                    HapticIconButton(onClick = { viewModel.onMedicineNameChange("") }) {
                        Icon(Icons.Default.Cancel, null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = medicineFieldColors(Blue500)
        )

        // Search results dropdown
        if (viewModel.isSearching || viewModel.searchResults.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                LazyColumn {
                    if (viewModel.isSearching) {
                        items(3) {
                            MedicineSearchShimmerRow()
                        }
                    } else {
                        items(viewModel.searchResults) { med ->
                            MedicineSearchResultRow(med) { viewModel.selectMedicine(med) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun shimmerBrush(): androidx.compose.ui.graphics.Brush {
    val colors = LocalMyColorScheme.current
    val shimmerColors = listOf(
        colors.dashboardContainerColor,
        colors.borderGreyLightColor.copy(.3f),
        colors.dashboardContainerColor
    )
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(1000, easing = androidx.compose.animation.core.LinearEasing), 
            androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return androidx.compose.ui.graphics.Brush.linearGradient(shimmerColors, start = androidx.compose.ui.geometry.Offset(x, 0f), end = androidx.compose.ui.geometry.Offset(x + 600f, 0f))
}

@Composable
fun MedicineSearchShimmerRow() {
    val brush = shimmerBrush()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(modifier = Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        Spacer(Modifier.height(6.dp))
        Box(modifier = Modifier.width(200.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
    }
    HorizontalDivider(color = Color.Gray.copy(0.1f))
}

@Composable
fun MedicineSearchResultRow(med: SearchMedicineItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(med.brandName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Text("${med.medicineName} - ${med.dosageFormName} ${med.doseStrength.toInt()} ${med.unitName}",
            fontSize = 11.sp, color = Color.Gray, maxLines = 1)
    }
    HorizontalDivider(color = Color.Gray.copy(0.1f))
}

// ── Frequency Section ─────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencySection(viewModel: AddMedicineReminderViewModel) {
    val items = listOf("Every day", "Every x day", "Every week", "Every month")
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = viewModel.frequency.ifEmpty { "Select Intake Frequency" },
            onValueChange = {},
            readOnly = true,
            leadingIcon = { Icon(Icons.Rounded.Repeat, null, tint = Purple500, modifier = Modifier.size(18.dp)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(14.dp),
            colors = medicineFieldColors(Purple500)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            items.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = { viewModel.frequency = item; expanded = false }
                )
            }
        }
    }

    Spacer(Modifier.height(8.dp))

    when (viewModel.frequency) {
        "Every x day" -> FrequencyStepperSection(viewModel)
        "Every week" -> WeeklyDaysSelectorSection(viewModel)
        "Every month" -> MonthlyDateSelectorSection(viewModel)
    }
}

@Composable
fun FrequencyStepperSection(viewModel: AddMedicineReminderViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(LocalMyColorScheme.current.dashboardBackgroundColor, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Repeat Interval", fontSize = 13.sp, color = Color.Gray)
            Text("Every ${viewModel.frequencyValue} days", fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            HapticIconButton(
                onClick = { if (viewModel.frequencyValue > 2) viewModel.frequencyValue-- },
                enabled = viewModel.frequencyValue > 2,
                modifier = Modifier.size(36.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(0.15f))
                ) { Icon(Icons.Rounded.Remove, null, modifier = Modifier.size(16.dp)) }
            }
            Text(
                "${viewModel.frequencyValue}",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                modifier = Modifier.width(28.dp),
                textAlign = TextAlign.Center
            )
            HapticIconButton(onClick = { viewModel.frequencyValue++ }, modifier = Modifier.size(36.dp)) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(0.15f))
                ) { Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
fun WeeklyDaysSelectorSection(viewModel: AddMedicineReminderViewModel) {
    val days = listOf(
        "Sunday" to "S", "Monday" to "M", "Tuesday" to "T",
        "Wednesday" to "W", "Thursday" to "T", "Friday" to "F", "Saturday" to "S"
    )
    Column {
        Text("Select Specific Days", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            days.forEach { (id, label) ->
                val isSelected = viewModel.selectedDays.contains(id)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(if (isSelected) Blue500 else Color.Gray.copy(0.12f))
                        .clickable {
                            viewModel.selectedDays = if (isSelected)
                                viewModel.selectedDays - id
                            else
                                viewModel.selectedDays + id
                        }
                ) {
                    Text(
                        label,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            isSelected -> Color.White
                            id == "Sunday" -> Color.Red
                            else -> Color.Unspecified
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyDateSelectorSection(viewModel: AddMedicineReminderViewModel) {
    var showCalendar by remember { mutableStateOf(false) }
    Column {
        Text("Select Dates of the Month", fontSize = 13.sp, color = Color.Gray)
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(LocalMyColorScheme.current.dashboardBackgroundColor, RoundedCornerShape(14.dp))
                .clickable { showCalendar = true }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                if (viewModel.selectedDates.isEmpty()) "Select dates" else viewModel.selectedDates.sorted().joinToString(", "),
                modifier = Modifier.weight(1f),
                fontSize = 14.sp,
                color = if (viewModel.selectedDates.isEmpty()) Color.Gray else Color.Unspecified
            )
            Icon(Icons.Default.DateRange, null, tint = Blue500)
        }
        if (showCalendar) {
            MultiDateCalendarDialog(
                selectedDates = viewModel.selectedDates,
                onDatesChanged = { viewModel.selectedDates = it },
                onDismiss = { showCalendar = false }
            )
        }
    }
}

@Composable
fun MultiDateCalendarDialog(selectedDates: List<Int>, onDatesChanged: (List<Int>) -> Unit, onDismiss: () -> Unit) {
    var local by remember { mutableStateOf(selectedDates.toMutableList()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Dates", fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn {
                itemsIndexed((1..31).toList()) { _, day ->
                    val isSelected = local.contains(day)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                local = if (isSelected) local.also { it.remove(day) }
                                else local.also { it.add(day) }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HapticCheckbox(checked = isSelected, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Day $day")
                    }
                }
            }
        },
        confirmButton = { HapticTextButton(onClick = { onDatesChanged(local.sorted()); onDismiss() }) { Text("Done") } },
        dismissButton = { HapticTextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

// ── Time Slots Section ────────────────────────────────────────────────────────

@Composable
fun TimeSlotsSection(viewModel: AddMedicineReminderViewModel) {
    val timeFmt = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    var editingSlot by remember { mutableStateOf<Int?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        viewModel.timeSlots.forEachIndexed { index, slot ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .background(LocalMyColorScheme.current.dashboardBackgroundColor, RoundedCornerShape(14.dp))
                        .clickable { editingSlot = index }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Schedule, null, tint = Orange500, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (slot.isSelected) timeFmt.format(slot.time) else "Tap to set time (HH:MM)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (slot.isSelected) Color.Unspecified else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.Edit, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                }
                if (viewModel.timeSlots.size > 1 && index > 0) {
                    HapticIconButton(
                        onClick = { viewModel.removeTimeSlot(index) },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.Red.copy(0.1f))
                    ) {
                        Icon(Icons.Rounded.Delete, null, tint = Color.Red, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        HapticTextButton(onClick = { viewModel.addTimeSlot() }) {
            Icon(Icons.Default.AddCircle, null, tint = Blue500, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add Another Time Slot", color = Blue500, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        }
    }

    editingSlot?.let { index ->
        val cal = Calendar.getInstance().also { it.time = viewModel.timeSlots[index].time }
        AppTimePickerDialog(
            initialHour = cal.get(Calendar.HOUR_OF_DAY),
            initialMinute = cal.get(Calendar.MINUTE),
            title = "Dose time",
            onDismiss = { editingSlot = null },
            onConfirm = { h, m ->
                cal.set(Calendar.HOUR_OF_DAY, h)
                cal.set(Calendar.MINUTE, m)
                viewModel.updateSlotTime(index, cal.time)
                editingSlot = null
            },
        )
    }
}

// ── Date Range Section ────────────────────────────────────────────────────────

@Composable
fun DateRangeSection(viewModel: AddMedicineReminderViewModel) {
    // Which tile's calendar is open: "start", "end" or none.
    var editing by remember { mutableStateOf<String?>(null) }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        DateTile(
            title = "Start Date",
            formattedDate = viewModel.startDateFormatted,
            modifier = Modifier.weight(1f)
        ) { editing = "start" }
        DateTile(
            title = "End Date",
            formattedDate = viewModel.endDateFormatted,
            modifier = Modifier.weight(1f)
        ) { editing = "end" }
    }

    val startDay = viewModel.startDate.toLocalDate()
    when (editing) {
        "start" -> AppDatePickerDialog(
            initial = startDay,
            onDismiss = { editing = null },
            onConfirm = { day ->
                viewModel.startDate = viewModel.startDate.withDay(day)
                // Keep the range valid: an end before the new start moves up to it.
                if (viewModel.endDate.toLocalDate().isBefore(day)) viewModel.endDate = viewModel.endDate.withDay(day)
                editing = null
            },
        )
        "end" -> AppDatePickerDialog(
            initial = viewModel.endDate.toLocalDate(),
            onDismiss = { editing = null },
            onConfirm = { day -> viewModel.endDate = viewModel.endDate.withDay(day); editing = null },
            isSelectable = { !it.isBefore(startDay) },
        )
    }
}

private fun Date.toLocalDate(): LocalDate = toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

/** Same time of day, moved to [day]. */
private fun Date.withDay(day: LocalDate): Date =
    Calendar.getInstance().also {
        it.time = this
        it.set(day.year, day.monthValue - 1, day.dayOfMonth)
    }.time

@Composable
fun DateTile(title: String, formattedDate: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier
            .background(LocalMyColorScheme.current.dashboardBackgroundColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Text(title, fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(formattedDate, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Icon(Icons.Default.DateRange, null, tint = Blue500, modifier = Modifier.size(14.dp))
        }
    }
}

// ── Success Bottom Sheet ──────────────────────────────────────────────────────

@Composable
fun MedicineSuccessSheet(onAddMore: () -> Unit, onDone: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Surface(
        color = colors.dashboardContainerColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { /* consume touches */ }
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                null,
                tint = Green500,
                modifier = Modifier.size(64.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("Reminder Scheduled!", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your medicine reminder has been created successfully. We'll notify you on time.",
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))
            HapticButton(
                onClick = onAddMore,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Blue500)
            ) {
                Text("Add Another Medicine", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(8.dp))
            HapticTextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            }
        }
    }
}

// ── Allergy Bottom Sheet (kept from original) ─────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAllergyBottomSheet(
    onDismiss: () -> Unit,
    onSubmit: (substanceName: String, severity: String, reaction: String, details: String, typeAllergy: String) -> Unit,
    isLoading: Boolean = false
) {
    val allergyTypes = listOf("MedicineAllergy", "FoodAllergy")
    var typeExpanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf("MedicineAllergy") }

    var substanceName by remember { mutableStateOf("") }
    var reaction by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var selectedSeverity by remember { mutableStateOf("Mild") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        containerColor = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add Allergy",
                style = com.critetiontech.ctvitalio.utils.AppTextStyles.style16BCN(),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Substance Type dropdown
            Text("Substance Type", style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = typeExpanded,
                onExpandedChange = { typeExpanded = !typeExpanded }
            ) {
                OutlinedTextField(
                    value = if (selectedType == "MedicineAllergy") "Medicine Allergy" else "Food Allergy",
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) }
                )
                ExposedDropdownMenu(
                    expanded = typeExpanded,
                    onDismissRequest = { typeExpanded = false }
                ) {
                    allergyTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(if (type == "MedicineAllergy") "Medicine Allergy" else "Food Allergy") },
                            onClick = {
                                selectedType = type
                                typeExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Substance Name
            Text("Substance Name", style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = substanceName,
                onValueChange = { substanceName = it },
                placeholder = { Text("Enter substance name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Reaction
            Text("Reaction / Allergy", style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = reaction,
                onValueChange = { reaction = it },
                placeholder = { Text("Enter reaction / allergy") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Details / Notes (optional)
            Text("Notes (optional)", style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = details,
                onValueChange = { details = it },
                placeholder = { Text("Additional details...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("How severe was the reaction?", style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN())
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Mild", "Moderate", "Severe").forEach { level ->
                    SeverityButton(
                        text = level,
                        isSelected = selectedSeverity == level,
                        onClick = { selectedSeverity = level }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            com.critetiontech.ctvitalio.utils.CommonButton(
                text = if (isLoading) "Saving..." else "Submit",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (!isLoading) {
                        onSubmit(substanceName.trim(), selectedSeverity, reaction.trim(), details.trim(), selectedType)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun SeverityButton(text: String, isSelected: Boolean = false, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .background(
                if (isSelected) Color(0xFF4A90D9) else Color(0xFFF3F4F6),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .then(Modifier.clickable(onClick = onClick))
    ) {
        Text(
            text,
            style = com.critetiontech.ctvitalio.utils.AppTextStyles.style12GCN(),
            color = if (isSelected) Color.White else Color.Unspecified
        )
    }
}