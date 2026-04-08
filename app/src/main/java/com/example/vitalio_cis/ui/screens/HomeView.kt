package com.example.vitalio_cis.ui.screens
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.NavigationManager

import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.ui.theme.getColorScheme
import com.example.vitalio_cis.utils.Patient
import com.example.vitalio_cis.utils.PrefsManager


data class GridItem(val title: String, val icon: Int)

@Composable
fun DashboardScreen() {
    var selectedIndex by remember { mutableStateOf(0) }
    val themeViewModel: ThemeViewModel = viewModel()

    // 3️⃣ Collect colors as Compose State to trigger recomposition when theme changes
    val colors by themeViewModel.colorScheme.collectAsState()

    Scaffold(
        modifier = Modifier
            .background(colors.white),
        bottomBar = {
            BottomNavigationBar(selectedIndex) { selectedIndex = it }
        }
    ) { padding ->

        Column  {
            Spacer(Modifier.height(20.dp))
            Column(
                modifier = Modifier
                    .background(colors.white)
                    .padding(horizontal = 16.dp)
                    .background(colors.white)

            ) {
                Header()
                Button(onClick = { themeViewModel.toggleTheme() }) {
                    Text("Toggle Theme")
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.white)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
                    .background(colors.white)
            ) {



                ToTakeCard()

                Spacer(Modifier.height(20.dp))

                VitalsCard()

                Spacer(Modifier.height(20.dp))

                Text(
                    text = "Primary Actions",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(12.dp))

                PrimaryActionsGrid()

                Spacer(Modifier.height(20.dp))

                HomeScreen()

                Spacer(Modifier.height(20.dp))

                OtherSection()
            }
        }
    }
}

// ------------------- Bottom Navigation -------------------
@Composable
fun BottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {
    NavigationBar(containerColor = Color.White) {

        val items = listOf(
            Triple(R.drawable.home,stringResource(R.string.home), 0),
            Triple(R.drawable.activity, stringResource(R.string.activity), 1),
            Triple(R.drawable.reminders, stringResource(R.string.reminders), 2),
            Triple(R.drawable.chat, stringResource(R.string.chat), 3)
        )

        items.forEach { (iconRes, label, index) ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = { onItemSelected(index) },
                icon = { Icon(painterResource(id = iconRes), contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Blue,
                    selectedTextColor = Color.Blue,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

// ------------------- Header -------------------
@Composable
fun Header() {

    val context = LocalContext.current
    val patientData= PrefsManager(context).getPatient()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.img),
            contentDescription = null,
            modifier = Modifier
                .size(45.dp)
                .clip(CircleShape)
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Good Morning,", color = Color.Gray)
            Text(patientData?.firstName.toString(), fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }

        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Default.Notifications, contentDescription = null)
    }
}

// ------------------- To Take Card -------------------
@Composable
fun ToTakeCard() {
    Column {
        Text("To Take", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3)),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("1 capsule", fontSize = 12.sp)
                    Text("Pan D", fontWeight = FontWeight.Bold)
                    Text("Take on an empty stomach", fontSize = 12.sp, color = Color.Gray)
                }

                Checkbox(checked = false, onCheckedChange = {})
            }
        }
    }
}

