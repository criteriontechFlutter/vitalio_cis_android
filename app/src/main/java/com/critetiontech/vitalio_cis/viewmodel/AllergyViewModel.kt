package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.vitalio_cis.model.AllergyApiResponse
import com.critetiontech.vitalio_cis.model.AllergyItem
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class AllergyViewModel @Inject constructor() : ViewModel() {

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

    fun fetchAllergies(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            val patient = PrefsManager(context).getPatient()
            try {
                val medicineJob = async { fetchByType(context, patient?.uhId.orEmpty(), patient?.clientId ?: 0, "MedicineAllergy") }
                val foodJob = async { fetchByType(context, patient?.uhId.orEmpty(), patient?.clientId ?: 0, "FoodAllergy") }
                _medicineAllergies.value = medicineJob.await()
                _foodAllergies.value = foodJob.await()
            } catch (e: Exception) {
                Log.e("AllergyViewModel", "fetchAllergies error: ${e.message}", e)
                _errorMessage.value = "Failed to load allergies."
            } finally {
                _loading.value = false
            }
        }
    }

    private suspend fun fetchByType(context: Context, uhId: String, clientId: Int, typeAllergy: String): List<AllergyItem> {
        val queryParams = mapOf(
            "uhid" to uhId,
            "clientId" to clientId.toString(),
            "typeAllergy" to typeAllergy
        )
        val result: String? = PrefsManager(context).getData(
            key = "${ApiEndPointCorporateModule().fetchAllergies}_$typeAllergy",
            shouldSave = false
        ) {
            val response = ApiHelper().callApi(
                context,
                ApiEndPointCorporateModule().fetchAllergies,
                showNoConnectionDialog = false
            ) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = queryParams)
            }
            if (response.isSuccessful) response.body()?.string()
            else throw Exception("API Error: ${response.code()}")
        }
        if (!result.isNullOrEmpty()) {
            val apiResponse = Gson().fromJson(result, AllergyApiResponse::class.java)
            if (apiResponse.status == 1) return apiResponse.responseValue
        }
        return emptyList()
    }

    fun addAllergy(
        context: Context,
        substanceName: String,
        severity: String,
        reaction: String,
        details: String,
        typeAllergy: String
    ) {
        if (substanceName.isBlank()) {
            _errorMessage.value = "Substance name is required."
            return
        }
        viewModelScope.launch {
            _addLoading.value = true
            _errorMessage.value = null
            val patient = PrefsManager(context).getPatient()
            try {
                val now = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val allergyEntry = listOf(
                    mapOf(
                        "detailID" to 0,
                        "detailsDate" to now,
                        "details" to details.ifBlank { "Patient reports allergy to $substanceName." },
                        "substanceName" to substanceName,
                        "allergy" to severity,
                        "reaction" to reaction
                    )
                )
                val body = mapOf(
                    "uhId" to patient?.uhId.orEmpty(),
                    "jsonAllergies" to Gson().toJson(allergyEntry),
                    "userId" to 0,
                    "clientId" to (patient?.clientId ?: 0),
                    "isFromPatient" to true,
                    "typeAllergy" to typeAllergy
                )
                Log.d("AllergyViewModel", "addAllergy request: $body")
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().addAllergies,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("AllergyViewModel", "addAllergy success: ${response.body()?.string()}")
                    _addSuccess.value = true
                    fetchAllergies(context)
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("AllergyViewModel", "addAllergy error: ${e.message}", e)
                _errorMessage.value = "Failed to add allergy. Please try again."
            } finally {
                _addLoading.value = false
            }
        }
    }

    fun resetAddSuccess() {
        _addSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}