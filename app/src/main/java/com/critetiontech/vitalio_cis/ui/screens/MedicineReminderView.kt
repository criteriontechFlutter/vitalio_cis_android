package com.critetiontech.ctvitalio.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.model.MedicineItem
import com.critetiontech.ctvitalio.model.MedicinePeriod
import com.critetiontech.ctvitalio.ui.components.CommonAppBar
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.viewmodel.MedicineReminderViewModel
import com.critetiontech.ctvitalio.viewmodel.ToastData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

// --- Status sealed class ------------------------------------------------------

private sealed class MedicineStatus {
    object Taken   : MedicineStatus()
    object Skipped : MedicineStatus()
    object TakeNow : MedicineStatus()
    data class Upcoming(val timeLeft: String) : MedicineStatus()
}

private fun resolveStatus(item: MedicineItem): MedicineStatus {
    if (item.isTaken && item.takenDateTime != "1900-01-01T00:00:00") return MedicineStatus.Taken
    if (item.isSkipped) return MedicineStatus.Skipped
    return try {
        val scheduled = LocalDateTime.parse(item.scheduledDateTime)
        val now = LocalDateTime.now()
        if (now >= scheduled) {
            MedicineStatus.TakeNow
        } else {
            val mins = ChronoUnit.MINUTES.between(now, scheduled)
            val label = when {
                mins < 60       -> "in ${mins}m"
                mins % 60 == 0L -> "in ${mins / 60}h"
                else            -> "in ${mins / 60}h ${mins % 60}m"
            }
            MedicineStatus.Upcoming(label)
        }
    } catch (e: Exception) {
        MedicineStatus.Upcoming("")
    }
}

private fun formatScheduledTime(dt: String): String = try {
    LocalDateTime.parse(dt).format(DateTimeFormatter.ofPattern("hh:mm a"))
} catch (e: Exception) { dt }

// --- Main Screen --------------------------------------------------------------

