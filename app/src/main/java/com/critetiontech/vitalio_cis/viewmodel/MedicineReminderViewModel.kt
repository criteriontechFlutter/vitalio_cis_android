package com.critetiontech.vitalio_cis.viewmodel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MedicineReminderViewModel : ViewModel() {

    private val fetchMedicineIntakeUseCase = AppDependencies.fetchMedicineIntake()
    private val markMedicineIntakeUseCase = AppDependencies.markMedicineIntake()
    private val prefs = AppDependencies.prefs

    private val _periods = MutableStateFlow<List<MedicinePeriod>>(emptyList())
    val periods: StateFlow<List<MedicinePeriod>> = _periods

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun fetchMedicineIntake() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            val givenDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now().toString() else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            when (val r = fetchMedicineIntakeUseCase(patient.pid, givenDate, patient.clientId)) {
                is DomainResult.Success -> _periods.value = r.data
                is DomainResult.Error -> Log.e("MedicineReminderVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun markIntake(id: Int, scheduledDateTime: String) {
        viewModelScope.launch {
            when (val r = markMedicineIntakeUseCase(id, scheduledDateTime)) {
                is DomainResult.Success -> fetchMedicineIntake()
                is DomainResult.Error -> Log.e("MedicineReminderVM", r.exception.message.orEmpty())
            }
        }
    }
}
