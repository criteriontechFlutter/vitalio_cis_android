package com.example.vitalio_cis.ui.screens

import com.critetiontech.ctvitalio.ui.screens.displayUnit
import com.critetiontech.ctvitalio.ui.feedback.HapticButton
import com.critetiontech.ctvitalio.ui.feedback.HapticCheckbox
import com.critetiontech.ctvitalio.ui.feedback.HapticIconButton
import com.critetiontech.ctvitalio.ui.feedback.HapticOutlinedButton
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.critetiontech.ctvitalio.utils.ToastUtils
import androidx.activity.compose.BackHandler
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.ui.graphics.Brush
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
 import com.critetiontech.ctvitalio.ui.screens.SmartRingScreen
 import com.critetiontech.ctvitalio.ui.theme.LocalMyColorScheme
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.viewmodel.HomeViewModel
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.critetiontech.ctvitalio.voice.VoiceListeningOverlay
import com.critetiontech.ctvitalio.voice.FloatingVoiceFab
import kotlinx.coroutines.delay
import java.util.Locale
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.Routes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.critetiontech.ctvitalio.model.UpcomingAppointment
import com.critetiontech.ctvitalio.model.Vital
import com.critetiontech.ctvitalio.ui.screens.AddActivityScreen
import com.critetiontech.ctvitalio.ui.adaptive.LocalWindowSizeClass
import com.critetiontech.ctvitalio.ui.adaptive.WindowWidthSizeClass
import com.critetiontech.ctvitalio.ui.adaptive.rememberResponsiveDimens
import com.critetiontech.ctvitalio.viewmodel.ListeningState
import com.critetiontech.ctvitalio.viewmodel.VoiceCommandViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.critetiontech.ctvitalio.ui.feedback.AnimatedEnter
import com.critetiontech.ctvitalio.ui.feedback.ContentState
import com.critetiontech.ctvitalio.ui.feedback.ContentStateCrossfade
import com.critetiontech.ctvitalio.ui.feedback.AnimatedCheckMark
import com.critetiontech.ctvitalio.ui.feedback.successPulse
import com.critetiontech.ctvitalio.ui.feedback.HapticEvent
import com.critetiontech.ctvitalio.ui.feedback.PullRefreshHaptics
import com.critetiontech.ctvitalio.ui.feedback.PullStretchHeader
import com.critetiontech.ctvitalio.ui.feedback.bounceClick
import com.critetiontech.ctvitalio.ui.feedback.hapticCombinedClick
import com.critetiontech.ctvitalio.ui.feedback.rememberHaptics
import com.critetiontech.ctvitalio.ui.feedback.stretchOffsetPx
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.compositeOver
import java.text.SimpleDateFormat
import kotlin.time.Duration.Companion.milliseconds

data class GridItem(val title: String, val icon: Int)

@Composable
private fun AnimatedSection(delayMillis: Int = 0, content: @Composable () -> Unit) =
    AnimatedEnter(delayMillis = delayMillis, content = content)


