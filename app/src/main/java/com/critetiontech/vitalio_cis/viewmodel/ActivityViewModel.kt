package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ActivityRepository
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.utils.Patient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Add Activity tab + Activity History. Everything the server needs comes from the logged-in
 * patient; with no patient nothing is sent (there is no fallback UHID/clientId).
 */
class ActivityViewModel(
    private val repository: ActivityRepository = AppDependencies.activityRepository,
    private val currentPatient: () -> Patient? = { AppDependencies.prefs.getPatient() },
    private val now: () -> Date = { Date() },
) : ViewModel() {

    // ── Activity master ──
    private val _allActivities = MutableStateFlow<List<ActivityModel>>(emptyList())
    val allActivities: StateFlow<List<ActivityModel>> = _allActivities

    private val _isLoadingMaster = MutableStateFlow(false)
    val isLoadingMaster: StateFlow<Boolean> = _isLoadingMaster

    /** Master list failed and there is nothing to show: the UI offers a retry. */
    private val _masterLoadFailed = MutableStateFlow(false)
    val masterLoadFailed: StateFlow<Boolean> = _masterLoadFailed

    // ── Today's logged activities ──
    private val _recentActivityList = MutableStateFlow<List<PatientActivityItem>>(emptyList())
    val recentActivityList: StateFlow<List<PatientActivityItem>> = _recentActivityList

    private val _isLoadingHistory = MutableStateFlow(false)
    val isLoadingHistory: StateFlow<Boolean> = _isLoadingHistory

    // ── Saving ──
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    /** One-shot user message (string resource); the UI shows it and calls [clearMessage]. */
    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message

    // ── Sheet form ──
    var selectedActivity by mutableStateOf("")
        private set
    var selectedActivityId by mutableStateOf<Int?>(null)
        private set
    var startTime by mutableStateOf(now())
        private set
    var endTime by mutableStateOf(now())
        private set

    var searchText by mutableStateOf("")
        private set

    fun onSearchChange(v: String) { searchText = v }

    /**
     * Name to show for a logged row. FetchPatientActivity returns the right activityId but a wrong
     * activityName (saved Biking/id 1, got back "Biathlon"), so the master list wins when loaded.
     */
    fun displayName(item: PatientActivityItem): String =
        _allActivities.value.firstOrNull { it.id == item.activityId }?.activityName ?: item.activityName

    /** Minutes between start and end; 0 or less means the times are invalid. */
    val durationMinutes: Long get() = (endTime.time - startTime.time) / 60_000

    val isTimeRangeValid: Boolean get() = durationMinutes > 0

    /**
     * Selects an activity and resets the times to the last 30 minutes (never before today's
     * midnight, because the server stores only a time of day against today's date).
     */
    fun openActivity(name: String, id: Int) {
        selectedActivity = name
        selectedActivityId = id
        val end = Calendar.getInstance().apply {
            time = now(); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val midnight = (end.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        }
        val start = (end.clone() as Calendar).apply { add(Calendar.MINUTE, -30) }
        endTime = end.time
        startTime = if (start.before(midnight)) midnight.time else start.time
    }

    fun clearSelection() {
        selectedActivity = ""
        selectedActivityId = null
    }

    fun updateStartTime(hour: Int, minute: Int) { startTime = todayAt(hour, minute) }
    fun updateEndTime(hour: Int, minute: Int) { endTime = todayAt(hour, minute) }

    private fun todayAt(hour: Int, minute: Int): Date = Calendar.getInstance().apply {
        time = now()
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.time

    fun resetSaveSuccess() { _saveSuccess.value = false }
    fun clearMessage() { _message.value = null }

    private fun show(@StringRes res: Int) { _message.value = res }

    // ── API calls ──

    fun getAllActivities() {
        viewModelScope.launch {
            _isLoadingMaster.value = true
            when (val r = repository.fetchActivityMaster()) {
                is DomainResult.Success -> {
                    _allActivities.value = r.data
                    _masterLoadFailed.value = r.data.isEmpty()
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "getAllActivities: ${r.exception.message}")
                    // Keep a list we already have; only a blank screen needs the retry state.
                    _masterLoadFailed.value = _allActivities.value.isEmpty()
                }
            }
            _isLoadingMaster.value = false
        }
    }

    // ── History date ──
    private fun today(): LocalDate = now().toInstant().atZone(ZoneId.systemDefault()).toLocalDate()

    private val _historyDate = MutableStateFlow(today())
    /** Day shown in Activity History; never in the future. */
    val historyDate: StateFlow<LocalDate> = _historyDate

    val isHistoryToday: Boolean get() = _historyDate.value == today()

    fun setHistoryDate(date: LocalDate) {
        val clamped = if (date.isAfter(today())) today() else date
        if (clamped == _historyDate.value) return
        _historyDate.value = clamped
        _recentActivityList.value = emptyList() // don't show the previous day's rows under the new date
        fetchPatientActivity(clamped)
    }

    fun shiftHistoryDate(days: Long) = setHistoryDate(_historyDate.value.plusDays(days))

    fun loadHistory() = fetchPatientActivity(_historyDate.value)

    private var fetchJob: Job? = null

    fun fetchPatientActivity(date: LocalDate = today()) {
        val patient = currentPatient() ?: run { _recentActivityList.value = emptyList(); return }
        // A newer request (e.g. the user tapped to another day) replaces any in flight.
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            _isLoadingHistory.value = true
            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            when (val r = repository.fetchPatientActivity(patient.uhId, dateStr, patient.clientId)) {
                // An empty answer is real: the last activity of the day may just have been deleted.
                is DomainResult.Success -> _recentActivityList.value = r.data
                is DomainResult.Error -> Log.e(TAG, "fetchPatientActivity: ${r.exception.message}")
            }
            _isLoadingHistory.value = false
        }
    }

    fun insertActivity() {
        if (_isSaving.value) return
        val patient = currentPatient() ?: return show(R.string.track_activity_login_required)
        val activityId = selectedActivityId ?: return show(R.string.track_activity_select_first)
        if (!isTimeRangeValid) return show(R.string.track_activity_end_after_start)

        val request = AddPatientActivityRequest(
            uhid = patient.uhId,
            activityId = activityId,
            timeFrom = SimpleDateFormat("HH:mm:ss", Locale.US).format(startTime),
            totalTimeInMinutes = durationMinutes.toString(),
            clientID = patient.clientId,
        )
        viewModelScope.launch {
            _isSaving.value = true
            when (val r = repository.addPatientActivity(request)) {
                is DomainResult.Success -> when (r.data) {
                    ActivitySaveResult.SAVED -> {
                        _saveSuccess.value = true
                        show(R.string.track_activity_saved)
                        clearSelection()
                        fetchPatientActivity()
                    }
                    ActivitySaveResult.ALREADY_EXISTS -> show(R.string.track_activity_already_logged)
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "insertActivity: ${r.exception.message}")
                    show(R.string.track_activity_save_failed)
                }
            }
            _isSaving.value = false
        }
    }

    /** Removes the row right away and puts it back if the server refuses. */
    fun removeActivity(item: PatientActivityItem) {
        val patient = currentPatient() ?: return show(R.string.track_activity_login_required)
        if (item.serverId <= 0) return show(R.string.track_activity_delete_failed)
        val before = _recentActivityList.value
        _recentActivityList.value = before.filterNot { it.serverId == item.serverId }
        viewModelScope.launch {
            when (val r = repository.removePatientActivity(item.serverId, patient.clientId, userId = 0)) {
                is DomainResult.Success -> Unit
                is DomainResult.Error -> {
                    Log.e(TAG, "removeActivity: ${r.exception.message}")
                    _recentActivityList.value = before
                    show(R.string.track_activity_delete_failed)
                }
            }
        }
    }

    private companion object { const val TAG = "ActivityVM" }
}
