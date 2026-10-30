package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.SymptomHistoryDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SymptomHistoryUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val days: List<SymptomHistoryDay> = emptyList(),
)

class SymptomHistoryViewModel : ViewModel() {

    private val deps = AppDependencies

    private val _state = MutableStateFlow(SymptomHistoryUiState())
    val state: StateFlow<SymptomHistoryUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val patient = deps.prefs.getPatient() ?: run {
                _state.value = SymptomHistoryUiState(loading = false, error = MyApplication.appContext.getString(R.string.track_session_expired))
                return@launch
            }
            _state.value = when (val r = deps.getSymptomHistory()(patient.uhId, patient.clientId.toString())) {
                is DomainResult.Success -> SymptomHistoryUiState(loading = false, days = r.data)
                is DomainResult.Error -> {
                    Log.e("SymptomHistoryVM", r.exception.message.orEmpty())
                    _state.value.copy(loading = false, error = MyApplication.appContext.getString(R.string.track_check_connection))
                }
            }
        }
    }
}
