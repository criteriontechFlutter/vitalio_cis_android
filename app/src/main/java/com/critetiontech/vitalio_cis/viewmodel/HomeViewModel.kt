package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.DashboardResponse
import com.critetiontech.vitalio_cis.model.UpcomingAppointment
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.utils.MyApplication
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val fetchLastVitalUseCase = AppDependencies.fetchLastVital()
    private val prefs = AppDependencies.prefs
    private val endpoints = ApiEndPointCorporateModule()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

    private val _appointments = MutableStateFlow<List<UpcomingAppointment>>(emptyList())
    val appointments: StateFlow<List<UpcomingAppointment>> = _appointments

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
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "fetchDashboard error: ${e.message}")
            }
            _loading.value = false
        }
    }

    fun fetchLastVital() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = fetchLastVitalUseCase(patient.uhId, patient.clientId.toString(), patient.pid.toString())) {
                is DomainResult.Success -> _vitalList.value = r.data
                is DomainResult.Error -> Log.e("HomeViewModel", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }
}