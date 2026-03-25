package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
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
import com.example.vitalio_cis.NavigationManager
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.utils.PatientResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import javax.inject.Inject


class OTPViewModel @Inject constructor() : ViewModel() {



    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _loginSuccess = MutableLiveData<Boolean>()
    val loginSuccess: LiveData<Boolean> = _loginSuccess

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage


    var employeeId by mutableStateOf("")
        private set

    var password by mutableStateOf("")
        private set


    fun onEmployeeIdChange(newId: String) {
        employeeId = newId
    }

    fun onPasswordChange(newPassword: String) {
        password = newPassword
    }
    var mobile by mutableStateOf("")
        private set

    fun onMobileChange(newValue: String) {
        mobile = newValue
    }




    fun verifyLogInOTPForSHFCApp(context: Context,uhid:String  ,otp: String  ) {

        viewModelScope.launch {
            getPatientDetailsByMobileNo(context)
            _loading.value = true
            _loginSuccess.value = false

            val prefsCache = PrefsManager(context)

            try {

                Log.d("LoginViewModel", "Sending OTP for mobile: $mobile")

                val queryParams = mapOf(
                    "otp" to otp,
                    "UHID" to uhid,
                    "deviceToken" to  "APA91bHxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
                    "ifLoggedOutFromAllDevices" to  "0"
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().verifyLogInOTPForSHFCApp,
                    clazz = String::class.java,
                    shouldSave = false
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().verifyLogInOTPForSHFCApp,
                        showNoConnectionDialog = true
                    ) { url ->
                        ApiClients.module4082.queryDynamicRawPost(
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

                    _loginSuccess.value = true
                    NavigationManager.navigate(Routes.OTP)
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")

                } else {
                    _errorMessage.value = "No data available"
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {

                _errorMessage.value = e.message ?: "Unknown error"
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }




    fun getPatientDetailsByMobileNo(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true
            _loginSuccess.value = false

            val prefsCache = PrefsManager(context)

            try {

                Log.d("LoginViewModel", "Sending OTP for mobile: $mobile")

                val queryParams = mapOf(

                    "mobileNo" to "6307748142",
                    "uhid" to "",
                    "ClientId" to 45
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getPatientDetailsByMobileNo,
                    clazz = String::class.java,
                    shouldSave = false
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getPatientDetailsByMobileNo,
                        showNoConnectionDialog = true
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

                    _loginSuccess.value = true

                    // ✅ JSON → Model convert
                    val apiResponse = Gson().fromJson(result, PatientResponse::class.java)

                    // ✅ responseValue se data nikaalo
                    val patient = apiResponse.responseValue.firstOrNull()

                    if (patient != null) {

                        PrefsManager(context).savePatient(patient)



                        NavigationManager.navigate(Routes.DASHBOARD)

                        Log.d("LoginViewModel", "Patient Name: ${patient.firstName}")
                    }


                } else {
                    _errorMessage.value = "No data available"
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {

                _errorMessage.value = e.message ?: "Unknown error"
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }


}