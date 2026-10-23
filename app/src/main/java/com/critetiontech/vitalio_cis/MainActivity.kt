package com.critetiontech.ctvitalio

import com.critetiontech.ctvitalio.ui.feedback.AppNavTransitions
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.imePadding
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.critetiontech.ctvitalio.ui.screens.LoginScreen
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.ctvitalio.ui.screens.AddActivityScreen
import com.critetiontech.ctvitalio.ui.screens.ActivityHistoryScreen
import com.critetiontech.ctvitalio.ui.screens.AddLabResultsScreen
import com.critetiontech.ctvitalio.ui.screens.AddMedicineReminderScreen
import com.critetiontech.ctvitalio.ui.screens.AddMemberScreen
import com.critetiontech.ctvitalio.ui.screens.AiReportScreen
import com.critetiontech.ctvitalio.ui.screens.AllergiesScreen
import com.critetiontech.ctvitalio.ui.screens.ArticleDetailScreen
import com.critetiontech.ctvitalio.ui.screens.BookingConfirmationScreen
import com.critetiontech.ctvitalio.ui.screens.BookingDetails
import com.critetiontech.ctvitalio.ui.screens.BookingDetailsScreen
import com.critetiontech.ctvitalio.ui.screens.ConnectWatchScreen
import com.critetiontech.ctvitalio.ui.screens.ConnectionScreen
import com.critetiontech.ctvitalio.ui.screens.HealthConnectSyncScreen
import com.critetiontech.ctvitalio.ui.screens.OmronDeviceScreen
import com.critetiontech.ctvitalio.ui.screens.YonkerDeviceScreen
 import com.critetiontech.ctvitalio.ui.screens.DietChecklistScreen
import com.critetiontech.ctvitalio.ui.screens.DoctorDetailsScreen
import com.critetiontech.ctvitalio.ui.screens.DrawerScreen
import com.critetiontech.ctvitalio.ui.screens.EmergencyContactsScreen
import com.critetiontech.ctvitalio.ui.screens.FAQScreen
import com.critetiontech.ctvitalio.ui.screens.FamilyHealthScreen
import com.critetiontech.ctvitalio.ui.screens.FeedbackScreen
import com.critetiontech.ctvitalio.ui.screens.FindDoctorsTopSection
import com.critetiontech.ctvitalio.ui.screens.FluidDataInputScreen
import com.critetiontech.ctvitalio.ui.screens.FluidInputHistoryScreen
import com.critetiontech.ctvitalio.ui.screens.FluidOutputHistoryScreen
import com.critetiontech.ctvitalio.ui.screens.InteractionCheckerScreen
import com.critetiontech.ctvitalio.ui.screens.InteractionInstructionView
import com.critetiontech.ctvitalio.viewmodel.InteractionCheckerViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.ui.screens.LabReportsScreen
import com.critetiontech.ctvitalio.ui.screens.ManageMedicationsPreview
import com.critetiontech.ctvitalio.ui.screens.ManageMedicationsScreen
import com.critetiontech.ctvitalio.ui.screens.MedicalProfileScreen
import com.critetiontech.ctvitalio.ui.screens.MedicineReminderScreen
import com.critetiontech.ctvitalio.ui.screens.MyObserversScreen
import com.critetiontech.ctvitalio.ui.screens.OtpScreen
import com.critetiontech.ctvitalio.ui.screens.PersonalInfoScreen
import com.critetiontech.ctvitalio.ui.screens.PrescriptionScreen
import com.critetiontech.ctvitalio.ui.screens.PreviewFindDoctors
 import com.critetiontech.ctvitalio.ui.screens.RemindersScreen
