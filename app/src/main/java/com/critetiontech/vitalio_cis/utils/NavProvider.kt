package com.critetiontech.myapplication.utils
import androidx.navigation.NavHostController
import androidx.compose.runtime.staticCompositionLocalOf

val LocalNavController = staticCompositionLocalOf<NavHostController> {
    error("NavController not provided")
}