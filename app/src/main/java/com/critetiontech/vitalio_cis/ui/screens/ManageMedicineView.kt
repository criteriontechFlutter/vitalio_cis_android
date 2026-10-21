package com.critetiontech.ctvitalio.ui.screens

import com.critetiontech.ctvitalio.ui.feedback.contentStateOf

import com.critetiontech.ctvitalio.ui.feedback.ContentStateCrossfade

import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter

import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticTextButton

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.AllMedicine
import com.critetiontech.ctvitalio.model.LoggedMedicine
import com.critetiontech.ctvitalio.reminder.AlarmSetup
import com.critetiontech.ctvitalio.reminder.ReminderSettings
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.ManageMedicationViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos

// ─── Colors ──────────────────────────────────────────────────────────────────

private val Blue500       = Color(0xFF2F7BF5)
private val BlueLight: Color @Composable get() = com.critetiontech.ctvitalio.ui.theme.softTint(Blue500, Color(0xFFE6EFFF))
private val Green500      = Color(0xFF2DBE7E)
private val TextSecondary = Color(0xFF8A90A0)
private val TextMuted     = Color(0xFFC0C5D0)
private val DividerColor: Color @Composable get() = if (com.critetiontech.ctvitalio.ui.theme.isAppDark) com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme.current.borderGreyLightColor else Color(0xFFEEF0F5)
private val CardWhite: Color @Composable get() = com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme.current.dashboardContainerColor
private val TextPrimary: Color @Composable get() = com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme.current.textDarkColor

// ─── Data models ─────────────────────────────────────────────────────────────

data class CalendarDay(val number: Int, val shortName: String)

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
        CalendarDay(day, DAY_NAMES[cal.get(Calendar.DAY_OF_WEEK) - 1])
    }
}

/** 0 = Sunday … 6 = Saturday, for the 1st of the month. */
private fun firstWeekdayOf(year: Int, month: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month, 1)
    return cal.get(Calendar.DAY_OF_WEEK) - 1
}

// yyyy-MM-dd, so plain string comparison orders dates correctly.
private fun formatDate(year: Int, month: Int, day: Int): String {
    val mm = (month + 1).toString().padStart(2, '0')
    val dd = day.toString().padStart(2, '0')
    return "$year-$mm-$dd"
}

// ─── Sizing ───────────────────────────────────────────────────────────────────

private val PILL_SIZE    = 38.dp
private val PILL_SPACING = 8.dp
private val ARC_DEPTH    = 20.dp
private val WEEKDAY_ROW  = 28.dp
private val GRID_CELL    = 44.dp

// ─── Centre-index helper ──────────────────────────────────────────────────────

private fun centreIndexOf(listState: LazyListState): Int {
    val info   = listState.layoutInfo
    val centre = (info.viewportStartOffset + info.viewportEndOffset) / 2f
    return info.visibleItemsInfo
        .minByOrNull { abs(it.offset + it.size / 2f - centre) }
        ?.index ?: listState.firstVisibleItemIndex
}

@Composable
private fun rememberCentreIndex(listState: LazyListState): Int =
    remember(listState) { derivedStateOf { centreIndexOf(listState) } }.value

// ─── U-curve Y offset ─────────────────────────────────────────────────────────

private fun uCurve(itemIndex: Int, centreIdx: Int, windowHalf: Int = 4): Float {
    val t = (abs(itemIndex - centreIdx).toFloat() / windowHalf).coerceIn(0f, 1f)
    return ((1.0 - cos(PI * t)) / 2.0).toFloat()
}

// ─── Curved-bottom card shape ────────────────────────────────────────────────