// ------------------- Vitals Card -------------------
@Composable
fun VitalsCard() {
    Column {
        Text("Vitals", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3))) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red)
                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("Heart Rate")
                    Text("1hr ago", fontSize = 12.sp, color = Color.Gray)
                }

                Text("60 BPM", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ------------------- Primary Actions Grid -------------------
@Composable
fun PrimaryActionsGrid(   ) {
    val navController = LocalNavController.current
    data class GridItem(
        val title: String,
        val icon: Int,
        val type: String
    )

    val items = listOf(

            GridItem(
                title = stringResource(R.string.vitals_details),
                icon = R.drawable.vital_details,
                type = "vitals"
            ),

    GridItem(
        title = stringResource(R.string.fluid_intake),
        icon = R.drawable.fluid_intake,
        type = "fluid"
    ),

    GridItem(
        title = stringResource(R.string.symptoms_tracker),
        icon = R.drawable.symptom_tracker,
        type = "symptomsTracker"
    ),

    GridItem(
        title = stringResource(R.string.medicine_reminder),
        icon = R.drawable.medicine_reminder,
        type = "medicine"
    ),

    GridItem(
        title = stringResource(R.string.diet_checklist),
        icon = R.drawable.diet_checklist,
        type = "diet"
    ),

    GridItem(
        title = stringResource(R.string.interactions_checker),
        icon = R.drawable.medicine_reminder,
        type = "interaction"
    ),

    GridItem(
        title = stringResource(R.string.appointments),
        icon = R.drawable.appointments,
        type = "appointments"
    ),

    GridItem(
        title = stringResource(R.string.research_based_articles),
        icon = R.drawable.medicine_reminder,
        type = "articles"
    )
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),   // ✅ FIXED HEIGHT (VERY IMPORTANT)
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        items(items) { item ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clickable {
                        when(item.type) {
                            "vitals" -> navController.navigate(Routes.VITALS)
                            "fluid" -> navController.navigate(Routes.FLUID)
                            "symptomsTracker" -> navController.navigate(Routes.SYMPTOMSTRACKER)
                            "medicine" -> navController.navigate(Routes.MEDICINE)
                            "diet" -> navController.navigate(Routes.MANAGE_MEDICINE)
                            "appointments" -> navController.navigate(Routes.APPOINTMENTS)
                            "articles" -> navController.navigate(Routes.ARTICLES)
                        }   // ✅ GLOBAL NAV
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3))
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {

                    Image(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.title,
                        modifier = Modifier.size(32.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = item.title,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}


//@Composable
//fun PrimaryActionsGrid( ) {
//
//    data class GridItem(
//        val title: String,
//        val icon: Int,
//        val route: String
//    )
//
//    )
//
//
//            Card(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(100.dp)
//                    .clickable {
//                        navController.navigate(item.route)
//                    },
//                shape = RoundedCornerShape(12.dp),
//                colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3)),
//                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, hoveredElevation = 4.dp,
//                    focusedElevation = 4.dp,
//                    draggedElevation = 41.dp,
//                    pressedElevation = 16.dp,
//                    disabledElevation = 41.dp ),
//
//
//            ) {
//
//                Column(
//                    modifier = Modifier.fillMaxSize().clickable(){
//
//
//                    },
//                    horizontalAlignment = Alignment.CenterHorizontally,
//                    verticalArrangement = Arrangement.Center
//                ) {
//                    Column() {
//                        Text("Vitals Details",
//                            textDecoration = TextDecoration(Circl))
//                    }
//
//                    Image(
//                        painter = painterResource(id = item.icon),
//                        contentDescription = item.title,
//                        modifier = Modifier.size(32.dp)
//                    )
//
//                    Spacer(modifier = Modifier.height(6.dp))
//
//                    Text(
//                        text = item.title,
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//
//                    Text(
//                        text = "vital Details",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//
//
//                    Text(
//                        text = "Fluid intake",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//
//
//
//                    Text(
//                        text = "vital Details",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//                    Text(
//                        text = "vital Details",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//
//
//                    Text(
//                        text = "Appointment Details",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//
//
//
//                    Text(
//                        text = "Appointment Details",
//                        textAlign = TextAlign.Center,
//                        fontSize = 12.sp,
//                        fontWeight = FontWeight.Medium
//                    )
//                }
//            }
//    }
//}


// ------------------- Home Screen -------------------
@Composable
fun HomeScreen() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(10.dp))
        Text("Upcoming Appointments", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        AppointmentCard()
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Featured Articles", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text("View All", color = Color(0xFF4A6CF7))
        }

        Spacer(Modifier.height(10.dp))
        ArticleCard(
            title = "Carbohydrate antigen 125 (CA125) following acute myocardial infarction: effects of empagliflozin and association with heart failure readouts in the EMMY trial",
            author = "Ahmed M. Hassan, and Others",
            date = "23 December 2025"
        )
        Spacer(Modifier.height(10.dp))
        ArticleCard(
            title = "Trial of High-Dose Oral Rifampin in Adults with Tuberculous Meningitis.",
            author = "D.B. Meya, and Others",
            date = "17 December 2025"
        )
    }
}

// ------------------- Appointment Card -------------------
@Composable
fun AppointmentCard() {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2458C6)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.vital_details),
                    contentDescription = null,
                    modifier = Modifier
                        .size(50.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Dr. Abdul Karim", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("MBBS, MS, MCn (Neurologist)", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color.White.copy(alpha = 0.3f))
            Spacer(Modifier.height(12.dp))

            AppointmentRow(Icons.Default.DateRange, "Tuesday - December 16")
            AppointmentRow(Icons.Default.DateRange, "09:00 AM - 10:00 AM")
            AppointmentRow(Icons.Default.LocationOn, "LifeSpring Medical Center")
        }
    }
}

@Composable
fun AppointmentRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, color = Color.White, fontSize = 13.sp)
    }
}

// ------------------- Article Card -------------------
@Composable
fun ArticleCard(title: String, author: String, date: String) {
    Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3)), modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(author, fontSize = 12.sp, color = Color.Gray)
            Text(date, fontSize = 12.sp, color = Color.Gray)
        }
    }
}

// ------------------- Other Section -------------------
@Composable
fun OtherSection() {
    Box {
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text("Other", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row {
                ChronicleCard(modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    UploadReportCard()
                    Spacer(Modifier.height(12.dp))
                    LifestyleCard()
                }
            }
        }

        FloatingActionButton(
            onClick = {},
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = Color(0xFF2F6FE4)
        ) {
            Icon(Icons.Default.Home, contentDescription = null)
        }
    }
}

@Composable
fun ChronicleCard(modifier: Modifier) {
    Card(modifier = modifier.height(200.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Activities", fontSize = 12.sp, color = Color.Gray)
                Text("Chronicle", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Image(
                painter = painterResource(R.drawable.reminders),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Column {
                Text("Share today's activities with us to understand your health pattern.", fontSize = 11.sp, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F6FE4))
                ) { Text("Add Now") }
            }
        }
    }
}

@Composable
fun UploadReportCard() {
    Card(modifier = Modifier
        .fillMaxWidth()
        .height(95.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3))) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(R.drawable.upload_report), contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
            Spacer(Modifier.height(6.dp))
            Text("Upload Report", fontSize = 13.sp)
        }
    }
}

@Composable
fun LifestyleCard() {
    Card(modifier = Modifier
        .fillMaxWidth()
        .height(95.dp), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFE9EDF3))) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(R.drawable.intervention), contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
            Spacer(Modifier.height(6.dp))
            Text("Lifestyle Intervention", fontSize = 12.sp, textAlign = TextAlign.Center)
        }
    }
}
