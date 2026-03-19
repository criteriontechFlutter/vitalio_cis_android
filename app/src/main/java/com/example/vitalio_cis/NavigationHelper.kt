package com.example.vitalio_cis

import androidx.navigation.NavHostController

fun navigateTo(navController: NavHostController, route: String) {
    navController.navigate(route)
}