private class CurvedBottomShape(private val depth: Dp) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val d = with(density) { depth.toPx() }
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width, size.height - d)
            quadraticTo(size.width / 2f, size.height + d, 0f, size.height - d)
            close()
        }
        return Outline.Generic(path)
    }
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

    com.critetiontech.ctvitalio.ui.components.AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        title            = "Select month",
        icon             = Icons.Rounded.CalendarMonth,
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
                HapticIconButton(onClick = { pickedYear-- }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Prev year", tint = TextSecondary)
                }
                Text(
                    text       = pickedYear.toString(),
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextPrimary,
                    modifier   = Modifier.width(80.dp),
                    textAlign  = TextAlign.Center,
                )
                HapticIconButton(onClick = { pickedYear++ }) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Next year", tint = TextSecondary)
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

            HapticButton(
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

// ─── Expandable calendar header ──────────────────────────────────────────────

/**
 * Collapsed: U-curve day strip. Swipe down on the card to drag open the full
 * month grid (follows the finger, springs to the nearest state on release);
 * swipe up, tap the handle, or pick a date to collapse.
 */
@Composable
private fun ExpandableCalendarCard(
    year:          Int,
    month:         Int,
    days:          List<CalendarDay>,
    selectedIdx:   Int,
    ringProgress:  Float,
    isFuture:      (Int) -> Boolean,
    onDaySelected: (Int) -> Unit,
    onMonthClick:  () -> Unit,
) {
    val colors  = LocalMyColorScheme.current
    val density = LocalDensity.current
    val scope   = rememberCoroutineScope()

    val weeks        = (firstWeekdayOf(year, month) + days.size + 6) / 7
    val stripHeight  = PILL_SIZE + ARC_DEPTH
    val gridHeight   = WEEKDAY_ROW + GRID_CELL * weeks
    val dragRangePx  = with(density) { (gridHeight - stripHeight).toPx() }.coerceAtLeast(1f)

    var progress by remember { mutableFloatStateOf(0f) }
    var settleJob by remember { mutableStateOf<Job?>(null) }

    fun settleTo(target: Float, velocity: Float = 0f) {
        settleJob?.cancel()
        settleJob = scope.launch {
            animate(
                initialValue    = progress,
                targetValue     = target,
                initialVelocity = velocity / dragRangePx,
                animationSpec   = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow),
            ) { value, _ -> progress = value }
        }
    }

    val dragState = rememberDraggableState { delta ->
        progress = (progress + delta / dragRangePx).coerceIn(0f, 1f)
    }

    val selected = days.getOrNull(selectedIdx) ?: days.first()

    Surface(
        color           = colors.dashboardContainerColor,
        shape           = CurvedBottomShape(18.dp),
        shadowElevation = 2.dp,
        modifier        = Modifier
            .fillMaxWidth()
            .draggable(
                state         = dragState,
                orientation   = Orientation.Vertical,
                onDragStarted = { settleJob?.cancel() },
                onDragStopped = { velocity ->
                    val target = when {
                        velocity > 600f  -> 1f
                        velocity < -600f -> 0f
                        else             -> if (progress > 0.5f) 1f else 0f
                    }
                    settleTo(target, velocity)
                },
            ),
    ) {
        Column(
            modifier            = Modifier.padding(top = 8.dp, bottom = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Mon • 15 • Sep ⌄
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selected.shortName, style = AppTextStyles.style14BCN().copy(fontSize = 16.sp))
                Spacer(Modifier.width(10.dp))
                SmallDot()
                Spacer(Modifier.width(10.dp))
                AnimatedContent(
                    targetState  = selected.number,
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInVertically { it * dir / 2 } + fadeIn(tween(180))) togetherWith
                            (slideOutVertically { -it * dir / 2 } + fadeOut(tween(120)))
                    },
                    label = "dayNumber",
                ) { number ->
                    Text(
                        text       = number.toString(),
                        style      = AppTextStyles.style14BCN().copy(fontSize = 34.sp, fontWeight = FontWeight.Medium),
                        lineHeight = 40.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                SmallDot()
                Spacer(Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onMonthClick)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                ) {
                    Text(MONTHS[month].take(3), style = AppTextStyles.style14BCN().copy(fontSize = 16.sp))
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown, "Pick month",
                        tint     = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(lerp(stripHeight, gridHeight, progress))
                    .clipToBounds(),
            ) {
                // Strip stays composed so its scroll position survives expand/collapse.
                ScrollableUCurveCalendar(
                    days          = days,
                    selectedIdx   = selectedIdx,
                    onDaySelected = onDaySelected,
                    isFuture      = isFuture,
                    ringProgress  = ringProgress,
                    modifier      = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha        = (1f - progress * 2.2f).coerceIn(0f, 1f)
                            translationY = progress * 24.dp.toPx()
                        },
                )

                if (progress > 0f) {
                    MonthGrid(
                        year         = year,
                        month        = month,
                        days         = days,
                        selectedIdx  = selectedIdx,
                        ringProgress = ringProgress,
                        isFuture     = isFuture,
                        progress     = progress,
                        onPick       = { idx ->
                            onDaySelected(idx)
                            settleTo(0f)
                        },
                        modifier     = Modifier
                            .wrapContentHeight(align = Alignment.Top, unbounded = true)
                            .height(gridHeight),
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Drag handle — also toggles on tap.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { settleTo(if (progress > 0.5f) 0f else 1f) }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Box(
                    Modifier
                        .width(lerp(32.dp, 44.dp, progress))
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(TextMuted)
                )
            }
        }
    }
}

