package com.example.vitalio_cis.ui.screens
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.graphics.Brush
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.critetiontech.myapplication.utils.LocalNavController
 import com.critetiontech.vitalio_cis.ui.screens.RemindersScreen
 import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.critetiontech.vitalio_cis.viewmodel.HomeViewModel
import com.example.vitalio_cis.viewmodel.ListeningState
import com.example.vitalio_cis.viewmodel.VoiceCommandViewModel
import kotlinx.coroutines.delay
import java.util.Locale
import com.critetiontech.vitalio_cis.R
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.model.Vital

data class GridItem(val title: String, val icon: Int)

@Composable
private fun AnimatedSection(delayMillis: Int = 0, content: @Composable () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(450, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "sectionAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else 24f,
        animationSpec = tween(450, delayMillis = delayMillis, easing = FastOutSlowInEasing),
        label = "sectionY"
    )
    Box(modifier = Modifier.graphicsLayer { this.alpha = alpha; translationY = offsetY }) {
        content()
    }
}


// ─────────────────────────────────────────────────────────────────────────────
//  DashboardScreen
//  Root composable that owns the Scaffold, bottom nav, and voice-assistant state.
//  Placing the FAB and VoiceAssistantSheet here (rather than inside HomeView)
//  makes the mic button persistent across all three bottom-nav tabs.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    homeViewModel: HomeViewModel = viewModel(),
    voiceViewModel: VoiceCommandViewModel = viewModel()
) {
    var selectedIndex by remember { mutableStateOf(0) }

    var showVoiceSheet by remember { mutableStateOf(false) }

    val colors = LocalMyColorScheme.current

    LaunchedEffect(Unit) {
        homeViewModel.fetchLastVital()
    }

    Scaffold(
        containerColor = colors.dashboardBackgroundColor,
        modifier = Modifier.background(colors.dashboardBackgroundColor),
        bottomBar = {
            BottomNavigationBar(selectedIndex) { selectedIndex = it }
        }
    ) { padding ->

        Box(modifier = Modifier.padding(padding)) {

            Column {
                Spacer(Modifier.height(20.dp))
                Column(
                    modifier = Modifier
                        .background(colors.dashboardBackgroundColor)
                        .padding(horizontal = 16.dp)
                ) {
                    Header()
                    Spacer(Modifier.height(20.dp))
                }

                // Content switches based on the selected bottom-nav tab
                when (selectedIndex) {
                    0 -> HomeView()
                    1 -> AddActivityScreen()
                    2 -> RemindersScreen()
                }
            }

            // Mic FAB – always visible regardless of the active tab
            FloatingActionButton(
                onClick = { showVoiceSheet = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 20.dp, end = 20.dp),
                containerColor = Color(0xFF2F6FE4)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Open voice assistant",
                    tint = Color.White
                )
            }
        }

        // Voice assistant sheet – rendered outside the padded Box so it
        // draws over everything including the bottom nav bar
        if (showVoiceSheet) {
            VoiceAssistantSheet(
                voiceViewModel = voiceViewModel,
                onDismiss = {
                    showVoiceSheet = false
                    voiceViewModel.reset()
                }
            )
        }
    }
}

@Composable
fun AddActivityScreen() {
    TODO("Not yet implemented")
}

// ─────────────────────────────────────────────────────────────────────────────
//  HomeView  (tab 0 content)
//  Pure content composable – no FAB, no dialogs.
//  The voice FAB lives in DashboardScreen so it stays visible on every tab.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeView(viewModel: HomeViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val vitals by viewModel.vitalList.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchLastVital()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.dashboardBackgroundColor)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        VitalsCard(vitals)

        Spacer(Modifier.height(20.dp))

        AnimatedSection(delayMillis = 200) {
            Text(text = "Primary Actions", style = AppTextStyles.style18BCB())
        }

        Spacer(Modifier.height(12.dp))

        PrimaryActionsGrid()

        Spacer(Modifier.height(20.dp))

        AnimatedSection(delayMillis = 600) { HomeScreen() }

        Spacer(Modifier.height(20.dp))

        AnimatedSection(delayMillis = 750) { OtherSection() }

        // Extra bottom padding so the last card clears the persistent mic FAB
        Spacer(Modifier.height(80.dp))
    }
}



