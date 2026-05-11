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
    val  doctorList: StateFlow<List<Doctor>> = _doctorList


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




    fun addOutput(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(
                    "userID" to  prefsCache.getPatient()?.pid.toString(),
                    "uhid" to  prefsCache.getPatient()?.uhId.toString(),
                    "unitID" to "1",
                    "clientId" to  prefsCache.getPatient()?.clientId.toString(),
                    "outputDate" to "2026-05-01 11:32",
                    "outputTypeID" to "51",
                    "colour" to "#FF0000",
                    "quantity" to "163"
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().addOutput,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().addOutput,
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
    fun addIntake(context: Context, value :String ,fId:String  ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {
                val foodDate = java.time.Instant.now().toString()
                val queryParams = mapOf(



                    "FoodDate" to  foodDate,
                "FoodQuantity" to  value.toString(),
                "UserID" to   prefsCache.getPatient()?.pid.toString(),
                "foodId" to  fId,
                "clientId" to   prefsCache.getPatient()?.clientId.toString(),
                "FoodUnitId" to  27,
                "uhid" to   prefsCache.getPatient()?.uhId.toString(),
                "isGiven" to  1,
                "entryType" to  "N",
                "isFromPatient" to  1
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().addIntake,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().addIntake,
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

}