@Composable
fun MedicineReminderScreen(
    viewModel: MedicineReminderViewModel = viewModel(),
    triggerReload: Boolean = false
) {
    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current

    val periods by viewModel.periods.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val toast by viewModel.toast.collectAsState()

    var showToast by remember { mutableStateOf(false) }
    var currentToast by remember { mutableStateOf<ToastData?>(null) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.fetchMedicineIntake()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(triggerReload) {
        if (triggerReload) viewModel.fetchMedicineIntake()
    }

    LaunchedEffect(toast) {
        toast?.let {
            currentToast = it
            showToast = true
            delay(3000)
            showToast = false
            viewModel.clearToast()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CommonAppBar(
            title = "Medicine Reminder",
            actions = {
                TextButton(onClick = { navController.navigate(Routes.MANAGEMEDICAIONS) }) {
                    Text(
                        "View Log",
                        color = colors.primaryBlueColor,
                        style = AppTextStyles.style14BCN(),
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Rounded.ChevronRight, null,
                        tint = colors.primaryBlueColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        ) {
            var isRefreshing by remember { mutableStateOf(false) }
            val coroutineScope = rememberCoroutineScope()
            @OptIn(ExperimentalMaterial3Api::class)
            androidx.compose.material3.pulltorefresh.PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        viewModel.fetchMedicineIntake()
                        delay(1000)
                        isRefreshing = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor)
            ) {
                when {
                    loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            repeat(3) { MedicinePeriodShimmer() }
                        }
                    }
                    periods.isEmpty() -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Rounded.Medication, null,
                                tint = colors.primaryBlueColor.copy(.3f),
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "No medicines scheduled for today",
                                style = AppTextStyles.style14BCN(),
                                color = colors.textGreyColor,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    else -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            periods.forEachIndexed { idx, period ->
                                MedicinePeriodSection(period, viewModel)
                                if (idx < periods.lastIndex)
                                    HorizontalDivider(color = colors.borderGreyLightColor.copy(.4f))
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        // Toast snackbar
        AnimatedVisibility(
            visible = showToast,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            currentToast?.let { t ->
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (t.isSuccess) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                    ),
                    elevation = CardDefaults.cardElevation(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (t.isSuccess) Icons.Rounded.CheckCircle else Icons.Rounded.Error,
                            null, tint = Color.White, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(t.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(t.message, color = Color.White.copy(.85f), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// --- Period Section -----------------------------------------------------------

@Composable
fun MedicinePeriodSection(period: MedicinePeriod, viewModel: MedicineReminderViewModel) {
    val iconRes = when (period.dayPeriod.lowercase()) {
        "morning"   -> R.drawable.morning_medicine
        "afternoon" -> R.drawable.afternoon_medicine
        "night"     -> R.drawable.night_medicine
        else        -> R.drawable.morning_medicine
    }

    Row(verticalAlignment = Alignment.Top) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(72.dp)
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                period.dayPeriod,
                textAlign = TextAlign.Center,
                color = LocalMyColorScheme.current.textGreyColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(Modifier.width(10.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            period.medicineData.forEach { item ->
                MedicineCard(item, viewModel)
            }
        }
    }
}

// --- Medicine Card ------------------------------------------------------------

@Composable
fun MedicineCard(item: MedicineItem, viewModel: MedicineReminderViewModel) {
    val colors  = LocalMyColorScheme.current
    val status  = remember(item) { resolveStatus(item) }
    val timeFmt = remember(item.scheduledDateTime) { formatScheduledTime(item.scheduledDateTime) }

    var showConfirm by remember { mutableStateOf(false) }

    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            icon = {
                Icon(
                    Icons.Rounded.Medication, null,
                    tint = colors.primaryBlueColor,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Take Medicine", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to mark ${item.medicineName.trim()} as taken?",
                    style = AppTextStyles.style14BCN(),
                    color = colors.textGreyColor
                )
            },
            confirmButton = {
                Button(
                    onClick = { showConfirm = false; viewModel.markIntake(item.id, item.scheduledDateTime) },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primaryBlueColor),
                    shape = RoundedCornerShape(10.dp)
                ) { Text("Yes, Take") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Cancel", color = colors.textGreyColor)
                }
            },
            containerColor = colors.dashboardContainerColor,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Surface(
        color = colors.dashboardContainerColor,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 1.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        item.medicineName.trim(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colors.textDarkColor
                    )
                    val brand = item.brandName.trim()
                    if (brand.isNotEmpty() && !brand.equals(item.medicineName.trim(), ignoreCase = true)) {
                        Text(brand, fontSize = 12.sp, color = colors.textGreyColor)
                    }
                }
                Spacer(Modifier.width(8.dp))
                StatusBadge(status, onClick = { showConfirm = true })
            }

            Spacer(Modifier.height(8.dp))

            val dosageText = "${item.dosageStrength.toInt()} ${item.unit} • ${item.instructions.trim()}"
            if (item.dosageStrength > 0 || item.instructions.trim().isNotEmpty()) {
                Text(
                    dosageText,
                    fontSize = 12.sp,
                    color = colors.textGreyColor,
                    maxLines = 2
                )
                Spacer(Modifier.height(6.dp))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Schedule, null,
                    tint = colors.textGreyColor,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(timeFmt, fontSize = 12.sp, color = colors.textGreyColor)

                if (status is MedicineStatus.Upcoming && status.timeLeft.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .background(colors.primaryBlueColor.copy(.08f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            status.timeLeft,
                            fontSize = 11.sp,
                            color = colors.primaryBlueColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// --- Status Badge -------------------------------------------------------------

@Composable
private fun StatusBadge(status: MedicineStatus, onClick: () -> Unit) {
    when (status) {
        is MedicineStatus.Taken -> StatusTag(Icons.Rounded.CheckCircle, "Taken",    Color(0xFF1564ED))
        is MedicineStatus.Skipped -> StatusTag(Icons.Rounded.Cancel,     "Skipped",  Color(0xFFE65100))
        is MedicineStatus.TakeNow -> {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF2E7D32).copy(.1f))
                    .clickable(onClick = onClick)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Take Now", color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }
            }
        }
        is MedicineStatus.Upcoming -> StatusTag(Icons.Rounded.Schedule, "Upcoming", Color(0xFF6A1B9A))
    }
}

@Composable
private fun StatusTag(icon: ImageVector, text: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(.1f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(5.dp))
        Text(text, color = color, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
    }
}

// --- Shimmer loading ----------------------------------------------------------

@Composable
private fun shimmerBrush(): Brush {
    val colors = LocalMyColorScheme.current
    val shimmerColors = listOf(
        colors.dashboardContainerColor,
        colors.borderGreyLightColor.copy(.3f),
        colors.dashboardContainerColor
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "shimmerX"
    )
    return Brush.linearGradient(shimmerColors, start = Offset(x, 0f), end = Offset(x + 600f, 0f))
}

@Composable
private fun MedicinePeriodShimmer() {
    val brush = shimmerBrush()
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.Top) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(brush))
            Spacer(Modifier.height(4.dp))
            Box(Modifier.width(48.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            repeat(2) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.dashboardContainerColor,
                    shadowElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Box(Modifier.width(140.dp).height(15.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                                Spacer(Modifier.height(6.dp))
                                Box(Modifier.width(90.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                            }
                            Box(Modifier.width(70.dp).height(26.dp).clip(RoundedCornerShape(14.dp)).background(brush))
                        }
                        Spacer(Modifier.height(10.dp))
                        Box(Modifier.width(160.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.width(80.dp).height(11.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    }
                }
            }
        }
    }
}
