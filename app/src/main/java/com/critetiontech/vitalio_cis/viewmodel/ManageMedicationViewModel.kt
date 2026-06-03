package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ManageMedicationViewModel : ViewModel() {

    private val fetchMedicineByDateUseCase = AppDependencies.fetchMedicineByDate()
    private val prefs = AppDependencies.prefs

    private val _loggedMedicines = MutableStateFlow<List<LoggedMedicine>>(emptyList())
    val loggedMedicines: StateFlow<List<LoggedMedicine>> = _loggedMedicines

    private val _allMedicines = MutableStateFlow<List<AllMedicine>>(emptyList())
    val allMedicines: StateFlow<List<AllMedicine>> = _allMedicines

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun fetchByDate(givenDate: String) {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = fetchMedicineByDateUseCase(patient.pid, givenDate, patient.clientId)) {
                is DomainResult.Success -> { _loggedMedicines.value = r.data.first; _allMedicines.value = r.data.second }
                is DomainResult.Error -> Log.e("ManageMedicationVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }
}
