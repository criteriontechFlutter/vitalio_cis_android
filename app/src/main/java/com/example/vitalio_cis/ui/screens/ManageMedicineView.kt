package com.example.vitalio_cis.ui.screens

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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
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
private val WarnBg        = Color(0xFFFFF3E0)
private val WarnText      = Color(0xFFE65100)

// ─── Data models ─────────────────────────────────────────────────────────────

data class CalendarDay(val number: Int, val shortName: String, val style: DayStyle)
enum class DayStyle { Muted, Green, Teal, Orange, Active }

data class LoggedMed(val name: String, val time: String)
data class Medication(val name: String, val type: String, val emoji: String)
data class DrugInteraction(val pair: String, val severity: String, val note: String)

// ─── Months ───────────────────────────────────────────────────────────────────

private val MONTHS = listOf(
    "January","February","March","April","May","June",
    "July","August","September","October","November","December"
)

// ─── Sample data ─────────────────────────────────────────────────────────────

private val calendarDays = listOf(
    CalendarDay(1,  "Mon", DayStyle.Muted),
    CalendarDay(2,  "Tue", DayStyle.Muted),
    CalendarDay(3,  "Wed", DayStyle.Muted),
    CalendarDay(4,  "Thu", DayStyle.Muted),
    CalendarDay(5,  "Fri", DayStyle.Muted),
    CalendarDay(6,  "Sat", DayStyle.Muted),
    CalendarDay(7,  "Sun", DayStyle.Muted),
    CalendarDay(8,  "Mon", DayStyle.Muted),
    CalendarDay(9,  "Tue", DayStyle.Muted),
    CalendarDay(10, "Wed", DayStyle.Muted),
    CalendarDay(11, "Thu", DayStyle.Muted),
    CalendarDay(12, "Fri", DayStyle.Muted),
    CalendarDay(13, "Sat", DayStyle.Green),
    CalendarDay(14, "Sun", DayStyle.Teal),
    CalendarDay(15, "Mon", DayStyle.Active),
    CalendarDay(16, "Tue", DayStyle.Orange),
    CalendarDay(17, "Wed", DayStyle.Muted),
    CalendarDay(18, "Thu", DayStyle.Muted),
    CalendarDay(19, "Fri", DayStyle.Muted),
    CalendarDay(20, "Sat", DayStyle.Muted),
    CalendarDay(21, "Sun", DayStyle.Muted),
    CalendarDay(22, "Mon", DayStyle.Muted),
    CalendarDay(23, "Tue", DayStyle.Muted),
    CalendarDay(24, "Wed", DayStyle.Muted),
    CalendarDay(25, "Thu", DayStyle.Muted),
    CalendarDay(26, "Fri", DayStyle.Muted),
    CalendarDay(27, "Sat", DayStyle.Muted),
    CalendarDay(28, "Sun", DayStyle.Muted),
    CalendarDay(29, "Mon", DayStyle.Muted),
    CalendarDay(30, "Tue", DayStyle.Muted),
)

private val loggedMeds = listOf(
    LoggedMed("Thyroxin",   "08:00 AM"),
    LoggedMed("Bilatop 40", "08:30 AM"),
)

private val medications = listOf(
    Medication("Thyroxine",  "Tablet", "💊"),
    Medication("Bilatop 40", "Tablet", "💊"),
    Medication("Lisinopril", "Tablet", "💊"),
    Medication("Vitamin",    "Syrup",  "🧴"),
)