// ─── Full month grid ─────────────────────────────────────────────────────────

@Composable
private fun MonthGrid(
    year:         Int,
    month:        Int,
    days:         List<CalendarDay>,
    selectedIdx:  Int,
    ringProgress: Float,
    isFuture:     (Int) -> Boolean,
    progress:     Float,
    onPick:       (Int) -> Unit,
    modifier:     Modifier = Modifier,
) {
    val leading = firstWeekdayOf(year, month)
    val cells   = List(leading) { null } + days.indices.toList()
    val rows    = cells.chunked(7)

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(WEEKDAY_ROW)
                .graphicsLayer { alpha = ((progress - 0.2f) / 0.5f).coerceIn(0f, 1f) },
        ) {
            DAY_NAMES.forEach { name ->
                Text(
                    text      = name.take(1),
                    style     = AppTextStyles.style12GCN(),
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.weight(1f),
                )
            }
        }

        rows.forEachIndexed { r, row ->
            // Rows cascade in one after another as the card opens.
            val rowT = ((progress - 0.25f - r * 0.08f) / 0.45f).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(GRID_CELL)
                    .graphicsLayer {
                        alpha        = rowT
                        translationY = (1f - rowT) * -12.dp.toPx()
                    },
            ) {
                for (c in 0 until 7) {
                    val idx = row.getOrNull(c)
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f)) {
                        if (idx != null) {
                            DayPill(
                                day          = days[idx],
                                isSelected   = idx == selectedIdx,
                                isFuture     = isFuture(days[idx].number),
                                ringProgress = ringProgress,
                                onClick      = { onPick(idx) },
                            )
                        }
                    }
                }
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
    modifier:      Modifier = Modifier,
    isFuture:      (Int) -> Boolean = { false },
    ringProgress:  Float = 1f,
    arcDepth:      Dp = ARC_DEPTH,
) {
    BoxWithConstraints(modifier) {
        // With side padding of (width - pill) / 2, "first visible item" == centred item.
        val listState    = rememberLazyListState(initialFirstVisibleItemIndex = selectedIdx)
        val snapBehavior = rememberSnapFlingBehavior(listState)
        val centreIdx    = rememberCentreIndex(listState)
        val onSelect     by rememberUpdatedState(onDaySelected)
        val sidePad      = ((maxWidth - PILL_SIZE) / 2).coerceAtLeast(0.dp)

        // Select whatever the user scrolled to, once the scroll settles.
        LaunchedEffect(listState) {
            snapshotFlow { listState.isScrollInProgress }
                .drop(1)
                .filter { !it }
                .collect { onSelect(centreIndexOf(listState)) }
        }

        // Follow selection changes made elsewhere (tap, month grid).
        LaunchedEffect(selectedIdx) {
            if (centreIndexOf(listState) != selectedIdx) listState.animateScrollToItem(selectedIdx)
        }

        LazyRow(
            state                 = listState,
            flingBehavior         = snapBehavior,
            contentPadding        = PaddingValues(horizontal = sidePad),
            horizontalArrangement = Arrangement.spacedBy(PILL_SPACING),
            verticalAlignment     = Alignment.Bottom,
            modifier              = Modifier.height(PILL_SIZE + arcDepth),
        ) {
            itemsIndexed(days) { i, day ->
                val curve by animateFloatAsState(uCurve(i, centreIdx), tween(180), label = "u$i")

                DayPill(
                    day          = day,
                    isSelected   = i == selectedIdx,
                    isFuture     = isFuture(day.number),
                    ringProgress = ringProgress,
                    onClick      = { onDaySelected(i) },
                    modifier     = Modifier
                        .offset(y = -arcDepth * curve)
                        .graphicsLayer { alpha = 1f - 0.65f * curve },
                )
            }
        }
    }
}

