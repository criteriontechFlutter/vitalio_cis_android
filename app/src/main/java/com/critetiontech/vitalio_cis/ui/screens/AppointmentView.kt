package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EventBusy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.AppointmentHistoryItem
import com.critetiontech.ctvitalio.model.UpcomingAppointmentItem
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.StretchPullRefreshBox
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.AppointmentViewModel
import com.critetiontech.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.shimmerBrush
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val OkGreen = Color(0xFF16A34A)
private val WarnOrange = Color(0xFFEA580C)
private val ErrorRed = Color(0xFFDC2626)

/** One row on either tab, built from the upcoming or history API. */
private data class AppointmentUi(
    val key: String,
    val doctorName: String,
    val subtitle: String,
    val date: LocalDate?,
    val time: String,
    val visitNo: String,
    val status: Status,
    val chips: List<Pair<String, Color>>,
) {
    enum class Status { Today, Tomorrow, Upcoming, Completed, Cancelled }
}

/** "2026-10-16T00:00:00" → date. */
private fun parseIsoDate(raw: String?): LocalDate? =
    runCatching { LocalDate.parse(raw.orEmpty().substringBefore("T").trim()) }.getOrNull()

/** "16-10-2026 11:17:29" → date. */
private fun parseDmyDate(raw: String?): LocalDate? =
    runCatching { LocalDate.parse(raw.orEmpty().substringBefore(" ").trim(), DateTimeFormatter.ofPattern("dd-MM-yyyy")) }.getOrNull()

/** "10:00:00.0000000" → "10:00 AM"; blank for a missing or midnight placeholder time. */
private fun appointmentTime(raw: String?): String {
    val t = raw.orEmpty().trim()
    if (t.isEmpty() || t.startsWith("00:00")) return ""
    return formatSlotTime(t.substringBefore("."))
}

private fun statusFor(date: LocalDate?, cancelled: Boolean): AppointmentUi.Status {
    val today = LocalDate.now()
    return when {
        cancelled -> AppointmentUi.Status.Cancelled
        date == null -> AppointmentUi.Status.Upcoming
        date == today -> AppointmentUi.Status.Today
        date == today.plusDays(1) -> AppointmentUi.Status.Tomorrow
        date.isAfter(today) -> AppointmentUi.Status.Upcoming
        else -> AppointmentUi.Status.Completed
    }
}

private fun paymentChip(status: String): Pair<String, Color>? = when {
    status.isBlank() -> null
    status.equals("Paid", ignoreCase = true) -> "Paid" to OkGreen
    status.equals("Pending", ignoreCase = true) -> "Payment pending" to WarnOrange
    else -> status to ErrorRed
}

/**
 * The upcoming API has no appointment date (its visitDate is when it was booked), so take the date
 * — and the department — from the matching history row when there is one.
 */
