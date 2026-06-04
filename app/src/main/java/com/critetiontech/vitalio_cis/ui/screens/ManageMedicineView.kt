package com.critetiontech.vitalio_cis.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.ui.components.CommonAppBar
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.viewmodel.ManageMedicationViewModel
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

// ─── Colors ──────────────────────────────────────────────────────────────────

private val Blue500       = Color(0xFF4A8EF5)
private val BlueLight     = Color(0xFFEAF1FF)
private val Green500      = Color(0xFF4ECB8D)
private val Teal500       = Color(0xFF3DBFA8)
private val Orange500     = Color(0xFFF5A623)
private val BgGray        = Color(0xFFF5F7FA)
private val CardWhite     = Color(0xFFFFFFFF)
private val TextPrimary   = Color(0xFF1A1D23)
private val TextSecondary = Color(0xFF8A90A0)
private val TextMuted     = Color(0xFFC0C5D0)
private val DividerColor  = Color(0xFFEEF0F5)

// ─── Data models ─────────────────────────────────────────────────────────────

data class CalendarDay(val number: Int, val shortName: String, val style: DayStyle)
enum class DayStyle { Muted, Green, Teal, Orange, Active }

// ─── Months ───────────────────────────────────────────────────────────────────

private val MONTHS = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)

private val DAY_NAMES = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

// ─── Calendar day generation ──────────────────────────────────────────────────

private fun generateCalendarDays(year: Int, month: Int): List<CalendarDay> {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return (1..daysInMonth).map { day ->
        cal.set(year, month, day)
        val shortName = DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1]
        CalendarDay(day, shortName, DayStyle.Muted)
    }
}

private fun formatDate(year: Int, month: Int, day: Int): String {
    val mm = (month + 1).toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$year-$mm-$dd"
}

// ─── Pill sizing ──────────────────────────────────────────────────────────────

private val PILL_SIZE    = 38.dp
private val PILL_SPACING = 8.dp

// ─── Centre-index helper ──────────────────────────────────────────────────────

@Composable
private fun rememberCentreIndex(listState: LazyListState): Int =
    remember(listState) {
        derivedStateOf {
            val info   = listState.layoutInfo
            val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
            info.visibleItemsInfo
                .minByOrNull { abs(it.offset + it.size / 2f - centre) }
                ?.index ?: 0
        }
    }.value

// ─── U-curve Y offset ─────────────────────────────────────────────────────────

private fun uYOffset(
    itemIndex:  Int,
    centreIdx:  Int,
    arcDepth:   Float,
    windowHalf: Int = 4,
): Float {
    val dist  = abs(itemIndex - centreIdx).toFloat()
    val t     = (dist / windowHalf).coerceIn(0f, 1f)
    val curve = ((1.0 - cos(PI * t)) / 2.0).toFloat()
    return -arcDepth * curve
}

// ─── Month Picker Bottom Sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthPickerSheet(
    selectedMonthIndex: Int,
    selectedYear:       Int,
    onMonthYearSelected: (monthIndex: Int, year: Int) -> Unit,
    onDismiss:          () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()
    val scope      = rememberCoroutineScope()

    var pickedMonth by remember { mutableIntStateOf(selectedMonthIndex) }
    var pickedYear  by remember { mutableIntStateOf(selectedYear) }

    ModalBottomSheet(
        onDismissRequest    = onDismiss,
        sheetState          = sheetState,
        containerColor      = CardWhite,
        shape               = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle          = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(TextMuted)
            )
        },
    ) {
        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))

            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { pickedYear-- }) {
                    Icon(Icons.Rounded.Add, "Prev year", tint = TextSecondary)
                }
                Text(
                    text       = pickedYear.toString(),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.width(80.dp),
                    textAlign  = TextAlign.Center,
                )
                IconButton(onClick = { pickedYear++ }) {
                    Icon(Icons.Rounded.Add, "Next year", tint = TextSecondary)
                }
            }

            Spacer(Modifier.height(16.dp))

            val rows = MONTHS.chunked(3)
            rows.forEach { rowMonths ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowMonths.forEach { month ->
                        val idx      = MONTHS.indexOf(month)
                        val isPicked = idx == pickedMonth
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPicked) Blue500
                                    else if (idx == selectedMonthIndex && pickedYear == selectedYear) BlueLight
                                    else Color.Transparent
                                )
                                .clickable { pickedMonth = idx },
                        ) {
                            Text(
                                text       = month.take(3),
                                fontSize   = 14.sp,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                color      = when {
                                    isPicked                                                         -> Color.White
                                    idx == selectedMonthIndex && pickedYear == selectedYear -> Blue500
                                    else                                                             -> TextPrimary
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        onMonthYearSelected(pickedMonth, pickedYear)
                        onDismiss()
                    }
                },
                colors  = ButtonDefaults.buttonColors(containerColor = Blue500),
                shape   = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
            ) {
                Text("Confirm", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            }
        }
    }
}

