package com.critetiontech.ctvitalio.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.model.AllMedicine
import com.critetiontech.ctvitalio.model.LoggedMedicine
import com.critetiontech.ctvitalio.model.MedicineIntakeByDateResponse
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ManageMedicationViewModel @Inject constructor() : ViewModel() {

    private val _loggedMedicines = MutableStateFlow<List<LoggedMedicine>>(emptyList())
    val loggedMedicines: StateFlow<List<LoggedMedicine>> = _loggedMedicines

    private val _allMedicines = MutableStateFlow<List<AllMedicine>>(emptyList())
    val allMedicines: StateFlow<List<AllMedicine>> = _allMedicines

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun fetchByDate(context: Context, givenDate: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val patient = PrefsManager(context).getPatient() ?: return@launch
                // Through the repository so the last list for this date is shown when offline.
                when (val r = com.critetiontech.ctvitalio.di.AppDependencies.fetchMedicineByDate()(patient.pid, givenDate, patient.clientId)) {
                    is com.critetiontech.ctvitalio.domain.model.DomainResult.Success -> {
                        _loggedMedicines.value = r.data.first
                        _allMedicines.value = r.data.second
                    }
                    is com.critetiontech.ctvitalio.domain.model.DomainResult.Error ->
                        Log.e("ManageMedicationVM", "fetchByDate error: ${r.exception.message}")
                }
            } catch (e: Exception) {
                Log.e("ManageMedicationVM", "fetchByDate error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }
}