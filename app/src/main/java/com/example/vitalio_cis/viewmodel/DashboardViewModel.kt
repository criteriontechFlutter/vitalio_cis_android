package com.critetiontech.ctvitalio.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class DashboardViewModel  @Inject constructor() : ViewModel() {
    private val _selectedItem = MutableStateFlow(0)
    val selectedItem: StateFlow<Int> = _selectedItem.asStateFlow()

    fun selectItem(index: Int) {
        _selectedItem.value = index
    }
}