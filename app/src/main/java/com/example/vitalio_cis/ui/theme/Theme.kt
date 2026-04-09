package com.example.vitalio_cis.ui.theme

import android.app.Activity
import android.app.Application
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel

import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.vitalio_cis.utils.App
import com.example.vitalio_cis.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn


//fun getColorScheme(selectedTheme: AppTheme): MyColorScheme {
//    return when (selectedTheme) {
//        AppTheme.DARK -> MyColorScheme(
//            login_bg_yellow = Color(0xFFFFDD00),
//            login_bg_red = Color(0xFFBB86FC),
//            primary = Color(0xFF0079FF),
//            text_color_black = Color(0xFF202529),
//            text_color_white = Color(0xFF202529),
//            textfeild_placehoder_color_grey = Color(0xFFBABFC9),
//            textfeild_title_color_grey = Color(0xFF546788),
//            textfeild_border_color_grey = Color(0xFFEDF1F6),
//            textfeild_validation_color  = Color(0xFFFF3737),
//            bottomsheet_slide_up_color  = Color(0xFFD9D9D9),
//            dashboard_bottomsheet_color  = Color(0xFFF9F9F9),
//            dashboard_energy_container_color  = Color(0xFFFFFFFF),
//            dashboard_priority_action_container_color  = Color(0xFFEAF4FF),
//            white  = Color(0xFFFFFFFF),
//        )
//        AppTheme.LIGHT -> MyColorScheme(
//            login_bg_yellow = Color(0xFFFFDD00),
//            login_bg_red = Color(0xFFBB86FC),
//            primary = Color(0xFF546788),
//            text_color_black = Color(0xFF202529),
//            text_color_white = Color(0xFFEDF1F6),
//            textfeild_placehoder_color_grey = Color(0xFFBABFC9),
//            textfeild_title_color_grey = Color(0xFF546788),
//            textfeild_border_color_grey = Color(0xFFEDF1F6),
//            textfeild_validation_color = Color(0xFFFF3737),
//            bottomsheet_slide_up_color  = Color(0xFFD9D9D9),
//            dashboard_bottomsheet_color  = Color(0xFFF9F9F9),
//            dashboard_energy_container_color  = Color(0xFFFFFFFF),
//            dashboard_priority_action_container_color  = Color(0xFFEAF4FF),
//            white  = Color(0xFFFFFFFF),
//        )
//    }
//}
//
//
//data class MyColorScheme(
//    val login_bg_yellow: Color,
//    val login_bg_red: Color,
//    val primary: Color,
//    val white: Color,
//
//
//
//    val text_color_white: Color,
//    val text_color_black: Color,
//    val textfeild_placehoder_color_grey: Color,
//    val textfeild_title_color_grey: Color,
//    val textfeild_border_color_grey: Color,
//    val textfeild_validation_color: Color,
//
//    val bottomsheet_slide_up_color: Color,
//    val  dashboard_bottomsheet_color: Color,
//
//    val  dashboard_energy_container_color: Color,
//    val  dashboard_priority_action_container_color: Color,
//
//)
//val LocalThemeViewModel = staticCompositionLocalOf<ThemeViewModel> {
//    ThemeViewModel(
//        colors = MyColorScheme(
//            login_bg_yellow = Color(0xFFFFEB3B),
//            login_bg_red = Color(0xFFF44336),
//            primary = Color(0xFF6200EE),
//            text_color_black = Color.Black,
//            text_color_white = Color.White,
//            textfeild_placehoder_color_grey = Color.Gray,
//            textfeild_title_color_grey = Color.DarkGray,
//            textfeild_border_color_grey = Color.LightGray,
//            textfeild_validation_color  = Color.Red,
//            bottomsheet_slide_up_color  = Color.White,
//            dashboard_bottomsheet_color  = Color.LightGray,
//            dashboard_energy_container_color  = Color.Green,
//            dashboard_priority_action_container_color  = Color.Blue,
//            white  = Color.White,
//        )
//    )
//}
//@Composable
//fun MyAppTheme(
//    selectedTheme: AppTheme,
//    content: @Composable () -> Unit
//) {
//    val myColors = getColorScheme(selectedTheme)
//
//    CompositionLocalProvider(
//        LocalMyColorScheme provides myColors
//    ) {
//        content()
//    }
//}
//
//enum class AppTheme {
//    LIGHT,
//    DARK
//}
//class ThemeViewModel : ViewModel() {
//    private val _selectedTheme = MutableStateFlow(AppTheme.LIGHT)
//    val selectedTheme: StateFlow<AppTheme> = _selectedTheme
//
//    val colorScheme: StateFlow<MyColorScheme> = _selectedTheme.map { theme ->
//        getColorScheme(theme)
//    }.stateIn(viewModelScope, SharingStarted.Eagerly, getColorScheme(AppTheme.LIGHT))
//
//    fun toggleTheme() {
//        _selectedTheme.value =
//            if (_selectedTheme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
//    }
//}

data class MyColorScheme(
    val dashboardBackgroundColor: Color,
    val dashboardContainerColor: Color,

    val textDarkColor: Color,
    val textGreyColor: Color,
    val textWhiteColor: Color,


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
        dashboardBackgroundColor = Color.White,
        dashboardContainerColor = Color(0xFFF5F8FC),
        textDarkColor = Color(0xFF202529),
        textGreyColor = Color(0xFF546788),
                textWhiteColor =  Color.White,
    )

    AppTheme.DARK -> MyColorScheme(
        dashboardBackgroundColor = Color(0xFF0F1419),   // dark bg
        dashboardContainerColor = Color(0xFF1C2228),    // dark card
        textDarkColor = Color(0xFFFFFFFF),              // white text
        textGreyColor = Color(0xFF9FB0C3)    ,
        textWhiteColor = Color.White,         // light grey
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
