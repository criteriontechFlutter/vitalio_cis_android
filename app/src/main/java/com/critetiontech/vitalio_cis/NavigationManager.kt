package com.critetiontech.vitalio_cis


import android.util.Log
import androidx.navigation.NavHostController

object NavigationManager {

    private var _navController: NavHostController? = null

    var navController: NavHostController?
        get() = _navController
        set(value) { _navController = value }

    fun navigate(route: String) {
        if (_navController == null) {
            Log.e("NAVIGATION", "NavController NOT INITIALIZED")
            return
        }
        _navController?.navigate(route)
    }
}