// ─────────────────────────────────────────────────────────────────────────────
//  DashboardScreen
//  Root composable that owns the Scaffold, bottom nav, and voice-assistant state.
//  Placing the FAB and VoiceAssistantSheet here (rather than inside HomeView)
//  makes the mic button persistent across all three bottom-nav tabs.
// ─────────────────────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    homeViewModel: HomeViewModel = viewModel(),
    voiceViewModel: VoiceCommandViewModel = viewModel()
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current

    // First visit after sign-in: explain notifications, then ask once
    com.critetiontech.ctvitalio.notification.RequestNotificationPermission()

    // ── VoiceNavigationManager state ──────────────────────────────────────────
    val isListening          by VoiceNavigationManager.isListening.collectAsState()
    val transcript           by VoiceNavigationManager.transcript.collectAsState()
    val showListeningOverlay by VoiceNavigationManager.showListeningOverlay.collectAsState()
    val showConfirmationCard by VoiceNavigationManager.showConfirmationCard.collectAsState()
    val pendingData          by VoiceNavigationManager.pendingData.collectAsState()
    val isSavingData         by VoiceNavigationManager.isSavingData.collectAsState()
    val isExtractingData     by VoiceNavigationManager.isExtractingData.collectAsState()
    val processingTranscript by VoiceNavigationManager.processingTranscript.collectAsState()
    val navigationCommand    by VoiceNavigationManager.navigationCommand.collectAsState()
    val anomalyAlerts        by VoiceNavigationManager.anomalyAlerts.collectAsState()
    val multiTurnPrompt      by VoiceNavigationManager.multiTurnPrompt.collectAsState()
    val vitalTrends          by VoiceNavigationManager.vitalTrends.collectAsState()
    val voiceStatusMessage   by VoiceNavigationManager.statusMessage.collectAsState()

    // Voice assistant: explain the mic, then start listening once it's allowed
    val micPermission = com.critetiontech.ctvitalio.ui.components.rememberPermissionRequest(
        com.critetiontech.ctvitalio.permissions.AppPermission.Microphone, "to hear your voice commands"
    ) {
        VoiceNavigationManager.startListening { route -> navController.navigate(route) }
    }

    // Navigate when VoiceNavigationManager resolves a command
    LaunchedEffect(navigationCommand) {
        navigationCommand?.let { route ->
            navController.navigate(route)
            VoiceNavigationManager.clearNavigationCommand()
        }
    }

    // Double-tap back to exit
    var backPressedOnce by remember { mutableStateOf(false) }
    BackHandler {
        if (backPressedOnce) {
            (context as? Activity)?.finish()
        } else {
            backPressedOnce = true
            Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(backPressedOnce) {
        if (backPressedOnce) {
            delay(2000)
            backPressedOnce = false
        }
    }

    LaunchedEffect(Unit) {
        homeViewModel.fetchDashboard()
    }

    val windowSize = LocalWindowSizeClass.current
    val useNavRail = windowSize != WindowWidthSizeClass.Compact

    // Dashboard look: white page with light-grey cards. Dark theme keeps its own palette.
    val baseColors = LocalMyColorScheme.current
    val dashboardColors = remember(baseColors) {
        if (baseColors.dashboardContainerColor == Color.White)
            baseColors.copy(dashboardBackgroundColor = Color.White, dashboardContainerColor = DashboardCardGrey)
        else baseColors
    }

    // Pull-to-refresh lives here (not in HomeView) so the header and status bar can stretch.
    val pullState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val refreshScope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        refreshScope.launch {
            val startedAt = System.currentTimeMillis()
            homeViewModel.fetchDashboard()
            // fetchDashboard flips loading on asynchronously; wait for it to settle.
            delay(300)
            kotlinx.coroutines.withTimeoutOrNull(20_000) { homeViewModel.loading.first { !it } }
            delay((900 - (System.currentTimeMillis() - startedAt)).coerceAtLeast(0))
            isRefreshing = false
        }
    }
    val pulledDown by remember { derivedStateOf { pullState.distanceFraction > 0.15f } }

    // Dark status-bar icons on the white page (light icons in dark theme, and over the blue pull area).
    val view = androidx.compose.ui.platform.LocalView.current
    val lightPage = dashboardColors.dashboardBackgroundColor == Color.White
    // Read in composition (not inside SideEffect) so a pull recomposes and re-applies it.
    val darkStatusIcons = lightPage && !pulledDown
    if (!view.isInEditMode) {
        SideEffect {
            (view.context as? Activity)?.window?.let { window ->
                androidx.core.view.WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                    darkStatusIcons
            }
        }
    }

    CompositionLocalProvider(LocalMyColorScheme provides dashboardColors) {
    val colors = LocalMyColorScheme.current
    val density = androidx.compose.ui.platform.LocalDensity.current
    // How far the page is pulled down; the content slides by this much and the blue area fills the gap.
    val stretchPx = { pullState.stretchOffsetPx(density) }
    PullRefreshHaptics(pullState, isRefreshing)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pullToRefresh(
                isRefreshing = isRefreshing,
                state = pullState,
                enabled = selectedIndex == 0,
                onRefresh = onRefresh,
            )
    ) {
        Scaffold(
            containerColor = colors.dashboardBackgroundColor,
            modifier = Modifier.background(colors.dashboardBackgroundColor),
            bottomBar = {
                // Only show bottom nav on phones
                if (!useNavRail) {
                    BottomNavigationBar(selectedIndex) { selectedIndex = it }
                }
            }
        ) { padding ->

            if (useNavRail) {
                // ── Tablet: Navigation Rail on the left ──
                Row(modifier = Modifier.padding(padding).fillMaxSize()) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = colors.dashboardContainerColor
                    ) {
                        Spacer(Modifier.height(12.dp))
                        val items = listOf(
                            Triple(R.drawable.home, "Home", 0),
                            Triple(R.drawable.activity, "Activity", 1),
                            Triple(R.drawable.ring, "Ring", 2)
                            // Chat is hidden until its screen is built.
                        )
                        items.forEach { (iconRes, label, index) ->
                            NavigationRailItem(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                                icon = { Icon(painterResource(id = iconRes), contentDescription = label) },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = colors.primaryBlueColor,
                                    selectedTextColor = colors.primaryBlueColor,
                                    unselectedIconColor = colors.textGreyColor,
                                    unselectedTextColor = colors.textGreyColor,
                                    indicatorColor = Color.Transparent
                                )
                            )
                        }
                    }

                    // Main content
                    Box(modifier = Modifier.weight(1f)) {
                        Column(Modifier.graphicsLayer { translationY = stretchPx() }) {
                            Spacer(Modifier.height(20.dp))
                            Column(
                                modifier = Modifier
                                    .background(colors.dashboardBackgroundColor)
                                    .padding(horizontal = 24.dp)
                            ) {
                                Header()
                                Spacer(Modifier.height(20.dp))
                            }
                            when (selectedIndex) {
                                0 -> HomeView()
                                1 -> AddActivityScreen()
                                2 -> SmartRingScreen()
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 20.dp, end = 20.dp)
                        ) {
                            FloatingVoiceFab(
                                isListening  = isListening,
                                showOverlay  = showListeningOverlay,
                                onClick = {
                                    micPermission.launch()
                                }
                            )
                        }
                    }
                }
            } else {
                // ── Phone: existing layout ──
            Box(modifier = Modifier.padding(padding)) {

                Column(Modifier.graphicsLayer { translationY = stretchPx() }) {
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
                        2 -> SmartRingScreen()
                    }
                }

                // New iOS-style floating mic FAB (bottom-right)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 20.dp, end = 20.dp)
                ) {
                    FloatingVoiceFab(
                        isListening  = isListening,
                        showOverlay  = showListeningOverlay,
                        onClick = {
                            micPermission.launch()
                        }
                    )
                }
            }
            }
        }

        PullStretchHeader(
            state = pullState,
            isRefreshing = isRefreshing,
            color = colors.primaryBlueColor,
            behindStatusBar = true,
        )

        // Full-screen voice listening overlay (drawn ABOVE everything, including bottom nav)
        VoiceListeningOverlay(
            isListening          = isListening,
            showListeningOverlay = showListeningOverlay,
            showConfirmationCard = showConfirmationCard,
            pendingData          = pendingData,
            transcript           = transcript,
            processingTranscript = processingTranscript,
            isSavingData         = isSavingData,
            isExtractingData     = isExtractingData,
            anomalyAlerts        = anomalyAlerts,
            multiTurnPrompt      = multiTurnPrompt,
            vitalTrends          = vitalTrends,
            statusMessage        = voiceStatusMessage,
            onStop               = { VoiceNavigationManager.stopListening() },
            onCancel             = { VoiceNavigationManager.cancelPendingData() },
            onSaveAll            = { VoiceNavigationManager.confirmAndSaveAllPendingData() },
            onStartListening     = { VoiceNavigationManager.startListening { route -> navController.navigate(route) } },
            onRemoveVital        = { keys -> VoiceNavigationManager.removeVital(keys) },
            onRemoveSymptom      = { id, name -> VoiceNavigationManager.removeSymptom(id, name) },
            onRemoveFluidIntake  = { type -> VoiceNavigationManager.removeFluidIntake(type) },
            onRemoveFluidOutput  = { VoiceNavigationManager.removeFluidOutput() },
            onRemoveMedicine     = { medicineId -> VoiceNavigationManager.removeMedicine(medicineId) }
        )
    }
    }
}

