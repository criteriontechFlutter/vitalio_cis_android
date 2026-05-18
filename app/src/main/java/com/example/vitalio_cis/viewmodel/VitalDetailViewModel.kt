package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.model.Vital
import com.example.vitalio_cis.model.VitalApiResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class VitalDetailViewModel @Inject constructor() : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _addLoading = MutableStateFlow(false)
    val addLoading: StateFlow<Boolean> = _addLoading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

    fun fetchLastVital(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            val prefsCache = PrefsManager(context)
            try {
                val queryParams = mapOf(
                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "userId" to prefsCache.getPatient()?.pid.toString(),
                )
                val result: String? = prefsCache.getData(
                    key = ApiEndPointCorporateModule().fetchLastVital,
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchLastVital,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                    }
                    if (response.isSuccessful) {
                        val bodyString = response.body()?.string()
                        Log.d("VitalDetailViewModel", "fetchLastVital: $bodyString")
                        bodyString
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }
                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, VitalApiResponse::class.java)
                    _vitalList.value = apiResponse.responseValue.lastVital
                }
            } catch (e: Exception) {
                Log.e("VitalDetailViewModel", "fetchLastVital error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }

    fun addVital(
        context: Context,
        vmValueBPSys: String = "",
        vmValueBPDias: String = "",
        vmValueSPO2: String = "",
        vmValueRespiratoryRate: String = "",
        vmValueHeartRate: String = "",
        vmValuePulse: String = "",
        vmValueRbs: String = "",
        vmValueTemperature: String = "",
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _addLoading.value = true
            val prefsCache = PrefsManager(context)
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            try {
                val body = mapOf(
                    "vmValueBPSys" to vmValueBPSys,
                    "vmValueBPDias" to vmValueBPDias,
                    "vmValueSPO2" to vmValueSPO2,
                    "vmValueRespiratoryRate" to vmValueRespiratoryRate,
                    "vmValueHeartRate" to vmValueHeartRate,
                    "vmValuePulse" to vmValuePulse,
                    "vmValueRbs" to vmValueRbs,
                    "vmValueTemperature" to vmValueTemperature,
                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "userId" to prefsCache.getPatient()?.pid.toString(),
                    "vitalDate" to currentDate,
                    "vitalTime" to currentTime,
                    "clientId" to prefsCache.getPatient()?.clientId.toString(),
                    "isFromPatient" to "true",
                    "isFromMachine" to "0",
                    "positionId" to "0"
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().addVital,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("VitalDetailViewModel", "addVital success")
                    fetchLastVital(context)
                    onSuccess()
                } else {
                    throw Exception("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("VitalDetailViewModel", "addVital error: ${e.message}", e)
            } finally {
                _addLoading.value = false
            }
        }
    }
}
