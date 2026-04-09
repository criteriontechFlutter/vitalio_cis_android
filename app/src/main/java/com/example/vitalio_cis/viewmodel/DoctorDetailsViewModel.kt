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
import com.example.vitalio_cis.model.Doctor
import com.example.vitalio_cis.model.DoctorDetails
import com.example.vitalio_cis.model.DoctorResponse
import com.example.vitalio_cis.model.DoctorResponsedata
import com.example.vitalio_cis.model.ShiftData
import com.example.vitalio_cis.model.ShiftResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class DoctorDetailsViewModel @Inject constructor() : ViewModel() {

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading

    private val _doctor = MutableStateFlow<DoctorDetails?>(null)
    val doctor: StateFlow<DoctorDetails?> = _doctor


    private val _slotList = MutableStateFlow<List<ShiftData>>(emptyList())
    val slotList: StateFlow<List<ShiftData>> = _slotList


    fun getDoctorProfile(context: Context,doctorId: String) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                    "doctorId"  to doctorId
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().getDoctorProfile,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().getDoctorProfile,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4084.dynamicGet(
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


                    val apiResponse = Gson().fromJson(result, DoctorResponsedata::class.java)

                    _doctor.value = apiResponse.responseValue.firstOrNull()


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

    fun fetchAvailableSlots(context: Context,doctorId: String) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "doctorId"  to doctorId,
                "scheduleDate" to "2026-02-17",
                "userId" to "1362",
                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                )
                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchAvailableSlots,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchAvailableSlots,
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


                    val apiResponse = Gson().fromJson(result, ShiftResponse::class.java)

                    _slotList.value = apiResponse.responseValue


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