// ─── Scrollable U-curve calendar ─────────────────────────────────────────────

@Composable
fun ScrollableUCurveCalendar(
    days:          List<CalendarDay>,
    selectedIdx:   Int,
    onDaySelected: (Int) -> Unit,
    arcDepth:      Dp = 20.dp,
    modifier:      Modifier = Modifier,
) {
    val listState    = rememberLazyListState()
    val snapBehavior = rememberSnapFlingBehavior(listState)
    val scope        = rememberCoroutineScope()
    val centreIdx    = rememberCentreIndex(listState)
    val density      = LocalDensity.current
    val arcDepthPx   = with(density) { arcDepth.toPx() }

    LaunchedEffect(centreIdx) { onDaySelected(centreIdx) }

    LaunchedEffect(Unit) {
        listState.scrollToItem(index = (selectedIdx - 1).coerceAtLeast(0))
    }

    val screenWidthDp   = LocalConfiguration.current.screenWidthDp.dp
    val sidePad         = (screenWidthDp - PILL_SIZE) / 2
    val containerHeight = PILL_SIZE + arcDepth

    LazyRow(
        state                 = listState,
        flingBehavior         = snapBehavior,
        contentPadding        = PaddingValues(horizontal = sidePad),
        horizontalArrangement = Arrangement.spacedBy(PILL_SPACING),
        verticalAlignment     = Alignment.Bottom,
        modifier              = modifier.height(containerHeight),
    ) {
        itemsIndexed(days) { i, day ->
            val rawPx = uYOffset(i, centreIdx, arcDepthPx, windowHalf = 4)
            val rawDp = with(density) { rawPx.toDp() }
            val animY by animateDpAsState(rawDp, tween(150), label = "y$i")

            DayPill(
                day        = day,
                isSelected = i == selectedIdx,
                yOffset    = animY,
                onClick    = {
                    onDaySelected(i)
                    scope.launch {
                        listState.animateScrollToItem((i - 1).coerceAtLeast(0))
                    }
                },
            )
        }
    }
}

