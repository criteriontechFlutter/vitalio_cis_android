package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.utils.Patient
import com.example.vitalio_cis.utils.PrefsManager
import com.example.vitalio_cis.utils.VerifyOtpResponse
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

    fun verifyLogInOTPForSHFCApp(context: Context, uhid: String, otp: String, navController: NavController) {

        viewModelScope.launch {
            _loading.value = true
            _loginSuccess.value = false

            val prefs = PrefsManager(context)

            try {
                val deviceToken = prefs.getDeviceToken() ?: "defaultToken"

                val queryParams = mapOf(
                    "key" to uhid,
                    "otp" to otp,
                    "deviceToken" to deviceToken,
                    "ifLoggedOutFromAllDevices" to true
                )

                val result: String? = prefs.getData(
                    key = ApiEndPointCorporateModule().verifyLogInOTPForSHFCApp,
                    shouldSave = false
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().verifyLogInOTPForSHFCApp,
                        showNoConnectionDialog = true
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) {
                        val body = response.body()?.string()
                        Log.d("OTPViewModel", "VerifyOTP response: $body")
                        body
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                if (!result.isNullOrEmpty()) {
                    val parsed = Gson().fromJson(result, VerifyOtpResponse::class.java)

                    if (parsed.status == 1 && parsed.responseValue != null) {
                        val p = parsed.responseValue
                        val patient = Patient(
                            pid = p.pid,
                            uhId = p.uhId,
                            firstName = p.firstName,
                            lastName = p.lastName,
                            dob = p.dob,
                            genderId = p.genderId,
                            mobileNo = p.mobileNo,
                            emailAddress = p.emailId,
                            age = p.age,
                            address = p.address,
                            cityId = p.cityId,
                            stateId = p.stateId,
                            bloodGroupId = p.bloodGroupId,
                            clientId = p.clientId,
                            countryCallingCode = "",
                            guardianRelationId = 0,
                            guardianName = "",
                            countryId = 0,
                            imageUrl = "",
                            zip = "",
                            isActive = true,
                            departmentName = null,
                            cityName = "",
                            stateName = "",
                            countryName = "",
                            genderName = "",
                            guardianRelationName = null,
                            bloodGroupName = null
                        )
                        prefs.savePatient(patient)
                        Log.d("OTPViewModel", "Patient saved: ${patient.firstName} ${patient.lastName}")
                        _loginSuccess.value = true
                        navController.navigate(Routes.DASHBOARD)
                    } else {
                        _errorMessage.value = parsed.message.ifEmpty { "OTP verification failed" }
                        Log.d("OTPViewModel", "OTP failed: ${parsed.message}")
                    }
                } else {
                    _errorMessage.value = "No response from server"
                }

            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Unknown error"
                Log.e("OTPViewModel", "Error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }
}
