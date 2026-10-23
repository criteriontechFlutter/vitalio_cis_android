package com.critetiontech.ctvitalio.ui.theme

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.critetiontech.ctvitalio.utils.PrefsManager
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
        LocalMyColorScheme provides colors,
        // Ripple + tap haptic for every Modifier.clickable in the app.
        androidx.compose.foundation.LocalIndication provides com.critetiontech.ctvitalio.ui.feedback.HapticIndication,
    ) {
        // Material parts (Text without a colour, Card, TextField, Switch, menus…) follow the app
        // theme too, instead of Material's default light purple.
        androidx.compose.material3.MaterialTheme(colorScheme = materialSchemeFor(colors), typography = LatoTypography) {
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides colors.textDarkColor) {
                content()
            }
        }
    }
}

/** Material's type scale in Lato, so every Text uses the app font unless it sets its own. */
private val LatoTypography = androidx.compose.material3.Typography().let { t ->
    fun androidx.compose.ui.text.TextStyle.lato() = copy(fontFamily = com.critetiontech.ctvitalio.utils.latoFont)
    t.copy(
        displayLarge = t.displayLarge.lato(), displayMedium = t.displayMedium.lato(), displaySmall = t.displaySmall.lato(),
        headlineLarge = t.headlineLarge.lato(), headlineMedium = t.headlineMedium.lato(), headlineSmall = t.headlineSmall.lato(),
        titleLarge = t.titleLarge.lato(), titleMedium = t.titleMedium.lato(), titleSmall = t.titleSmall.lato(),
        bodyLarge = t.bodyLarge.lato(), bodyMedium = t.bodyMedium.lato(), bodySmall = t.bodySmall.lato(),
        labelLarge = t.labelLarge.lato(), labelMedium = t.labelMedium.lato(), labelSmall = t.labelSmall.lato(),
    )
}

private fun materialSchemeFor(c: MyColorScheme): androidx.compose.material3.ColorScheme {
    val dark = c.dashboardBackgroundColor.luminance() < 0.5f
    val blue = c.primaryBlueColor
    val blueTint = blue.copy(alpha = if (dark) 0.25f else 0.12f).compositeOver(c.dashboardContainerColor)
    val base = if (dark) androidx.compose.material3.darkColorScheme() else androidx.compose.material3.lightColorScheme()
    return base.copy(
        primary = blue,
        onPrimary = Color.White,
        primaryContainer = blueTint,
        onPrimaryContainer = if (dark) Color.White else blue,
        secondary = blue,
        onSecondary = Color.White,
        secondaryContainer = blueTint,
        onSecondaryContainer = if (dark) Color.White else blue,
        tertiaryContainer = blueTint,
        onTertiaryContainer = if (dark) Color.White else blue,
        background = c.dashboardBackgroundColor,
        onBackground = c.textDarkColor,
        surface = c.dashboardContainerColor,
        onSurface = c.textDarkColor,
        surfaceVariant = c.dashboardBackgroundColor,
        onSurfaceVariant = c.textGreyColor,
        surfaceContainerLowest = c.dashboardContainerColor,
        surfaceContainerLow = c.dashboardContainerColor,
        surfaceContainer = c.dashboardContainerColor,
        surfaceContainerHigh = c.dashboardContainerColor,
        surfaceContainerHighest = c.dashboardContainerColor,
        inverseSurface = c.textDarkColor,
        inverseOnSurface = c.dashboardContainerColor,
        outline = c.borderGreyLightColor,
        outlineVariant = c.borderGreyLightColor.copy(alpha = 0.5f).compositeOver(c.dashboardContainerColor),
        error = Color(0xFFE5484D),
        scrim = Color.Black,
    )
}

/** True while the app's dark theme is on. */
val isAppDark: Boolean
    @Composable get() = LocalMyColorScheme.current.dashboardBackgroundColor.luminance() < 0.5f

/** A soft tint for chips and badges: the light colour by day, [base] at low alpha at night. */
@Composable
fun softTint(base: Color, light: Color): Color = if (isAppDark) base.copy(alpha = 0.18f) else light
