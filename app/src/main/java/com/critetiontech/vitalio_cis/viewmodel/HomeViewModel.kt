package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.model.DashboardResponse
import com.critetiontech.ctvitalio.model.UpcomingAppointment
import com.critetiontech.ctvitalio.model.Vital
import com.critetiontech.ctvitalio.utils.MyApplication
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs
    private val networkMonitor = AppDependencies.networkMonitor
    private val endpoints = ApiEndPointCorporateModule()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

    private val _appointments = MutableStateFlow<List<UpcomingAppointment>>(emptyList())
    val appointments: StateFlow<List<UpcomingAppointment>> = _appointments

    private val _dailyMedicines = MutableStateFlow<List<com.critetiontech.ctvitalio.model.MedicineItem>>(emptyList())
    val dailyMedicines: StateFlow<List<com.critetiontech.ctvitalio.model.MedicineItem>> = _dailyMedicines

    fun fetchDashboard() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            try {
                // 1. Fetch Dashboard (Vitals + Appointments)
                val params = mapOf("UHID" to patient.uhId, "clientId" to patient.clientId.toString())
                val response = ApiHelper().callApi(
                    MyApplication.appContext,
                    endpoints.getPatientDashboardData,
                    cacheResponse = true
                ) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = params)
                }
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    val parsed = Gson().fromJson(body, DashboardResponse::class.java)
                    if (parsed.status == 1) {
                        _vitalList.value = parsed.responseValue.latestVitals
                        _appointments.value = parsed.responseValue.upcomingAppointments
                    }
                }

                // 2. Fetch Full Daily Medicine Intake (To show upcoming/taken status)
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val medsRes = AppDependencies.fetchMedicineIntake()(patient.pid, currentDate, patient.clientId)
                if (medsRes is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                    val allMeds = medsRes.data.flatMap { it.medicineData }
                        .filter { it.medicineName.isNotBlank() }
                        .sortedBy { it.scheduledDateTime }
                    _dailyMedicines.value = allMeds
                }

            } catch (e: Exception) {
                Log.e("HomeViewModel", "fetchDashboard error: ${e.message}")
            }
            _loading.value = false
        }
    }

    private val _markingMedicines = MutableStateFlow<Set<Int>>(emptySet())
    val markingMedicines: StateFlow<Set<Int>> = _markingMedicines

    fun markMedicineIntake(medicine: com.critetiontech.ctvitalio.model.MedicineItem) {
        viewModelScope.launch {
            _markingMedicines.value += medicine.id
            Log.d("HomeViewModel", "🔵 markMedicineIntake called → id=${medicine.id}, scheduledDateTime=${medicine.scheduledDateTime}, medicineName=${medicine.medicineName}")
            try {
                val result = AppDependencies.markMedicineIntake()(medicine.id, medicine.scheduledDateTime)
                Log.d("HomeViewModel", "🔵 markMedicineIntake result → $result")
                if (result is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                    // Refetch medicines
                    fetchDashboardNoCache()
                } else if (result is com.critetiontech.ctvitalio.domain.model.DomainResult.Error) {
                    Log.e("HomeViewModel", "🔴 markMedicineIntake API error: ${result.exception.message}", result.exception)
                    android.widget.Toast.makeText(MyApplication.appContext, "Failed to mark medicine", android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "🔴 Error marking intake: ${e.message}", e)
            } finally {
                _markingMedicines.value = _markingMedicines.value - medicine.id
            }
        }
    }

    fun fetchDashboardNoCache() {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            try {
                val params = mapOf("UHID" to patient.uhId, "clientId" to patient.clientId.toString())
                val response = ApiHelper().callApi(MyApplication.appContext, endpoints.getPatientDashboardData, cacheResponse = false) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = params)
                }
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    val parsed = Gson().fromJson(body, DashboardResponse::class.java)
                    if (parsed.status == 1) {
                        _vitalList.value = parsed.responseValue.latestVitals
                        _appointments.value = parsed.responseValue.upcomingAppointments
                    }
                }
                val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                val medsRes = AppDependencies.fetchMedicineIntake()(patient.pid, currentDate, patient.clientId)
                if (medsRes is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                    val allMeds = medsRes.data.flatMap { it.medicineData }
                        .filter { it.medicineName.isNotBlank() }
                        .sortedBy { it.scheduledDateTime }
                    _dailyMedicines.value = allMeds
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "fetchDashboardNoCache error: ${e.message}")
            }
        }
    }


}