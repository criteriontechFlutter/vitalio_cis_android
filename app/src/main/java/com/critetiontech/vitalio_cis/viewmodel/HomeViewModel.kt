package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Vital
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {

    private val fetchLastVitalUseCase = AppDependencies.fetchLastVital()
    private val prefs = AppDependencies.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

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
