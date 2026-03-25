package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.NavigationManager
import com.example.vitalio_cis.Routes
import com.example.vitalio_cis.model.Vital
import com.example.vitalio_cis.model.VitalApiResponse
import com.example.vitalio_cis.utils.PatientResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class VitalDetailViewModel   @Inject constructor() : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList
    fun getPatientDetailsByMobileNo(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "ClientId" to prefsCache.getPatient()?.clientId.toString(),
                    "userId" to "1354",
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchLastVital,
                    clazz = String::class.java,
                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchLastVital,
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


                    val apiResponse = Gson().fromJson(result, VitalApiResponse::class.java)

                    _vitalList.value = apiResponse.responseValue.lastVital


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