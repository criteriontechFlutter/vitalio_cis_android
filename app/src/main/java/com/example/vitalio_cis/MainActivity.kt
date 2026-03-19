package com.example.vitalio_cis

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.critetiontech.ctvitalio.ui.screens.LoginScreen
import com.example.vitalio_cis.ui.screens.DashboardScreen
import com.example.vitalio_cis.ui.screens.FindDoctorsTopSection
import com.example.vitalio_cis.ui.screens.VitalsScreen
import com.example.vitalio_cis.ui.theme.LocalMyColorScheme
import com.example.vitalio_cis.ui.theme.MyAppTheme
import com.example.vitalio_cis.ui.theme.ThemeViewModel

class MainActivity : ComponentActivity() {
    @SuppressLint("ViewModelConstructorInComposable")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val navController = rememberNavController()
            val viewModel = ThemeViewModel()
            val theme = viewModel.selectedTheme.collectAsState()

            NavigationManager.navController = navController

            MyAppTheme(selectedTheme = theme.value) {


                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LocalMyColorScheme.current.dashboard_bottomsheet_color // 👈 use your color
                ) {

                    NavHost(
                        navController = navController,
                        startDestination = Routes.DASHBOARD
                    ) {
                        composable(Routes.DASHBOARD) { LoginScreen() }
                        composable(Routes.VITALS) { VitalsScreen() }
                        composable(Routes.APPOINTMENTS) { FindDoctorsTopSection() }
                    }
                }
            }
        }
    }
}