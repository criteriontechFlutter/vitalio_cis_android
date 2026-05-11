package com.example.vitalio_cis.ui.screens.onboarding

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.example.myapplication.utils.LocalNavController
import com.example.vitalio_cis.ui.screens.onboarding.components.OnboardingLayout
import com.example.vitalio_cis.ui.screens.onboarding.components.SelectionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen( ) {

    val navController = LocalNavController.current
    val timeState = rememberTimePickerState()
    var show by remember { mutableStateOf(false) }
    var time by remember { mutableStateOf("Select Time") }

    if (show) {
        AlertDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    time = "${timeState.hour}:${timeState.minute}"
                    show = false
                }) { Text("OK") }
            },
            text = { TimePicker(state = timeState) }
        )
    }

    OnboardingLayout(
        title = "Set Reminder",
        buttonText = "Next",
        onNext = { navController.navigate("hydration") }
    ) {
        SelectionCard(time, false) { show = true }
    }
}