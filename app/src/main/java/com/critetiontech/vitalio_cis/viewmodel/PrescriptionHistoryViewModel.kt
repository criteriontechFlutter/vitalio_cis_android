package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.utils.AppGson

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.config.ApiConfig
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.InsertMedicineRequest
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.ApiErrorReporter
import com.critetiontech.ctvitalio.utils.MyApplication
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

// ─── PatientMedicalSummary (prescription list) ────────────────────────────────

data class MedicalSummaryItem(
    @SerializedName("pmID") val pmId: Int = 0,
    val visitDate: String? = null,
    val crNo: String? = null,
    val doctorId: Int = 0,
    val departmentId: Int = 0,
    val diagnosis: String? = null,
    val ipdPatient: String? = null,
    val doctorName: String? = null,
    val departmentName: String? = null,
)

data class MedicalSummaryResponse(
    val status: Int = 0,
    val message: String? = null,
    val responseValue: List<MedicalSummaryItem>? = null,
)

// ─── PatientMedicalDetails (one prescription) ─────────────────────────────────

data class MedicalDetailVital(
    val vmId: Int = 0,
    val vitalName: String? = null,
    val vmValue: Double? = null,
    val unit: String? = null,
    val vitalDateTime: String? = null,
)

data class MedicalDetailSymptom(
    val pmID: Int = 0,
    val pdmId: Int = 0,
    val detailsName: String? = null,
    val symptoms: String? = null,
)

data class MedicalDetailMedicine(
    val pmId: Int = 0,
    val drugId: Int = 0,
    val drugName: String? = null,
    val dosageForm: String? = null,
    val dosageStrength: String? = null,
    val doseUnit: String? = null,
    val doseFrequency: String? = null,
    val duration: String? = null,
    val remark: String? = null,
    val medicineStatus: String? = null,
    val isAntibiotic: Boolean = false,
)

data class MedicalDetailInvestigation(
    val id: Int = 0,
    val itemId: Int = 0,
    val isDone: Boolean = false,
    val investigationName: String? = null,
)

data class MedicalDetailAdvice(val otherAdvice: String? = null)
data class MedicalDetailAllergy(val allergies: String? = null)

data class MedicalDetailsValue(
    val vital: List<MedicalDetailVital>? = null,
    val signAndSymptoms: List<MedicalDetailSymptom>? = null,
    val prescription: List<MedicalDetailMedicine>? = null,
    val allergies: List<MedicalDetailAllergy>? = null,
    val otherAdvice: List<MedicalDetailAdvice>? = null,
    val investigation: List<MedicalDetailInvestigation>? = null,
) {
    /** pdmId 4 = "Patient Diagnosis" */
    val diagnosis: String?
        get() = signAndSymptoms?.firstOrNull { it.pdmId == 4 }?.symptoms?.takeIf { it.isNotBlank() }

    val notes: String?
        get() = otherAdvice?.mapNotNull { it.otherAdvice?.trim()?.takeIf { s -> s.isNotEmpty() } }
            ?.joinToString("\n")?.takeIf { it.isNotEmpty() }
}

data class MedicalDetailsResponse(
    val status: Int = 0,
    val message: String? = null,
    val responseValue: MedicalDetailsValue? = null,
)

sealed interface RxUiState<out T> {
    data object Loading : RxUiState<Nothing>
    data class Error(val message: String) : RxUiState<Nothing>
    data class Success<T>(val data: T) : RxUiState<T>
}

private data class PatientKeys(val uhid: String, val clientId: String, val userId: String)

/** Null when there is no usable login: no silent fallback to another hospital's clientId. */
private fun patientKeys(): PatientKeys? {
    val patient = AppDependencies.prefs.getPatient() ?: return null
    if (patient.uhId.isBlank() || patient.pid <= 0 || patient.clientId <= 0) return null
    return PatientKeys(uhid = patient.uhId, clientId = patient.clientId.toString(), userId = patient.pid.toString())
}

private fun str(id: Int, vararg args: Any): String = MyApplication.appContext.getString(id, *args)

/** The backend answers "No record found" with status 0 (often HTTP 400) when a list is just empty. */
private fun isNoRecord(text: String?): Boolean = runCatching {
    val v = JsonParser.parseString(text.orEmpty()).asJsonObject.get("responseValue")
    v != null && v.isJsonPrimitive && v.asString.contains("no record", ignoreCase = true)
}.getOrDefault(false)

