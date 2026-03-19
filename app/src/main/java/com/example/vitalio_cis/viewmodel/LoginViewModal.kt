package com.critetiontech.ctvitalio.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.ui.components.MyDialog
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.utils.ErrorUtils.parseErrorMessage
import com.example.vitalio_cis.utils.BaseResponse
import com.example.vitalio_cis.utils.Patient
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
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
//    }



    fun sendOTP(context: Context) {

        viewModelScope.launch {

            _loading.value = true
            _loginSuccess.value = false

            val prefsCache = PrefsManager(context)

            try {

                Log.d("LoginViewModel", "Sending OTP for mobile: $mobile")

                val queryParams = mapOf(
                    "key" to mobile.toString(),
                    "ifLoggedOutFromAllDevices" to true
                )

                // ✅ Correct Generic Type (Array use karo)
                val patientArray: Array<Patient>? = prefsCache.getData(
                    key = ApiEndPointCorporateModule().corporateEmployeeLogin,
                    clazz = Array<Patient>::class.java
                ) {

                    // 🔥 DIRECT API CALL (NO ApiHelper)
                    val response = ApiClients.module4082.queryDynamicRawPost(
                        url = ApiEndPointCorporateModule().corporateEmployeeLogin,
                        params = queryParams
                    )


                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "6307748142: $bodyString")

                        if (!bodyString.isNullOrEmpty()) {

                            val type = object : TypeToken<BaseResponse<List<Patient>>>() {}.type
                            val parsed: BaseResponse<List<Patient>> =
                                Gson().fromJson(bodyString, type)

                            // ✅ IMPORTANT: List → Array convert
                            parsed.responseValue?.toTypedArray()

                        } else {
                            null
                        }

                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ Convert Array → List
                val patientList = patientArray?.toList()

                // ✅ Final result handle
                if (!patientList.isNullOrEmpty()) {

                    _loginSuccess.value = true
                    Log.d("LoginViewModel", "Login Success (API/Cache)")

                    // Optional: first patient save
                    val firstPatient = patientList.first()
                    Log.d("LoginViewModel", "Patient: ${firstPatient.patientName}")

                } else {
                    _errorMessage.value = "No data available"
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