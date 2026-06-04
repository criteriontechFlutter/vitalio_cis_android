package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllergyItem
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AllergyViewModel : ViewModel() {

    private val fetchAllergiesUseCase = AppDependencies.fetchAllergies()
    private val addAllergyUseCase = AppDependencies.addAllergy()
    private val prefs = AppDependencies.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _addLoading = MutableStateFlow(false)
    val addLoading: StateFlow<Boolean> = _addLoading

    private val _medicineAllergies = MutableStateFlow<List<AllergyItem>>(emptyList())
    val medicineAllergies: StateFlow<List<AllergyItem>> = _medicineAllergies

    private val _foodAllergies = MutableStateFlow<List<AllergyItem>>(emptyList())
    val foodAllergies: StateFlow<List<AllergyItem>> = _foodAllergies

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _addSuccess = MutableStateFlow(false)
    val addSuccess: StateFlow<Boolean> = _addSuccess

    fun fetchAllergies() {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            try {
                val medJob = async { fetchAllergiesUseCase(patient.uhId, patient.clientId, "MedicineAllergy") }
                val foodJob = async { fetchAllergiesUseCase(patient.uhId, patient.clientId, "FoodAllergy") }
                when (val med = medJob.await()) {
                    is DomainResult.Success -> _medicineAllergies.value = med.data
                    is DomainResult.Error -> Log.e("AllergyVM", med.exception.message.orEmpty())
                }
                when (val food = foodJob.await()) {
                    is DomainResult.Success -> _foodAllergies.value = food.data
                    is DomainResult.Error -> Log.e("AllergyVM", food.exception.message.orEmpty())
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load allergies."
            } finally {
                _loading.value = false
            }
        }
    }

    fun addAllergy(substanceName: String, severity: String, reaction: String, details: String, typeAllergy: String) {
        if (substanceName.isBlank()) { _errorMessage.value = "Substance name is required."; return }
        viewModelScope.launch {
            _addLoading.value = true
            _errorMessage.value = null
            val patient = prefs.getPatient() ?: run { _addLoading.value = false; return@launch }
            val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val allergyEntry = listOf(mapOf("detailID" to 0, "detailsDate" to now, "details" to details.ifBlank { "Patient reports allergy to $substanceName." }, "substanceName" to substanceName, "allergy" to severity, "reaction" to reaction))
            val body = mapOf("uhId" to patient.uhId, "jsonAllergies" to Gson().toJson(allergyEntry), "userId" to 0, "clientId" to patient.clientId, "isFromPatient" to true, "typeAllergy" to typeAllergy)
            when (val r = addAllergyUseCase(body)) {
                is DomainResult.Success -> { _addSuccess.value = true; fetchAllergies() }
                is DomainResult.Error -> { _errorMessage.value = "Failed to add allergy."; Log.e("AllergyVM", r.exception.message.orEmpty()) }
            }
            _addLoading.value = false
        }
    }

    fun resetAddSuccess() { _addSuccess.value = false }
    fun clearError() { _errorMessage.value = null }
}
