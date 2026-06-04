package com.critetiontech.vitalio_cis.viewmodel

import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Problem
import com.critetiontech.vitalio_cis.model.SymptomItem
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SymptomTrackerViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _addLoading = MutableStateFlow(false)
    val addLoading: StateFlow<Boolean> = _addLoading

    private val _hasFetched = MutableStateFlow(false)
    val hasFetched: StateFlow<Boolean> = _hasFetched

    private val _symptomTrackerList = MutableStateFlow<List<SymptomItem>>(emptyList())
    val symptomTrackerList: StateFlow<List<SymptomItem>> = _symptomTrackerList

    private val _symptomIconsList = MutableStateFlow<List<Problem>>(emptyList())
    val symptomIconsList: StateFlow<List<Problem>> = _symptomIconsList

    private val _searchedSymptomList = MutableStateFlow<List<Problem>>(emptyList())
    val searchedsymptomList: StateFlow<List<Problem>> = _searchedSymptomList

    private val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
    val selectedSymptoms: StateFlow<List<SymptomItem>> = _selectedSymptoms

    private val _popBackEvent = MutableSharedFlow<Unit>()
    val popBackEvent: SharedFlow<Unit> = _popBackEvent

    fun updateSelectedSymptoms() { _selectedSymptoms.value = emptyList() }
    fun clearSearchResults() { _searchedSymptomList.value = emptyList() }
    fun addSymptom(symptom: SymptomItem) { _selectedSymptoms.value = (_selectedSymptoms.value + symptom).distinctBy { it.detailId } }
    fun removeSymptom(symptom: SymptomItem) { _selectedSymptoms.value = _selectedSymptoms.value.filterNot { it.detailId == symptom.detailId } }

    fun getSymptoms() {
        viewModelScope.launch {
            _hasFetched.value = false; _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; _hasFetched.value = true; return@launch }
            when (val r = deps.fetchSymptoms()(patient.uhId, patient.clientId.toString())) {
                is DomainResult.Success -> _symptomTrackerList.value = r.data
                is DomainResult.Error -> Log.e("SymptomTrackerVM", r.exception.message.orEmpty())
            }
            _loading.value = false; _hasFetched.value = true
        }
    }

    fun insertSymptoms() {
        if (_selectedSymptoms.value.isEmpty()) { viewModelScope.launch { _popBackEvent.emit(Unit) }; return }
        viewModelScope.launch {
            _addLoading.value = true
            val patient = prefs.getPatient() ?: run { _addLoading.value = false; return@launch }
            val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
            else SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
            val dtDataTable = _selectedSymptoms.value.map { mapOf("detailID" to it.detailId, "detailsDate" to now, "details" to it.details, "pdmId" to 2) }
            val body = mapOf("uhId" to patient.uhId, "jsonSymtoms" to Gson().toJson(dtDataTable), "clientId" to patient.clientId, "type" to "Symptoms", "isFromPatient" to true)
            when (val r = deps.insertSymptoms()(body)) {
                is DomainResult.Success -> _popBackEvent.emit(Unit)
                is DomainResult.Error -> Log.e("SymptomTrackerVM", r.exception.message.orEmpty())
            }
            _addLoading.value = false
        }
    }

    fun saveProblems(selectedIds: List<Int>, allProblems: List<Problem>) {
        _selectedSymptoms.value = allProblems.filter { it.problemId in selectedIds }.map { SymptomItem(detailId = it.problemId, details = it.problemName) }
        insertSymptoms()
    }

    fun getProblemsWithIcon() {
        viewModelScope.launch {
            _loading.value = true
            when (val r = deps.getProblemsWithIcon()()) {
                is DomainResult.Success -> _symptomIconsList.value = r.data
                is DomainResult.Error -> Log.e("SymptomTrackerVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun getAllProblems(query: String) {
        viewModelScope.launch {
            _loading.value = true
            when (val r = deps.getAllProblems()(query)) {
                is DomainResult.Success -> _searchedSymptomList.value = r.data
                is DomainResult.Error -> { _searchedSymptomList.value = emptyList(); Log.e("SymptomTrackerVM", r.exception.message.orEmpty()) }
            }
            _loading.value = false
        }
    }
}
