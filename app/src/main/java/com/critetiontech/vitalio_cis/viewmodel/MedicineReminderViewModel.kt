package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.model.MedicineIntakeResponse
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.http.QueryMap
import java.time.LocalDate
import javax.inject.Inject

class MedicineReminderViewModel @Inject constructor() : ViewModel() {

    private val _periods = MutableStateFlow<List<MedicinePeriod>>(emptyList())
    val periods: StateFlow<List<MedicinePeriod>> = _periods

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _intakeSuccess = MutableStateFlow<Int?>(null)
    val intakeSuccess: StateFlow<Int?> = _intakeSuccess

    fun markIntake(context: Context, id: Int, scheduledDateTime: String) {
        viewModelScope.launch {
            try {
                val body = mapOf(
                    "id" to id,
                    "ScheduledDateTime" to scheduledDateTime
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().insertMedicineIntake,
                    showNoConnectionDialog = false
                ) { url ->
                    ApiClients.module4082.queryRawPostApi(url = url, params = body, body =body )
                }
                if (response.isSuccessful) {
                    _intakeSuccess.value = id
                    fetchMedicineIntake(context)
                } else {
                    Log.e("MedicineReminderVM", "markIntake error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("MedicineReminderVM", "markIntake error: ${e.message}", e)
            }
        }
    }

    fun fetchMedicineIntake(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val patient = PrefsManager(context).getPatient()
                val pid = patient?.pid ?: return@launch
                val clientId = patient.clientId
                val givenDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    LocalDate.now().toString()
                } else {
                    TODO("VERSION.SDK_INT < O")
                }

                val endpoint = ApiEndPointCorporateModule().fetchPatientMedicineIntake
                val queryParams = mapOf(
                    "pid" to pid,
                    "givenDate" to givenDate,
                    "clientId" to clientId
                )

                val result: String? = PrefsManager(context).getData(
                    key = "$endpoint?pid=$pid&givenDate=$givenDate",
                    shouldSave = true
                ) {
                    val response = ApiHelper().callApi(
                        context,
                        endpoint,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(
                            url = url,
                            params = queryParams
                        )
                    }
                    if (response.isSuccessful) {
                        response.body()?.string()
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                if (!result.isNullOrEmpty()) {
                    val apiResponse = Gson().fromJson(result, MedicineIntakeResponse::class.java)
                    if (apiResponse.status == 1) {
                        _periods.value = apiResponse.responseValue
                    }
                }
            } catch (e: Exception) {
                Log.e("MedicineReminderVM", "Error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }
}
