package com.critetiontech.ctvitalio.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.DoctorDetails
import com.critetiontech.ctvitalio.model.SlotData
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.ctvitalio.viewmodel.DoctorDetailsViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.google.gson.Gson
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val SosRed = Color(0xFFE53935)

/** How many days ahead a patient can book. */
private const val BOOKING_WINDOW_DAYS = 90L

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoctorDetailsScreen(
    doctorId: String,
    days: String,
    viewModel: DoctorDetailsViewModel = viewModel()
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val doctor by viewModel.doctor.collectAsState()
    val isSlotBased by viewModel.isSlotBased.collectAsState()
    val slotTime by viewModel.slotTime.collectAsState()
    val latestToken by viewModel.latestToken.collectAsState()
    val slotSize by viewModel.slotSize.collectAsState()
    var mode by rememberSaveable { mutableStateOf(0) }

    val profileError by viewModel.profileError.collectAsState()
    val tokenLoading by viewModel.tokenLoading.collectAsState()

    LaunchedEffect(doctorId) { viewModel.initDoctorConfiguration(doctorId) }

    // Coming back (e.g. the slot was taken meanwhile): reload availability and drop the stale pick.
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        var first = true
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (first) first = false
                else viewModel.slotDate.value.takeIf { it.isNotEmpty() }?.let { date ->
                    viewModel.setSlotTime("")
                    viewModel.setShiftId("")
                    viewModel.onDateSelected(date, doctorId)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(Modifier.fillMaxSize().background(colors.dashboardContainerColor)) {
        RoundBackTopBar(
            title = "Doctor’s Details",
            actions = {
                HeaderCircleButton(Icons.Rounded.Sos, "Emergency contacts", SosRed) {
                    navController.navigate(Routes.EMERGENCYCONTACTS)
                }
                val mobile = doctor?.mobileNo?.trim().orEmpty()
                if (mobile.isNotEmpty()) {
                    HeaderCircleButton(Icons.AutoMirrored.Rounded.Chat, "Message doctor", colors.primaryBlueColor) {
                        runCatching { context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$mobile"))) }
                    }
                }
            },
        )

        val doc = doctor
        if (doc == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                if (profileError) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Couldn't load the doctor's details", color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(6.dp))
                        Text("Check your connection and try again.", color = colors.textGreyColor, fontSize = 14.sp)
                        Spacer(Modifier.height(16.dp))
                        RetryPill { viewModel.initDoctorConfiguration(doctorId) }
                    }
                } else {
                    CircularProgressIndicator(color = colors.primaryBlueColor)
                }
            }
            return@Column
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            DoctorTopSection(doc, days)
            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                InfoCard(
                    icon = Icons.Rounded.WorkspacePremium,
                    title = doc.experience.trim().ifEmpty { "—" },
                    subtitle = "Experience",
                    modifier = Modifier.weight(1f),
                )
                InfoCard(
                    icon = Icons.Rounded.Person,
                    title = doc.consultedPatientCount.trim().ifEmpty { "0" }.let { if (it == "0") it else "$it+" },
                    subtitle = "Patients",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("Doctor Biography")
            Spacer(Modifier.height(6.dp))
            Text(
                text = doc.biography.trim().ifEmpty { "No biography available" },
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = colors.textGreyColor,
            )

            Spacer(Modifier.height(20.dp))
            ClinicToggle(selected = mode, onSelect = { mode = it })

            Spacer(Modifier.height(20.dp))
            SelectDateUI(days, doctorId, viewModel)

            Spacer(Modifier.height(20.dp))
            SlotScreen(viewModel)
            Spacer(Modifier.height(24.dp))
        }

        CommonButton(
            text = "Book Appointment",
            enabled = slotTime.isNotEmpty() && (isSlotBased || (latestToken != null && !tokenLoading)),
            cornerRadius = 14.dp,
            containerColor = colors.primaryBlueColor,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
                .height(54.dp),
            onClick = {
                val shift = if (isSlotBased) null
                else viewModel.scheduleList.value.firstOrNull { it.shiftId.toString() == viewModel.shiftId.value }
                val data = BookingDetails(
                    dID = doctorId,
                    pName = doc.name,
                    qly = doc.qualificationLine(),
                    atHospital = doc.departmentName,
                    onDate = viewModel.slotDate.value,
                    onTime = viewModel.slotTime.value,
                    free = doc.name,
                    shiftId = viewModel.shiftId.value.toIntOrNull() ?: 0,
                    queueNo = if (isSlotBased) null else latestToken?.nextQueueNumber,
                    mode = ConsultModes[mode],
                    slotMinutes = if (isSlotBased) slotSize else 0,
                    fee = doc.consultationFee,
                    shiftName = shift?.shiftName.orEmpty(),
                    shiftEnd = shift?.shiftTimeTo.orEmpty(),
                )
                navController.navigate(Routes.BOOKINGCONFERMATION + "/${Uri.encode(Gson().toJson(data))}")
            },
        )
    }
}