private class ServerError(val code: Int) : Exception("HTTP $code")

/**
 * GET [path] on the 4082 backend and return the body, served from the saved copy when the network
 * or server fails, so a patient can still read their prescriptions offline. Empty lists are cached
 * as such (an empty answer replaces an older non-empty copy).
 */
internal suspend fun fetchCached(cacheKey: String, path: String, params: Map<String, Any>, emptyBody: String): String? =
    AppDependencies.prefs.getData(key = cacheKey) {
        try {
            val api = RetrofitInstance.createApiService(ApiConfig.MODULE_4082)
            val response = api.dynamicGet(url = "${ApiConfig.MODULE_4082}$path", params = params)
            val text = (if (response.isSuccessful) response.body() else response.errorBody())?.string()
            when {
                isNoRecord(text) -> emptyBody
                response.isSuccessful && !text.isNullOrBlank() -> text
                else -> {
                    ApiErrorReporter.httpError(path, response.code())
                    throw ServerError(response.code())
                }
            }
        } catch (e: Exception) {
            if (e !is ServerError) ApiErrorReporter.exception(path, e)
            throw e
        }
    }

// ─── List ViewModel ───────────────────────────────────────────────────────────

class PrescriptionHistoryViewModel : ViewModel() {

    private val _state = MutableStateFlow<RxUiState<List<MedicalSummaryItem>>>(RxUiState.Loading)
    val state: StateFlow<RxUiState<List<MedicalSummaryItem>>> = _state.asStateFlow()

