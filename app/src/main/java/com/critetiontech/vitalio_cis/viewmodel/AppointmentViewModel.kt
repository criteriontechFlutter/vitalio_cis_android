package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AppointmentHistoryItem
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

class AppointmentViewModel : ViewModel() {

    private val fetchUpcomingUseCase = AppDependencies.fetchUpcomingAppointments()
    private val fetchHistoryUseCase = AppDependencies.fetchAppointmentHistory()
    private val prefs = AppDependencies.prefs
    private val networkMonitor = AppDependencies.networkMonitor

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _upcoming = MutableStateFlow<List<UpcomingAppointmentItem>>(emptyList())
    val upcoming: StateFlow<List<UpcomingAppointmentItem>> = _upcoming

    private val _history = MutableStateFlow<List<AppointmentHistoryItem>>(emptyList())
    val history: StateFlow<List<AppointmentHistoryItem>> = _history

    init {
        // Auto-refetch whenever network comes back online after being offline
        viewModelScope.launch {
            networkMonitor.isOnline
                .drop(1)               // skip the initial emission
                .filter { it }         // only act when going online (not when going offline)
                .collect {
                    Log.d("AppointmentVM", "Network restored — re-fetching")
                    fetchAll()
                }
        }
    }

    fun fetchAll() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            val pid = patient.pid.toString()
            val clientId = patient.clientId.toString()

            // Fetch upcoming first — this is the default tab, loading ends here
            when (val r = fetchUpcomingUseCase(pid, clientId)) {
                is DomainResult.Success -> _upcoming.value = r.data
                is DomainResult.Error -> Log.e("AppointmentVM", "upcoming: ${r.exception.message}")
            }
            _loading.value = false

            // History loads in background — won't block the shimmer/UI
            launch {
                when (val r = fetchHistoryUseCase(pid, clientId)) {
                    is DomainResult.Success -> _history.value = r.data
                    is DomainResult.Error -> Log.e("AppointmentVM", "history: ${r.exception.message}")
                }
            }
        }
    }
}