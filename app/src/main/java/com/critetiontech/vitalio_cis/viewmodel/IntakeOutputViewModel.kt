package com.critetiontech.vitalio_cis.viewmodel

import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.FluidSummaryItem
import com.critetiontech.vitalio_cis.model.IntakeItem
import com.critetiontech.vitalio_cis.model.ManualFoodAssignItem
import com.critetiontech.vitalio_cis.model.OutputItem
import com.critetiontech.vitalio_cis.model.OutputSummaryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class IntakeOutputViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    var searchText by mutableStateOf(""); private set
    fun onSearchChange(value: String) { searchText = value }

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _outputList = MutableStateFlow<List<OutputItem>>(emptyList())
    val outputList: StateFlow<List<OutputItem>> = _outputList

    private val _outputSummaryList = MutableStateFlow<List<OutputSummaryItem>>(emptyList())
    val outputSummaryList: StateFlow<List<OutputSummaryItem>> = _outputSummaryList

    private val _outputLoading = MutableStateFlow(false)
    val outputLoading: StateFlow<Boolean> = _outputLoading

    private val _intakeList = MutableStateFlow<List<IntakeItem>>(emptyList())
    val intakeList: StateFlow<List<IntakeItem>> = _intakeList

    private val _fluidSummaryList = MutableStateFlow<List<FluidSummaryItem>>(emptyList())
    val fluidSummaryList: StateFlow<List<FluidSummaryItem>> = _fluidSummaryList

    private val _intakeLoading = MutableStateFlow(false)
    val intakeLoading: StateFlow<Boolean> = _intakeLoading

    private val _manualFoodList = MutableStateFlow<List<ManualFoodAssignItem>>(emptyList())
    val manualFoodList: StateFlow<List<ManualFoodAssignItem>> = _manualFoodList

    fun getManualFoodAssignList() {
        viewModelScope.launch {
            _intakeLoading.value = true
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            when (val r = deps.getManualFoodList()(patient.uhId)) {
                is DomainResult.Success -> _manualFoodList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchIntakeItems(fromDate: String) {
        viewModelScope.launch {
            _intakeLoading.value = true; _intakeList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            when (val r = deps.fetchIntake()(patient.uhId, patient.clientId.toString(), fromDate)) {
                is DomainResult.Success -> _intakeList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun addIntake(value: String, fId: String) {
        viewModelScope.launch {
            _intakeLoading.value = true
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            val foodDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) Instant.now().toString() else System.currentTimeMillis().toString()
            val body = mapOf<String, Any>("FoodDate" to foodDate, "FoodQuantity" to value, "UserID" to patient.pid, "foodId" to fId, "clientId" to patient.clientId, "FoodUnitId" to 27, "uhid" to patient.uhId, "isGiven" to 1, "entryType" to "N", "isFromPatient" to 1)
            when (val r = deps.addIntake()(body)) {
                is DomainResult.Success -> Log.d("IntakeOutputVM", "addIntake success")
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchOutput(date: String) {
        viewModelScope.launch {
            _outputLoading.value = true; _outputList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            when (val r = deps.fetchOutput()(patient.uhId, patient.clientId.toString(), patient.pid.toString(), date)) {
                is DomainResult.Success -> _outputList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun addOutput(quantity: Int, colour: String) {
        viewModelScope.launch {
            _outputLoading.value = true
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            val outputDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) else java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val body = mapOf<String, Any>("outputTypeID" to 51, "quantity" to quantity, "unitID" to 1, "outputDate" to outputDate, "colour" to colour, "userID" to patient.pid, "clientId" to patient.clientId, "uhid" to patient.uhId)
            when (val r = deps.addOutput()(body)) {
                is DomainResult.Success -> Log.d("IntakeOutputVM", "addOutput success")
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun fetchOutputSummary(fromDate: String, toDate: String) {
        viewModelScope.launch {
            _outputLoading.value = true; _outputSummaryList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            when (val r = deps.fetchOutputSummary()(patient.uhId, patient.clientId.toString(), fromDate, toDate)) {
                is DomainResult.Success -> _outputSummaryList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun fetchFluidSummary(fromDate: String, toDate: String) {
        viewModelScope.launch {
            _intakeLoading.value = true; _fluidSummaryList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            when (val r = deps.fetchFluidSummary()(patient.uhId, fromDate, toDate)) {
                is DomainResult.Success -> _fluidSummaryList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchIntake() {
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now().toString() else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        fetchIntakeItems(today)
    }
}