private val interactions = listOf(
    DrugInteraction(
        pair     = "Thyroxine + Lisinopril",
        severity = "⚠ Mild interaction",
        note     = "Thyroid levels can slightly affect how your body responds to BP medicine. Monitor blood pressure regularly.",
    )
)

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
    val sheetState = rememberModalBottomSheetState( )
    val scope      = rememberCoroutineScope()

    // local state so user can browse before confirming
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

            // ── Year row ─────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier              = Modifier.fillMaxWidth(),
            ) {
                IconButton(onClick = { pickedYear-- }) {
                    Icon(
                        Icons.Rounded.Add, "Prev year",
                        tint = TextSecondary
                    )
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
                    Icon(
                        Icons.Rounded.Add, "Next year",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ── Month grid (3 columns × 4 rows) ──────────────────────────
            val rows = MONTHS.chunked(3)
            rows.forEach { rowMonths ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowMonths.forEach { month ->
                        val idx       = MONTHS.indexOf(month)
                        val isChosen  = idx == pickedMonth && pickedYear == selectedYear ||
                                idx == pickedMonth && pickedYear != selectedYear
                        val isPicked  = idx == pickedMonth

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPicked) Blue500
                                    else if (idx == selectedMonthIndex && pickedYear == selectedYear)
                                        BlueLight
                                    else Color.Transparent
                                )
                                .clickable { pickedMonth = idx },
                        ) {
                            Text(
                                text       = month.take(3),   // "Jan", "Feb" ...
                                fontSize   = 14.sp,
                                fontWeight = if (isPicked) FontWeight.Bold else FontWeight.Normal,
                                color      = when {
                                    isPicked                                              -> Color.White
                                    idx == selectedMonthIndex && pickedYear == selectedYear -> Blue500
                                    else                                                  -> TextPrimary
                                },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(8.dp))

            // ── Confirm button ────────────────────────────────────────────
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
                Text(
                    "Confirm",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = Color.White,
                )
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

    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val sidePad       = (screenWidthDp - PILL_SIZE) / 2
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
            val rawPx  = uYOffset(i, centreIdx, arcDepthPx, windowHalf = 4)
            val rawDp  = with(density) { rawPx.toDp() }
            val animY  by animateDpAsState(rawDp, tween(150), label = "y$i")

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
            .shadow(if (isSelected) 8.dp else 0.dp, CircleShape,
                ambientColor = Blue500.copy(.4f), spotColor = Blue500.copy(.4f))
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
fun ManageMedicationsScreen(onBackClick: () -> Unit = {}) {

    var selectedIdx     by remember { mutableIntStateOf(
        calendarDays.indexOfFirst { it.style == DayStyle.Active }.coerceAtLeast(0)
    )}
    var selectedMonth   by remember { mutableIntStateOf(8) }   // September = index 8
    var selectedYear    by remember { mutableIntStateOf(2025) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val selected = calendarDays[selectedIdx]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGray)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 88.dp),
        ) {

            // ── White top card ─────────────────────────────────────────────
            Surface(
                color           = CardWhite,
                shape           = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
                shadowElevation = 2.dp,
                modifier        = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier            = Modifier.padding(top = 16.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {

                    // Header
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back",
                                tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }

                        Text("Manage Medications",
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)

                        Row(
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(BlueLight)
                                .clickable { }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text("Prescription", fontSize = 12.sp,
                                fontWeight = FontWeight.Medium, color = Blue500)
                            Icon(Icons.Rounded.Add, null,
                                tint = Blue500, modifier = Modifier.size(13.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Big date row
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Text(selected.shortName, fontSize = 14.sp, color = TextSecondary)
                        Spacer(Modifier.width(8.dp))
                        SmallDot()
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text       = selected.number.toString(),
                            fontSize   = 46.sp,
                            fontWeight = FontWeight.Light,
                            color      = TextPrimary,
                            lineHeight = 46.sp,
                        )
                        Spacer(Modifier.width(8.dp))
                        SmallDot()
                        Spacer(Modifier.width(8.dp))

                        // ── Month + arrow (tap to open picker) ──────────────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { showMonthPicker = true }   // ← opens sheet
                                .padding(horizontal = 6.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text       = MONTHS[selectedMonth].take(3),
                                fontSize   = 14.sp,
                                color      = TextSecondary,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.width(2.dp))
                            Icon(
                                Icons.Rounded.KeyboardArrowDown, "Pick month",
                                tint     = TextSecondary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    // U-curve calendar
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

            SectionLabel("Logged")
            Spacer(Modifier.height(8.dp))
            SectionCard {
                loggedMeds.forEachIndexed { i, med ->
                    LoggedRow(med)
                    if (i < loggedMeds.lastIndex)
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionLabel("Your Medications (0${medications.size})")
            Spacer(Modifier.height(8.dp))
            SectionCard {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 10.dp),
                ) {
                    medications.forEach { MedChip(it) }
                }
            }

            Spacer(Modifier.height(16.dp))

            SectionLabel("Drug Interaction (0${interactions.size})")
            Spacer(Modifier.height(8.dp))
            SectionCard {
                interactions.forEach { InteractionCard(it) }
            }
        }

        // FAB
        FloatingActionButton(
            onClick        = { },
            containerColor = Blue500,
            contentColor   = Color.White,
            shape          = CircleShape,
            modifier       = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        ) {
            Icon(Icons.Rounded.Add, "Add")
        }
    }

    // ── Month Picker Bottom Sheet ──────────────────────────────────────────────
    if (showMonthPicker) {
        MonthPickerSheet(
            selectedMonthIndex   = selectedMonth,
            selectedYear         = selectedYear,
            onMonthYearSelected  = { m, y ->
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
    Text(
        text       = text,
        fontSize   = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color      = TextPrimary,
        modifier   = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        color           = CardWhite,
        shape           = RoundedCornerShape(16.dp),
        shadowElevation = 1.dp,
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)) {
            content()
        }
    }
}

@Composable
private fun LoggedRow(med: LoggedMed) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier              = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        Column {
            Text(med.name, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Add, null,
                    tint = TextSecondary, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(4.dp))
                Text(med.time, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Icon(Icons.Rounded.Check, null, tint = Green500, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MedChip(med: Medication) {
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
            Text(med.emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(med.name, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(med.type, fontSize = 11.sp, color = TextSecondary)
    }
}

@Composable
private fun InteractionCard(it: DrugInteraction) {
    Column(modifier = Modifier.padding(vertical = 12.dp)) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier              = Modifier.fillMaxWidth(),
        ) {
            Text(it.pair, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(WarnBg)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(it.severity, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = WarnText)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(it.note, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
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