private val DashboardCardGrey = Color(0xFFF3F5F8)

// ─────────────────────────────────────────────────────────────────────────────
//  HomeView  (tab 0 content)
//  Pure content composable – no FAB, no dialogs.
//  The voice FAB lives in DashboardScreen so it stays visible on every tab.
// ─────────────────────────────────────────────────────────────────────────────
@Composable
fun HomeView(viewModel: HomeViewModel = viewModel()) {
    val colors = LocalMyColorScheme.current
    val vitals by viewModel.vitalList.collectAsState()
    val appointments by viewModel.appointments.collectAsState()
    val dailyMedicines by viewModel.dailyMedicines.collectAsState()
    val markingMedicines by viewModel.markingMedicines.collectAsState()
    val loading by viewModel.loading.collectAsState()

    val homeDimens = rememberResponsiveDimens()

    // Pull-to-refresh is handled by DashboardScreen so the header can stretch with the pull.
    Box(modifier = Modifier.fillMaxSize().background(colors.dashboardBackgroundColor)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(homeDimens.screenPadding)
        ) {
            ContentStateCrossfade(
                // No separate empty state here: each section shows its own.
                state = if (loading && vitals.isEmpty() && dailyMedicines.isEmpty()) ContentState.Loading else ContentState.Content,
                loading = { VitalsCardShimmer() },
            ) {
                Column {
                    AnimatedSection(delayMillis = 50) {
                        UpcomingMedicationsSection(
                            dailyMedicines = dailyMedicines,
                            markingMedicines = markingMedicines,
                            onMarkIntake = { medicine ->
                                viewModel.markMedicineIntake(medicine)
                            }
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    AnimatedSection(delayMillis = 100) { VitalsCard(vitals = vitals, onUpdateClick = { viewModel.fetchDashboardNoCache() }) }
                }
            }

            Spacer(Modifier.height(20.dp))

        // Renders nothing (not even spacing) when there are no appointments.
        AnimatedSection(delayMillis = 150) { HomeScreen(appointments = appointments, loading = loading) }

        AnimatedSection(delayMillis = 200) {
            Text(text = "Primary Actions", style = AppTextStyles.style18BCB())
        }

        Spacer(Modifier.height(12.dp))

        PrimaryActionsGrid()

        Spacer(Modifier.height(20.dp))

        AnimatedSection(delayMillis = 750) { OtherSection() }

        // Extra bottom padding so the last card clears the persistent mic FAB
        Spacer(Modifier.height(80.dp))
    }
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
            HapticOutlinedButton(
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
    val haptic = rememberHaptics()

    NavigationBar(containerColor = colors.dashboardBackgroundColor) {

        val items = listOf(
            Triple(R.drawable.home,stringResource(R.string.home), 0),
            Triple(R.drawable.activity, stringResource(R.string.activity), 1),
            Triple(R.drawable.ring, stringResource(R.string.ring), 2)
            // Chat is hidden until its screen is built.
        )

        items.forEach { (iconRes, label, index) ->
            NavigationBarItem(
                selected = selectedIndex == index,
                onClick = {
                    if (index != selectedIndex) haptic(HapticEvent.Selection)
                    onItemSelected(index)
                },
                icon = { Icon(painterResource(id = iconRes), contentDescription = label) },
                label = { Text(label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primaryBlueColor,
                    selectedTextColor = colors.primaryBlueColor,
                    unselectedIconColor = colors.textGreyColor,
                    unselectedTextColor = colors.textGreyColor,
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

    // Name follows the greeting a beat later
    val nameProgress by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(550, delayMillis = 180, easing = FastOutSlowInEasing),
        label = "nameIn"
    )
    // Bell rings once when the dashboard opens
    val bell = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(600)
        bell.animateTo(0f, keyframes {
            durationMillis = 900
            18f at 90; -15f at 210; 11f at 330; -7f at 450; 3f at 570; 0f at 900
        })
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { this.alpha = alpha; translationY = offsetY },
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.critetiontech.ctvitalio.ui.components.ProfileRing(size = 51.dp) {
            AsyncImage(
                model = PrefsManager(context).getPatient()?.imageUrl
                    ?.takeIf { it.isNotBlank() },
                placeholder = painterResource(R.drawable.img),
                error = painterResource(R.drawable.img),
                contentDescription = "Patient Profile",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(43.dp)
                    .clip(CircleShape)
                    .clickable {
                        navController.navigate(Routes.DRAWER)
                    }
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${ToastUtils.getSimpleGreeting()},",
                        style = AppTextStyles.style12GCN())
                Spacer(Modifier.width(5.dp))
                TimeOfDayIcon()
            }
            Text(patientData?.firstName.toString(),
                    style = AppTextStyles.style18BCN(),
                    modifier = Modifier.graphicsLayer {
                        this.alpha = nameProgress
                        translationX = (1f - nameProgress) * -16.dp.toPx()
                    })
        }

        Spacer(Modifier.width(10.dp))
        Icon(
            Icons.Default.Notifications, contentDescription = null,
            modifier = Modifier.graphicsLayer {
                rotationZ = bell.value
                // Swing from the bell's top, not its centre
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0.5f, 0.1f)
            }
        )
    }
}

/** Small animated icon for the time of day: a turning sun, a setting sun, or a gently rocking moon. */
@Composable
private fun TimeOfDayIcon() {
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val motion = rememberInfiniteTransition(label = "timeOfDay")
    val spin by motion.animateFloat(0f, 360f, infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "sunSpin")
    val rock by motion.animateFloat(-10f, 10f, infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "moonRock")
    val glow by motion.animateFloat(0.75f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "glow")
    val (icon, tint, rotation) = when (hour) {
        in 5..17 -> Triple(Icons.Default.WbSunny, Color(0xFFF59E0B), spin)
        in 18..21 -> Triple(Icons.Default.WbTwilight, Color(0xFFF97316), 0f)
        else -> Triple(Icons.Default.Bedtime, Color(0xFF818CF8), rock)
    }
    Icon(
        icon, contentDescription = null, tint = tint,
        modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = rotation; this.alpha = glow; scaleX = glow; scaleY = glow }
    )
}

// ------------------- To Take Card -------------------
@Composable
fun ToTakeCard() {

    val navController = LocalNavController.current
    val colors = LocalMyColorScheme.current
    Column(modifier = Modifier.clickable {

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

                HapticCheckbox(checked = false, onCheckedChange = {})
            }
        }
    }
}

fun getTimeAgo(dateTime: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
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
                unit = bpSys.unit.trim(),
                vitalDateTime = bpSys.vitalDateTime,
                displayValue = "${bpSys.vitalValue.toInt()}/${bpDias.vitalValue.toInt()}"
            )
        )
    }

    return otherVitals
}
// ------------------- Shimmer -------------------
@Composable
fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFFE0E0E0),
        Color(0xFFF5F5F5),
        Color(0xFFE0E0E0),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateX by transition.animateFloat(
        initialValue = -600f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerX"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateX, 0f),
        end = Offset(translateX + 600f, 0f)
    )
}