// ─── Day Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun DayPill(
    day:        CalendarDay,
    isSelected: Boolean,
    yOffset:    Dp,
    onClick:    () -> Unit,
) {
    val bg: Color = if (isSelected) Blue500 else Color.Transparent
    val textColor: Color = when {
        isSelected                   -> Color.White
        day.style == DayStyle.Green  -> Green500
        day.style == DayStyle.Teal   -> Teal500
        day.style == DayStyle.Orange -> Orange500
        else                         -> TextMuted
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .offset(y = yOffset)
            .size(PILL_SIZE)
            .shadow(
                if (isSelected) 8.dp else 0.dp, CircleShape,
                ambientColor = Blue500.copy(.4f), spotColor = Blue500.copy(.4f)
            )
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick),
    ) {
        Text(
            text       = day.number.toString(),
            fontSize   = if (isSelected) 15.sp else 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color      = textColor,
        )
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageMedicationsScreen(
    viewModel: ManageMedicationViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current

    val today = remember { Calendar.getInstance() }
    val todayYear  = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay   = today.get(Calendar.DAY_OF_MONTH)

    var selectedMonth   by remember { mutableIntStateOf(todayMonth) }
    var selectedYear    by remember { mutableIntStateOf(todayYear) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val calendarDays by remember(selectedMonth, selectedYear) {
        derivedStateOf { generateCalendarDays(selectedYear, selectedMonth) }
    }

    val initialIdx = remember(calendarDays, todayDay, todayMonth, todayYear, selectedMonth, selectedYear) {
        if (selectedMonth == todayMonth && selectedYear == todayYear)
            (todayDay - 1).coerceIn(0, calendarDays.lastIndex)
        else 0
    }

    var selectedIdx by remember(selectedMonth, selectedYear) { mutableIntStateOf(initialIdx) }

    val selected = calendarDays.getOrNull(selectedIdx) ?: calendarDays.first()

    val loggedMedicines by viewModel.loggedMedicines.collectAsState()
    val allMedicines    by viewModel.allMedicines.collectAsState()
    val loading         by viewModel.loading.collectAsState()

    LaunchedEffect(selectedIdx, selectedMonth, selectedYear) {
        val date = formatDate(selectedYear, selectedMonth, selected.number)
        viewModel.fetchByDate(date)
    }

    CommonAppBar(
        title = "Manage Medications",
        actions = {
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                horizontalArrangement = Arrangement.spacedBy(4.dp),
//                modifier = Modifier
//                    .clip(RoundedCornerShape(20.dp))
//                    .background(colors.btnDarkColor)
//                    .clickable { }
//                    .padding(horizontal = 10.dp, vertical = 5.dp),
//            ) {
//                Text("Prescription", style = AppTextStyles.style12WCN())
//                Icon(Icons.Rounded.Add, null, tint = Blue500, modifier = Modifier.size(13.dp))
//            }
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .padding(horizontal = 16.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 88.dp),
            ) {

                // ── White top card ─────────────────────────────────────────────
                Surface(
                    color           = colors.dashboardContainerColor,
                    shape           = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                    shadowElevation = 2.dp,
                    modifier        = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier            = Modifier.padding(top = 16.dp, bottom = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Spacer(Modifier.height(10.dp))

                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(selected.shortName, style = AppTextStyles.style14BCN())
                            Spacer(Modifier.width(8.dp))
                            SmallDot()
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text       = selected.number.toString(),
                                style      = AppTextStyles.style14BCN().copy(fontSize = 46.sp),
                                lineHeight = 46.sp,
                            )
                            Spacer(Modifier.width(8.dp))
                            SmallDot()
                            Spacer(Modifier.width(8.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { showMonthPicker = true }
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                            ) {
                                Text(MONTHS[selectedMonth].take(3), style = AppTextStyles.style14BCN())
                                Spacer(Modifier.width(2.dp))
                                Icon(
                                    Icons.Rounded.KeyboardArrowDown, "Pick month",
                                    tint     = TextSecondary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        ScrollableUCurveCalendar(
                            days          = calendarDays,
                            selectedIdx   = selectedIdx,
                            onDaySelected = { selectedIdx = it },
                            arcDepth      = 20.dp,
                            modifier      = Modifier.fillMaxWidth(),
                        )

                        Spacer(Modifier.height(4.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── Logged section ─────────────────────────────────────────────
                SectionLabel("Logged")
                Spacer(Modifier.height(8.dp))
                if (loading) {
                    Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    }
                } else if (loggedMedicines.isEmpty()) {
                    SectionCard {
                        Text(
                            "No medicines logged for this date",
                            color    = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    SectionCard {
                        loggedMedicines.forEachIndexed { i, med ->
                            LoggedRow(med)
                            if (i < loggedMedicines.lastIndex)
                                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ── All Medications section ────────────────────────────────────
                SectionLabel("Your Medications (${allMedicines.size.toString().padStart(2, '0')})")
                Spacer(Modifier.height(8.dp))
                if (allMedicines.isEmpty() && !loading) {
                    SectionCard {
                        Text(
                            "No medications found",
                            color    = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                } else {
                    SectionCard {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(vertical = 10.dp),
                        ) {
                            allMedicines.forEach { MedChip(it) }
                        }
                    }
                }
            }

//            // FAB
//            FloatingActionButton(
//                onClick        = { navController.navigate(Routes.ADDMEDICINEREMINDER) },
//                containerColor = Blue500,
//                contentColor   = Color.White,
//                shape          = CircleShape,
//                modifier       = Modifier
//                    .align(Alignment.BottomEnd)
//                    .padding(20.dp)
//            ) {
//                Icon(Icons.Rounded.Add, "Add")
//            }
        }
    }

    if (showMonthPicker) {
        MonthPickerSheet(
            selectedMonthIndex  = selectedMonth,
            selectedYear        = selectedYear,
            onMonthYearSelected = { m, y ->
                selectedMonth = m
                selectedYear  = y
            },
            onDismiss = { showMonthPicker = false },
        )
    }
}

// ─── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(text = text, style = AppTextStyles.style14BCN())
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    val colors = LocalMyColorScheme.current
    Surface(
        color           = colors.dashboardContainerColor,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier        = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            content()
        }
    }
}

@Composable
private fun LoggedRow(med: LoggedMedicine) {
    val timeFormatted = try {
        val parts = med.takenDateTime.split("T")
        if (parts.size == 2) {
            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toInt()
            val min  = timeParts[1]
            val amPm = if (hour < 12) "AM" else "PM"
            val h12  = if (hour % 12 == 0) 12 else hour % 12
            "$h12:$min $amPm"
        } else med.takenDateTime
    } catch (e: Exception) { med.takenDateTime }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column {
            Text(med.medicineName.trim(), style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(2.dp))
            Text(
                "${med.dosageType} • $timeFormatted",
                style = AppTextStyles.style12GCN()
            )
        }
        Icon(Icons.Rounded.Check, null, tint = Green500, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MedChip(med: AllMedicine) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(70.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(BgGray),
        ) {
            Text("💊", fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(med.medicineName.trim(), style = AppTextStyles.style12BCN())
        Text(med.dosageType, style = AppTextStyles.style12GCN())
    }
}

@Composable
private fun SmallDot() {
    Box(
        modifier = Modifier
            .size(4.dp)
            .clip(CircleShape)
            .background(TextMuted)
    )
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ManageMedicationsPreview() {
    MaterialTheme {
        ManageMedicationsScreen()
    }
}