import com.critetiontech.ctvitalio.ui.screens.ResearchArticlesScreen
import com.critetiontech.ctvitalio.ui.screens.SelectClinicScreen
import com.critetiontech.ctvitalio.ui.screens.SharedAccountScreen
import com.critetiontech.ctvitalio.ui.screens.SymptomTrackerScreen
import com.critetiontech.ctvitalio.ui.screens.SymptomsView
import com.critetiontech.ctvitalio.ui.screens.VitalHistoryScreen
import com.critetiontech.ctvitalio.ui.screens.VitalsScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.BloodScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.ChronicDiseaseScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.CreateAccountPreviewScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.DobScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.FamilyHistoryScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.FluidDetailsScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.FluidIntakeDetailsScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.GenderScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.HeightScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.HydrationScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.LocationScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.RegistrationCompletedView
import com.critetiontech.ctvitalio.ui.screens.onboarding.ReminderPreferenceScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.ReminderScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.ThankYouView
import com.critetiontech.ctvitalio.ui.screens.onboarding.WeightScreen
import com.critetiontech.ctvitalio.ui.screens.onboarding.WelcomeScreen
import com.critetiontech.ctvitalio.ui.theme.LocalThemeViewModel
import com.critetiontech.ctvitalio.ui.theme.MyAppTheme
import com.critetiontech.ctvitalio.consultation.callPush
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.viewmodel.UploadReportViewModel
import com.critetiontech.ctvitalio.viewmodel.UploadPrescriptionViewModel
import com.critetiontech.ctvitalio.ui.screens.UploadPrescriptionScreen
import com.critetiontech.ctvitalio.ui.screens.PrescriptionAnalysisScreen
import com.critetiontech.ctvitalio.ui.adaptive.AdaptiveLayoutProvider
import com.critetiontech.ctvitalio.voice.VoiceNavigationManager
import com.example.vitalio_cis.ui.screens.DashboardScreen
import com.google.gson.Gson
import androidx.lifecycle.lifecycleScope
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import kotlinx.coroutines.launch
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {

    override fun onResume() {
        super.onResume()
        // Pull new readings from the user's watch/health apps (throttled inside)
        lifecycleScope.launch { WearableSync.syncIfDue(applicationContext) }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        logPushTap(intent)
    }

    /** A push tapped while the app was in the background brings its data in the launch intent. */
    private fun logPushTap(intent: android.content.Intent?) {
        val extras = intent?.extras ?: return
        if (!extras.containsKey("google.message_id")) return
        val data = extras.keySet().associateWith { extras.get(it) }
        android.util.Log.d("FCM", "Opened from push: $data")
        // A call push that Firebase showed itself (app was closed): tapping it means "join"
        val call = extras.callPush()
        if (call is com.critetiontech.ctvitalio.model.CallPush.Incoming) {
            startActivity(com.critetiontech.ctvitalio.consultation.CallActivity.intent(this, call.call, answer = true))
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        logPushTap(intent)
        setContent {
            val navController = rememberNavController()
            val uploadReportViewModel: UploadReportViewModel = viewModel()
            val uploadPrescriptionViewModel: UploadPrescriptionViewModel = viewModel()
            val interactionViewModel: InteractionCheckerViewModel = viewModel()
            val context = LocalContext.current
            VoiceNavigationManager.initTts(this)
            AdaptiveLayoutProvider {
            MyAppTheme {
                CompositionLocalProvider(LocalNavController provides navController) {
                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                     ) {
                        NavHost(
                            navController = navController,
                            // The window is adjustResize (manifest), which on Android 15+ no
                            // longer shrinks the window — so shrink every screen above the
                            // keyboard here, once. Screens' own imePadding() then become no-ops.
                            modifier = androidx.compose.ui.Modifier.imePadding(),
                            startDestination = if (PrefsManager(context).getPatient()?.uhId?.isNotEmpty()
                                    ?: false) Routes.DASHBOARD else Routes.LOGIN,
                            enterTransition = AppNavTransitions.enter,
                            exitTransition = AppNavTransitions.exit,
                            popEnterTransition = AppNavTransitions.popEnter,
                            popExitTransition = AppNavTransitions.popExit,
                        ) {
                            composable(Routes.DASHBOARD) { DashboardScreen() }
                            composable(Routes.LOGIN) { LoginScreen() }
                            composable(Routes.MANAGEMEDICAIONS) { ManageMedicationsPreview() }
                            composable(Routes.ADDMEDICINEREMINDER) { AddMedicineReminderScreen() }
                            composable(Routes.INTERACTIONCHECKER) { InteractionCheckerScreen(viewModel = interactionViewModel) }
                            composable(Routes.INTERACTIONINSTRUCTION) { InteractionInstructionView(viewModel = interactionViewModel) }
                            composable(Routes.DIETCHECKLIST) { DietChecklistScreen() }
                            composable(Routes.NUTRIENTS) { com.critetiontech.ctvitalio.ui.screens.NutrientSearchScreen() }
                            composable(Routes.PRESCRIPTION) { PrescriptionScreen() }
                            composable(Routes.FLUIDDATAINPUT) { FluidDataInputScreen() }
                            composable(Routes.FLUIDOUTPUTHISTORY) { FluidOutputHistoryScreen() }
                            composable(Routes.FLUIDINPUTHISTORY) { FluidInputHistoryScreen() }
                            composable(
                                route = "${Routes.VITALHISTORY}?vitalIds={vitalIds}&vitalName={vitalName}&unit={unit}",
                                arguments = listOf(
                                    navArgument("vitalIds") { type = NavType.StringType; defaultValue = "" },
                                    navArgument("vitalName") { type = NavType.StringType; defaultValue = "Vital" },
                                    navArgument("unit") { type = NavType.StringType; defaultValue = "" }
                                )
                            ) { backStackEntry ->
                                val vitalIds = backStackEntry.arguments?.getString("vitalIds") ?: ""
                                val vitalName = backStackEntry.arguments?.getString("vitalName") ?: "Vital"
                                val unit = backStackEntry.arguments?.getString("unit") ?: ""
                                VitalHistoryScreen(vitalIds = vitalIds, vitalName = vitalName, unit = unit)
                            }
                            composable(
                                Routes.ADDLABRESULTS
                            ) {

                                AddLabResultsScreen(

                                    viewModel =
                                        uploadReportViewModel
                                )
                            }

                            composable(
                                Routes.AIREPORT
                            ) {

                                AiReportScreen(

                                    viewModel =
                                        uploadReportViewModel
                                )
                            }
                            composable(Routes.UPLOADPRESCRIPTION) {
                                UploadPrescriptionScreen(viewModel = uploadPrescriptionViewModel)
                            }
                            composable(Routes.PRESCRIPTIONANALYSIS) {
                                PrescriptionAnalysisScreen(viewModel = uploadPrescriptionViewModel)
                            }
                            composable(
                                route = Routes.CONNECTION + "/{vitalName}/{unitName}"
                            ) { backStackEntry ->

                                val vitalName =
                                    backStackEntry.arguments
                                        ?.getString("vitalName") ?: ""
                                val unitName =
                                    backStackEntry.arguments
                                        ?.getString("unitName") ?: ""

                                ConnectionScreen(
                                    vitalName = vitalName,
                                    unitName = unitName
                                )
                            }
                            composable(Routes.ARTICALEDETAILS) { ArticleDetailScreen() }
                            composable(Routes.MYOBSERVERS) { MyObserversScreen() }
                            composable(Routes.FAMILYHEALTH) { FamilyHealthScreen() }
                            composable(Routes.ADDACTIVITY) { AddActivityScreen() }
                            composable(Routes.ACTIVITYHISTORY) { ActivityHistoryScreen() }
                            composable(Routes.REMINDERS) { RemindersScreen() }
                            composable(Routes.EMERGENCYCONTACTS) { EmergencyContactsScreen() }
                            composable(Routes.CONNECTWATCH) { ConnectWatchScreen() }
                            composable(Routes.SHAREDACCOUNT) { SharedAccountScreen() }
                            composable(Routes.ADDMEMBER) { AddMemberScreen() }
                            composable(Routes.FAQ) { FAQScreen() }
                            composable(Routes.FEEDBACK) { FeedbackScreen() }
                            composable(
                                Routes.OTP,
                                arguments = listOf(navArgument("mobile") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val mobile = backStackEntry.arguments?.getString("mobile") ?: ""
                                OtpScreen(mobile = mobile)
                            }
                            composable(Routes.SELECTCLINIC) { SelectClinicScreen() }
                            composable(Routes.MEDICALPROFILE) { MedicalProfileScreen() }
                            composable(Routes.BOOKINGDETAILS+"/{bookingDetails}",
                                arguments = listOf(
                                    navArgument("bookingDetails") { type = NavType.StringType }
                                )) {backStackEntry ->
                                val json = backStackEntry.arguments?.getString("bookingDetails")

                                val bookingDetails =
                                    Gson().fromJson(json, BookingDetails::class.java)

                                BookingDetailsScreen(
                                    bookingDetails =bookingDetails) }

                            composable(Routes.BOOKINGCONFERMATION+"/{bookingDetails}",

                                arguments = listOf(
                                    navArgument("bookingDetails") { type = NavType.StringType }
                                ))
                            { backStackEntry ->

                                val json = backStackEntry.arguments?.getString("bookingDetails")

                                val bookingDetails =
                                    Gson().fromJson(json, BookingDetails::class.java)

                                BookingConfirmationScreen(
                                bookingDetails =bookingDetails

                            )
                            }
                            composable(Routes.DOCTORDETAILS+"/{doctorId}/{days}",
                                arguments = listOf(
                                    navArgument("doctorId") { type = NavType.StringType },
                                    navArgument("days") { type = NavType.StringType }
                                )
                            ) { backStackEntry ->

                                val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
                                val days = backStackEntry.arguments?.getString("days") ?: ""
                                DoctorDetailsScreen(
                                    doctorId = doctorId,
                                    days = days,
                                 )}


                            composable(Routes.ALLERGIESSCREEN) { AllergiesScreen() }
                            composable(Routes.VITALS) { VitalsScreen() }
                            composable(Routes.SYMPTOMSTRACKER) { SymptomTrackerScreen() }
                            composable(Routes.APPOINTMENTS) { FindDoctorsTopSection() }
                            composable(Routes.SYMPTOMS) { SymptomsView() }
                            composable(Routes.MANAGE_MEDICINE) { ManageMedicationsScreen() }
                            composable(Routes.MEDICINE) { MedicineReminderScreen() }
                            composable(Routes.DRAWER) { DrawerScreen() }
                            composable(Routes.PERSONALIFSCREEN) { PersonalInfoScreen() }
                            composable(Routes.LABREPORTS) { LabReportsScreen() }
                            composable(Routes.FINDDOCTOR) { PreviewFindDoctors() }
                            composable(Routes.RESEARCHARTICLES) { ResearchArticlesScreen() }
                            composable(Routes.HEALTHCONNECTSYNC) { HealthConnectSyncScreen() }
                            composable(Routes.PERMISSIONS) { com.critetiontech.ctvitalio.ui.screens.PermissionsScreen() }
                            composable(Routes.OMRON_DEVICE + "/{model}") { backStackEntry ->
                                OmronDeviceScreen(modelCode = backStackEntry.arguments?.getString("model").orEmpty())
                            }
                            composable(Routes.YONKER_DEVICE + "/{kind}") { backStackEntry ->
                                YonkerDeviceScreen(kindCode = backStackEntry.arguments?.getString("kind").orEmpty())
                            }
                            composable("welcome") { WelcomeScreen( ) }
                            composable("gender") { GenderScreen( ) }
                            composable("dob") { DobScreen( ) }
                            composable("blood") { BloodScreen( ) }
                            composable("location") { LocationScreen( ) }
                            composable("reminder") { ReminderScreen( ) }
                            composable("hydration") { HydrationScreen( ) }
                            composable("weight") { WeightScreen( ) }
                            composable("height") { HeightScreen( ) }
                            composable("chronicDisease") { ChronicDiseaseScreen( ) }
                            composable("familyHistory") { FamilyHistoryScreen( ) }
                            composable("createAccount") { CreateAccountPreviewScreen( ) }
                            composable("thankYou") { ThankYouView( ) }
                            composable("reminderPreference") { ReminderPreferenceScreen( ) }
                            composable("fluidIntakeDetails") { FluidIntakeDetailsScreen( ) }
                            composable("fluidDetails") { FluidDetailsScreen( ) }
                            composable("registrationCompleted") { RegistrationCompletedView( ) }
                            composable(Routes.BREATHING_THERAPY) { com.critetiontech.ctvitalio.ui.screens.BreathingTherapyScreen() }
                            composable(Routes.DIGITAL_TWIN) { com.critetiontech.ctvitalio.ui.screens.DigitalTwinScreen() }
                            composable(Routes.HEALTH_ORBIT) { com.critetiontech.ctvitalio.ui.screens.HealthOrbitScreen() }
                            composable(Routes.EDITPROFILE) { com.critetiontech.ctvitalio.ui.screens.EditProfileScreen() }
                            composable(Routes.MEDICINE_REMINDER_SETTINGS) { com.critetiontech.ctvitalio.ui.screens.MedicineReminderSettingsScreen() }
                        }

                        // Status/navigation bar icons that stay readable on every page: dark icons
                        // on the light theme, light icons on the dark theme and on the blue
                        // login/OTP header. Home sets its own (it changes while pulling down).
                        val backStack by navController.currentBackStackEntryAsState()
                        val route = backStack?.destination?.route
                        val darkTheme = com.critetiontech.ctvitalio.ui.theme.isAppDark
                        val view = androidx.compose.ui.platform.LocalView.current
                        androidx.compose.runtime.LaunchedEffect(route, darkTheme) {
                            if (route == null || route == Routes.DASHBOARD) return@LaunchedEffect
                            val blueHeader = route == Routes.LOGIN || route == Routes.OTP
                            androidx.core.view.WindowCompat.getInsetsController(window, view).apply {
                                isAppearanceLightStatusBars = !darkTheme && !blueHeader
                                isAppearanceLightNavigationBars = !darkTheme
                            }
                        }
                    }
                }
            }
            }
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        VoiceNavigationManager.releaseTts()
    }
}