@Composable
fun VitalsCardShimmer() {
    val brush = shimmerBrush()
    Column {
        Box(
            modifier = Modifier
                .width(60.dp).height(18.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(brush)
        )
        Spacer(Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F0F0)),
            modifier = Modifier.fillMaxWidth().padding(5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.size(24.dp).clip(CircleShape).background(brush))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.fillMaxWidth(0.55f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.fillMaxWidth(0.35f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                }
                Box(modifier = Modifier.width(60.dp).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            repeat(4) {
                Box(modifier = Modifier.padding(2.dp).size(6.dp).clip(CircleShape).background(brush))
            }
        }
    }
}

@Composable
fun AppointmentCardShimmer() {
    val brush = shimmerBrush()
    val whiteOverlay = Color.White.copy(alpha = 0.25f)
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2458C6).copy(alpha = 0.85f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(whiteOverlay))
                Spacer(Modifier.width(12.dp))
                Column {
                    Box(modifier = Modifier.width(130.dp).height(16.dp).clip(RoundedCornerShape(4.dp)).background(whiteOverlay))
                    Spacer(Modifier.height(6.dp))
                    Box(modifier = Modifier.width(95.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(whiteOverlay))
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.width(75.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(whiteOverlay))
                }
            }
            Spacer(Modifier.height(12.dp))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.White.copy(alpha = 0.3f)
            )
            Spacer(Modifier.height(12.dp))
            repeat(3) {
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(whiteOverlay))
                    Spacer(Modifier.width(8.dp))
                    Box(modifier = Modifier.width(150.dp).height(13.dp).clip(RoundedCornerShape(4.dp)).background(whiteOverlay))
                }
            }
        }
    }
}

