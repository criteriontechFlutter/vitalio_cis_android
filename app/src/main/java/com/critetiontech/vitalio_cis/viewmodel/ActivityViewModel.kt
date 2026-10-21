package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.model.ActivityMasterResponse
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.model.PatientActivityResponse
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ActivityViewModel : ViewModel() {

    private val prefs = PrefsManager(MyApplication.appContext)
    private val endpoints = ApiEndPointCorporateModule()
    private val context = MyApplication.appContext

    // ── State ──
    private val _allActivities = MutableStateFlow<List<ActivityModel>>(emptyList())
    val allActivities: StateFlow<List<ActivityModel>> = _allActivities

    private val _recentActivityList = MutableStateFlow<List<PatientActivityItem>>(emptyList())
    val recentActivityList: StateFlow<List<PatientActivityItem>> = _recentActivityList

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    var selectedActivity by mutableStateOf("");
    var selectedActivityId by mutableStateOf("");
    var startTime by mutableStateOf(Date());
    var endTime by mutableStateOf(Date());

    var searchText by mutableStateOf("");
    fun onSearchChange(v: String) { searchText = v }

    fun selectActivity(name: String, id: String) {
        selectedActivity = name
        selectedActivityId = id
    }

    fun updateStartTime(d: Date) {
        startTime = if (d.after(endTime)) endTime else d
    }
    fun updateEndTime(d: Date) {
        endTime = if (d.before(startTime)) startTime else d
    }

    fun resetSaveSuccess() { _saveSuccess.value = false }

    /** Formatted duration string like "20 mins" or "1 hrs 10 mins" */
    val formattedDuration: String
        get() {
            val diff = ((endTime.time - startTime.time) / 60_000).coerceAtLeast(0)
            val hrs = diff / 60
            val mins = diff % 60
            return when {
                hrs == 0L -> "$mins mins"
                mins == 0L -> "$hrs hrs"
                else -> "$hrs hrs $mins mins"
            }
        }

    private val defaultActivities = listOf(
        ActivityModel(1, "Hiking", "General"),
        ActivityModel(2, "Biking", "General"),
        ActivityModel(3, "Aerobics", "General"),
        ActivityModel(4, "Archery", "General"),
        ActivityModel(5, "Baseball", "General"),
        ActivityModel(6, "Basketball", "General"),
        ActivityModel(7, "Biathlon", "General"),
        ActivityModel(8, "Handbiking", "General"),
        ActivityModel(9, "Mountain Biking", "General"),
    )

    // ── API Calls ──

    fun getAllActivities() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = ApiHelper().callApi(context, endpoints.getAllActivityMaster, showNoConnectionToast = false) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = emptyMap())
                }
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (!body.isNullOrEmpty()) {
                        val parsed = Gson().fromJson(body, ActivityMasterResponse::class.java)
                        if (parsed.responseValue.isNotEmpty()) {
                            _allActivities.value = parsed.responseValue
                        } else if (_allActivities.value.isEmpty()) {
                            _allActivities.value = defaultActivities
                        }
                    } else if (_allActivities.value.isEmpty()) {
                        _allActivities.value = defaultActivities
                    }
                } else if (_allActivities.value.isEmpty()) {
                    _allActivities.value = defaultActivities
                }
            } catch (e: Exception) {
                Log.e("ActivityVM", "getAllActivities error: ${e.message}")
                if (_allActivities.value.isEmpty()) _allActivities.value = defaultActivities
            }
            _isLoading.value = false
        }
    }

    fun fetchPatientActivity(date: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val patient = prefs.getPatient()
                val uhid = patient?.uhId ?: "UHID00003"
                val clientId = patient?.clientId?.toString() ?: "45"
                val dateStr = date ?: SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                val params = mapOf("uhid" to uhid, "date" to dateStr, "clientId" to clientId)
                val response = ApiHelper().callApi(context, endpoints.fetchPatientActivity, showNoConnectionToast = false) { url ->
                    ApiClients.module4082.dynamicGet(url = url, params = params)
                }
                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    if (!body.isNullOrEmpty()) {
                        val trimmed = body.trim()
                        val items: List<PatientActivityItem> = if (trimmed.startsWith("[")) {
                            // Bare JSON array
                            Gson().fromJson(trimmed, Array<PatientActivityItem>::class.java).toList()
                        } else {
                            // Wrapped response object
                            val parsed = Gson().fromJson(trimmed, PatientActivityResponse::class.java)
                            parsed.responseValue
                        }
                        if (items.isNotEmpty()) {
                            _recentActivityList.value = items
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ActivityVM", "fetchPatientActivity error: ${e.message}")
            }
            _isLoading.value = false
        }
    }

    fun insertActivity() {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val patient = prefs.getPatient()
                val uhid = patient?.uhId ?: "UHID00003"
                val clientId = patient?.clientId ?: 45

                val diffInMinutes = ((endTime.time - startTime.time) / 60_000).coerceAtLeast(0)
                val timeFromStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(startTime)
                val actId = selectedActivityId.toIntOrNull() ?: 2

                val body = mapOf(
                    "uhid" to uhid,
                    "activityId" to actId,
                    "timeFrom" to timeFromStr,
                    "totalTimeInMinutes" to diffInMinutes.toString(),
                    "remark" to "NA",
                    "userId" to 0,
                    "clientID" to clientId
                )

                val response = ApiHelper().callApi(context, endpoints.addPatientActivity, showNoConnectionToast = false) { url ->
                    ApiClients.module4082.dynamicRawPost(url = url, body = body)
                }
                if (response.isSuccessful) {
                    Log.d("ActivityVM", "insertActivity success")
                    _saveSuccess.value = true
                    Haptics.success()
                    // Refresh recent list
                    fetchPatientActivity()
                } else {
                    Log.e("ActivityVM", "insertActivity failed: ${response.code()}")
                    Haptics.error()
                }
            } catch (e: Exception) {
                Log.e("ActivityVM", "insertActivity error: ${e.message}")
            }
            _isSaving.value = false
        }
    }

    fun removeRecentActivity(item: PatientActivityItem) {
        // Use referential identity to remove only the exact item, not all with same serverId
        _recentActivityList.value = _recentActivityList.value.filterNot { it === item }
        if (item.serverId > 0) {
            viewModelScope.launch {
                deleteActivity(item.serverId, prefs.getPatient()?.pid ?: 0)
            }
        }
    }

    private suspend fun deleteActivity(id: Int, pid: Int) {
        try {
            val params = mapOf("id" to id.toString(), "pid" to pid.toString())
            ApiHelper().callApi(context, endpoints.deleteActivityMaster, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicDelete(url = url, params = params)
            }
        } catch (e: Exception) {
            Log.e("ActivityVM", "deleteActivity error: ${e.message}")
        }
    }
}
