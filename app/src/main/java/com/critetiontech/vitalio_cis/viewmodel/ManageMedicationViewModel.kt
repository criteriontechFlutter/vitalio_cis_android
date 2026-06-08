package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.model.MedicineIntakeByDateResponse
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ManageMedicationViewModel @Inject constructor() : ViewModel() {

    private val _loggedMedicines = MutableStateFlow<List<LoggedMedicine>>(emptyList())
    val loggedMedicines: StateFlow<List<LoggedMedicine>> = _loggedMedicines

    private val _allMedicines = MutableStateFlow<List<AllMedicine>>(emptyList())
    val allMedicines: StateFlow<List<AllMedicine>> = _allMedicines

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    fun fetchByDate(context: Context, givenDate: String) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val patient = PrefsManager(context).getPatient() ?: return@launch
                val queryParams = mapOf(
                    "pid" to patient.pid,
                    "givenDate" to givenDate,
                    "clientId" to patient.clientId
                )
                val response = ApiHelper().callApi(
                    context,
                    ApiEndPointCorporateModule().fetchPatientMedicineIntakeByDate,
                    showNoConnectionToast = false
                ) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = queryParams)
                }
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (!body.isNullOrEmpty()) {
                        val parsed = Gson().fromJson(body, MedicineIntakeByDateResponse::class.java)
                        if (parsed.status == 1) {
                            _loggedMedicines.value = parsed.responseValue.loggedMedicines
                            _allMedicines.value = parsed.responseValue.allMedicines
                        } else {
                            _loggedMedicines.value = emptyList()
                            _allMedicines.value = emptyList()
                        }
                    }
                } else {
                    Log.e("ManageMedicationVM", "fetchByDate error: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("ManageMedicationVM", "fetchByDate error: ${e.message}", e)
            } finally {
                _loading.value = false
            }
        }
    }
}