package com.example.vitalio_cis.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import coil.compose.AsyncImage
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.NavigationManager.navController
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.SlotData
import com.example.vitalio_cis.ui.components.CommonAppBar
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.CommonButton
import com.example.vitalio_cis.viewmodel.DoctorDetailsViewModel
import com.google.gson.Gson
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@SuppressLint("UnrememberedGetBackStackEntry")
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DoctorDetailsScreen(
    doctorId: String,
    days: String,

      viewModel: DoctorDetailsViewModel = viewModel()
 ) {
    val navController = LocalNavController.current


    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val doctor by viewModel.doctor.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.setDoctorId( doctorId)




        viewModel.getDoctorProfile(context,)
        val today = LocalDate.now()

        viewModel.fetchAvailableSlots(
            context,
            "${today.year}/${today.monthValue}/${today.dayOfMonth}"
        )
        viewModel.setSlotDate("${today.year}/${today.monthValue}/${today.dayOfMonth}")

    }

    CommonAppBar(
        title = "Doctor’s Details",
    ) {

        val navController = LocalNavController.current
        val scrollState = rememberScrollState()


        if (doctor == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.dashboardBackgroundColor),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@CommonAppBar
        }
        doctor?.let { doc ->

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .background(colors.dashboardBackgroundColor)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)   // important
                        .background(colors.dashboardBackgroundColor)
                        .verticalScroll(scrollState)
                )
                {


                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(0.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.dashboardBackgroundColor
                        )
                    ) {

                        Column( ) {

                            DoctorTopSection(doc, days)

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                InfoCard(
                                    title = doc.experience.toString(),
                                    subtitle = "Experience",
                                    modifier = Modifier.weight(1f)
                                )

                                InfoCard(
                                    title = doc.consultedPatientCount,
                                    subtitle = "Patients",
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Doctor Biography",
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = doc.biography ?: "No biography available",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            ClinicToggle()

                            Spacer(modifier = Modifier.height(16.dp))

                            SelectDateUI()
                            SlotScreen(viewModel)

                            Spacer(modifier = Modifier.weight(1f))

                            Spacer(modifier = Modifier.height(16.dp))


                        }

                    }
                }

                CommonButton(
                    text = "Book Appointment",
                    onClick = {


                        val data = BookingDetails(
                            dID = doctorId.toString(),
                            pName = doc.name.toString(),
                            qly = "MBBS",
                            atHospital = "City Hospital",
                            onDate = viewModel.slotDate.value.toString(),
                            onTime =  viewModel.slotTime.value.toString(),
                            free = doc.name.toString(),
                        )

                        val json = Gson().toJson(data)
                        navController.navigate(
                            Routes.BOOKINGCONFERMATION + "/${Uri.encode(json)}"
                        )
                    })
            }
        }
    }


    }



@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun SlotScreen(viewModel: DoctorDetailsViewModel) {

//    val parentEntry = remember {
//        navController?.currentBackStackEntry!!
//    }
//
//    val viewModel: DoctorDetailsViewModel = viewModel(parentEntry)
    val shiftList by viewModel.slotList.collectAsState()

    Column {

        shiftList.forEach { shift ->

            Column(modifier = Modifier ) {

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = shift.shift_Name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                val slotList: List<SlotData> = remember(shift.slotJson) {
                    Gson().fromJson(
                        shift.slotJson,
                        Array<SlotData>::class.java
                    ).toList()
                }


                    Spacer(modifier = Modifier.height(6.dp))
                    SlotGrid(slotList,shift.shiftId.toString(),viewModel)


            }
        }
    }
}