// ------------------- Vitals Card -------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VitalsCard(vitals: List<Vital>, onUpdateClick: () -> Unit = {}) {

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

    // Auto slide, unhurried, and held while the user is swiping
    LaunchedEffect(pagerState) {
        while (true) {
            delay(3500.milliseconds)
            if (pagerState.isScrollInProgress) continue
            val nextPage = (pagerState.currentPage + 1) % mergedVitals.size
            pagerState.animateScrollToPage(nextPage, animationSpec = tween(700, easing = FastOutSlowInEasing))
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
            val isCurrent = pagerState.currentPage == page

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.dashboardContainerColor
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(5.dp)
                    .graphicsLayer {
                        // Pages leaving the centre shrink and fade a little, for depth
                        val offset = kotlin.math.abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        val scale = 1f - 0.08f * offset
                        scaleX = scale; scaleY = scale
                        alpha = 1f - 0.45f * offset
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(com.critetiontech.ctvitalio.ui.theme.softTint(colors.primaryBlueColor, Color(0xFFEAF2FF))),
                        contentAlignment = Alignment.Center
                    ) {
                        val beat = rememberInfiniteTransition(label = "vitalBeat")
                        val beatScale by beat.animateFloat(
                            1f, 1.12f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "beatScale"
                        )
                        Image(
                            painter = painterResource(id = R.drawable.vital_details),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).graphicsLayer {
                                val s = if (isCurrent) beatScale else 1f
                                scaleX = s; scaleY = s
                            }
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = vital.vitalName,
                            style = AppTextStyles.style14BCB().copy(color = colors.textDarkColor, fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = getTimeAgo(vital.vitalDateTime),
                            style = AppTextStyles.style12GCN().copy(color = colors.textGreyColor)
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center
                    ) {
                        val displayVal = if (vital.displayValue.isNotEmpty()) vital.displayValue else vital.vitalValue.toInt().toString()
                        
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = countUpText(displayVal, run = isCurrent),
                                style = AppTextStyles.style24BCB().copy(fontSize = 24.sp, color = colors.textDarkColor, fontWeight = FontWeight.Bold),
                                modifier = Modifier.alignByBaseline()
                            )
                            Spacer(Modifier.width(2.dp))
                            Text(
                                text = displayUnit(vital.unit),
                                style = AppTextStyles.style12BCN().copy(color = colors.textGreyColor),
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                        
                        Spacer(Modifier.height(4.dp))
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable { onUpdateClick() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Update",
                                style = AppTextStyles.style12BCN().copy(color = colors.textGreyColor)
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colors.textGreyColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
                // The active dot stretches into a pill in the brand colour
                val width by animateDpAsState(if (selected) 18.dp else 6.dp, spring(stiffness = Spring.StiffnessMediumLow), label = "dotW")
                val dotColor by animateColorAsState(
                    if (selected) colors.primaryBlueColor else colors.textGreyColor.copy(alpha = 0.3f), label = "dotC"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.dp)
                        .height(6.dp)
                        .width(width)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
        }
    }
}

/**
 * Counts a vital's number up from zero each time [run] turns on: "72" or "120/80" (both parts).
 * Anything else (decimals, text) is shown as is.
 */
@Composable
private fun countUpText(value: String, run: Boolean): String {
    val parts = value.split("/")
    if (parts.size > 2 || parts.any { it.isEmpty() || !it.all(Char::isDigit) || it.length > 4 }) return value
    val progress = remember { Animatable(1f) }
    LaunchedEffect(run, value) {
        if (run) {
            progress.snapTo(0f)
            progress.animateTo(1f, tween(800, easing = FastOutSlowInEasing))
        }
    }
    return parts.joinToString("/") { (it.toInt() * progress.value).toInt().toString() }
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

//    GridItem(
//        title = stringResource(R.string.research_based_articles),
//        icon = R.drawable.medicine_reminder,
//        type = "articles"
//    ),

//    GridItem(
//        title = "Digital Twin",
//        icon = R.drawable.vital_details,
//        type = "digitalTwin"
//    ),
//
//    GridItem(
//        title = "Vitals Orbit",
//        icon = R.drawable.activity,
//        type = "healthOrbit"
//    )a
    )

    var gridVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { gridVisible = true }

    val dimens = rememberResponsiveDimens()
    val gridColumns = if (LocalWindowSizeClass.current == WindowWidthSizeClass.Compact) 3 else 4
    // Calculate grid height based on number of rows
    val rowCount = (items.size + gridColumns - 1) / gridColumns
    val gridHeight = (rowCount * 110 + (rowCount - 1) * 10).dp

    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = Modifier
            .fillMaxWidth()
            .height(gridHeight),
        verticalArrangement = Arrangement.spacedBy(dimens.cardSpacing),
        horizontalArrangement = Arrangement.spacedBy(dimens.cardSpacing)
    ) {

        itemsIndexed(items) { index, item ->
            // Tiles pop in one after another with a soft spring
            val pop = remember { Animatable(0f) }
            LaunchedEffect(gridVisible) {
                if (gridVisible) {
                    delay(80L + index * 60L)
                    pop.animateTo(1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
                }
            }
            val itemAlpha = pop.value.coerceIn(0f, 1f)
            val itemY = (1f - pop.value) * 28f

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .graphicsLayer {
                        alpha = itemAlpha; translationY = itemY
                        val sc = 0.82f + 0.18f * pop.value
                        scaleX = sc; scaleY = sc
                    }
                    .bounceClick {
                        when (item.type) {
                            "vitals" -> navController.navigate(Routes.VITALS)
                            "fluid" -> navController.navigate(Routes.FLUIDDATAINPUT)
                            "symptomsTracker" -> navController.navigate(Routes.SYMPTOMSTRACKER)
                            "medicine" -> navController.navigate(Routes.MEDICINE)
                            "diet" -> navController.navigate(Routes.DIETCHECKLIST)
                            "interaction" -> navController.navigate(Routes.INTERACTIONCHECKER)
                            "findDoctor" -> navController.navigate(Routes.FINDDOCTOR)
                            "articles" -> navController.navigate(Routes.RESEARCHARTICLES)
//                            "digitalTwin" -> navController.navigate(Routes.DIGITAL_TWIN)
//                            "healthOrbit" -> navController.navigate(Routes.HEALTH_ORBIT)
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
fun HomeScreen(
    appointments: List<UpcomingAppointment> = emptyList(),
    loading: Boolean = false
) {
    val navController = LocalNavController.current
    Column(modifier = Modifier.fillMaxWidth()) {
        when {
            loading && appointments.isEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Appointments", style = AppTextStyles.style18BCB())
                }
                Spacer(Modifier.height(12.dp))
                AppointmentCardShimmer()
                Spacer(Modifier.height(20.dp))
            }
            appointments.isNotEmpty() -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Upcoming Appointments", style = AppTextStyles.style18BCB())
                    Text(
                        "View All",
                        style = AppTextStyles.style12GCN(),
                        color = Color(0xFF2F6FE4),
                        modifier = Modifier.clickable { navController.navigate(Routes.APPOINTMENTS) }
                    )
                }
                Spacer(Modifier.height(12.dp))
                // Several appointments: cards scroll sideways and the next one peeks in.
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val cardWidth = if (appointments.size == 1) maxWidth else maxWidth * 0.85f
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(appointments) { appointment ->
                            AppointmentCard(appointment, modifier = Modifier.width(cardWidth))
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

// ------------------- Appointment Card -------------------
fun formatAppointmentDate(date: String): String = try {
    val input = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val output = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    output.format(input.parse(date)!!)
} catch (e: Exception) { date }

fun formatSlotTime(time: String): String = try {
    val input = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val output = SimpleDateFormat("h:mm a", Locale.getDefault())
    output.format(input.parse(time)!!)
} catch (e: Exception) { time }

@Composable
fun AppointmentCard(appointment: UpcomingAppointment, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2458C6)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Photo when the server has one, else the doctor's initials.
                com.critetiontech.ctvitalio.ui.screens.DoctorPhoto(
                    name = appointment.doctorName.trim(),
                    url = com.critetiontech.ctvitalio.ui.screens.doctorPhotoUrl(appointment.profileImage),
                    initialsSize = 18.sp,
                    modifier = Modifier
                        .size(52.dp)
                        .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape)
                        .padding(2.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        appointment.doctorName.trim(),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        appointment.qualification,
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                    if (appointment.departmentName.isNotEmpty()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            appointment.departmentName,
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(
                Modifier,
                DividerDefaults.Thickness,
                color = Color.White.copy(alpha = 0.2f)
            )
            Spacer(Modifier.height(14.dp))

            AppointmentRow(Icons.Default.DateRange, formatAppointmentDate(appointment.appointmentDate))
            // Token (shift) bookings have no slot time.
            if (appointment.slotTime.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                AppointmentRow(Icons.Default.Schedule, formatSlotTime(appointment.slotTime))
            }
            if (appointment.clinicName.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                AppointmentRow(Icons.Default.LocationOn, appointment.clinicName)
            }
        }
    }
}

@Composable
fun AppointmentRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.White.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(text, color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
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
            .clickable {

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
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Quick Actions", style = AppTextStyles.style18BCB())
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Max),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UploadReportCard(Modifier.weight(1f).fillMaxHeight())
            UploadPrescriptionCard(Modifier.weight(1f).fillMaxHeight())
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
                HapticButton(
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
fun UploadReportCard(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    QuickActionCard(
        title = "Upload Report",
        subtitle = "Add your lab reports & documents",
        accent = Color(0xFF2F6FE4),
        modifier = modifier,
        onClick = { navController.navigate(Routes.LABREPORTS) }
    ) {
        Icon(
            painter = painterResource(R.drawable.upload_report),
            contentDescription = null,
            modifier = Modifier.size(26.dp),
            tint = Color.Unspecified
        )
    }
}

@Composable
fun UploadPrescriptionCard(modifier: Modifier = Modifier) {
    val navController = LocalNavController.current
    QuickActionCard(
        title = "Upload Prescription",
        subtitle = "AI reads medicines & sets reminders",
        accent = Color(0xFF0E9384),
        modifier = modifier,
        onClick = { navController.navigate(Routes.UPLOADPRESCRIPTION) }
    ) {
        Icon(
            Icons.Default.DocumentScanner,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = Color(0xFF0E9384)
        )
    }
}

@Composable
private fun QuickActionCard(
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    val colors = LocalMyColorScheme.current
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = colors.dashboardContainerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) { icon() }
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color(0xFFAAAAAA),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = AppTextStyles.style14BCN())
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = AppTextStyles.style12GCN())
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
private enum class IntakeStatus { Taken, Skipped, Due, Upcoming }

private fun com.critetiontech.ctvitalio.model.MedicineItem.scheduledAt(): java.time.LocalDateTime? =
    try { java.time.LocalDateTime.parse(scheduledDateTime) } catch (e: Exception) { null }

private fun com.critetiontech.ctvitalio.model.MedicineItem.intakeStatus(): IntakeStatus {
    val scheduled = scheduledAt()
    return when {
        isTaken && takenDateTime != "1900-01-01T00:00:00" -> IntakeStatus.Taken
        isSkipped -> IntakeStatus.Skipped
        scheduled != null && java.time.LocalDateTime.now() >= scheduled -> IntakeStatus.Due
        else -> IntakeStatus.Upcoming
    }
}

private val MedBlue = Color(0xFF2F6FE4)

/**
 * Today's medicines as a card stack: the next one needing attention sits on top with
 * the rest peeking out underneath. Tapping the stack (or "+N More") fans it out into a list.
 */
@Composable
fun UpcomingMedicationsSection(
    dailyMedicines: List<com.critetiontech.ctvitalio.model.MedicineItem>,
    markingMedicines: Set<Int>,
    onMarkIntake: (com.critetiontech.ctvitalio.model.MedicineItem) -> Unit
) {
    if (dailyMedicines.isEmpty()) return

    var expanded by remember { mutableStateOf(false) }
    val ordered = remember(dailyMedicines) {
        val (pending, done) = dailyMedicines.partition {
            it.intakeStatus() == IntakeStatus.Due || it.intakeStatus() == IntakeStatus.Upcoming
        }
        pending + done
    }
    val extra = ordered.size - 1
    val arrowRotation by animateFloatAsState(if (expanded) 180f else 0f, label = "medArrow")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Today's Medications", style = AppTextStyles.style18BCB())
            if (extra > 0) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(0.dp))
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (expanded) "Show Less" else "+$extra More",
                        style = AppTextStyles.style12BCN().copy(color = MedBlue, fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Default.KeyboardArrowDown, null,
                        tint = MedBlue,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = arrowRotation }
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        androidx.compose.animation.AnimatedContent(
            targetState = expanded,
            transitionSpec = {
                (androidx.compose.animation.fadeIn(tween(220, delayMillis = 60)) togetherWith
                    androidx.compose.animation.fadeOut(tween(120)))
                    .using(androidx.compose.animation.SizeTransform(clip = false))
            },
            label = "medStack"
        ) { isExpanded ->
            if (isExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ordered.forEach { med ->
                        MedicineCardItem(
                            medicine = med,
                            isMarking = med.id in markingMedicines,
                            onMarkIntake = onMarkIntake,
                        )
                    }
                }
            } else {
                MedicineStack(
                    medicines = ordered,
                    markingMedicines = markingMedicines,
                    onMarkIntake = onMarkIntake,
                    onExpand = { if (extra > 0) expanded = true }
                )
            }
        }
    }
}

@Composable
private fun MedicineStack(
    medicines: List<com.critetiontech.ctvitalio.model.MedicineItem>,
    markingMedicines: Set<Int>,
    onMarkIntake: (com.critetiontech.ctvitalio.model.MedicineItem) -> Unit,
    onExpand: () -> Unit,
) {
    val colors = LocalMyColorScheme.current
    val layers = (medicines.size - 1).coerceAtMost(2)
    val shape = RoundedCornerShape(16.dp)
    val peek = 7.dp

    Box(modifier = Modifier.fillMaxWidth().padding(bottom = peek * layers)) {
        // Back layers first so the front card draws on top.
        for (layer in layers downTo 1) {
            Box(
                Modifier
                    .matchParentSize()
                    .padding(horizontal = 10.dp * layer)
                    .offset(y = peek * layer)
                    .shadow(1.dp, shape)
                    .clip(shape)
                    .background(colors.dashboardContainerColor)
            )
        }
        MedicineCardItem(
            medicine = medicines.first(),
            isMarking = medicines.first().id in markingMedicines,
            onMarkIntake = onMarkIntake,
            onCardClick = if (layers > 0) onExpand else null,
        )
    }
}

@Composable
fun MedicineCardItem(
    medicine: com.critetiontech.ctvitalio.model.MedicineItem,
    isMarking: Boolean,
    onMarkIntake: (com.critetiontech.ctvitalio.model.MedicineItem) -> Unit,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null,
) {
    val colors = LocalMyColorScheme.current
    val context = LocalContext.current
    val navController = LocalNavController.current
    val status = medicine.intakeStatus()
    val shape = RoundedCornerShape(16.dp)
    val timeText = medicine.scheduledAt()?.format(
        java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.getDefault())
    ).orEmpty()
    val strength = medicine.dosageStrength.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
    val doseText = "$strength ${medicine.unit.orEmpty()}".trim()
    var menuOpen by remember { mutableStateOf(false) }

    val onCheck: () -> Unit = {
        when (status) {
            IntakeStatus.Due -> onMarkIntake(medicine)
            IntakeStatus.Upcoming -> android.widget.Toast.makeText(
                context, "Scheduled for $timeText. You can mark it once it is due.", android.widget.Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape, ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            .clip(shape)
            .background(colors.dashboardContainerColor)
            // Long-press opens the same menu as the ⋮ button.
            .hapticCombinedClick(onClick = onCardClick, onLongClick = { menuOpen = true })
            .padding(start = 16.dp, end = 6.dp, top = 14.dp, bottom = 12.dp)
    ) {
        CapsuleIcon(tint = if (status == IntakeStatus.Taken) Color(0xFF2DBE7E) else Color(0xFFA3AAB8))

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (doseText.isNotBlank()) {
                    Text(
                        doseText,
                        style = AppTextStyles.style12BCN().copy(fontSize = 11.sp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.dashboardBackgroundColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                val (label, labelColor) = when (status) {
                    IntakeStatus.Taken -> "Taken" to Color(0xFF2DBE7E)
                    IntakeStatus.Skipped -> "Skipped" to Color(0xFFE65100)
                    IntakeStatus.Due -> "Take now · $timeText" to MedBlue
                    IntakeStatus.Upcoming -> timeText to Color(0xFF94A3B8)
                }
                if (label.isNotBlank()) {
                    Text(label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = labelColor)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                medicine.medicineName.orEmpty(),
                style = AppTextStyles.style18BCB().copy(fontSize = 17.sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                medicine.instructions.orEmpty().ifBlank { "As prescribed" },
                style = AppTextStyles.style12BCN().copy(color = MedBlue, fontSize = 13.sp),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IntakeCheckbox(status = status, isMarking = isMarking, onClick = onCheck)
            Box {
                HapticIconButton(onClick = { menuOpen = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.MoreVert, "More", tint = colors.textDarkColor, modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (status == IntakeStatus.Due) {
                        DropdownMenuItem(
                            text = { Text("Mark as taken") },
                            onClick = { menuOpen = false; onMarkIntake(medicine) }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Manage medications") },
                        onClick = { menuOpen = false; navController.navigate(Routes.MANAGE_MEDICINE) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CapsuleIcon(tint: Color) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp)) {
        Box(
            Modifier
                .graphicsLayer { rotationZ = -45f }
                .size(width = 30.dp, height = 13.dp)
                .border(1.5.dp, tint, RoundedCornerShape(50))
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .width(1.5.dp)
                    .fillMaxHeight()
                    .background(tint)
            )
        }
    }
}

@Composable
private fun IntakeCheckbox(status: IntakeStatus, isMarking: Boolean, onClick: () -> Unit) {
    val colors = LocalMyColorScheme.current
    val checked = status == IntakeStatus.Taken
    val fill by animateColorAsState(if (checked) MedBlue else Color.Transparent, label = "boxFill")
    val stroke by animateColorAsState(
        when (status) {
            IntakeStatus.Taken -> MedBlue
            IntakeStatus.Due -> colors.textDarkColor
            else -> Color(0xFFCBD5E1)
        },
        label = "boxStroke"
    )
    // A small bounce of the box as it fills.
    val boxScale by animateFloatAsState(
        if (checked) 1f else 0.92f,
        spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium),
        label = "boxScale"
    )

    // 40dp touch target around a 22dp box.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .successPulse(done = checked, color = MedBlue)
            .clickable(enabled = !isMarking && (status == IntakeStatus.Due || status == IntakeStatus.Upcoming), onClick = onClick)
    ) {
        // The box stays composed under the spinner so the tick can draw in once the save lands.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer { alpha = if (isMarking) 0f else 1f; scaleX = boxScale; scaleY = boxScale }
                .clip(RoundedCornerShape(6.dp))
                .background(fill)
                .border(1.6.dp, stroke, RoundedCornerShape(6.dp))
        ) {
            AnimatedCheckMark(checked = checked, modifier = Modifier.size(18.dp))
        }
        if (isMarking) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MedBlue, strokeWidth = 2.dp)
        }
    }
}
