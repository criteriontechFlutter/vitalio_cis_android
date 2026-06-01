package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
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
import java.time.LocalDate
import javax.inject.Inject

class MedicineReminderViewModel @Inject constructor() : ViewModel() {

    private val _periods = MutableStateFlow<List<MedicinePeriod>>(emptyList())
    val periods: StateFlow<List<MedicinePeriod>> = _periods

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun fetchMedicineIntake(context: Context) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val pid = 77
                val clientId = 45
                val givenDate = LocalDate.now().toString()

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
