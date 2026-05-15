package com.example.vitalio_cis.ui.screens
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.ComponentActivity
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.NavigationManager

import com.example.vitalio_cis.R
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.Vital
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.LocalThemeViewModel
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.ui.theme.getColorScheme
import com.example.vitalio_cis.utils.Patient
import com.example.vitalio_cis.utils.PrefsManager
import com.example.vitalio_cis.viewmodel.FindDoctorViewModel
import com.example.vitalio_cis.viewmodel.HomeViewModel
import kotlinx.coroutines.delay
import java.util.Locale


data class GridItem(val title: String, val icon: Int)


@Composable
fun DashboardScreen(viewModel: HomeViewModel = viewModel(), ) {
    var selectedIndex by remember { mutableStateOf(0) }
    val themeViewModel = LocalThemeViewModel.current
    val colors = LocalMyColorScheme.current
    val vitals by viewModel.vitalList.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchLastVital(context)
    }


    Scaffold( containerColor = colors.dashboardBackgroundColor,
        modifier = Modifier
            .background(colors.dashboardBackgroundColor),
        bottomBar = {
            BottomNavigationBar(selectedIndex) { selectedIndex = it }
        }
    ) { padding ->

        Box {
            Column {
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .background(colors.dashboardBackgroundColor)
                        .padding(horizontal = 16.dp)
                        .background(colors.dashboardBackgroundColor)

                ) {
                    Header()

                    Spacer(Modifier.height(20.dp))
                }
                if(selectedIndex==0){
                    HomeView()
                }
                else if(selectedIndex==1){
                    AddActivityScreen()
                }
                else if(selectedIndex==2){
                    RemindersScreen()
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
}

// ------------------- Bottom Navigation -------------------
@Composable
fun HomeView(viewModel: HomeViewModel = viewModel()){


    var selectedIndex by remember { mutableStateOf(0) }
    val themeViewModel = LocalThemeViewModel.current
    val colors = LocalMyColorScheme.current
    val vitals by viewModel.vitalList.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.fetchLastVital(context)
    }
    var showDialog by remember {
        mutableStateOf(false)
    }

    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.dashboardBackgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .background(colors.dashboardBackgroundColor)
        )
        {


//            ToTakeCard()
//
//            Spacer(Modifier.height(20.dp))

            VitalsCard(vitals)

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Primary Actions",
                style = AppTextStyles.style18BCB()
            )

            Spacer(Modifier.height(12.dp))

            PrimaryActionsGrid()

            Spacer(Modifier.height(20.dp))

            HomeScreen()

            Spacer(Modifier.height(20.dp))

            OtherSection()
            Spacer(Modifier.height(70.dp))
        }

        FloatingActionButton(
            onClick = {
//                6307748142
                showDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 132.dp, end = 32.dp),
            containerColor = Color(0xFF2F6FE4)
        ) {
            Icon(
                Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White
            )
        }
        if (showDialog) {

            VoiceToTextDialog(

                onDismiss = {

                    showDialog = false
                } )
        }
    }
}



