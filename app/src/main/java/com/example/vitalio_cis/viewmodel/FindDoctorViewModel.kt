package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.State
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
import com.example.vitalio_cis.model.Problem
import com.example.vitalio_cis.model.SymptomApiResponse
import com.example.vitalio_cis.model.SymptomItem
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
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
    val  doctorList: StateFlow<List<Doctor>> = _doctorList



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