    /** True during a pull-to-refresh, while the current list stays on screen. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = RxUiState.Loading
            _state.value = fetch()
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val result = fetch()
            // Keep what's on screen if the refresh failed and there was something to show
            if (result is RxUiState.Success || _state.value !is RxUiState.Success) _state.value = result
            _refreshing.value = false
        }
    }

    private suspend fun fetch(): RxUiState<List<MedicalSummaryItem>> {
        val keys = patientKeys() ?: return RxUiState.Error(str(R.string.report_error_patient_not_found))
        return try {
            val body = fetchCached(
                cacheKey = "rx_summary_${keys.userId}",
                path = "api/PatientHistoryByUHID/PatientMedicalSummary",
                params = mapOf("uhid" to keys.uhid, "clientId" to keys.clientId, "userId" to keys.userId),
                emptyBody = """{"status":1,"responseValue":[]}""",
            ) ?: return RxUiState.Error(str(R.string.report_something_went_wrong))
            val parsed = AppGson.fromJson(body, MedicalSummaryResponse::class.java)
            RxUiState.Success(parsed?.responseValue.orEmpty().sortedByDescending { it.visitDate.orEmpty() })
        } catch (e: Exception) {
            Log.e("PrescriptionHistoryVM", "Summary failed", e)
            RxUiState.Error(str(R.string.report_something_went_wrong))
        }
    }
}

// ─── Detail ViewModel ─────────────────────────────────────────────────────────

sealed interface RxReminderState {
    data object Idle : RxReminderState
    data object Saving : RxReminderState
    data class Added(val count: Int) : RxReminderState
    data object Failed : RxReminderState
}

class PrescriptionDetailViewModel : ViewModel() {

    private val _state = MutableStateFlow<RxUiState<MedicalDetailsValue>>(RxUiState.Loading)
    val state: StateFlow<RxUiState<MedicalDetailsValue>> = _state.asStateFlow()

    private val _reminders = MutableStateFlow<RxReminderState>(RxReminderState.Idle)
    val reminders: StateFlow<RxReminderState> = _reminders.asStateFlow()

    private var loadedPmId: Int? = null

    fun load(pmId: Int, force: Boolean = false) {
        if (!force && loadedPmId == pmId && _state.value is RxUiState.Success) return
        loadedPmId = pmId
        viewModelScope.launch {
            _state.value = RxUiState.Loading
            val keys = patientKeys()
            if (keys == null) {
                _state.value = RxUiState.Error(str(R.string.report_error_patient_not_found))
                return@launch
            }
            _state.value = try {
                val body = fetchCached(
                    cacheKey = "rx_detail_$pmId",
                    path = "api/PatientHistoryByUHID/PatientMedicalDetails",
                    params = mapOf("pmId" to pmId, "clientId" to keys.clientId, "userId" to keys.userId),
                    emptyBody = """{"status":1,"responseValue":{}}""",
                )
                if (body == null) RxUiState.Error(str(R.string.report_something_went_wrong))
                else RxUiState.Success(AppGson.fromJson(body, MedicalDetailsResponse::class.java)?.responseValue ?: MedicalDetailsValue())
            } catch (e: Exception) {
                Log.e("PrescriptionDetailVM", "Details failed", e)
                RxUiState.Error(str(R.string.report_something_went_wrong))
            }
        }
    }

    /**
     * Puts the prescribed medicines into the patient's medicine schedule (the same API the
     * Upload Prescription flow uses), so reminders ring for them. Starts today and ends after each
     * medicine's duration.
     */
    fun addToReminders(medicines: List<MedicalDetailMedicine>) {
        if (_reminders.value == RxReminderState.Saving) return
        val patient = AppDependencies.prefs.getPatient()
        val usable = medicines.filter { it.drugId > 0 }
        if (patient == null || patient.pid <= 0 || patient.clientId <= 0 || usable.isEmpty()) {
            _reminders.value = RxReminderState.Failed
            return
        }
        viewModelScope.launch {
            _reminders.value = RxReminderState.Saving
            val today = LocalDate.now()
            var added = 0
            for (med in usable) {
                val request = InsertMedicineRequest(
                    pid = "${patient.pid}",
                    medicineId = med.drugId,
                    dosageType = med.dosageForm?.takeIf { it.isNotBlank() } ?: "Tablet",
                    dosageStrength = med.dosageStrength?.trim()?.toDoubleOrNull()?.toInt() ?: 0,
                    frequency = "Every day",
                    frequencyValue = "0",
                    weekDays = "",
                    monthDates = "",
                    instructions = med.remark?.trim()?.takeIf { it.isNotEmpty() && it != "0" } ?: "As Prescribed",
                    timeSlotsJson = timeSlotsFor(med.doseFrequency),
                    startdate = today.toString(),
                    enddate = today.plusDays(durationDays(med.duration)).toString(),
                    clientId = "${patient.clientId}",
                )
                when (val r = AppDependencies.insertMedicine()(request)) {
                    is DomainResult.Success -> if (r.data.status == 1) added++
                        else Log.w("PrescriptionDetailVM", "Insert ${med.drugName}: ${r.data.message}")
                    is DomainResult.Error -> Log.e("PrescriptionDetailVM", "Insert ${med.drugName} failed", r.exception)
                }
            }
            if (added > 0) {
                Haptics.success()
                _reminders.value = RxReminderState.Added(added)
                // Ring for the new doses without waiting for the daily resync
                runCatching { MedicineReminderScheduler.sync(MyApplication.appContext) }
            } else {
                Haptics.error()
                _reminders.value = RxReminderState.Failed
            }
        }
    }

    fun reminderMessageShown() {
        if (_reminders.value is RxReminderState.Failed) _reminders.value = RxReminderState.Idle
    }

    companion object {
        /** "Once a Day (24 Hourly)", "Twice a Day (12 Hourly)", … → the schedule's time slots. */
        fun timeSlotsFor(doseFrequency: String?): String {
            val f = doseFrequency.orEmpty().lowercase()
            val slots = when {
                "four" in f || "6 hourly" in f || "qid" in f -> listOf("08:00", "12:00", "16:00", "21:00")
                "thrice" in f || "three" in f || "8 hourly" in f || "tds" in f || "tid" in f -> listOf("08:00", "14:00", "21:00")
                "twice" in f || "12 hourly" in f || "bd" == f.trim() || "bid" in f -> listOf("08:00", "21:00")
                "night" in f || "bedtime" in f || "hs" == f.trim() -> listOf("21:00")
                else -> listOf("08:00")
            }
            return slots.joinToString(prefix = "[", postfix = "]") { """{"timeSlot":"$it"}""" }
        }

        /** The API sends duration in days ("2", "10"); fall back to a week when it's missing. */
        fun durationDays(duration: String?): Long =
            duration?.trim()?.toLongOrNull()?.takeIf { it > 0 } ?: 7L
    }
}