@Composable
fun VoiceToTextDialog(
    onDismiss: () -> Unit
) {

    val navController = LocalNavController.current
    val context = LocalContext.current

    var text by remember { mutableStateOf("Listening...") }

    val speechRecognizer = remember {
        SpeechRecognizer.createSpeechRecognizer(context)
    }

    val intent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault()
            )
            putExtra(
                RecognizerIntent.EXTRA_PARTIAL_RESULTS,
                true
            )
        }
    }

    DisposableEffect(Unit) {

        val listener = object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                text = "Listening..."
            }

            override fun onBeginningOfSpeech() {
                text = "Speak now..."
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                text = "Processing..."
            }

            override fun onError(error: Int) {
                text = "Try again"
            }

            override fun onResults(results: Bundle?) {

                val data = results?.getStringArrayList(
                    SpeechRecognizer.RESULTS_RECOGNITION
                )

                val spokenText = data?.get(0)?.lowercase() ?: ""

                text = spokenText

                handleNavigation(spokenText, navController)

                onDismiss() // 🔥 AUTO CLOSE
            }

            override fun onPartialResults(partialResults: Bundle?) {}

            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer.setRecognitionListener(listener)

        // 🔥 AUTO START LISTENING (NO BUTTON NEEDED)
        speechRecognizer.startListening(intent)

        onDispose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listening...") },
        text = {
            Text(text)
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
data class VoiceCommand(
    val keywords: List<String>,
    val route: String
)

val voiceCommands = listOf(

    VoiceCommand(
        listOf("dashboard", "home", "main"),
        Routes.DASHBOARD
    ),

    VoiceCommand(
        listOf("vitals", "bp", "blood pressure", "heart rate", "spo2", "Temperature",  "RR", "Weight"),
        Routes.VITALS
    ),

    VoiceCommand(
        listOf("medicine", "tablet", "drug", "medication"),
        Routes.MEDICINE
    ),

    VoiceCommand(
        listOf("reminder", "alarm"),
        Routes.REMINDERS
    ),

    VoiceCommand(
        listOf("lab", "report", "test"),
        Routes.LABREPORTS
    ),

    VoiceCommand(
        listOf("diet", "food"),
        Routes.DIETCHECKLIST
    ),

    VoiceCommand(
        listOf("fluid", "water", "Milk", "Juice", "Tea", "Coffee", "Beverage"),
        Routes.FLUIDDATAINPUT
    ),

    VoiceCommand(
        listOf("symptom", "fever", "cough", "pain"),
        Routes.SYMPTOMSTRACKER
    ),

    VoiceCommand(
        listOf("doctor", "appointment"),
        Routes.FINDDOCTOR
    ),

    VoiceCommand(
        listOf("article", "research"),
        Routes.RESEARCHARTICLES
    ),

    VoiceCommand(
        listOf("profile"),
        Routes.MEDICALPROFILE
    ),

    VoiceCommand(
        listOf("emergency", "help"),
        Routes.EMERGENCYCONTACTS
    ),
    VoiceCommand(
        listOf("Watch", "Connect Smart Watch", "Connect watch"),
        Routes.CONNECTWATCH
    ),

    VoiceCommand(
        listOf("Family", "Family Health", "Family Health History"),
        Routes.FAMILYHEALTH
    ),


    VoiceCommand(
        listOf("Family", "Family Health", "Family Health History"),
        Routes.SHAREDACCOUNT
    ),

    VoiceCommand(
        listOf("Shared", "Shared Accounts",  ),
        Routes.SHAREDACCOUNT
    ),


    VoiceCommand(
        listOf("My Observer",   ),
        Routes.MYOBSERVERS
    ),
    VoiceCommand(
        listOf("Prescriptions",  "Prescription" ),
        Routes.PRESCRIPTION
    ),

    VoiceCommand(
        listOf("Allergies",  "Allergie" ),
        Routes.ALLERGIESSCREEN
    ),

    VoiceCommand(
        listOf("FAQ",   ),
        Routes.FAQ
    ),
    VoiceCommand(
        listOf("Feedback",   ),
        Routes.FEEDBACK
    ),
)
fun handleNavigation(
    spokenText: String,
    navController: NavController
) {

    val text = spokenText.lowercase()

    val route = voiceCommands.firstOrNull { command ->
        command.keywords.any { keyword ->
            text.contains(keyword)
        }
    }?.route

    route?.let {
        navController.navigate(it)
    }
}
// ------------------- Bottom Navigation -------------------
@Composable
fun BottomNavigationBar(selectedIndex: Int, onItemSelected: (Int) -> Unit) {

    val colors = LocalMyColorScheme.current

    NavigationBar(containerColor = colors.dashboardBackgroundColor) {

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
    val navController = LocalNavController.current

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
                .clickable() {

                    navController.navigate(Routes.DRAWER)
                }
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text("Good Morning,",
                    style = AppTextStyles.style12GCN())
            Text(patientData?.firstName.toString(),
                    style = AppTextStyles.style18BCN())
        }

        Icon(Icons.Default.Search, contentDescription = null)
        Spacer(Modifier.width(10.dp))
        Icon(Icons.Default.Notifications, contentDescription = null)
    }
}

// ------------------- To Take Card -------------------
@Composable
fun ToTakeCard() {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    Column(modifier = Modifier.clickable(){

        navController.navigate("welcome")
    }) {
        Text("To Take",
            style = AppTextStyles.style18BCB())
        Spacer(Modifier.height(8.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor =colors.dashboardContainerColor),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("1 capsule",
                        style = AppTextStyles.style12BCN())
                    Text("Pan D",
                            style = AppTextStyles.style12BCN() )
                    Text("Take on an empty stomach",
                                    style = AppTextStyles.style12GCN())
                }

                Checkbox(checked = false, onCheckedChange = {})
            }
        }
    }
}

