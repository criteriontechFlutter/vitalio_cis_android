package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.Problem
import com.example.vitalio_cis.model.ProblemResponse
import com.example.vitalio_cis.model.SymptomApiResponse
import com.example.vitalio_cis.model.SymptomItem
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class SymptomTrackerViewModel @Inject constructor() : ViewModel() {

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

    private val _searchedsymptomList = MutableStateFlow<List<Problem>>(emptyList())
    val searchedsymptomList: StateFlow<List<Problem>> = _searchedsymptomList

    private val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
    val selectedSymptoms: StateFlow<List<SymptomItem>> = _selectedSymptoms

    fun updateSelectedSymptoms() {
        _selectedSymptoms.value = emptyList()
    }

    fun clearSearchResults() {
        _searchedsymptomList.value = emptyList()
    }

    fun addSymptom(symptom: SymptomItem) {
        val updatedList = _selectedSymptoms.value.toMutableList()
        if (updatedList.none { it.detailId == symptom.detailId }) {
            updatedList.add(symptom)
        }
        _selectedSymptoms.value = updatedList
    }

    fun removeSymptom(symptom: SymptomItem) {
        _selectedSymptoms.value = _selectedSymptoms.value.filterNot { it.detailId == symptom.detailId }
    }

    fun submitSymptoms() {
        val result = _selectedSymptoms.value.map {
            mapOf("detailId" to it.detailId, "name" to it.details)
        }
        println("FINAL RESULT: $result")
    }

    fun getSymptoms(context: Context) {
        viewModelScope.launch {
            _hasFetched.value = false
            _symptomTrackerList.value = emptyList()
            _loading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "type" to "Symptoms"
                )
                val result: String? = prefsCache.getData(
                    key = ApiEndPointCorporateModule().getSymptoms,
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getSymptoms,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, SymptomApiResponse::class.java)
                    if (apiResponse.status == 1) {
                        _symptomTrackerList.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("SymptomTrackerViewModel", "getSymptoms error: ${e.message}", e)
            } finally {
                _loading.value = false
                _hasFetched.value = true
            }
        }
    }

    fun insertSymptoms(context: Context, navController: NavController) {
        viewModelScope.launch {
            _addLoading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }
                val dtDataTable = _selectedSymptoms.value.map { symptom ->
                    mapOf(
                        "detailID" to symptom.detailId,
                        "detailsDate" to now,
                        "details" to symptom.details,
                        "pdmId" to 2
                    )
                }
                val body = mapOf(
                    "uhId" to prefsCache.getPatient()?.uhId.orEmpty(),
                    "jsonSymtoms" to Gson().toJson(dtDataTable),
                    "clientId" to (prefsCache.getPatient()?.clientId ?: 0),
                    "type" to "Symptoms",
                    "isFromPatient" to true
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().insertSymtoms,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("SymptomTrackerViewModel", "insertSymptoms success: ${response.body()?.string()}")
                    navController.popBackStack()
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SymptomTrackerViewModel", "insertSymptoms error: ${e.message}", e)
            } finally {
                _addLoading.value = false
            }
        }
    }

    fun saveProblems(
        context: Context,
        selectedIds: List<Int>,
        allProblems: List<Problem>,
        navController: NavController
    ) {
        _selectedSymptoms.value = allProblems
            .filter { it.problemId in selectedIds }
            .map { SymptomItem(detailId = it.problemId, details = it.problemName) }
        insertSymptoms(context, navController)
    }

    fun getProblemsWithIcon(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "problemName" to "",
                    "languageId" to "1"
                )
                val result: String? = prefsCache.getData(
                    key = ApiEndPointCorporateModule().getProblemsWithIcon,
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getProblemsWithIcon,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, ProblemResponse::class.java)
                    _symptomIconsList.value = apiResponse.responseValue ?: emptyList()
                }
            } catch (e: Exception) {
                Log.e("SymptomTrackerViewModel", "getProblemsWithIcon error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun getAllProblems(context: Context, query: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val queryParams = mapOf(
                    "alphabet" to query,
                    "language" to 1
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().getAllProblems,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = queryParams)
                }
                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    if (!bodyString.isNullOrEmpty()) {
                        val apiResponse = Gson().fromJson(bodyString, ProblemResponse::class.java)
                        _searchedsymptomList.value = apiResponse.responseValue ?: emptyList()
                    }
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SymptomTrackerViewModel", "getAllProblems error: ${e.message}", e)
                _searchedsymptomList.value = emptyList()
            } finally {
                _loading.value = false
            }
        }
    }
}
