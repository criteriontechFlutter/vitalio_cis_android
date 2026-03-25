package com.example.vitalio_cis.ui.theme

import android.app.Activity
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
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

//private val DarkColorScheme = darkColorScheme(
//    primary = Purple80,
//    secondary = PurpleGrey80,
//    tertiary = Pink80
//)
//
//private val LightColorScheme = lightColorScheme(
//    primary = Purple40,
//    secondary = PurpleGrey40,
//    tertiary = Pink40
//
//    /* Other default colors to override
//    background = Color(0xFFFFFBFE),
//    surface = Color(0xFFFFFBFE),
//    onPrimary = Color.White,
//    onSecondary = Color.White,
//    onTertiary = Color.White,
//    onBackground = Color(0xFF1C1B1F),
//    onSurface = Color(0xFF1C1B1F),
//    */
//)

fun getColorScheme(selectedTheme: AppTheme): MyColorScheme {
    return when (selectedTheme) {
        AppTheme.DARK -> MyColorScheme(
            login_bg_yellow = Color(0xFFFFDD00),
            login_bg_red = Color(0xFFBB86FC),
            primary = Color(0xFF0079FF),
            text_color_black = Color(0xFF202529),
            text_color_white = Color(0xFF202529),
            textfeild_placehoder_color_grey = Color(0xFFBABFC9),
            textfeild_title_color_grey = Color(0xFF546788),
            textfeild_border_color_grey = Color(0xFFEDF1F6),
            textfeild_validation_color  = Color(0xFFFF3737),
            bottomsheet_slide_up_color  = Color(0xFFD9D9D9),
            dashboard_bottomsheet_color  = Color(0xFFF9F9F9),
            dashboard_energy_container_color  = Color(0xFFFFFFFF),
            dashboard_priority_action_container_color  = Color(0xFFEAF4FF),
        )
        AppTheme.LIGHT -> MyColorScheme(
            login_bg_yellow = Color(0xFFFFDD00),
            login_bg_red = Color(0xFFBB86FC),
            primary = Color(0xFF546788),
            text_color_black = Color(0xFF202529),
            text_color_white = Color(0xFFEDF1F6),
            textfeild_placehoder_color_grey = Color(0xFFBABFC9),
            textfeild_title_color_grey = Color(0xFF546788),
            textfeild_border_color_grey = Color(0xFFEDF1F6),
            textfeild_validation_color = Color(0xFFFF3737),
            bottomsheet_slide_up_color  = Color(0xFFD9D9D9),
            dashboard_bottomsheet_color  = Color(0xFFF9F9F9),
            dashboard_energy_container_color  = Color(0xFFFFFFFF),
            dashboard_priority_action_container_color  = Color(0xFFEAF4FF),
        )
    }
}


data class MyColorScheme(
    val login_bg_yellow: Color,
    val login_bg_red: Color,
    val primary: Color,



    val text_color_white: Color,
    val text_color_black: Color,
    val textfeild_placehoder_color_grey: Color,
    val textfeild_title_color_grey: Color,
    val textfeild_border_color_grey: Color,
    val textfeild_validation_color: Color,

    val bottomsheet_slide_up_color: Color,
    val  dashboard_bottomsheet_color: Color,

    val  dashboard_energy_container_color: Color,
    val  dashboard_priority_action_container_color: Color,

)
val LocalMyColorScheme = staticCompositionLocalOf {
    MyColorScheme(
        login_bg_yellow = Color.Unspecified,
        login_bg_red = Color.Unspecified,
        primary = Color.Unspecified,
        text_color_black = Color.Unspecified,
        text_color_white = Color.Unspecified,
        textfeild_placehoder_color_grey = Color.Unspecified,
        textfeild_title_color_grey = Color.Unspecified,
        textfeild_border_color_grey = Color.Unspecified,
        textfeild_validation_color  = Color.Unspecified,
        bottomsheet_slide_up_color  = Color.Unspecified,
        dashboard_bottomsheet_color  = Color.Unspecified,
        dashboard_energy_container_color  = Color.Unspecified,
        dashboard_priority_action_container_color  = Color.Unspecified,
    )
}
@Composable
fun MyAppTheme(
    selectedTheme: AppTheme,
    content: @Composable () -> Unit
) {
    val myColors = getColorScheme(selectedTheme)

    CompositionLocalProvider(
        LocalMyColorScheme provides myColors
    ) {
        content()
    }
}enum class AppTheme {
    LIGHT,
    DARK
}
class ThemeViewModel : ViewModel() {
    private val _selectedTheme = MutableStateFlow(AppTheme.LIGHT)
    val selectedTheme: StateFlow<AppTheme> = _selectedTheme

    val colorScheme: StateFlow<MyColorScheme> = _selectedTheme.map { theme ->
        getColorScheme(theme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, getColorScheme(AppTheme.LIGHT))

    fun toggleTheme() {
        _selectedTheme.value =
            if (_selectedTheme.value == AppTheme.LIGHT) AppTheme.DARK else AppTheme.LIGHT
    }
}

@Composable
fun currentThemeColors(): MyColorScheme {
    val theme = viewModel<ThemeViewModel>().selectedTheme.collectAsState().value
    return getColorScheme(theme)
}
//@Composable
//fun Vitalio_composeTheme(
//    darkTheme: Boolean = isSystemInDarkTheme(),
//    // Dynamic color is available on Android 12+
//    dynamicColor: Boolean = true,
//    content: @Composable () -> Unit
//) {
//    val colorScheme = when {
//        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
//            val context = LocalContext.current
//            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
//        }
//
//        darkTheme -> DarkColorScheme
//        else -> LightColorScheme
//    }
//
//    MaterialTheme(
//        colorScheme = colorScheme,
//        typography = Typography,
//        content = content
//    )
//}