package com.critetiontech.ctvitalio.ui.screens

import android.content.Intent
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.EventAvailable
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tag
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.ui.components.RoundBackTopBar
import com.critetiontech.ctvitalio.ui.feedback.AnimatedCheckMark
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.successPulse
import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.CommonButton
import com.critetiontech.myapplication.utils.LocalNavController
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SuccessGreen = Color(0xFF16A34A)

/** Shown once an appointment is booked: what, when and where, plus what to do next. */
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BookingDetailsScreen(bookingDetails: BookingDetails?) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current

    val details = bookingDetails
    val date = remember(details?.onDate) { parseBookingDate(details?.onDate.orEmpty()) }
    val time = details?.onTime.orEmpty()
    val minutes = details?.slotMinutes ?: 0
    val virtual = details?.mode == "Virtual"

    fun goHome() {
        navController.navigate(Routes.DASHBOARD) {
            popUpTo(Routes.DASHBOARD) { inclusive = false }
            launchSingleTop = true
        }
    }
    // The booking flow is gone from the back stack, so back leads home.
    BackHandler { goHome() }

    Column(Modifier.fillMaxSize().background(colors.dashboardContainerColor)) {
        RoundBackTopBar(title = "Booking Details", onBack = { goHome() })

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))
            SuccessBadge()
            Spacer(Modifier.height(18.dp))
            Text("Appointment Booked!", color = colors.textDarkColor, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your appointment with ${details?.pName?.ifEmpty { null } ?: "the doctor"} is confirmed.",
                color = colors.textGreyColor,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))
            AnimatedEnter(index = 0) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    WhenTile(
                        icon = Icons.Rounded.CalendarMonth,
                        label = "Date",
                        value = date?.format(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.getDefault()))
                            ?: details?.onDate.orEmpty().ifEmpty { "—" },
                        modifier = Modifier.weight(1f),
                    )
                    WhenTile(
                        icon = Icons.Rounded.Schedule,
                        label = when {
                            minutes > 0 -> "Time · $minutes min"
                            !details?.shiftName.isNullOrEmpty() -> "Shift · ${details!!.shiftName}"
                            else -> "Time"
                        },
                        value = details?.timeRange().orEmpty().ifEmpty { "—" },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            AnimatedEnter(index = 1) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.dashboardBackgroundColor)
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        DoctorPhoto(
                            name = details?.pName.orEmpty(),
                            url = null,
                            initialsSize = 18.sp,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(details?.pName.orEmpty(), color = colors.textDarkColor, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            if (!details?.qly.isNullOrEmpty()) {
                                Text(details!!.qly, color = colors.textGreyColor, fontSize = 13.sp)
                            }
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 14.dp), thickness = 1.dp, color = Color(0xFFE6EAF0))
                    DetailLine(
                        icon = if (virtual) Icons.Rounded.Videocam else Icons.Rounded.LocalHospital,
                        label = details?.mode?.ifEmpty { null } ?: "Walk-in",
                        value = details?.atHospital?.ifEmpty { null }?.let { "$it Department" }
                            ?: if (virtual) "Online consultation" else "In-clinic visit",
                    )
                    details?.visitNo?.let {
                        Spacer(Modifier.height(12.dp))
                        DetailLine(icon = Icons.Rounded.Tag, label = "Visit No.", value = it)
                    }
                    details?.queueNo?.let {
                        Spacer(Modifier.height(12.dp))
                        DetailLine(icon = Icons.Rounded.ConfirmationNumber, label = "Token No.", value = it.toString())
                    }
                }
            }

            if (date != null && time.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                AddToCalendarPill {
                    val start = runCatching {
                        val (h, m) = time.split(":").map { it.toInt() }
                        date.atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                    }.getOrNull()
                    if (start != null) {
                        val intent = Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.Events.TITLE, "Appointment with ${details?.pName.orEmpty()}")
                            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
                            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + (if (minutes > 0) minutes else 30) * 60_000L)
                        runCatching { context.startActivity(intent) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 12.dp, top = 8.dp)
        ) {
            CommonButton(
                text = "View My Appointments",
                cornerRadius = 14.dp,
                containerColor = colors.primaryBlueColor,
                modifier = Modifier.height(54.dp),
                onClick = {
                    navController.navigate(Routes.APPOINTMENTS) {
                        popUpTo(Routes.DASHBOARD) { inclusive = false }
                    }
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Back to Home",
                color = colors.primaryBlueColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .bounceClick { goHome() }
                    .padding(vertical = 12.dp),
            )
        }
    }
}

/** Green circle that pops in and draws its tick. */
@Composable
private fun SuccessBadge() {
    var done by remember { mutableStateOf(false) }
    val scale = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        done = true
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(112.dp)
            .scale(scale.value)
            .clip(CircleShape)
            .background(SuccessGreen.copy(alpha = 0.12f)),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(76.dp)
                .successPulse(done, SuccessGreen)
                .clip(CircleShape)
                .background(SuccessGreen),
        ) {
            AnimatedCheckMark(checked = done, color = Color.White, strokeWidth = 4.dp, modifier = Modifier.size(38.dp))
        }
    }
}

@Composable
private fun WhenTile(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    val colors = LocalMyColorScheme.current
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.primaryBlueColor.copy(alpha = 0.08f))
            .padding(14.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(colors.dashboardContainerColor),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(10.dp))
        Text(label, color = colors.textGreyColor, fontSize = 12.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DetailLine(icon: ImageVector, label: String, value: String) {
    val colors = LocalMyColorScheme.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(colors.dashboardContainerColor),
        ) {
            Icon(icon, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = colors.textGreyColor, fontSize = 12.sp)
            Text(value, color = colors.textDarkColor, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AddToCalendarPill(onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val shape = RoundedCornerShape(50)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, colors.primaryBlueColor.copy(alpha = 0.35f), shape)
            .bounceClick(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Icon(Icons.Rounded.EventAvailable, contentDescription = null, tint = colors.primaryBlueColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Add to Calendar", color = colors.primaryBlueColor, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}