@Composable
private fun HeaderCircleButton(icon: ImageVector, description: String, color: Color, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(color)
            .bounceClick(onClick = onClick),
    ) {
        Icon(icon, contentDescription = description, tint = Color.White, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun RetryPill(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Text(
        "Try again",
        color = colors.primaryBlueColor,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, colors.primaryBlueColor.copy(alpha = 0.35f), RoundedCornerShape(50))
            .bounceClick(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalMyColorScheme.current.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

// -------------------- Slots --------------------

/** "14:30:00" or "14:30" as minutes since midnight. */
private fun minutesOfDay(raw: String): Int? {
    val parts = raw.trim().split(":")
    val h = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val m = parts.getOrNull(1)?.take(2)?.toIntOrNull() ?: return null
    return if (h in 0..23 && m in 0..59) h * 60 + m else null
}

private fun formatMinutes(total: Int): String {
    val t = ((total % (24 * 60)) + 24 * 60) % (24 * 60)
    val h = t / 60
    val h12 = if (h % 12 == 0) 12 else h % 12
    return String.format(Locale.US, "%02d:%02d %s", h12, t % 60, if (h < 12) "AM" else "PM")
}

/** "14:30:00" or "14:30" → "02:30 PM"; anything else is shown as it came. */
internal fun formatSlotTime(raw: String): String = minutesOfDay(raw)?.let(::formatMinutes) ?: raw

/** [raw] plus [minutes], formatted like [formatSlotTime]. */
internal fun slotEnd(raw: String, minutes: Int): String =
    minutesOfDay(raw)?.let { formatMinutes(it + minutes) } ?: raw

@Composable
fun SlotScreen(viewModel: DoctorDetailsViewModel) {
    val colors = LocalMyColorScheme.current
    val isSlotBased by viewModel.isSlotBased.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val slotSize by viewModel.slotSize.collectAsState()
    val slotsError by viewModel.slotsError.collectAsState()
    val slotDate by viewModel.slotDate.collectAsState()

    SectionTitle("Slot’s Availability")

    if (slotSize > 0) {
        Spacer(Modifier.height(10.dp))
        Text("Appointment Duration", color = colors.textGreyColor, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "$slotSize min",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(colors.primaryBlueColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }

    Spacer(Modifier.height(14.dp))
    Text(if (isSlotBased) "Select Time Slot" else "Select Shift", color = colors.textGreyColor, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))

    if (isSlotBased) {
        val shiftList by viewModel.slotList.collectAsState()
        when {
            loading && shiftList.isEmpty() -> SlotsMessage(loading = true, text = "Checking availability…")
            slotsError != null -> SlotsMessage(text = slotsError!!, onRetry = { viewModel.onDateSelected(slotDate) })
            shiftList.isEmpty() -> SlotsMessage(text = "No slots available on this day. Try another date.")
            shiftList.all { shift ->
                runCatching { Gson().fromJson(shift.slotJson, Array<SlotData>::class.java).none { it.slotStatus == "empty" } }.getOrDefault(false)
            } -> SlotsMessage(text = "All slots on this day are booked. Try another date.")
            else -> {
                // First shift open by default; tapping a header toggles it.
                var expanded by remember(shiftList) { mutableStateOf(setOf(shiftList.first().shiftId)) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    shiftList.forEach { shift ->
                        val slots: List<SlotData> = remember(shift.slotJson) {
                            runCatching { Gson().fromJson(shift.slotJson, Array<SlotData>::class.java).toList() }.getOrDefault(emptyList())
                        }
                        val open = shift.shiftId in expanded
                        ShiftAccordion(
                            title = if (slots.isEmpty()) shift.shift_Name
                            else "${formatSlotTime(slots.first().slotTime)} - ${slotEnd(slots.last().slotTime, slotSize)}",
                            subtitle = shift.shift_Name.takeIf { slots.isNotEmpty() },
                            freeCount = slots.count { it.slotStatus == "empty" },
                            open = open,
                            onToggle = { expanded = if (open) expanded - shift.shiftId else expanded + shift.shiftId },
                        ) {
                            SlotGrid(slots, shift.shiftId.toString(), viewModel)
                        }
                    }
                }
            }
        }
    } else {
        // Token Based Flow
        val scheduleList by viewModel.scheduleList.collectAsState()
        val latestToken by viewModel.latestToken.collectAsState()
        val tokenLoading by viewModel.tokenLoading.collectAsState()
        val tokenError by viewModel.tokenError.collectAsState()
        val currentShiftId by viewModel.shiftId.collectAsState()
        val haptic = rememberHaptics()
        // On today, shifts that are already over can't be booked.
        val isToday = remember(slotDate) { parseBookingDate(slotDate) == java.time.LocalDate.now() }
        val nowMinutes = remember(slotDate) { java.time.LocalTime.now().let { it.hour * 60 + it.minute } }

        if (scheduleList.isEmpty()) {
            SlotsMessage(text = "No shifts available on this day. Try another date.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                scheduleList.forEach { schedule ->
                    val selected = currentShiftId == schedule.shiftId.toString()
                    val ended = isToday && (minutesOfDay(schedule.shiftTimeTo) ?: Int.MAX_VALUE) <= nowMinutes
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selected) colors.primaryBlueColor.copy(alpha = 0.1f) else colors.dashboardBackgroundColor)
                            .clickable(enabled = !ended) {
                                if (!selected) haptic(HapticEvent.Selection)
                                viewModel.setShiftId(schedule.shiftId.toString())
                                viewModel.setSlotTime(schedule.shiftTimeFrom)
                                viewModel.fetchLatestToken(
                                    scheduleDate = viewModel.slotDate.value,
                                    shiftId = schedule.shiftId.toString()
                                )
                            }
                            .padding(16.dp)
                    ) {
                        Text(
                            "${formatSlotTime(schedule.shiftTimeFrom)} - ${formatSlotTime(schedule.shiftTimeTo)}",
                            color = when {
                                ended -> colors.textGreyColor.copy(alpha = 0.5f)
                                selected -> colors.primaryBlueColor
                                else -> colors.textDarkColor
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            if (ended) "${schedule.shiftName} · Shift has ended for today" else schedule.shiftName,
                            color = colors.textGreyColor.copy(alpha = if (ended) 0.6f else 1f),
                            fontSize = 12.sp,
                        )
                        if (selected) {
                            Spacer(Modifier.height(10.dp))
                            when {
                                tokenLoading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
                                latestToken != null -> Text(
                                    "Your Token No: ${latestToken!!.nextQueueNumber}",
                                    color = colors.textDarkColor,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                tokenError != null -> Text(tokenError!!, color = Color(0xFFDC2626), fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotsMessage(text: String, loading: Boolean = false, onRetry: (() -> Unit)? = null) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(16.dp),
    ) {
        if (loading) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = colors.primaryBlueColor)
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = colors.textGreyColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        if (onRetry != null) {
            Spacer(Modifier.width(10.dp))
            RetryPill(onRetry)
        }
    }
}

/** Shift time range that opens to show its slots, like the design's "12:00 PM - 01:00 PM ^". */
@Composable
private fun ShiftAccordion(
    title: String,
    subtitle: String?,
    freeCount: Int,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val arrow by animateFloatAsState(if (open) 180f else 0f, label = "shiftArrow")
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { haptic(HapticEvent.Toggle); onToggle() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(
                        "$subtitle · $freeCount ${if (freeCount == 1) "slot" else "slots"} free",
                        color = colors.textGreyColor,
                        fontSize = 12.sp,
                    )
                }
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = if (open) "Collapse" else "Expand",
                tint = colors.textGreyColor,
                modifier = Modifier.rotate(arrow),
            )
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Box(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) { content() }
        }
    }
}

@Composable
fun SlotGrid(list: List<SlotData>, ShifId: String, viewModel: DoctorDetailsViewModel) {
    val slotTime by viewModel.slotTime.collectAsState()
    val shiftId by viewModel.shiftId.collectAsState()
    val haptic = rememberHaptics()

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        list.forEach { slot ->
            SlotItem(
                slot = slot,
                isSelected = shiftId == ShifId && slotTime == slot.slotTime,
                onClick = {
                    if (slot.slotStatus == "empty") {
                        haptic(HapticEvent.Selection)
                        viewModel.setSlotTime(slot.slotTime)
                        viewModel.setShiftId(ShifId)
                    }
                }
            )
        }
    }
}

@Composable
fun SlotItem(
    slot: SlotData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val booked = slot.slotStatus != "empty"
    val background by animateColorAsState(
        when {
            isSelected -> colors.primaryBlueColor
            else -> colors.dashboardContainerColor
        },
        label = "slotBg",
    )
    val shape = RoundedCornerShape(8.dp)

    Text(
        text = formatSlotTime(slot.slotTime),
        color = when {
            isSelected -> Color.White
            booked -> colors.textGreyColor.copy(alpha = 0.45f)
            else -> colors.textDarkColor
        },
        fontSize = 13.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        textDecoration = if (booked) TextDecoration.LineThrough else null,
        modifier = Modifier
            .clip(shape)
            .background(background)
            // Selected keeps a same-colour border so the chip doesn't change size.
            .border(1.dp, if (isSelected) background else DoctorOutline, shape)
            .clickable(enabled = !booked, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
    )
}

// -------------------- Header --------------------
@Composable
fun DoctorTopSection(doc: DoctorDetails, days: String) {
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        DoctorPhoto(
            name = doc.name,
            url = doctorPhotoUrl(doc.profilePath),
            initialsSize = 26.sp,
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(12.dp)),
        )

        Spacer(Modifier.width(14.dp))

        Column {
            Text(doc.name, color = colors.textDarkColor, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val qualification = doc.qualificationLine()
            if (qualification.isNotEmpty()) {
                Text(qualification, color = colors.textGreyColor, fontSize = 13.sp, fontStyle = FontStyle.Italic)
            }
            if (days.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                DoctorDaysChip(days)
            }
        }
    }
}

@Composable
fun InfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.textGreyColor, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = colors.textDarkColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, fontSize = 12.sp, color = colors.textGreyColor)
        }
    }
}

// -------------------- Date --------------------

/**
 * Weekdays the doctor works, from the "Mon, Tue" text passed by Find Doctors.
 * "Daily", "N/A" or anything unreadable means every day is offered.
 */
@RequiresApi(Build.VERSION_CODES.O)
private fun parseWorkingDays(days: String): Set<DayOfWeek> {
    val all = DayOfWeek.values().toSet()
    val tokens = days.split(",").map { it.trim().take(3).lowercase() }.filter { it.isNotEmpty() }
    val parsed = DayOfWeek.values().filter { d ->
        d.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).take(3).lowercase() in tokens
    }.toSet()
    return parsed.ifEmpty { all }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectDateUI(days: String, doctorId: String, viewModel: DoctorDetailsViewModel) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()
    val today = remember { LocalDate.now() }
    val workingDays = remember(days) { parseWorkingDays(days) }
    val lastDay = remember { today.plusDays(BOOKING_WINDOW_DAYS) }

    var month by remember { mutableStateOf(today.withDayOfMonth(1)) }
    var selected by remember { mutableStateOf<LocalDate?>(null) }

    fun select(date: LocalDate) {
        selected = date
        // A new day has different slots, so drop the old pick.
        viewModel.setSlotTime("")
        viewModel.setShiftId("")
        viewModel.onDateSelected("${date.year}-${date.monthValue}-${date.dayOfMonth}", doctorId)
    }

    // Start on the first day the doctor is in.
    LaunchedEffect(workingDays) {
        if (selected == null) {
            val first = generateSequence(today) { it.plusDays(1) }.take(BOOKING_WINDOW_DAYS.toInt())
                .firstOrNull { it.dayOfWeek in workingDays } ?: today
            month = first.withDayOfMonth(1)
            select(first)
        }
    }

    val dates = remember(month) {
        (0 until month.lengthOfMonth()).map { month.plusDays(it.toLong()) }
            .filter { !it.isBefore(today) && !it.isAfter(lastDay) }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(dates, selected) {
        val i = dates.indexOf(selected)
        if (i >= 0) listState.animateScrollToItem((i - 1).coerceAtLeast(0))
    }

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        SectionTitle("Select Date", Modifier.weight(1f))
        MonthYearDropdown(
            month = month,
            months = remember { generateSequence(today.withDayOfMonth(1)) { it.plusMonths(1) }.takeWhile { !it.isAfter(lastDay) }.toList() },
        ) { picked ->
            if (picked != month) {
                haptic(HapticEvent.Selection)
                month = picked
                val first = generateSequence(if (picked.isBefore(today)) today else picked) { it.plusDays(1) }
                    .takeWhile { it.month == picked.month && !it.isAfter(lastDay) }
                    .firstOrNull { it.dayOfWeek in workingDays }
                if (first != null) select(first)
            }
        }
    }

    Spacer(Modifier.height(12.dp))

    LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(dates, key = { _, d -> d.toString() }) { _, date ->
            DateItemView(
                date = date,
                selected = date == selected,
                available = date.dayOfWeek in workingDays,
            ) {
                if (date != selected) haptic(HapticEvent.Selection)
                select(date)
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthYearDropdown(
    month: LocalDate,
    months: List<LocalDate>,
    onSelected: (LocalDate) -> Unit
) {
    val colors = LocalMyColorScheme.current
    var expanded by remember { mutableStateOf(false) }
    fun label(d: LocalDate) = "${d.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())} ${d.year}"

    Box {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { expanded = true }
                .padding(horizontal = 4.dp, vertical = 2.dp),
        ) {
            Text(label(month), color = colors.primaryBlueColor, fontSize = 14.sp)
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Change month", tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp))
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = colors.dashboardContainerColor,
        ) {
            months.forEach { m ->
                DropdownMenuItem(
                    text = {
                        Text(
                            label(m),
                            color = if (m == month) colors.primaryBlueColor else colors.textDarkColor,
                            fontWeight = if (m == month) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    },
                    onClick = {
                        onSelected(m)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** One day in the strip: filled when picked, blue-tinted when the doctor is in, faded when not. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateItemView(
    date: LocalDate,
    selected: Boolean,
    available: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMyColorScheme.current
    val background by animateColorAsState(
        when {
            selected -> colors.primaryBlueColor
            available -> colors.primaryBlueColor.copy(alpha = 0.1f)
            else -> colors.dashboardBackgroundColor
        },
        label = "dateBg",
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(width = 46.dp, height = 46.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(background)
                .clickable(enabled = available, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Text(
                date.dayOfMonth.toString(),
                fontSize = 17.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> Color.White
                    available -> colors.primaryBlueColor
                    else -> colors.textGreyColor.copy(alpha = 0.35f)
                },
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            fontSize = 12.sp,
            color = when {
                selected -> colors.textDarkColor
                available -> colors.textGreyColor
                else -> colors.textGreyColor.copy(alpha = 0.35f)
            },
        )
    }
}

/** "MBBS (Medicine)". */
internal fun DoctorDetails.qualificationLine(): String =
    listOf(highestQualificationName, departmentName.takeIf { it.isNotBlank() }?.let { "($it)" })
        .filter { !it.isNullOrBlank() }
        .joinToString(" ")

internal val ConsultModes = listOf("Walk-in", "Virtual")

/** Walk-in / Virtual switch. */
@Composable
fun ClinicToggle(selected: Int, onSelect: (Int) -> Unit) {
    val colors = LocalMyColorScheme.current
    val haptic = rememberHaptics()

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(4.dp)
    ) {
        ConsultModes.forEachIndexed { i, label ->
            val bg by animateColorAsState(if (selected == i) colors.primaryBlueColor else Color.Transparent, label = "modeBg")
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable {
                        if (selected != i) haptic(HapticEvent.Selection)
                        onSelect(i)
                    }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (selected == i) Color.White else colors.textGreyColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