// ═══════════════════════════════════════════════════════════════════════════════
//  Voice Assistant UI  –  Modern AI-style components
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Compact waveform rendered inside the orb while the mic is active.
 * Five bars animate at different speeds to mimic real speech energy.
 */
@Composable
private fun MiniWaveform() {
    val tr = rememberInfiniteTransition(label = "miniWave")

    val b0 by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(360, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(0)),   "mw0")
    val b1 by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(440, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(100)), "mw1")
    val b2 by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(200)), "mw2")
    val b3 by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(460, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(80)),  "mw3")
    val b4 by tr.animateFloat(0.25f, 1f, infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse, StartOffset(160)), "mw4")

    Row(
        modifier = Modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(b0, b1, b2, b3, b4).forEach { fraction ->
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight(fraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White)
            )
        }
    }
}

/**
 * Central glowing orb with three staggered ripple rings.
 *
 * Visual encoding:
 *  - Blue  → idle / listening / processing
 *  - Green → command matched
 *  - Red   → no match or mic error
 *
 * The rings only expand when the mic is actively open (LISTENING / PROCESSING).
 * The core orb breathes continuously via a gentle scale animation.
 */
@Composable
private fun VoiceOrb(state: ListeningState) {

    val isActive = state == ListeningState.LISTENING || state == ListeningState.PROCESSING

    // Smooth color transition between states
    val orbColor by animateColorAsState(
        targetValue = when (state) {
            ListeningState.MATCHED              -> Color(0xFF22C55E) // success green
            ListeningState.NO_MATCH,
            ListeningState.ERROR                -> Color(0xFFEF4444) // error red
            else                                -> Color(0xFF3B82F6) // primary blue
        },
        animationSpec = tween(400),
        label = "orbColor"
    )

    val tr = rememberInfiniteTransition(label = "orbAnim")

    // Three ripple rings with staggered 500 ms offsets
    val r1s by tr.animateFloat(0.85f, if (isActive) 1.75f else 0.85f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(0)),    "r1s")
    val r1a by tr.animateFloat(0.5f,  0f,                              infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(0)),    "r1a")
    val r2s by tr.animateFloat(0.85f, if (isActive) 1.75f else 0.85f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(500)),  "r2s")
    val r2a by tr.animateFloat(0.5f,  0f,                              infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(500)),  "r2a")
    val r3s by tr.animateFloat(0.85f, if (isActive) 1.75f else 0.85f, infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(1000)), "r3s")
    val r3a by tr.animateFloat(0.5f,  0f,                              infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart, StartOffset(1000)), "r3a")

    // Subtle scale-breathe on the core orb
    val breath by tr.animateFloat(0.88f, 1f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), "breath")

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {

        // Ripple rings (graphicsLayer avoids allocation of new composable per frame)
        listOf(r1s to r1a, r2s to r2a, r3s to r3a).forEach { (scale, alpha) ->
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }
                    .clip(CircleShape)
                    .background(orbColor.copy(alpha = 0.22f))
            )
        }

        // Ambient radial glow
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(
                            orbColor.copy(alpha = 0.28f),
                            orbColor.copy(alpha = 0.07f),
                            Color.Transparent
                        )
                    )
                )
        )

        // Solid core with breathing scale
        Box(
            modifier = Modifier
                .size(80.dp)
                .graphicsLayer { scaleX = breath; scaleY = breath }
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(orbColor, orbColor.copy(alpha = 0.7f)))),
            contentAlignment = Alignment.Center
        ) {
            when (state) {
                ListeningState.LISTENING,
                ListeningState.PROCESSING -> MiniWaveform()

                ListeningState.MATCHED    -> Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(30.dp))
                ListeningState.NO_MATCH,
                ListeningState.ERROR      -> Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(30.dp))
                else                      -> Icon(Icons.Default.Mic,   null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
        }
    }
}

/**
 * Hint chip styled for the dark sheet.
 * Uses a dark-tinted background and a subtle border so it reads well at small sizes.
 */
