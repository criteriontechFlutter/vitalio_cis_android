package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.MedicinePeriod
import com.critetiontech.ctvitalio.reminder.MedicineIntakeEvents
import com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler
import com.critetiontech.ctvitalio.reminder.withLocallyTaken
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class ToastData(val title: String, val message: String, val isSuccess: Boolean)

class MedicineReminderViewModel : ViewModel() {

    private val fetchMedicineIntakeUseCase = AppDependencies.fetchMedicineIntake()
    private val markMedicineIntakeUseCase = AppDependencies.markMedicineIntake()
    private val prefs = AppDependencies.prefs

    private val _periods = MutableStateFlow<List<MedicinePeriod>>(emptyList())
    val periods: StateFlow<List<MedicinePeriod>> = _periods

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _toast = MutableStateFlow<ToastData?>(null)
    val toast: StateFlow<ToastData?> = _toast

    fun clearToast() { _toast.value = null }

    init {
        // A dose was logged from a medicine alarm; refresh so it shows as taken.
        viewModelScope.launch { MedicineIntakeEvents.changed.collect { fetchMedicineIntake() } }
    }

    fun fetchMedicineIntake() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            val givenDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                LocalDate.now().toString()
            else
                java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
            when (val r = fetchMedicineIntakeUseCase(patient.pid, givenDate, patient.clientId)) {
                is DomainResult.Success -> _periods.value = r.data.map { period ->
                    period.copy(medicineData = period.medicineData.withLocallyTaken(MyApplication.appContext))
                }
                is DomainResult.Error -> Log.e("MedicineReminderVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun markIntake(id: Int, scheduledDateTime: String) {
        viewModelScope.launch {
            when (val r = markMedicineIntakeUseCase(id, scheduledDateTime)) {
                is DomainResult.Success -> {
                    _toast.value = ToastData("Taken", "Medication marked as taken successfully.", true)
                    Haptics.success()
                    fetchMedicineIntake()
                    // Drop the alarm for the dose that was just taken.
                    MedicineReminderScheduler.requestSync(MyApplication.appContext)
                }
                is DomainResult.Error -> {
                    _toast.value = ToastData("Error", r.exception.message ?: "Unknown error", false)
                    Haptics.error()
                    Log.e("MedicineReminderVM", r.exception.message.orEmpty())
                }
            }
        }
    }
}
