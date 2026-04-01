package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.model.Problem
import com.example.vitalio_cis.model.SymptomApiResponse
import com.example.vitalio_cis.model.SymptomItem
import com.example.vitalio_cis.model.Vital
import com.example.vitalio_cis.model.VitalApiResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import com.example.vitalio_cis.model.ProblemResponse

class SymptomTrackerViewModel @Inject constructor() : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _symptomTrackerList = MutableStateFlow<List<SymptomItem>>(emptyList())
    val symptomTrackerList: StateFlow<List<SymptomItem>> = _symptomTrackerList



    private val _symptomIconsList = mutableStateOf<List<Problem>>(emptyList())
    val symptomIconsList: State<List<Problem>> = _symptomIconsList

    private val _searchedsymptomList = mutableStateOf<List<Problem>>(emptyList())
    val  searchedsymptomList: State<List<Problem>> = _searchedsymptomList

    private val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
    val selectedSymptoms: StateFlow<List<SymptomItem>> = _selectedSymptoms
    fun updateSelectedSymptoms(){
        _selectedSymptoms.value=emptyList<SymptomItem>()
    }


    fun addSymptom(symptom: SymptomItem) {
        val updatedList = _selectedSymptoms.value.toMutableList()
        if (!updatedList.contains(symptom)) {
            updatedList.add(symptom)
        }
        _selectedSymptoms.value = updatedList
    }

    fun removeSymptom(symptom: SymptomItem) {
        val updatedList = _selectedSymptoms.value.toMutableList()
        updatedList.remove(symptom)
        _selectedSymptoms.value = updatedList
    }

    fun submitSymptoms() {
        val result = _selectedSymptoms.value.map {
            mapOf(
                "detailId" to it.detailId,
                "name" to it.details
            )
        }

        println("FINAL RESULT: $result")

        // 👉 API call here
    }
    fun getSymptoms(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "uhID" to prefsCache.getPatient()?.uhId.toString(),
                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                    "type"  to "Symptoms"
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getSymptoms,
                    clazz = String::class.java,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getSymptoms,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, SymptomApiResponse::class.java)

                    _symptomTrackerList.value = apiResponse.responseValue


                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }

    fun insertSymptoms(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {

                val dtDataTable = mutableListOf<Map<String, String>>()

                // Get the current timestamp once
                val now = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS"))
                } else {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                }

                // Populate data table from selected symptoms
//                selectedSymptoms.forEach { symptom ->
//                    dtDataTable.add(
//                        mapOf(
//                            "detailID" to symptom.pdmID.toString(),
//                            "detailsDate" to now,
//                            "details" to symptom.details,
//                            "pdmId" to "1"
//                        )
//                    )
//                }

                val queryParams = mapOf(
                    "uhID" to (prefsCache.getPatient()?.uhId ?: ""),
                    "jsonSymtoms" to Gson().toJson(dtDataTable),
                    "clientId" to  prefsCache.getPatient()?.clientId.toString(),
                    "isFromPatient" to true,
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getSymptoms,
                    clazz = String::class.java,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getSymptoms,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, SymptomApiResponse::class.java)

                    _symptomTrackerList.value = apiResponse.responseValue


                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }

    fun getProblemsWithIcon(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(
                    "problemName" to "",
                    "languageId" to "1"
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getProblemsWithIcon,
                    clazz = String::class.java,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getProblemsWithIcon,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.Digidoctor_BaseURL.dynamicRawPost(
                            url = url,
                            body = queryParams,
                        )
                    }


                    _loading.value = false
                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, ProblemResponse::class.java)

                    _symptomIconsList.value = apiResponse.responseValue


                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }





    fun getAllProblems(context: Context, query: String  ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(
                    "alphabet" to query,
                    "language" to  1
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getAllProblems,
                    clazz = String::class.java,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getAllProblems,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.Digidoctor_BaseURL.dynamicRawPost(
                            url = url,
                            body = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, ProblemResponse::class.java)

                    _searchedsymptomList.value = apiResponse.responseValue


                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }

}