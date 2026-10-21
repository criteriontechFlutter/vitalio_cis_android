package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.AppointmentHistoryItem
import com.critetiontech.ctvitalio.model.UpcomingAppointmentItem
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

    private val _historyLoading = MutableStateFlow(false)
    val historyLoading: StateFlow<Boolean> = _historyLoading

    private val _upcoming = MutableStateFlow<List<UpcomingAppointmentItem>>(emptyList())
    val upcoming: StateFlow<List<UpcomingAppointmentItem>> = _upcoming

    private val _history = MutableStateFlow<List<AppointmentHistoryItem>>(emptyList())
    val history: StateFlow<List<AppointmentHistoryItem>> = _history

    /** Set when a list couldn't be loaded and there's nothing to show, so the screen can offer a retry. */
    private val _upcomingError = MutableStateFlow(false)
    val upcomingError: StateFlow<Boolean> = _upcomingError

    private val _historyError = MutableStateFlow(false)
    val historyError: StateFlow<Boolean> = _historyError

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
            _historyLoading.value = true
            val patient = prefs.getPatient() ?: run {
                _loading.value = false; _historyLoading.value = false
                _upcomingError.value = true; _historyError.value = true
                return@launch
            }
            val pid = patient.pid.toString()
            val clientId = patient.clientId.toString()

            // History loads alongside — it also carries the real appointment dates the upcoming list lacks.
            launch {
                when (val r = fetchHistoryUseCase(pid, clientId)) {
                    is DomainResult.Success -> { _history.value = r.data; _historyError.value = false }
                    is DomainResult.Error -> {
                        _historyError.value = _history.value.isEmpty()
                        Log.e("AppointmentVM", "history: ${r.exception.message}")
                    }
                }
                _historyLoading.value = false
            }

            when (val r = fetchUpcomingUseCase(pid, clientId)) {
                is DomainResult.Success -> { _upcoming.value = r.data; _upcomingError.value = false }
                is DomainResult.Error -> {
                    _upcomingError.value = _upcoming.value.isEmpty()
                    Log.e("AppointmentVM", "upcoming: ${r.exception.message}")
                }
            }
            _loading.value = false
        }
    }
}
