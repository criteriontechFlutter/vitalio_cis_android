package com.example.vitalio_cis

import android.annotation.SuppressLint
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.critetiontech.ctvitalio.ui.screens.LoginScreen
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.AppointmentRow
import com.example.vitalio_cis.ui.screens.DashboardScreen
import com.example.vitalio_cis.ui.screens.FindDoctorsTopSection
import com.example.vitalio_cis.ui.screens.ManageMedicine
import com.example.vitalio_cis.ui.screens.MedicineCard
import com.example.vitalio_cis.ui.screens.MedicineReminderScreen
import com.example.vitalio_cis.ui.screens.OtpScreen
import com.example.vitalio_cis.ui.screens.SymptomTrackerScreen
import com.example.vitalio_cis.ui.screens.SymptomsView
import com.example.vitalio_cis.ui.screens.VitalsScreen
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.LocalThemeViewModel
import com.example.vitalio_cis.ui.theme.MyAppTheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel
import com.example.vitalio_cis.utils.FCMHelper
import com.example.vitalio_cis.utils.MyApplication
import com.google.firebase.FirebaseApp


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()

            MyAppTheme {
                CompositionLocalProvider(LocalNavController provides navController) {
                    val themeViewModel = LocalThemeViewModel.current
                    val colors by themeViewModel.colorScheme.collectAsState()

                    Surface(
                        modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                        color = colors.dashboard_bottomsheet_color
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.DASHBOARD
                        ) {
                            composable(Routes.DASHBOARD) { DashboardScreen() }
                            composable(Routes.SYMPTOMSTRACKER) { SymptomTrackerScreen() }
                            composable(Routes.APPOINTMENTS) { FindDoctorsTopSection() }
                            composable(Routes.SYMPTOMS) { SymptomsView() }
                            composable(Routes.MANAGE_MEDICINE) { ManageMedicine() }
                        }
                    }
                }
            }
        }
    }
}