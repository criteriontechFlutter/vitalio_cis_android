package com.critetiontech.vitalio_cis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class RegistrationViewModel : ViewModel() {
    var perc by mutableFloatStateOf(0f); private set
    fun updatePer(value: Float) { perc = value }
    fun getPer(): Float = perc
}
