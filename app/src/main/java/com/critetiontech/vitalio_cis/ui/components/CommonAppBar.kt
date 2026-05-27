package com.critetiontech.vitalio_cis.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.critetiontech.ctvitalio.utils.AppTextStyles
import com.critetiontech.myapplication.utils.LocalNavController
import com.critetiontech.vitalio_cis.ui.theme.LocalMyColorScheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyAppBar(
    title: String,
    showBack: Boolean = true,
    onBack: () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {}
) {


    val navController = LocalNavController.current

    val colors = LocalMyColorScheme.current

    TopAppBar(
        title = {
            Text(
                title,
                style = AppTextStyles.style18BCB()
            )
        },

        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = null
                    )
                }
            }
        },

        actions = actions,

        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colors.dashboardBackgroundColor,
            titleContentColor = colors.textDarkColor,
            navigationIconContentColor = colors.textDarkColor,
            actionIconContentColor = colors.textDarkColor
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommonAppBar(
    title: String,
    showBack: Boolean = true,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit
    = {
//        Column() {
//            val themeViewModel = LocalThemeViewModel.current
//
//            Text(text = "theme",
//                modifier = Modifier.clickable(){
//                    themeViewModel.toggleTheme()
//                })
//        }
    },
    content: @Composable ( ) -> Unit
) {

    val navController = LocalNavController.current

    val colors = LocalMyColorScheme.current

    Scaffold(

        topBar = {
            MyAppBar(
                title = title,
                showBack = showBack,
                onBack = {
                    onBack?.invoke() ?: navController.popBackStack()
                },
                actions = actions
            )
        },

        containerColor = colors.dashboardBackgroundColor,

        ) { padding ->

        Box (
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ){
            content( )
        }


    }
}