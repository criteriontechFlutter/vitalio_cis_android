package com.critetiontech.vitalio_cis.ui.theme

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.vitalio_cis.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class MyColorScheme(
    val dashboardBackgroundColor: Color,
    val dashboardContainerColor: Color,
    val primaryBlueColor: Color,


    val textDarkColor: Color,
    val textGreyColor: Color,
    val textWhiteColor: Color,

    val textPrimaryBlueColor: Color,



    val btnDarkColor: Color,
    val btnGreyColor: Color,
    val btnWhiteColor: Color,
    val btnPrimaryBlueColor: Color,


    val borderGreyLightColor: Color,

)

// -----------------------------
// 2️⃣ Theme Enum
// -----------------------------
enum class AppTheme {
    LIGHT, DARK
}

// -----------------------------
// 3️⃣ Get Color Scheme
// -----------------------------
fun getColorScheme(theme: AppTheme): MyColorScheme = when (theme) {

    AppTheme.LIGHT -> MyColorScheme(
        dashboardBackgroundColor = Color(0xFFF5F8FC),
        dashboardContainerColor = Color.White,
        textDarkColor = Color(0xFF202529),
        textGreyColor = Color(0xFF546788),
                textWhiteColor =  Color.White,
        textPrimaryBlueColor = Color(0xFF1564ED),
        primaryBlueColor = Color(0xFF1564ED),


        btnDarkColor = Color(0xFF202529),
        btnGreyColor = Color(0xFF546788),
        btnWhiteColor =  Color(0xFFFFFFFF),
        btnPrimaryBlueColor = Color(0xFF1564ED),



        borderGreyLightColor =Color(0xFFBABFC9)

    )

    AppTheme.DARK -> MyColorScheme(
        dashboardBackgroundColor =Color(0xFF1C2228),     // dark bg
        dashboardContainerColor =Color(0xFF0F1419),  // dark card
        textDarkColor = Color(0xFFFFFFFF),              // white text
        textGreyColor = Color(0xFF9FB0C3) ,
        textWhiteColor = Color.White,
        textPrimaryBlueColor = Color(0xFF1564ED),
                primaryBlueColor = Color(0xFF1564ED),

        btnDarkColor = Color(0xFFFFFFFF),              // white text
        btnGreyColor = Color(0xFF9FB0C3) ,
        btnWhiteColor =Color(0xFF0F1419),
        btnPrimaryBlueColor = Color(0xFF1564ED),// light grey
        borderGreyLightColor = Color(0xFF2A3138)
    )
}
// -----------------------------
// 4️⃣ Theme ViewModel
// -----------------------------
class ThemeViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs by lazy { PrefsManager(application) }

    private val _selectedTheme = MutableStateFlow(
        if (prefs.getTheme()) AppTheme.DARK else AppTheme.LIGHT
    )
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme

    val colorScheme: StateFlow<MyColorScheme> = _selectedTheme
        .map { getColorScheme(it) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, getColorScheme(AppTheme.LIGHT))

    fun toggleTheme() {
        val newTheme = if (_selectedTheme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
        _selectedTheme.value = newTheme
        prefs.saveTheme(newTheme == AppTheme.DARK)
    }
}

// -----------------------------
// 5️⃣ Composition Locals
// -----------------------------
val LocalThemeViewModel = staticCompositionLocalOf<ThemeViewModel> {
    error("ThemeViewModel not provided")
}

val LocalMyColorScheme = staticCompositionLocalOf { getColorScheme(AppTheme.LIGHT) }

// -----------------------------
// 6️⃣ AppTheme Composable
// -----------------------------
@Composable
fun MyAppTheme(
    content: @Composable () -> Unit
) {
    val application = LocalContext.current.applicationContext as Application

    val themeViewModel: ThemeViewModel = viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(application)
    )

    val colors by themeViewModel.colorScheme.collectAsState()

    CompositionLocalProvider(
        LocalThemeViewModel provides themeViewModel,
        LocalMyColorScheme provides colors
    ) {
        content()
    }
}
