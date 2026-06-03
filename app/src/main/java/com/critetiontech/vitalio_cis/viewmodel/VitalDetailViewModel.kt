package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VitalDetailViewModel : ViewModel() {

    private val fetchLastVitalUseCase = AppDependencies.fetchLastVital()
    private val addVitalUseCase = AppDependencies.addVital()
    private val fetchVitalAnalyticsUseCase = AppDependencies.fetchVitalAnalytics()
    private val prefs = AppDependencies.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _addLoading = MutableStateFlow(false)
    val addLoading: StateFlow<Boolean> = _addLoading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

    private val _analyticsData = MutableStateFlow<List<VitalGraphEntry>>(emptyList())
    val analyticsData: StateFlow<List<VitalGraphEntry>> = _analyticsData

    private val _analyticsLoading = MutableStateFlow(false)
    val analyticsLoading: StateFlow<Boolean> = _analyticsLoading

    fun fetchLastVital() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = fetchLastVitalUseCase(patient.uhId, patient.clientId.toString(), patient.pid.toString())) {
                is DomainResult.Success -> _vitalList.value = r.data
                is DomainResult.Error -> Log.e("VitalDetailVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun fetchVitalAnalytics(fromDate: String, toDate: String, searchIds: String) {
        viewModelScope.launch {
            _analyticsLoading.value = true
            _analyticsData.value = emptyList()
            val patient = prefs.getPatient() ?: run { _analyticsLoading.value = false; return@launch }
            val params = mapOf("fromDate" to fromDate, "toDate" to toDate, "uhid" to patient.uhId, "clientId" to patient.clientId.toString(), "userId" to patient.pid.toString(), "searchId" to searchIds)
            when (val r = fetchVitalAnalyticsUseCase(params)) {
                is DomainResult.Success -> _analyticsData.value = r.data
                is DomainResult.Error -> Log.e("VitalDetailVM", r.exception.message.orEmpty())
            }
            _analyticsLoading.value = false
        }
    }

    fun addVital(vmValueBPSys: String = "", vmValueBPDias: String = "", vmValueSPO2: String = "", vmValueRespiratoryRate: String = "", vmValueHeartRate: String = "", vmValuePulse: String = "", vmValueRbs: String = "", vmValueTemperature: String = "", onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _addLoading.value = true
            val patient = prefs.getPatient() ?: run { _addLoading.value = false; return@launch }
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            val body = mapOf("vmValueBPSys" to vmValueBPSys, "vmValueBPDias" to vmValueBPDias, "vmValueSPO2" to vmValueSPO2, "vmValueRespiratoryRate" to vmValueRespiratoryRate, "vmValueHeartRate" to vmValueHeartRate, "vmValuePulse" to vmValuePulse, "vmValueRbs" to vmValueRbs, "vmValueTemperature" to vmValueTemperature, "uhid" to patient.uhId, "userId" to patient.pid.toString(), "vitalDate" to currentDate, "vitalTime" to currentTime, "clientId" to patient.clientId.toString(), "isFromPatient" to "true", "isFromMachine" to "0", "positionId" to "0")
            when (val r = addVitalUseCase(body)) {
                is DomainResult.Success -> { fetchLastVital(); onSuccess() }
                is DomainResult.Error -> Log.e("VitalDetailVM", r.exception.message.orEmpty())
            }
            _addLoading.value = false
        }
    }
}
