package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.model.DashboardResponse
import com.critetiontech.vitalio_cis.model.UpcomingAppointment
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.utils.MyApplication
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

    private val _pendingMedicines = MutableStateFlow<List<com.critetiontech.vitalio_cis.model.PendingMedicine>>(emptyList())
    val pendingMedicines: StateFlow<List<com.critetiontech.vitalio_cis.model.PendingMedicine>> = _pendingMedicines

    init {
        // Auto-refetch dashboard when network comes back after being offline
        viewModelScope.launch {
            networkMonitor.isOnline
                .drop(1)           // skip initial emission
                .filter { it }     // only fire when going offline → online
                .collect {
                    Log.d("HomeViewModel", "Network restored — refreshing dashboard")
                    fetchDashboard()
                }
        }
    }

    fun fetchDashboard() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            try {
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
                        _pendingMedicines.value = parsed.responseValue.pendingMedicines.filter { it.medicineName.isNotBlank() }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "fetchDashboard error: ${e.message}")
            }
            _loading.value = false
        }
    }

    private val _markingMedicines = MutableStateFlow<Set<Int>>(emptySet())
    val markingMedicines: StateFlow<Set<Int>> = _markingMedicines

    fun markMedicineIntake(medicine: com.critetiontech.vitalio_cis.model.PendingMedicine) {
        viewModelScope.launch {
            // Use 'id' instead of 'medicineId' for unique tracking
            _markingMedicines.value = _markingMedicines.value + medicine.id

            try {
                val patient = prefs.getPatient() ?: run {
                    _markingMedicines.value = _markingMedicines.value - medicine.id
                    return@launch
                }

                var realId   = medicine.id
                var realTime = medicine.scheduledDateTime

                // Fallback lookup only if dashboard didn't provide scheduledDateTime or valid id
                if (realId == 0 || realTime.isEmpty()) {
                    val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
                    val medsRes = AppDependencies.fetchMedicineIntake()(patient.pid, currentDate, patient.clientId)
                    
                    if (medsRes is com.critetiontech.vitalio_cis.domain.model.DomainResult.Success) {
                        val realMed = medsRes.data
                            .flatMap { it.medicineData }
                            .find { it.medicineName.trim().equals(medicine.medicineName.trim(), ignoreCase = true) && !it.isTaken }
                        if (realMed != null) {
                            realId   = realMed.id
                            realTime = realMed.scheduledDateTime
                        }
                    }
                }

                if (realId != 0 && realTime.isNotEmpty()) {
                    // Mark as taken via API
                    val result = AppDependencies.markMedicineIntake()(realId, realTime)
                    if (result is com.critetiontech.vitalio_cis.domain.model.DomainResult.Success) {
                        // After success, fetch fresh dashboard so the list updates naturally
                        fetchDashboardNoCache()
                    } else {
                        android.widget.Toast.makeText(
                            MyApplication.appContext, "Failed to mark medicine",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    android.widget.Toast.makeText(
                        MyApplication.appContext, "Medicine schedule not found",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error marking intake: ${e.message}")
            } finally {
                // Clear spinner
                _markingMedicines.value = _markingMedicines.value - medicine.id
            }
        }
    }

    /**
     * Same as [fetchDashboard] but bypasses the API cache so the
     * medicine list is guaranteed to be fresh after a mark-as-taken.
     */
    fun fetchDashboardNoCache() {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            try {
                val params = mapOf(
                    "UHID"     to patient.uhId,
                    "clientId" to patient.clientId.toString()
                )
                val response = ApiHelper().callApi(
                    MyApplication.appContext,
                    endpoints.getPatientDashboardData,
                    cacheResponse = false          // ← force fresh response
                ) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = params)
                }
                if (response.isSuccessful) {
                    val body   = response.body()?.string()
                    val parsed = com.google.gson.Gson().fromJson(
                        body, com.critetiontech.vitalio_cis.model.DashboardResponse::class.java
                    )
                    if (parsed.status == 1) {
                        _vitalList.value        = parsed.responseValue.latestVitals
                        _appointments.value     = parsed.responseValue.upcomingAppointments
                        _pendingMedicines.value = parsed.responseValue.pendingMedicines.filter { it.medicineName.isNotBlank() }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "fetchDashboardNoCache error: ${e.message}")
            }
        }
    }

}