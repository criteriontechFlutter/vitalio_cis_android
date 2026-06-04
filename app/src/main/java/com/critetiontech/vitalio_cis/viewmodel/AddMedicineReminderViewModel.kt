package com.critetiontech.vitalio_cis.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class AddMedicineReminderViewModel : ViewModel() {
    var searchText by mutableStateOf(""); private set
    fun onSearchChange(value: String) { searchText = value }
}
