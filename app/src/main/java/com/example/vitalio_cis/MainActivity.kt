package com.example.vitalio_cis

import android.R.attr.type
import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.critetiontech.ctvitalio.ui.screens.LoginScreen
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.AppointmentRow
import com.example.vitalio_cis.ui.screens.BookingConfirmationScreen
import com.example.vitalio_cis.ui.screens.DashboardScreen
import com.example.vitalio_cis.ui.screens.DoctorDetailsScreen
import com.example.vitalio_cis.ui.screens.FindDoctorsTopSection
import com.example.vitalio_cis.ui.screens.ManageMedicationsScreen
 import com.example.vitalio_cis.ui.screens.MedicineCard
import com.example.vitalio_cis.ui.screens.MedicineReminderScreen
import com.example.vitalio_cis.ui.screens.OtpScreen
import com.example.vitalio_cis.ui.screens.PreviewFindDoctors
import com.example.vitalio_cis.ui.screens.SymptomTrackerScreen
import com.example.vitalio_cis.ui.screens.SymptomsView
import com.example.vitalio_cis.ui.screens.VitalsScreen
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.LocalThemeViewModel
import com.example.vitalio_cis.ui.theme.MyAppTheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.FCMHelper
import com.example.vitalio_cis.utils.MyApplication
import com.example.vitalio_cis.utils.PrefsManager
import com.google.firebase.FirebaseApp


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
                            composable(Routes.OTP) { OtpScreen() }
                            composable(Routes.BOOKINGCONFERMATION) { BookingConfirmationScreen() }
                            composable(Routes.VITALS) { VitalsScreen() }
                            composable(Routes.SYMPTOMSTRACKER) { SymptomTrackerScreen() }
                            composable(Routes.APPOINTMENTS) { FindDoctorsTopSection() }
                            composable(Routes.SYMPTOMS) { SymptomsView() }
                            composable(Routes.MANAGE_MEDICINE) { ManageMedicationsScreen() }
                            composable(Routes.MEDICINE) { MedicineReminderScreen() }
                            composable(Routes.FINDDOCTOR) { PreviewFindDoctors() }
                            composable(Routes.DOCTORDETAILS+"/{doctorId}/{days}", arguments = listOf(
                                navArgument("doctorId") { type = NavType.StringType },
                                navArgument("days") { type = NavType.StringType }
                            )
                                ) { backStackEntry ->

                                val doctorId = backStackEntry.arguments?.getString("doctorId") ?: ""
                                val days = backStackEntry.arguments?.getString("days") ?: ""
                                DoctorDetailsScreen(
                                doctorId = doctorId,
                                days = days
                            )}
                        }
                    }
                }
            }
        }
    }
}