@Composable
private fun VoiceHintChip(label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFF2D3748), RoundedCornerShape(20.dp))
            .background(Color(0xFF161B2E), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color(0xFF9CA3AF),
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Full-featured voice assistant bottom sheet.
 *
 * Session lifecycle:
 *  1. Sheet appears → SpeechRecognizer starts automatically (DisposableEffect)
 *  2. User speaks → partial transcript shown in real time
 *  3. Speech ends → ViewModel classifies as MATCHED / NO_MATCH
 *  4. MATCHED: shows "Navigating to X" for 800 ms, navigates, then closes sheet
 *  5. NO_MATCH / ERROR: shows error message, auto-restarts the recognizer after 1.5 s
 *
 * All mutable state lives in [VoiceCommandViewModel]; this composable is stateless.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceAssistantSheet(
    voiceViewModel: VoiceCommandViewModel,
    onDismiss: () -> Unit
) {
    val navController = LocalNavController.current
    val context = LocalContext.current
    val colors = LocalMyColorScheme.current

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Observe ViewModel state
    val listeningState by voiceViewModel.listeningState.collectAsState()
    val spokenText by voiceViewModel.spokenText.collectAsState()
    val matchedLabel by voiceViewModel.matchedLabel.collectAsState()
    val matchedRoute by voiceViewModel.matchedRoute.collectAsState()

    // ── SpeechRecognizer setup ─────────────────────────────────────────────────
    // remember keeps the same instance across recompositions
    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }
    val recognizerIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }
    }

    // Wire RecognitionListener callbacks → ViewModel state transitions
    DisposableEffect(Unit) {
        val listener = object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = voiceViewModel.onReadyForSpeech()
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() = voiceViewModel.onSpeechEnded()
            override fun onError(error: Int) = voiceViewModel.onRecognitionError()
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                voiceViewModel.onPartialResult(partial)
            }
            override fun onResults(results: Bundle?) {
                val spoken = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull() ?: ""
                voiceViewModel.onResult(spoken)
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }
        speechRecognizer.setRecognitionListener(listener)
        speechRecognizer.startListening(recognizerIntent)

        onDispose {
            speechRecognizer.stopListening()
            speechRecognizer.destroy()
        }
    }

    // ── TTS setup ─────────────────────────────────────────────────────────────────
    // Provides audio confirmation so the user gets feedback without watching the screen.
    // Stored as MutableState so the async init callback (fires on main thread) can update it.
    val ttsEngine = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        // lateinit var so the lambda can capture it before TextToSpeech() returns
        // (TTS init callback is always async, so engine is assigned before it fires)
        lateinit var engine: TextToSpeech
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsEngine.value = engine
            }
        }
        onDispose {
            ttsEngine.value = null
            engine.shutdown()
        }
    }

    // React to terminal states with audio + visual feedback:
    //  MATCHED  → speak "Opening <X>" → 1.2 s pause → navigate → dismiss
    //  NO_MATCH → speak retry prompt  → 2 s pause   → restart mic
    //  ERROR    → speak error message → 2 s pause   → restart mic
    LaunchedEffect(listeningState) {
        when (listeningState) {

            ListeningState.MATCHED -> {
                // Speak the destination name before navigating so the user hears confirmation
                ttsEngine.value?.speak(
                    "Opening $matchedLabel",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "voice_nav_confirm"
                )
                // 1.2 s gives TTS time to finish before the screen transition
                delay(1200)
                matchedRoute?.let { navController.navigate(it) }
                onDismiss()
            }

            ListeningState.NO_MATCH -> {
                ttsEngine.value?.speak(
                    "Sorry, I didn't catch that. Try saying vitals, medicine, or diet.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "voice_no_match"
                )
                delay(2000)
                voiceViewModel.onReadyForSpeech()
                speechRecognizer.startListening(recognizerIntent)
            }

            ListeningState.ERROR -> {
                ttsEngine.value?.speak(
                    "Microphone error. Retrying.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "voice_mic_error"
                )
                delay(2000)
                voiceViewModel.onReadyForSpeech()
                speechRecognizer.startListening(recognizerIntent)
            }

            else -> {}
        }
    }

    // ── Sheet UI ─────────────────────────────────────────────────────────────────
    // Dark background regardless of app theme – standard for AI voice interfaces
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0B0F1A),
        dragHandle = {
            // Subtle custom drag handle
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFF2D3748))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Brand label ───────────────────────────────────────────────────
            Text(
                text = "VITALIO VOICE",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF4B6EFF),
                letterSpacing = 2.sp
            )

            Spacer(Modifier.height(20.dp))

            // ── Animated orb – the main visual focal point ────────────────────
            VoiceOrb(state = listeningState)

            Spacer(Modifier.height(20.dp))

            // ── Primary state text ────────────────────────────────────────────
            Text(
                text = when (listeningState) {
                    ListeningState.IDLE       -> "Tap to speak"
                    ListeningState.LISTENING  -> "Listening..."
                    ListeningState.PROCESSING -> "Understanding..."
                    ListeningState.MATCHED    -> "Opening $matchedLabel"
                    ListeningState.NO_MATCH   -> "Didn't catch that"
                    ListeningState.ERROR      -> "Mic unavailable"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = when (listeningState) {
                    ListeningState.MATCHED              -> Color(0xFF22C55E)
                    ListeningState.NO_MATCH,
                    ListeningState.ERROR                -> Color(0xFFEF4444)
                    else                                -> Color.White
                },
                textAlign = TextAlign.Center
            )

            // ── Live partial transcript ───────────────────────────────────────
            if (spokenText.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "\"$spokenText\"",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(28.dp))

            // Thin separator between action area and hint chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFF1E2433))
            )

            Spacer(Modifier.height(16.dp))

            // ── Hint section header ───────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4B6EFF))
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Try saying",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(10.dp))

            // ── Hint chips in rows of 4 ───────────────────────────────────────
            voiceViewModel.hintCommands.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { cmd ->
                        Box(modifier = Modifier.weight(1f)) {
                            VoiceHintChip(label = cmd.label)
                        }
                    }
                    // Pad short rows so chip widths stay consistent
                    repeat(4 - rowItems.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            // ── Cancel button ─────────────────────────────────────────────────
            OutlinedButton(
                onClick = onDismiss,
                border = BorderStroke(1.dp, Color(0xFF2D3748)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(44.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF6B7280)
                )
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
        }
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
    val patientData = PrefsManager(context).getPatient()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "headerAlpha"
    )
    val offsetY by animateFloatAsState(
        targetValue = if (visible) 0f else -30f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "headerY"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha; translationY = offsetY },
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

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "vitalsAlpha"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.94f,
        animationSpec = tween(500, delayMillis = 100, easing = FastOutSlowInEasing),
        label = "vitalsScale"
    )

    val pagerState = rememberPagerState(pageCount = { mergedVitals.size })

    // auto slide
    LaunchedEffect(Unit) {
        while (true) {
            delay(2000)
            val nextPage = (pagerState.currentPage + 1) % mergedVitals.size
            pagerState.animateScrollToPage(nextPage)
        }
    }

    Column(modifier = Modifier.graphicsLayer { alpha = cardAlpha; scaleX = cardScale; scaleY = cardScale }) {

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

    var gridVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { gridVisible = true }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        itemsIndexed(items) { index, item ->
            val itemAlpha by animateFloatAsState(
                targetValue = if (gridVisible) 1f else 0f,
                animationSpec = tween(350, delayMillis = 80 + index * 55, easing = FastOutSlowInEasing),
                label = "gridAlpha_$index"
            )
            val itemY by animateFloatAsState(
                targetValue = if (gridVisible) 0f else 28f,
                animationSpec = tween(350, delayMillis = 80 + index * 55, easing = FastOutSlowInEasing),
                label = "gridY_$index"
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .graphicsLayer { alpha = itemAlpha; translationY = itemY }
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

        /* Upcoming Appointments
        Text("Upcoming Appointments",
                style = AppTextStyles.style18BCB())
        Spacer(Modifier.height(10.dp))
        AppointmentCard()
        Spacer(Modifier.height(20.dp))
        */

        /* Featured Articles
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
        */
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
                /* Activities Chronicle
                ChronicleCard(modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                */
                Column(modifier = Modifier.weight(1f)) {
                    UploadReportCard()
                    /* Lifestyle Intervention
                    Spacer(Modifier.height(12.dp))
                    LifestyleCard()
                    */
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
