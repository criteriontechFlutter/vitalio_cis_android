package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FindDoctorViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    var searchText by mutableStateOf(""); private set
    fun onSearchChange(value: String) { searchText = value }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _doctorList = MutableStateFlow<List<Doctor>>(emptyList())
    val doctorList: StateFlow<List<Doctor>> = _doctorList

    private val _doctorProfiles = MutableStateFlow<Map<Int, DoctorDetails>>(emptyMap())
    val doctorProfiles: StateFlow<Map<Int, DoctorDetails>> = _doctorProfiles

    fun fetchDoctorsAvalability() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchDoctors()(patient.clientId.toString())) {
                is DomainResult.Success -> {
                    _doctorList.value = r.data
                    fetchDoctorProfiles(r.data, patient.clientId.toString())
                }
                is DomainResult.Error -> Log.e("FindDoctorVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    private fun fetchDoctorProfiles(doctors: List<Doctor>, clientId: String) {
        viewModelScope.launch {
            val results = doctors.map { doctor ->
                async {
                    try {
                        when (val r = deps.fetchDoctorProfile()(clientId, doctor.assignedUserId.toString())) {
                            is DomainResult.Success -> r.data?.let { doctor.assignedUserId to it }
                            is DomainResult.Error -> null
                        }
                    } catch (e: Exception) { null }
                }
            }.awaitAll()
            _doctorProfiles.value = results.filterNotNull().toMap()
        }
    }
}