private fun UpcomingAppointmentItem.toUi(byVisit: Map<String, AppointmentHistoryItem>): AppointmentUi {
    val match = byVisit[visitNo]
    val date = parseIsoDate(match?.appointmentDate) ?: parseDmyDate(visitDate)
    val chips = buildList {
        if (tokenNumber > 0) add("Token #$tokenNumber" to Color(0xFF7C3AED))
        paymentChip(paymentStatus)?.let(::add)
        if (patientType.isNotBlank()) add(patientType to Color(0xFF475569))
    }
    return AppointmentUi(
        key = "u_${appoientmentId}_$visitNo",
        doctorName = doctorName.trim().ifEmpty { "Doctor" },
        subtitle = listOfNotNull(
            match?.departmentName?.takeIf { it.isNotBlank() },
            match?.qualification?.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        date = date,
        time = appointmentTime(slotTime),
        visitNo = visitNo,
        status = statusFor(date, cancelled = false),
        chips = chips,
    )
}

private fun AppointmentHistoryItem.toUi(): AppointmentUi {
    val date = parseIsoDate(appointmentDate) ?: parseIsoDate(visitDate)
    val status = statusFor(date, isCancelAppointment)
    val chips = buildList {
        if (tokenNumber > 0) add("Token #$tokenNumber" to Color(0xFF7C3AED))
        if (shiftName.isNotBlank()) add(shiftName to Color(0xFF475569))
        if (status == AppointmentUi.Status.Completed && prescriptionStatus.equals("Prescribed", ignoreCase = true))
            add("Prescription ready" to OkGreen)
    }
    return AppointmentUi(
        key = "h_${appoientmentId}_$visitNo",
        doctorName = doctorName.trim().ifEmpty { "Doctor" },
        subtitle = listOfNotNull(
            departmentName?.takeIf { it.isNotBlank() } ?: designation.takeIf { it.isNotBlank() },
            qualification?.takeIf { it.isNotBlank() },
        ).joinToString(" · "),
        date = date,
        time = appointmentTime(slotTime),
        visitNo = visitNo,
        status = status,
        chips = chips,
    )
}

/** "Today", "Tomorrow" or "Mon, 19 Oct" for a group header. */
private fun dayHeader(date: LocalDate?): String {
    val today = LocalDate.now()
    return when (date) {
        null -> "Date to be confirmed"
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEE, dd MMM" else "EEE, dd MMM yyyy", Locale.getDefault()))
    }
}

@Composable
fun FindDoctorsTopSection(vm: AppointmentViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val navController = LocalNavController.current
    val haptic = rememberHaptics()
    val scope = rememberCoroutineScope()

    val upcoming by vm.upcoming.collectAsState()
    val history by vm.history.collectAsState()
    val loading by vm.loading.collectAsState()
    val historyLoading by vm.historyLoading.collectAsState()
    val upcomingError by vm.upcomingError.collectAsState()
    val historyError by vm.historyError.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.fetchAll() }

    val upcomingUi = remember(upcoming, history) {
        val byVisit = history.associateBy { it.visitNo }
        upcoming.map { it.toUi(byVisit) }
            .sortedWith(compareBy<AppointmentUi> { it.date ?: LocalDate.MAX }.thenBy { it.time.isEmpty() }.thenBy { slotSortKey(it.time) })
    }
    // Past and cancelled visits; today's and future ones already sit on the Upcoming tab.
    val historyUi = remember(history, upcoming) {
        val upcomingVisits = upcoming.map { it.visitNo }.toSet()
        history.map { it.toUi() }
            .filter { it.visitNo !in upcomingVisits || it.status == AppointmentUi.Status.Cancelled }
            .sortedWith(compareByDescending<AppointmentUi> { it.date ?: LocalDate.MIN }.thenByDescending { slotSortKey(it.time) })
    }

    Column(Modifier.fillMaxSize().background(colors.dashboardContainerColor)) {
        RoundBackTopBar(
            title = "My Appointments",
            // Go back to Find Doctors if we came from there, instead of stacking another copy.
            actions = {
                BookPill {
                    navController.navigate(Routes.FINDDOCTOR) {
                        popUpTo(Routes.FINDDOCTOR)
                        launchSingleTop = true
                    }
                }
            },
        )

        AppointmentToggle(
            selected = selectedTab,
            upcomingCount = upcomingUi.size,
            historyCount = historyUi.size,
            onSelect = {
                if (it != selectedTab) haptic(HapticEvent.Selection)
                selectedTab = it
            },
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        StretchPullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                scope.launch {
                    vm.fetchAll()
                    delay(300)
                    withTimeoutOrNull(20_000) { vm.loading.first { !it } }
                    isRefreshing = false
                }
            },
            refreshingText = "Updating your appointments…",
            modifier = Modifier.fillMaxSize().padding(top = 12.dp),
        ) {
            val showUpcoming = selectedTab == 0
            val items = if (showUpcoming) upcomingUi else historyUi
            val busy = if (showUpcoming) loading else historyLoading
            val failed = if (showUpcoming) upcomingError else historyError

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    busy && items.isEmpty() -> items(3) { AppointmentShimmerCard() }
                    failed && items.isEmpty() -> item {
                        AppointmentsMessage(
                            icon = Icons.Rounded.WifiOff,
                            title = "Couldn't load your appointments",
                            body = "Check your connection, then pull down or tap below to try again.",
                            action = "Try again",
                            onAction = { vm.fetchAll() },
                        )
                    }
                    items.isEmpty() && showUpcoming -> item {
                        AppointmentsMessage(
                            icon = Icons.Rounded.EventBusy,
                            title = "No upcoming appointments",
                            body = "Book a visit with a doctor and it will show up here.",
                            action = "Find a Doctor",
                            onAction = { navController.navigate(Routes.FINDDOCTOR) },
                        )
                    }
                    items.isEmpty() -> item {
                        AppointmentsMessage(
                            icon = Icons.Rounded.History,
                            title = "No past appointments",
                            body = "Your completed and cancelled visits will appear here.",
                        )
                    }
                    else -> {
                        var lastHeader: String? = null
                        items.forEachIndexed { i, appt ->
                            // Upcoming is grouped by day so "Today" stands out; history stays a plain list.
                            val header = if (showUpcoming) dayHeader(appt.date) else null
                            if (header != null && header != lastHeader) {
                                lastHeader = header
                                item(key = "hdr_$header") {
                                    Text(
                                        header,
                                        color = colors.textDarkColor,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = if (i == 0) 4.dp else 10.dp),
                                    )
                                }
                            }
                            item(key = appt.key) {
                                AnimatedEnter(index = i) { AppointmentCard(appt) }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** "10:30 AM" → minutes for sorting; unknown times go last. */
private fun slotSortKey(time: String): Int {
    val m = Regex("""(\d{1,2}):(\d{2})\s*([AP]M)""").find(time) ?: return Int.MAX_VALUE
    val (h, min, ap) = m.destructured
    return (h.toInt() % 12 + if (ap == "PM") 12 else 0) * 60 + min.toInt()
}

@Composable
private fun AppointmentCard(appt: AppointmentUi) {
    val colors = LocalMyColorScheme.current
    val past = appt.status == AppointmentUi.Status.Completed || appt.status == AppointmentUi.Status.Cancelled
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            DateBlock(appt.date, highlighted = appt.status == AppointmentUi.Status.Today, muted = past)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        appt.doctorName,
                        color = colors.textDarkColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    Spacer(Modifier.width(8.dp))
                    StatusChip(appt.status)
                }
                if (appt.subtitle.isNotEmpty()) {
                    Text(appt.subtitle, color = colors.textGreyColor, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (appt.time.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Schedule, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(appt.time, color = colors.textDarkColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (appt.chips.isNotEmpty() || appt.visitNo.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    appt.chips.forEach { (label, color) -> TintChip(label, color) }
                }
                if (appt.visitNo.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Text(appt.visitNo, color = colors.textGreyColor, fontSize = 11.sp)
                }
            }
        }
    }
}

/** Calendar-style "16 / OCT" tile. */
@Composable
private fun DateBlock(date: LocalDate?, highlighted: Boolean, muted: Boolean) {
    val colors = LocalMyColorScheme.current
    val bg = if (highlighted) colors.primaryBlueColor else colors.dashboardContainerColor
    val fg = when {
        highlighted -> Color.White
        muted -> colors.textGreyColor
        else -> colors.primaryBlueColor
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 56.dp, height = 62.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(bg),
    ) {
        Text(date?.dayOfMonth?.toString() ?: "–", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 24.sp)
        Text(
            date?.month?.getDisplayName(TextStyle.SHORT, Locale.getDefault())?.uppercase().orEmpty(),
            color = fg.copy(alpha = 0.8f),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun StatusChip(status: AppointmentUi.Status) {
    val colors = LocalMyColorScheme.current
    val (label, color) = when (status) {
        AppointmentUi.Status.Today -> "Today" to OkGreen
        AppointmentUi.Status.Tomorrow -> "Tomorrow" to colors.primaryBlueColor
        AppointmentUi.Status.Upcoming -> "Upcoming" to colors.primaryBlueColor
        AppointmentUi.Status.Completed -> "Completed" to Color(0xFF475569)
        AppointmentUi.Status.Cancelled -> "Cancelled" to ErrorRed
    }
    TintChip(label, color)
}

@Composable
private fun TintChip(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun BookPill(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(colors.primaryBlueColor)
            .bounceClick(onClick = onClick)
            .padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp),
    ) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(4.dp))
        Text("Book", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AppointmentsMessage(
    icon: ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    val colors = LocalMyColorScheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp, start = 24.dp, end = 24.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(84.dp).clip(CircleShape).background(colors.primaryBlueColor.copy(alpha = 0.08f)),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(38.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(title, color = colors.textDarkColor, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Text(body, color = colors.textGreyColor, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            Text(
                action,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(colors.primaryBlueColor)
                    .bounceClick(onClick = onAction)
                    .padding(horizontal = 22.dp, vertical = 11.dp),
            )
        }
    }
}

@Composable
private fun AppointmentShimmerCard() {
    val colors = LocalMyColorScheme.current
    val brush = shimmerBrush()
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(14.dp)
    ) {
        Box(Modifier.size(width = 56.dp, height = 62.dp).clip(RoundedCornerShape(14.dp)).background(brush))
        Spacer(Modifier.width(14.dp))
        Column {
            Box(Modifier.width(150.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            Spacer(Modifier.height(10.dp))
            Box(Modifier.width(180.dp).height(20.dp).clip(RoundedCornerShape(10.dp)).background(brush))
        }
    }
}

/** Upcoming / History switch with counts, styled like the Walk-in / Virtual toggle. */
@Composable
fun AppointmentToggle(
    selected: Int = 0,
    upcomingCount: Int = 0,
    historyCount: Int = 0,
    onSelect: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalMyColorScheme.current
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.dashboardBackgroundColor)
            .padding(4.dp)
    ) {
        listOf("Upcoming" to upcomingCount, "History" to historyCount).forEachIndexed { i, (label, count) ->
            val active = selected == i
            val bg by animateColorAsState(if (active) colors.primaryBlueColor else Color.Transparent, label = "apptTab")
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bg)
                    .clickable { onSelect(i) }
                    .padding(vertical = 11.dp),
            ) {
                Text(label, color = if (active) Color.White else colors.textGreyColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (count > 0) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        count.toString(),
                        color = if (active) colors.primaryBlueColor else colors.textGreyColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (active) Color.White else colors.textGreyColor.copy(alpha = 0.12f))
                            .padding(horizontal = 7.dp, vertical = 1.dp),
                    )
                }
            }
        }
    }
}
