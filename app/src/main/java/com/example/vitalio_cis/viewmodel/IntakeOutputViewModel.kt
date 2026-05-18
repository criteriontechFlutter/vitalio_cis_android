package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.model.Doctor
import com.example.vitalio_cis.model.DoctorResponse
import com.example.vitalio_cis.model.FluidSummaryItem
import com.example.vitalio_cis.model.FluidSummaryResponse
import com.example.vitalio_cis.model.IntakeItem
import com.example.vitalio_cis.model.IntakeResponse
import com.example.vitalio_cis.model.OutputItem
import com.example.vitalio_cis.model.OutputResponse
import com.example.vitalio_cis.model.OutputSummaryItem
import com.example.vitalio_cis.model.OutputSummaryResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class IntakeOutputViewModel @Inject constructor() : ViewModel() {


//http://172.16.61.31:4096/api/ManualFoodAssign/GetManualFoodAssignList?Uhid=UHID2604500011&intervalTimeInHour=24
//
//http://172.16.61.31:4096/api/FoodIntake/FetchIntake?entryType=N&Uhid=UHID2604500011&clientId=45&fromDate=2026-05-01
//
//http://172.16.61.31:4096/api/ManualFoodAssign/FluidSummaryByDateRange?toDate=2026-05-01&intervalTimeInHour=24&fromDate=2026-04-25&Uhid=UHID2604500011
//
    var searchText by mutableStateOf("")
        private set

    fun onSearchChange(value: String) {
        searchText = value
    }

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _doctorList = MutableStateFlow<List<Doctor>>(emptyList())
    val doctorList: StateFlow<List<Doctor>> = _doctorList

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


    @RequiresApi(Build.VERSION_CODES.O)
    fun fetchIntake(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {

                val fromDate = java.time.LocalDate.now().toString()
                val queryParams = mapOf(
                   "entryType" to "N",
                    "Uhid" to prefsCache.getPatient()?.uhId.toString(),
                   "clientId" to prefsCache.getPatient()?.clientId.toString(),
                   "fromDate" to fromDate
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchIntake,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchIntake,
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


                _loading.value = false
                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {



                    Log.d("LoginViewModel", "OTP SuccessSuccess (API/Cache): $result")
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

    fun getManualFoodAssignList(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(
                    "Uhid" to  prefsCache.getPatient()?.uhId.toString(),
                    "intervalTimeInHour" to 24
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getManualFoodAssignList,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getManualFoodAssignList,
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


                _loading.value = false
                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {


                    val apiResponse = Gson().fromJson(result, DoctorResponse::class.java)

                    _doctorList.value = apiResponse.responseValue


                    Log.d("LoginViewModel", "OTP SuccessSuccess (API/Cache): $result")
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




    @RequiresApi(Build.VERSION_CODES.O)
    fun addOutput(context: Context, quantity: Int, colour: String) {
        viewModelScope.launch {
            _outputLoading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val outputDate = java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                val body = mapOf<String, Any>(
                    "outputTypeID" to 51,
                    "quantity" to quantity,
                    "unitID" to 1,
                    "outputDate" to outputDate,
                    "colour" to colour,
                    "userID" to (prefsCache.getPatient()?.pid ?: 0),
                    "clientId" to (prefsCache.getPatient()?.clientId ?: 0),
                    "uhid" to prefsCache.getPatient()?.uhId.orEmpty()
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().addOutput,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("IntakeOutputViewModel", "addOutput success: ${response.body()?.string()}")
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "addOutput error: ${e.message}", e)
            } finally {
                _outputLoading.value = false
            }
        }
    }




    fun fetchOutput(context: Context, date: String) {
        viewModelScope.launch {
            _outputLoading.value = true
            _outputList.value = emptyList()
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "userId" to prefsCache.getPatient()?.pid.toString(),
                    "fromdate" to date
                )
                val result: String? = prefsCache.getData(
                    key = "${ApiEndPointCorporateModule().fetchOutput}_$date",
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchOutput,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, OutputResponse::class.java)
                    if (apiResponse.status == 1) {
                        _outputList.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "fetchOutput error: ${e.message}", e)
            } finally {
                _outputLoading.value = false
            }
        }
    }

    fun fetchOutputSummary(context: Context, fromDate: String, toDate: String) {
        viewModelScope.launch {
            _outputLoading.value = true
            _outputSummaryList.value = emptyList()
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "Uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "fromDate" to fromDate,
                    "toDate" to toDate
                )
                val result: String? = prefsCache.getData(
                    key = "${ApiEndPointCorporateModule().outputSummaryByDateRange}_${fromDate}_$toDate",
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().outputSummaryByDateRange,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, OutputSummaryResponse::class.java)
                    if (apiResponse.status == 1) {
                        _outputSummaryList.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "fetchOutputSummary error: ${e.message}", e)
            } finally {
                _outputLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun addIntake(context: Context, value: String, fId: String) {
        viewModelScope.launch {
            _intakeLoading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val foodDate = java.time.Instant.now().toString()
                val body = mapOf<String, Any>(
                    "FoodDate" to foodDate,
                    "FoodQuantity" to value,
                    "UserID" to (prefsCache.getPatient()?.pid ?: 0),
                    "foodId" to fId,
                    "clientId" to (prefsCache.getPatient()?.clientId ?: 0),
                    "FoodUnitId" to 27,
                    "uhid" to prefsCache.getPatient()?.uhId.orEmpty(),
                    "isGiven" to 1,
                    "entryType" to "N",
                    "isFromPatient" to 1
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().addIntake,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("IntakeOutputViewModel", "addIntake success: ${response.body()?.string()}")
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "addIntake error: ${e.message}", e)
            } finally {
                _intakeLoading.value = false
            }
        }
    }

    fun fetchIntakeItems(context: Context, fromDate: String) {
        viewModelScope.launch {
            _intakeLoading.value = true
            _intakeList.value = emptyList()
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "Uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "entryType" to "N",
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "fromDate" to fromDate
                )
                val result: String? = prefsCache.getData(
                    key = "${ApiEndPointCorporateModule().fetchIntake}_$fromDate",
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchIntake,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, IntakeResponse::class.java)
                    if (apiResponse.status == 1) {
                        _intakeList.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "fetchIntakeItems error: ${e.message}", e)
            } finally {
                _intakeLoading.value = false
            }
        }
    }

    fun fetchFluidSummary(context: Context, fromDate: String, toDate: String) {
        viewModelScope.launch {
            _intakeLoading.value = true
            _fluidSummaryList.value = emptyList()
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "Uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "fromDate" to fromDate,
                    "toDate" to toDate
                )
                val result: String? = prefsCache.getData(
                    key = "${ApiEndPointCorporateModule().fluidSummaryByDateRange}_${fromDate}_$toDate",
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fluidSummaryByDateRange,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) response.body()?.string()
                    else throw Exception("API Error: ${response.code()}")
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, FluidSummaryResponse::class.java)
                    if (apiResponse.status == 1) {
                        _fluidSummaryList.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("IntakeOutputViewModel", "fetchFluidSummary error: ${e.message}", e)
            } finally {
                _intakeLoading.value = false
            }
        }
    }

}