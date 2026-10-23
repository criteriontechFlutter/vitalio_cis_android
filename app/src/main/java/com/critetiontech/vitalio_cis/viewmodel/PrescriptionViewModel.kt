package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.config.ApiConfig

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.di.AppDependencies
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ─── Data Models ───────────────────────────────────────────────────────────────

data class PrescriptionDetail(
    val id: Int = 0,
    val pmId: Int = 0,
    val medicineId: Int = 0,
    val drugName: String = "",
    val dosageForm: String = "",
    val dosageStrength: String = "",
    val doseUnit: String = "",
    val doseFrequency: String = "",
    val duration: String = "",
    val deptId: Int = 0,
    val userId: Int = 0,
    val rationalId: Int = 0,
    val createdDate: String = "",
    val remark: String = "",
    val medicineStatus: String = "",
    val stopBy: String? = null,
    val stopDate: String? = null
)

data class InvestigationDetail(
    val id: Int = 0,
    val pmId: Int = 0,
    val itemId: Int = 0,
    val doctorId: Int = 0,
    val isDone: Boolean = false
)

data class PatientVitalDetail(
    val vmId: Int = 0,
    val vmValue: Double = 0.0,
    val vitalDateTime: String = ""
)

data class ProblemDetail(
    val id: Int = 0,
    val problemId: Int = 0,
    val problemName: String = "",
    val pdmId: Int = 0,
    val doctorId: Int = 0,
    val userId: Int = 0,
    val isProvisionalDiagnosis: Boolean = false
)

data class VitalFrequencyDetail(
    val id: Int = 0,
    val pid: Int = 0,
    val pmId: Int = 0,
    val parameterId: Int = 0,
    val parameterTypeId: Int = 0,
    val frequencyTypeId: Int = 0,
    val clientId: Int = 0,
    val userId: Int = 0,
    val createdDate: String = "",
    val status: Boolean = false,
    val quantity: Double? = null
)

data class PatientOPDHistoryValue(
    val allPrescription: List<PrescriptionDetail>? = null,
    val patientInvestigation: List<InvestigationDetail>? = null,
    val patientVitals: List<PatientVitalDetail>? = null,
    val patientComplainHistory: List<ProblemDetail>? = null,
    @SerializedName("patientDaignosis")
    val patientDiagnosis: List<ProblemDetail>? = null,
    val vitalFrequency: List<VitalFrequencyDetail>? = null
)

data class PatientOPDHistoryResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: PatientOPDHistoryValue? = null
)

// ─── ViewModel ─────────────────────────────────────────────────────────────────

class PrescriptionViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs

    private val _prescriptions = MutableStateFlow<List<PrescriptionDetail>>(emptyList())
    val prescriptions: StateFlow<List<PrescriptionDetail>> = _prescriptions

    private val _vitals = MutableStateFlow<List<PatientVitalDetail>>(emptyList())
    val vitals: StateFlow<List<PatientVitalDetail>> = _vitals

    private val _investigations = MutableStateFlow<List<InvestigationDetail>>(emptyList())
    val investigations: StateFlow<List<InvestigationDetail>> = _investigations

    private val _complaints = MutableStateFlow<List<ProblemDetail>>(emptyList())
    val complaints: StateFlow<List<ProblemDetail>> = _complaints

    private val _diagnoses = MutableStateFlow<List<ProblemDetail>>(emptyList())
    val diagnoses: StateFlow<List<ProblemDetail>> = _diagnoses

    private val _vitalFrequencies = MutableStateFlow<List<VitalFrequencyDetail>>(emptyList())
    val vitalFrequencies: StateFlow<List<VitalFrequencyDetail>> = _vitalFrequencies

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun fetchOPDHistory() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val patient = prefs.getPatient()
                if (patient == null) {
                    _errorMessage.value = "Patient data not found. Please login again."
                    _isLoading.value = false
                    return@launch
                }

                val uhid = patient.uhId.ifEmpty { "UHID2604500016" }
                val clientId = patient.clientId.takeIf { it != 0 }?.toString() ?: "45"
                val userId = patient.pid.takeIf { it != 0 }?.toString() ?: "323"

                // Step 1: Resolve crNo — use stored value, else fetch fresh from patient details API
                val crno = patient.crNo?.takeIf { it.isNotBlank() }
                    ?: fetchCrNoFromApi(uhid, clientId)

                Log.d("PrescriptionVM", "Using crNo=$crno uhid=$uhid clientId=$clientId userId=$userId")

                val params = mapOf<String, Any>(
                    "uhid" to uhid,
                    "clientId" to clientId,
                    "userId" to userId,
                    "crno" to (crno ?: "")
                )

                val apiService = RetrofitInstance.createApiService(
                    baseUrl = ApiConfig.MODULE_4082
                )

                val response = apiService.dynamicGet(
                    url = "${ApiConfig.MODULE_4082}api/PatientPrescription/PatientOPDAllHistory",
                    params = params
                )

                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()
                    Log.d("PrescriptionVM", "Response: $bodyString")

                    val parsed = Gson().fromJson(bodyString, PatientOPDHistoryResponse::class.java)
                    val value = parsed?.responseValue

                    _prescriptions.value = value?.allPrescription ?: emptyList()
                    _vitals.value = value?.patientVitals ?: emptyList()
                    _investigations.value = value?.patientInvestigation ?: emptyList()
                    _complaints.value = value?.patientComplainHistory ?: emptyList()
                    _diagnoses.value = value?.patientDiagnosis ?: emptyList()
                    _vitalFrequencies.value = value?.vitalFrequency ?: emptyList()
                } else {
                    val errBody = response.body()?.string() ?: ""
                    _errorMessage.value = "Server error ${response.code()}. $errBody"
                    Log.e("PrescriptionVM", "Error ${response.code()}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.localizedMessage ?: "Something went wrong"
                Log.e("PrescriptionVM", "Exception: ${e.message}", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Fetches crNo from GetPatientDetailsByMobileNo if not available in stored prefs.
     * Also saves the crNo back into prefs so next time it's available instantly.
     */
    private suspend fun fetchCrNoFromApi(uhid: String, clientId: String): String? {
        return try {
            val apiService = RetrofitInstance.createApiService(ApiConfig.MODULE_4082)
            val response = apiService.dynamicGet(
                url = "${ApiConfig.MODULE_4082}api/PatientRegistration/GetPatientDetailsByMobileNo",
                params = mapOf("uhId" to uhid, "clientId" to clientId)
            )
            if (response.isSuccessful) {
                val body = response.body()?.string() ?: return null
                // Parse crNo from the response JSON generically
                val jsonObj = com.google.gson.JsonParser.parseString(body).asJsonObject
                val responseValue = jsonObj.getAsJsonArray("responseValue")
                    ?.firstOrNull()?.asJsonObject
                val crNo = responseValue?.get("crNo")?.asString?.takeIf { it.isNotBlank() }
                Log.d("PrescriptionVM", "Fetched crNo from API: $crNo")

                // Save it back to prefs if found
                if (!crNo.isNullOrBlank()) {
                    val currentPatient = prefs.getPatient()
                    if (currentPatient != null) {
                        prefs.savePatient(currentPatient.copy(crNo = crNo))
                    }
                }
                crNo
            } else null
        } catch (e: Exception) {
            Log.w("PrescriptionVM", "Could not fetch crNo: ${e.message}")
            null
        }
    }
}
