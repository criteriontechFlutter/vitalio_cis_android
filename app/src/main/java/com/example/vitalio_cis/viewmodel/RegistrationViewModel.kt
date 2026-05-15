package com.example.vitalio_cis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


 class RegistrationViewModel : ViewModel() {



     var perc by mutableFloatStateOf(0f)
         private set


     fun updatePer(value: Float) {
        perc = value
    }

    fun getPer(): Float {
        return perc
    }
}