@SuppressLint("UnrememberedGetBackStackEntry")
@Composable
fun SlotGrid(list: List<SlotData>, ShifId: String, viewModel: DoctorDetailsViewModel, ) {

//    val parentEntry = remember {
//        navController?.currentBackStackEntry!!
//    }
//
//    val viewModel: DoctorDetailsViewModel = viewModel(parentEntry)
    var selectedSlot by remember { mutableStateOf<SlotData?>(null) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        list.forEach { slot ->

            SlotItem(
                slot = slot,
                isSelected = selectedSlot == slot,
                onClick = {
                    if (slot.slotStatus == "empty") {
                        selectedSlot = slot
                        viewModel.setSlotTime(slot.slotTime.toString())

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

    val backgroundColor = when {
        slot.slotStatus != "empty" -> Color.LightGray   // booked
        isSelected -> Color(0xFF1E5BD8)                 // selected
        else -> Color.White                             // available
    }

    val textColor = when {
        slot.slotStatus != "empty" -> Color.DarkGray
        isSelected -> Color.White
        else -> Color.Black
    }

    val border =
        if (slot.slotStatus == "empty" && !isSelected)
            BorderStroke(1.dp, Color(0xFF1E5BD8))
        else null


    val formattedTime = remember(slot.slotTime) {
        try {
            val input = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val output = java.text.SimpleDateFormat("h:mm a", Locale.getDefault())

            val date = input.parse(slot.slotTime)
            output.format(date!!)
        } catch (e: Exception) {
            slot.slotTime
        }
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .then(
                if (border != null)
                    Modifier.border(border, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(
                enabled = slot.slotStatus == "empty"
            ) {
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {

        Text(
            text = formattedTime,
            color = textColor
        )
    }
}

@Composable
fun DoctorTopSection(
    doc: com.example.vitalio_cis.model.DoctorDetails,
    days: String
) {

    Row {

        AsyncImage(
            model = doc.profilePath?.replace("\\", "/"),
            contentDescription = null,
            modifier = Modifier
                .size(70.dp)
                .clip(RoundedCornerShape(12.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {

            Text(doc.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)

            Text(
                doc.highestQualificationName,
                color = Color(0xFF2E7DFF),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .background(Color(0xFFE9EDF3), RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(days, fontSize = 12.sp)
            }
        }
    }
}

data class DateItem(
    val date: String,
    val day: String
)

@RequiresApi(Build.VERSION_CODES.O)
fun getDates(month: Int, year: Int): List<DateItem> {

    val start = LocalDate.of(year, month, 1)
    val end = start.withDayOfMonth(start.lengthOfMonth())

    val list = mutableListOf<DateItem>()
    var date = start

    while (!date.isAfter(end)) {

        list.add(
            DateItem(
                date.dayOfMonth.toString(),
                date.dayOfWeek.getDisplayName(
                    TextStyle.SHORT,
                    Locale.getDefault()
                )
            )
        )

        date = date.plusDays(1)
    }

    return list
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectDateUI( ) {

//    val parentEntry = remember(navController) {
//        navController?.getBackStackEntry(Routes.DOCTORDETAILS)
//    }
//    val parentEntry = remember(navController) {
//        navController?.getBackStackEntry("doctorDetails/{doctorId}/{days}")
//    }
    val viewModel: DoctorDetailsViewModel = viewModel()
    val today = LocalDate.now()
    val context = LocalContext.current

    var month by remember { mutableStateOf(today.monthValue) }
    var year by remember { mutableStateOf(today.year) }

    // always start from first visible item
    var selectedIndex by remember { mutableStateOf(0) }

    // show only future dates
    val dates = remember(month, year) {

        val allDates = getDates(month, year)

        if (month == today.monthValue && year == today.year) {
            allDates.drop(today.dayOfMonth - 1)
        } else {
            allDates
        }
    }

    Column {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text("Select Date", fontWeight = FontWeight.SemiBold)

            MonthYearDropdown(month, year) { m, y ->
                month = m
                year = y
                selectedIndex = 0
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {

            itemsIndexed(dates) { index, item ->

                DateItemView(
                    item = item,
                    selected = index == selectedIndex
                ) {

                    selectedIndex = index

                    Log.d(
                        "DateClick",
                        "Selected: $year-$month-${item.date}"
                    )
                    viewModel.setSlotDate("$year-$month-${item.date}")
                    viewModel.fetchAvailableSlots(
                        context,
                        "$year-$month-${item.date}"
                    )
                }
            }
        }
    }
}

@Composable
fun MonthYearDropdown(
    month: Int,
    year: Int,
    onSelected: (Int, Int) -> Unit
) {

    var expanded by remember { mutableStateOf(false) }

    val months = listOf(
        "Jan","Feb","Mar","Apr","May","Jun",
        "Jul","Aug","Sep","Oct","Nov","Dec"
    )

    val years = (year - 5..year + 5).toList()

    Box {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { expanded = true }
        ) {

            Text(
                "${months[month - 1]} $year",
                color = Color(0xFF2E5BFF)
            )

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF2E5BFF)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {

            months.forEachIndexed { index, m ->
                DropdownMenuItem(
                    text = { Text(m) },
                    onClick = {
                        onSelected(index + 1, year)
                        expanded = false
                    }
                )
            }

            Divider()

            years.forEach { y ->
                DropdownMenuItem(
                    text = { Text(y.toString()) },
                    onClick = {
                        onSelected(month, y)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DateItemView(
    item: DateItem,
    selected: Boolean,
    onClick: () -> Unit
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (selected)
                        Color(0xFFE8F0FF)
                    else
                        Color.Transparent
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                item.date,
                color =
                    if (selected) Color(0xFF2E5BFF)
                    else Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            item.day,
            fontSize = 11.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun InfoCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF1F3F6)
        )
    ) {

        Column(modifier = Modifier.padding(12.dp)) {

            Text(title, fontWeight = FontWeight.Bold)

            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ClinicToggle() {

    var selected by remember { mutableStateOf(0) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Color(0xFFE9EDF3),
                RoundedCornerShape(12.dp)
            )
            .padding(4.dp)
    ) {

        Row {

            // In Clinic
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected == 0) Color(0xFF2E5BFF)
                        else Color.Transparent
                    )
                    .clickable { selected = 0 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "In Clinic",
                    color = if (selected == 0) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }

            // Virtual
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (selected == 1) Color(0xFF2E5BFF)
                        else Color.Transparent
                    )
                    .clickable { selected = 1 }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Virtual",
                    color = if (selected == 1) Color.White else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}