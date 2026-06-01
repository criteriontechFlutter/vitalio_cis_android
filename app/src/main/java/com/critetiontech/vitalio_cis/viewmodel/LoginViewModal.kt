package com.critetiontech.ctvitalio.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.launch
import javax.inject.Inject

class LoginViewModel @Inject constructor() : ViewModel() {

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

//    fun sendOTP(
//        context: Context,
//    ) {
//
//        viewModelScope.launch {
//
//            _loading.value = true
//            _loginSuccess.value = false
//
//            try {
//                // DEBUG: Mobile number
//                println("sendOTP -> Mobile: $mobile")
//                Log.d("LoginViewModel", "Sending OTP for mobile: $mobile")
//
//                val queryParams = mapOf(
//                    "key" to mobile.toString(),
//                    "ifLoggedOutFromAllDevices" to true
//                )
//
//                // DEBUG: Query params
//                println("sendOTP -> Query Params: $queryParams")
//                Log.d("LoginViewModel", "Query Params: $queryParams")
//
//                val response = ApiHelper().callApi(
//                    context,
//                    ApiEndPointCorporateModule().corporateEmployeeLogin
//                ) { url ->
//                    ApiClients.module4082.queryDynamicRawPost(
//                        url = url,
//                        params = queryParams,
//                    )
//                }
//
//                // DEBUG: Response code
//                println("sendOTP -> Response code: ${response.code()}")
//                Log.d("LoginViewModel", "Response code: ${response.code()}")
//
//                if (response.isSuccessful) {
//
//                    val responseBodyString = response.body()?.string()
//                    println("sendOTP -> Response body: $responseBodyString")
//                    Log.d("LoginViewModel", "Response body: $responseBodyString")
//
//                    if (!responseBodyString.isNullOrEmpty()) {
//
////                        val type = object : TypeToken<BaseResponse<List<Patient>>>() {}.type
////
////                        val parsed: BaseResponse<List<Patient>> =
////                            Gson().fromJson(responseBodyString, type)
//
////                        parsed.responseValue?.firstOrNull()?.let {
////                            PrefsManager(context).savePatient(it)
////                            println("sendOTP -> Patient saved: $it")
////                            Log.d("LoginViewModel", "Patient saved: $it")
////                        }
////
////                        _loginSuccess.value = true
////                        println("sendOTP -> Login Success: true")
//                    }
//
//                } else {
//
//                    val errorMsg = parseErrorMessage(response.errorBody())
//                    _errorMessage.value = errorMsg ?: "Error: ${response.code()}"
//                    println("sendOTP -> Error: ${_errorMessage.value}")
//                    Log.d("LoginViewModel", "Error: ${_errorMessage.value}")
//                }
//
//            } catch (e: Exception) {
//                _errorMessage.value = e.message ?: "Unknown error occurred"
//                println("sendOTP -> Exception: ${e.message}")
//                Log.e("LoginViewModel", "Exception in sendOTP", e)
//            } finally {
//                _loading.value = false
//                println("sendOTP -> Loading finished")
//                Log.d("LoginViewModel", "Loading finished")
//            }
//        }
//    }6307748142



    fun sendOTP(context: Context, navController: NavController) {

        viewModelScope.launch {

            _loading.value = true
            _loginSuccess.value = false

            try {

                Log.d("LoginViewModel", "Sending OTP for mobile: $mobile")

                val queryParams = mapOf(
                    "key" to mobile.toString(),
                    "ifLoggedOutFromAllDevices" to true
                )

                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().corporateEmployeeLogin,
                    showNoConnectionDialog = true
                ) { url ->
                    ApiClients.module4082.queryDynamicRawPost(
                        url = url,
                        params = queryParams,
                    )
                }

                if (response.isSuccessful) {
                    val result = response.body()?.string()
                    Log.d("LoginViewModel", "API Response: $result")

                    if (!result.isNullOrEmpty()) {
                        val parsed = Gson().fromJson(result, SendOtpResponse::class.java)
                        Log.d("LoginViewModel", "isRegisterd: ${parsed.responseValue.isRegisterd}")

                        if (parsed.responseValue.isRegisterd == 1) {
                            _loginSuccess.value = true
                            navController.navigate("otp/$mobile")
                        } else {
                            Toast.makeText(context, "You are not registered in any clinic yet", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(context, "No data available", Toast.LENGTH_LONG).show()
                    }
                } else {
                    val errorBodyString = response.errorBody()?.string()
                    Log.e("LoginViewModel", "Error body: $errorBodyString")
                    val errorMsg = try {
                        val parsed = Gson().fromJson(errorBodyString, SendOtpResponse::class.java)
                        parsed.message.takeIf { it.isNotEmpty() } ?: "Error: ${response.code()}"
                    } catch (e: Exception) {
                        errorBodyString?.takeIf { it.isNotEmpty() } ?: "Error: ${response.code()}"
                    }
                    _errorMessage.value = errorMsg
                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {

                val errorMsg = e.message ?: "Unknown error"
                _errorMessage.value = errorMsg
                Log.e("LoginViewModel", "Error: $errorMsg", e)
                Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }
}

data class SendOtpResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: ResponseValue,
    val isRegisterd: Int = 0,  // API typo preserved intentionally
    val clientId: Int = 0
)


data class ResponseValue(
    val uhid: String?,
    val mobileNo: String?,
    val otp: Int?,
    val countryCallingCode: String?,
    val email: String?,
    val patientName: String?,
    val clientId: Int?,
    val isRegisterd: Int?
)