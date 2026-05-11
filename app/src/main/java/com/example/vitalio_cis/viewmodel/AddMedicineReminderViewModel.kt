package com.example.vitalio_cis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import javax.inject.Inject

class AddMedicineReminderViewModel @Inject constructor() : ViewModel() {




    var searchText by mutableStateOf("")
        private set

    fun onSearchChange(value: String) {
        searchText = value
    }
}