// ─── Day Pill ─────────────────────────────────────────────────────────────────

@Composable
private fun DayPill(
    day:          CalendarDay,
    isSelected:   Boolean,
    isFuture:     Boolean,
    ringProgress: Float,
    onClick:      () -> Unit,
    modifier:     Modifier = Modifier,
) {
    val colors = LocalMyColorScheme.current
    val sweep by animateFloatAsState(
        targetValue   = if (isSelected) ringProgress.coerceIn(0f, 1f) else 0f,
        animationSpec = tween(450, easing = FastOutSlowInEasing),
        label         = "ring",
    )
    val textColor = when {
        isSelected -> Blue500
        isFuture   -> TextMuted
        else       -> colors.textDarkColor
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(PILL_SIZE)
            .clip(CircleShape)
            .clickable(onClick = onClick),
    ) {
        if (isSelected) {
            val ringTrack = BlueLight
            Canvas(Modifier.matchParentSize().padding(2.dp)) {
                val stroke = 3.dp.toPx()
                drawCircle(ringTrack, style = Stroke(stroke))
                drawArc(
                    color      = Blue500,
                    startAngle = -90f,
                    sweepAngle = 360f * sweep,
                    useCenter  = false,
                    style      = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
        }
        Text(
            text       = day.number.toString(),
            fontSize   = if (isSelected) 15.sp else 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
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
    val context = LocalContext.current

    val today = remember { Calendar.getInstance() }
    val todayYear  = today.get(Calendar.YEAR)
    val todayMonth = today.get(Calendar.MONTH)
    val todayDay   = today.get(Calendar.DAY_OF_MONTH)
    val todayKey   = formatDate(todayYear, todayMonth, todayDay)

    var selectedMonth   by remember { mutableIntStateOf(todayMonth) }
    var selectedYear    by remember { mutableIntStateOf(todayYear) }
    var showMonthPicker by remember { mutableStateOf(false) }

    val calendarDays = remember(selectedMonth, selectedYear) {
        generateCalendarDays(selectedYear, selectedMonth)
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

    // Share of medications logged on the selected day — drives the ring around it.
    val ringProgress = if (allMedicines.isEmpty()) 0f
        else loggedMedicines.distinctBy { it.medicineId }.size.toFloat() / allMedicines.size

    LaunchedEffect(selectedIdx, selectedMonth, selectedYear) {
        val date = formatDate(selectedYear, selectedMonth, selected.number)
        viewModel.fetchByDate(context, date)
    }

    CommonAppBar(
        title = "Manage Medications",
        actions = {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(end = 8.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.dashboardContainerColor)
                    .clickable { navController.navigate(Routes.PRESCRIPTION) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text("Prescription", style = AppTextStyles.style12BCN(color = Blue500).copy(fontWeight = FontWeight.SemiBold))
                Icon(Icons.AutoMirrored.Outlined.ReceiptLong, null, tint = Blue500, modifier = Modifier.size(16.dp))
            }
            HapticIconButton(
                onClick  = { navController.navigate(Routes.MEDICINE_REMINDER_SETTINGS) },
                modifier = Modifier.padding(end = 4.dp),
            ) {
                Icon(Icons.Rounded.Settings, "Reminder settings", tint = Blue500)
            }
        }
    ) {
        var isRefreshing by remember { mutableStateOf(false) }
        val coroutineScope = rememberCoroutineScope()
        @OptIn(ExperimentalMaterial3Api::class)
        com.critetiontech.ctvitalio.ui.feedback.StretchPullRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                coroutineScope.launch {
                    val date = formatDate(selectedYear, selectedMonth, selected.number)
                    viewModel.fetchByDate(context, date)
                    kotlinx.coroutines.delay(1000)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor),
            ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp),
            ) {

                // Fresh strip state per month so it starts centred on the right day.
                key(selectedYear, selectedMonth) {
                    ExpandableCalendarCard(
                        year          = selectedYear,
                        month         = selectedMonth,
                        days          = calendarDays,
                        selectedIdx   = selectedIdx,
                        ringProgress  = ringProgress,
                        isFuture      = { day -> formatDate(selectedYear, selectedMonth, day) > todayKey },
                        onDaySelected = { selectedIdx = it },
                        onMonthClick  = { showMonthPicker = true },
                    )
                }

                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(20.dp))

                    // ── Logged section ─────────────────────────────────────────
                    SectionLabel("Logged")
                    Spacer(Modifier.height(10.dp))
                    ContentStateCrossfade(
                        state = contentStateOf(loading = loading, empty = loggedMedicines.isEmpty()),
                        loading = {
                            LoggedSectionShimmer()
                        },
                        empty = {
                            SectionCard {
                                                        Text(
                                                            "No medicines logged for this date",
                                                            color    = TextSecondary,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(vertical = 16.dp)
                                                        )
                                                    }
                        },
                    ) {
                        SectionCard {
                                                    loggedMedicines.forEachIndexed { i, med ->
                                                        AnimatedEnter(index = i) {
                                                            LoggedRow(med)
                                                            if (i < loggedMedicines.lastIndex)
                                                                HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                                                        }
                                                    }
                                                }
                    }

                    Spacer(Modifier.height(20.dp))

                    // ── All Medications section ────────────────────────────────
                    SectionLabel(
                        if (loading) "Your Medications"
                        else "Your Medications (${allMedicines.size.toString().padStart(2, '0')})"
                    )
                    Spacer(Modifier.height(10.dp))
                    ContentStateCrossfade(
                        state = contentStateOf(loading = loading, empty = allMedicines.isEmpty()),
                        loading = {
                            MedChipsSectionShimmer()
                        },
                        empty = {
                            SectionCard {
                                                        Text(
                                                            "No medications found",
                                                            color    = TextSecondary,
                                                            fontSize = 13.sp,
                                                            modifier = Modifier.padding(vertical = 16.dp)
                                                        )
                                                    }
                        },
                    ) {
                        SectionCard {
                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                        modifier = Modifier
                                                            .horizontalScroll(rememberScrollState())
                                                            .padding(vertical = 14.dp),
                                                    ) {
                                                        allMedicines.forEach { MedChip(it) }
                                                    }
                                                }
                    }
                }
            }

            // FAB
            FloatingActionButton(
                onClick        = { navController.navigate(Routes.ADDMEDICINEREMINDER) },
                containerColor = Blue500,
                contentColor   = Color.White,
                shape          = CircleShape,
                modifier       = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .size(56.dp)
            ) {
                Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(28.dp))
            }
            }
        }
    }

    // One-time nudge: without these permissions alarms ring late or only as a small notification.
    var showAlarmSetup by remember {
        val settings = ReminderSettings(context)
        mutableStateOf(!settings.setupPromptShown && !AlarmSetup.isComplete(context))
    }
    if (showAlarmSetup) {
        com.critetiontech.ctvitalio.ui.components.AppDialog(
            title = "Set up medicine alarms",
            message = "Allow a few permissions so your medicine alarm rings on time and opens full screen, even while you're using your phone.",
            icon = Icons.Rounded.Alarm,
            confirmText = "Set up",
            dismissText = "Later",
            onDismiss = {
                ReminderSettings(context).setupPromptShown = true
                showAlarmSetup = false
            },
            onConfirm = {
                ReminderSettings(context).setupPromptShown = true
                showAlarmSetup = false
                navController.navigate(Routes.MEDICINE_REMINDER_SETTINGS)
            },
        )
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

// ─── Shimmer ──────────────────────────────────────────────────────────────────

@Composable
private fun medShimmerBrush(): Brush {
    val colors = LocalMyColorScheme.current
    val shimmerColors = listOf(
        colors.dashboardContainerColor,
        colors.borderGreyLightColor.copy(.3f),
        colors.dashboardContainerColor
    )
    val transition = rememberInfiniteTransition(label = "medShimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f, targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "medShimmerX"
    )
    return Brush.linearGradient(shimmerColors, start = Offset(translateX, 0f), end = Offset(translateX + 600f, 0f))
}

@Composable
private fun LoggedSectionShimmer() {
    val brush = medShimmerBrush()
    SectionCard {
        repeat(3) { i ->
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            ) {
                Column {
                    Box(modifier = Modifier.width(140.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.width(100.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
                Box(modifier = Modifier.size(20.dp).clip(CircleShape).background(brush))
            }
            if (i < 2) HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun MedChipsSectionShimmer() {
    val brush = medShimmerBrush()
    SectionCard {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier              = Modifier.padding(vertical = 14.dp),
        ) {
            repeat(4) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.width(76.dp),
                ) {
                    Box(modifier = Modifier.size(52.dp).clip(CircleShape).background(brush))
                    Spacer(Modifier.height(8.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
            }
        }
    }
}

// ─── Reusable composables ─────────────────────────────────────────────────────

@Composable
private fun SectionLabel(text: String) {
    Text(
        text  = text,
        style = AppTextStyles.style14BCN().copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    )
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
    val raw = med.takenDateTime.orEmpty()
    val timeFormatted = try {
        val parts = raw.split("T")
        if (parts.size == 2) {
            val timeParts = parts[1].split(":")
            val hour = timeParts[0].toInt()
            val min  = timeParts[1]
            val amPm = if (hour < 12) "AM" else "PM"
            val h12  = if (hour % 12 == 0) 12 else hour % 12
            "${h12.toString().padStart(2, '0')}:$min $amPm"
        } else raw
    } catch (e: Exception) { raw }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                med.medicineName.orEmpty().trim(),
                style = AppTextStyles.style14BCN().copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
            )
            if (timeFormatted.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(timeFormatted, style = AppTextStyles.style12GCN())
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Rounded.Check, "Taken", tint = Green500, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun MedChip(med: AllMedicine) {
    val dosageType = med.dosageType.orEmpty()
    val isLiquid = dosageType.contains("syrup", true) || dosageType.contains("liquid", true)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier            = Modifier.width(76.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(BlueLight),
        ) {
            Icon(
                if (isLiquid) Icons.Rounded.LocalDrink else Icons.Rounded.Medication,
                null,
                tint     = Blue500,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            med.medicineName.orEmpty().trim(),
            style     = AppTextStyles.style12BCN().copy(fontWeight = FontWeight.SemiBold),
            maxLines  = 1,
            overflow  = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            dosageType,
            style    = AppTextStyles.style12GCN().copy(fontSize = 11.sp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
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
