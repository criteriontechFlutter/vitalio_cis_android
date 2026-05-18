package com.example.vitalio_cis

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.critetiontech.ctvitalio.ui.screens.LoginScreen
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.AddActivityScreen
import com.example.vitalio_cis.ui.screens.AddLabResultsScreen
import com.example.vitalio_cis.ui.screens.AddMedicineReminderScreen
import com.example.vitalio_cis.ui.screens.AddMemberScreen
import com.example.vitalio_cis.ui.screens.AllergiesScreen
import com.example.vitalio_cis.ui.screens.ArticleDetailScreen
import com.example.vitalio_cis.ui.screens.BookingConfirmationScreen
import com.example.vitalio_cis.ui.screens.BookingDetails
import com.example.vitalio_cis.ui.screens.BookingDetailsScreen
import com.example.vitalio_cis.ui.screens.ConnectWatchScreen
import com.example.vitalio_cis.ui.screens.ConnectionScreen
import com.example.vitalio_cis.ui.screens.DashboardScreen
import com.example.vitalio_cis.ui.screens.DietChecklistScreen
import com.example.vitalio_cis.ui.screens.DoctorDetailsScreen
import com.example.vitalio_cis.ui.screens.DrawerScreen
import com.example.vitalio_cis.ui.screens.EmergencyContactsScreen
import com.example.vitalio_cis.ui.screens.FAQScreen
import com.example.vitalio_cis.ui.screens.FamilyHealthScreen
import com.example.vitalio_cis.ui.screens.FeedbackScreen
import com.example.vitalio_cis.ui.screens.FindDoctorsTopSection
import com.example.vitalio_cis.ui.screens.FluidDataInputScreen
import com.example.vitalio_cis.ui.screens.FluidInputHistoryScreen
import com.example.vitalio_cis.ui.screens.FluidOutputHistoryScreen
import com.example.vitalio_cis.ui.screens.InteractionCheckerScreen
import com.example.vitalio_cis.ui.screens.ManageMedicationsPreview
import com.example.vitalio_cis.ui.screens.ManageMedicationsScreen
import com.example.vitalio_cis.ui.screens.MedicalProfileScreen
import com.example.vitalio_cis.ui.screens.MedicineReminderScreen
import com.example.vitalio_cis.ui.screens.MyObserversScreen
import com.example.vitalio_cis.ui.screens.OtpScreen
import com.example.vitalio_cis.ui.screens.PersonalInfoScreen
import com.example.vitalio_cis.ui.screens.PrescriptionScreen
import com.example.vitalio_cis.ui.screens.PreviewFindDoctors
import com.example.vitalio_cis.ui.screens.PreviewLabReports
import com.example.vitalio_cis.ui.screens.RemindersScreen
import com.example.vitalio_cis.ui.screens.ResearchArticlesScreen
import com.example.vitalio_cis.ui.screens.SelectClinicScreen
import com.example.vitalio_cis.ui.screens.SharedAccountScreen
import com.example.vitalio_cis.ui.screens.SymptomTrackerScreen
import com.example.vitalio_cis.ui.screens.SymptomsView
import com.example.vitalio_cis.ui.screens.VitalHistoryScreen
import com.example.vitalio_cis.ui.screens.VitalsScreen
import com.example.vitalio_cis.ui.screens.onboarding.BloodScreen
import com.example.vitalio_cis.ui.screens.onboarding.ChronicDiseaseScreen
import com.example.vitalio_cis.ui.screens.onboarding.CreateAccountPreviewScreen
import com.example.vitalio_cis.ui.screens.onboarding.DobScreen
import com.example.vitalio_cis.ui.screens.onboarding.FamilyHistoryScreen
import com.example.vitalio_cis.ui.screens.onboarding.FluidDetailsScreen
import com.example.vitalio_cis.ui.screens.onboarding.FluidIntakeDetailsScreen
import com.example.vitalio_cis.ui.screens.onboarding.GenderScreen
import com.example.vitalio_cis.ui.screens.onboarding.HeightScreen
import com.example.vitalio_cis.ui.screens.onboarding.HydrationScreen
import com.example.vitalio_cis.ui.screens.onboarding.LocationScreen
import com.example.vitalio_cis.ui.screens.onboarding.RegistrationCompletedView
import com.example.vitalio_cis.ui.screens.onboarding.ReminderPreferenceScreen
import com.example.vitalio_cis.ui.screens.onboarding.ReminderScreen
import com.example.vitalio_cis.ui.screens.onboarding.ThankYouView
import com.example.vitalio_cis.ui.screens.onboarding.WeightScreen
import com.example.vitalio_cis.ui.screens.onboarding.WelcomeScreen
import com.example.vitalio_cis.ui.theme.LocalThemeViewModel
import com.example.vitalio_cis.ui.theme.MyAppTheme
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint

 class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            val context = LocalContext.current
            MyAppTheme {
                CompositionLocalProvider(LocalNavController provides navController) {
                    val themeViewModel = LocalThemeViewModel.current
                    val colors by themeViewModel.colorScheme.collectAsState()

                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                     ) {
                        NavHost(
                            navController = navController,
                            startDestination = if (PrefsManager(context).getPatient()?.uhId?.isNotEmpty()
                                    ?: false) Routes.DASHBOARD else Routes.LOGIN
                        ) {
                            composable(Routes.DASHBOARD) { DashboardScreen() }
                            composable(Routes.LOGIN) { LoginScreen() }
                            composable(Routes.ADDLABRESULTS) { AddLabResultsScreen() }
                            composable(Routes.MANAGEMEDICAIONS) { ManageMedicationsPreview() }
                            composable(Routes.ADDMEDICINEREMINDER) { AddMedicineReminderScreen() }
                            composable(Routes.INTERACTIONCHECKER) { InteractionCheckerScreen() }
                            composable(Routes.DIETCHECKLIST) { DietChecklistScreen() }
                            composable(Routes.PRESCRIPTION) { PrescriptionScreen() }
                            composable(Routes.FLUIDDATAINPUT) { FluidDataInputScreen() }
                            composable(Routes.FLUIDOUTPUTHISTORY) { FluidOutputHistoryScreen() }
                            composable(Routes.FLUIDINPUTHISTORY) { FluidInputHistoryScreen() }
                            composable(Routes.VITALHISTORY) { VitalHistoryScreen() }

//                            composable(Routes.CONNECTION+"/{vitalName}") {
//
//                                ConnectionScreen() }

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
                            composable(Routes.REMINDERS) { RemindersScreen() }
                            composable(Routes.EMERGENCYCONTACTS) { EmergencyContactsScreen() }
                            composable(Routes.CONNECTWATCH) { ConnectWatchScreen() }
                            composable(Routes.SHAREDACCOUNT) { SharedAccountScreen() }
                            composable(Routes.ADDMEMBER) { AddMemberScreen() }
                            composable(Routes.FAQ) { FAQScreen() }
                            composable(Routes.FEEDBACK) { FeedbackScreen() }
                            composable(Routes.OTP) { OtpScreen() }
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
                            composable(Routes.LABREPORTS) { PreviewLabReports() }
                            composable(Routes.FINDDOCTOR) { PreviewFindDoctors() }
                            composable(Routes.RESEARCHARTICLES) { ResearchArticlesScreen() }




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
                        }
                    }
                }
            }
        }
    }
}