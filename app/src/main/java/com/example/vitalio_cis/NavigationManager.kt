package com.example.vitalio_cis


import android.util.Log
import androidx.navigation.NavHostController

object NavigationManager {

    var navController: NavHostController?    // ✅ nullable
        get() = null
        set(value) = TODO()

    fun navigate(route: String) {
        if (navController == null) {
            Log.e("NAVIGATION", "NavController NOT INITIALIZED")
            return
        }
        navController?.navigate(route)
    }
}