fun getTimeAgo(dateTime: String): String {
    return try {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val past = format.parse(dateTime)
        val now = java.util.Date()

        val diff = now.time - (past?.time ?: 0)

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }

    } catch (e: Exception) {
        ""
    }
}
fun mergeVitals(vitals: List<Vital>): List<Vital> {

    val bpSys = vitals.find { it.vitalName == "BP_Sys" }
    val bpDias = vitals.find { it.vitalName == "BP_Dias" }

    val otherVitals = vitals.filter {
        it.vitalName != "BP_Sys" && it.vitalName != "BP_Dias"
    }.toMutableList()

    if (bpSys != null && bpDias != null) {
        otherVitals.add(
            Vital(
                vitalName = "BP",
                unit = "mmHg",
                vitalDateTime = bpSys.vitalDateTime,
                displayValue = "${bpSys.vitalValue.toInt()}/${bpDias.vitalValue.toInt()}"
            )
        )
    }

    return otherVitals
}
// ------------------- Vitals Card -------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VitalsCard(vitals: List<Vital>) {

    val colors = LocalMyColorScheme.current

    val mergedVitals = remember(vitals) { mergeVitals(vitals) }

    if (mergedVitals.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { mergedVitals.size })

    // auto slide
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val nextPage = (pagerState.currentPage + 1) % mergedVitals.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column {

        Text(
            "Vitals",
            style = AppTextStyles.style18BCB()
        )

        Spacer(Modifier.height(8.dp))

        HorizontalPager(
            state = pagerState
        ) { page ->

            val vital = mergedVitals[page]

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color.Red
                    )

                    Spacer(Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {

                        Text(
                            vital.vitalName,
                            style = AppTextStyles.style12BCN()
                        )

                        Text(
                            getTimeAgo(vital.vitalDateTime),
                            style = AppTextStyles.style12GCN()
                        )
                    }

                    Text(
                        if (vital.displayValue.isNotEmpty())
                            "${vital.displayValue} ${vital.unit}"
                        else
                            "${vital.vitalValue.toInt()} ${vital.unit}",
                        style = AppTextStyles.style12GCN()
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // dots
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {

            repeat(mergedVitals.size) { index ->

                val selected = pagerState.currentPage == index

                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected)
                                colors.primaryBlueColor
                            else
                                Color.LightGray
                        )
                )
            }
        }
    }
}

// ------------------- Primary Actions Grid -------------------
@Composable
fun PrimaryActionsGrid(   ) {
    val navController = LocalNavController.current

    val colors = LocalMyColorScheme.current
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
        type = "findDoctor"
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
                        when (item.type) {
                            "vitals" -> navController.navigate(Routes.VITALS)
                            "fluid" -> navController.navigate(Routes.FLUIDDATAINPUT)
                            "symptomsTracker" -> navController.navigate(Routes.SYMPTOMSTRACKER)
                            "medicine" -> navController.navigate(Routes.MEDICINE)
                            "diet" -> navController.navigate(Routes.DIETCHECKLIST)
                            "interaction" -> navController.navigate(Routes.INTERACTIONCHECKER)
                            "findDoctor" -> navController.navigate(Routes.FINDDOCTOR)
                            "articles" -> navController.navigate(Routes.RESEARCHARTICLES)
                        }   // ✅ GLOBAL NAV
                    },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)
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
                        style = AppTextStyles.style14BCB()
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
        Text("Upcoming Appointments",
                style = AppTextStyles.style18BCB())
        Spacer(Modifier.height(10.dp))
        AppointmentCard()
        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Featured Articles",
                style = AppTextStyles.style18BCB())
            Text("View All",
                style = AppTextStyles.style18BCN())
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
    val colors = LocalMyColorScheme.current

    val navController = LocalNavController.current

    Card(shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable() {

                navController.navigate(Routes.ARTICALEDETAILS)
            }) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(title,
                style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(6.dp))
            Text(author,
                style = AppTextStyles.style12GCN())
            Text(date,
                style = AppTextStyles.style12GCN())
        }
    }
}

// ------------------- Other Section -------------------
@Composable
fun OtherSection() {
    Box {
//        modifier =
//            Modifier.padding(bottom = 35.dp)
        Column(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            Text("Other",
                style = AppTextStyles.style18BCN())
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
    }
}

@Composable
fun ChronicleCard(modifier: Modifier) {
    val colors = LocalMyColorScheme.current
    Card(modifier = modifier.height(200.dp),
        shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Activities",
                        style = AppTextStyles.style12GCN())
                Text("Chronicle",
                    style = AppTextStyles.style18BCB())
            }

            Image(
                painter = painterResource(R.drawable.reminders),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp)
            )

            Column {
                Text("Share today's activities with us to understand your health pattern."
                    ,
                    style = AppTextStyles.style12GCN())
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {},
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2F6FE4))
                ) { Text("Add Now",
                        style = AppTextStyles.style12GCN())  }
            }
        }
    }
}

@Composable
fun UploadReportCard() {
    val colors = LocalMyColorScheme.current


    val context = LocalContext.current

    val navController = LocalNavController.current

    Card(modifier = Modifier
        .fillMaxWidth()
        .height(95.dp)
        .clickable() {

            navController.navigate(Routes.LABREPORTS)
        }, shape = RoundedCornerShape(18.dp), colors =
        CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(R.drawable.upload_report), contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
            Spacer(Modifier.height(6.dp))
            Text("Upload Report",
                style = AppTextStyles.style14BCN())
        }
    }
}

@Composable
fun LifestyleCard() {
    val colors = LocalMyColorScheme.current
    Card(modifier = Modifier
        .fillMaxWidth()
        .height(95.dp), shape = RoundedCornerShape(18.dp), colors =
        CardDefaults.cardColors(containerColor = colors.dashboardContainerColor)) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(painter = painterResource(R.drawable.intervention), contentDescription = null, modifier = Modifier.size(28.dp), tint = Color.Unspecified)
            Spacer(Modifier.height(6.dp))
            Text("Lifestyle Intervention",
                style = AppTextStyles.style14BCN())
        }
    }
}
