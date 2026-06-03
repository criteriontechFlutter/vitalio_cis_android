package com.critetiontech.vitalio_cis.viewmodel

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
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.model.DoctorResponse
import com.critetiontech.vitalio_cis.model.DoctorResponsedata
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class FindDoctorViewModel @Inject constructor() : ViewModel() {




    var searchText by mutableStateOf("")
        private set

    fun onSearchChange(value: String) {
        searchText = value
    }

    private val _loading = MutableLiveData(false)
    val loading: LiveData<Boolean> = _loading
    private val _doctorList = MutableStateFlow<List<Doctor>>(emptyList())
    val doctorList: StateFlow<List<Doctor>> = _doctorList

    // keyed by assignedUserId → fetched profile
    private val _doctorProfiles = MutableStateFlow<Map<Int, DoctorDetails>>(emptyMap())
    val doctorProfiles: StateFlow<Map<Int, DoctorDetails>> = _doctorProfiles



    fun fetchDoctorsAvalability(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "clientID" to prefsCache.getPatient()?.clientId.toString(),
                    "departmentId"  to "0"
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchDoctorsAvalability,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchDoctorsAvalability,
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
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, DoctorResponse::class.java)
                    _doctorList.value = apiResponse.responseValue
                    fetchDoctorProfiles(context, apiResponse.responseValue, prefsCache)
                }

            } catch (e: Exception) {
                Log.e("FindDoctorVM", "fetchDoctorsAvalability error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

    private fun fetchDoctorProfiles(context: Context, doctors: List<Doctor>, prefsCache: PrefsManager) {
        viewModelScope.launch {
            val results = doctors.map { doctor ->
                async {
                    try {
                        val queryParams = mapOf(
                            "clientID" to prefsCache.getPatient()?.clientId.toString(),
                            "doctorId" to doctor.assignedUserId.toString()
                        )
                        val response = ApiHelper().callApi(
                            context,
                            ApiEndPointCorporateModule().getDoctorProfile,
                            showNoConnectionDialog = false
                        ) { url ->
                            ApiClients.module4084.dynamicGet(url = url, params = queryParams)
                        }
                        if (response.isSuccessful) {
                            val body = response.body()?.string()
                            if (!body.isNullOrEmpty()) {
                                val parsed = Gson().fromJson(body, DoctorResponsedata::class.java)
                                parsed.responseValue.firstOrNull()?.let { doctor.assignedUserId to it }
                            } else null
                        } else null
                    } catch (e: Exception) {
                        Log.e("FindDoctorVM", "profile fetch error for ${doctor.assignedUserId}: ${e.message}")
                        null
                    }
                }
            }.awaitAll()

            _doctorProfiles.value = results.filterNotNull().toMap()
        }
    }
}
