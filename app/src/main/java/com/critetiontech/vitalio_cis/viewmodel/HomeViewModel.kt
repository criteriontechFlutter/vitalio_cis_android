package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.utils.AppGson

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.model.DashboardResponse
import com.critetiontech.ctvitalio.model.UpcomingAppointment
import com.critetiontech.ctvitalio.model.Vital
import com.critetiontech.ctvitalio.reminder.MedicineIntakeEvents
import com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler
import com.critetiontech.ctvitalio.reminder.withLocallyTaken
import com.critetiontech.ctvitalio.utils.MyApplication
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.data.offline.PendingEntries
import kotlinx.coroutines.flow.drop

class HomeViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs
    private val networkMonitor = AppDependencies.networkMonitor
    private val endpoints = ApiEndPointCorporateModule()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
    val vitalList: StateFlow<List<Vital>> = _vitalList

    private val _appointments = MutableStateFlow<List<UpcomingAppointment>>(emptyList())
    val appointments: StateFlow<List<UpcomingAppointment>> = _appointments

    private val _dailyMedicines = MutableStateFlow<List<com.critetiontech.ctvitalio.model.MedicineItem>>(emptyList())
    val dailyMedicines: StateFlow<List<com.critetiontech.ctvitalio.model.MedicineItem>> = _dailyMedicines

    private val _dietOverview = MutableStateFlow<com.critetiontech.ctvitalio.model.DailyDietOverview?>(null)
    val dietOverview: StateFlow<com.critetiontech.ctvitalio.model.DailyDietOverview?> = _dietOverview

    private val _dietOverviewLoading = MutableStateFlow(true)
    val dietOverviewLoading: StateFlow<Boolean> = _dietOverviewLoading

    init {
        // Offline logs uploaded: the dashboard's latest vitals now include them.
        viewModelScope.launch {
            OfflineOutbox.lastSyncedAt.drop(1).collect { fetchDashboardNoCache() }
        }
        // A dose was logged from a medicine alarm; refresh so the card shows it as taken.
        viewModelScope.launch {
            MedicineIntakeEvents.changed.collect { refreshTodayMedicines() }
        }
    }

    /** When the last full load finished; coming back to Home within a minute reuses it. */
    private var lastLoadedAt = 0L

    /**
     * Loads Home. Opening Home again (e.g. back from another screen) within a minute of the last
     * load keeps what's shown, since these APIs take 10-18 s; [force] (pull-to-refresh) always reloads.
     */
    fun fetchDashboard(force: Boolean = false) {
        if (!force && _loading.value) return
        if (!force && System.currentTimeMillis() - lastLoadedAt < 60_000L) return
        viewModelScope.launch {
            _loading.value = true
            // Vitals/appointments and today's medicines come from two slow APIs (~10 s each);
            // load them side by side instead of one after the other.
            kotlinx.coroutines.coroutineScope {
                launch { loadDashboard(cache = true) }
                launch { refreshTodayMedicines(syncReminders = true) }
            }
            lastLoadedAt = System.currentTimeMillis()
            _loading.value = false
        }
    }

    /** Vitals and upcoming appointments. */
    private suspend fun loadDashboard(cache: Boolean) {
        val patient = prefs.getPatient() ?: return
        try {
            val params = mapOf("UHID" to patient.uhId, "clientId" to patient.clientId.toString())
            val response = ApiHelper().callApi(
                MyApplication.appContext,
                endpoints.getPatientDashboardData,
                cacheResponse = cache
            ) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) {
                val body = response.body()?.string()
                val parsed = AppGson.fromJson(body, DashboardResponse::class.java)
                if (parsed.status == 1) {
                    _vitalList.value = PendingEntries.overlayVitals(parsed.responseValue.latestVitals)
                    _appointments.value = parsed.responseValue.upcomingAppointments
                }
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "loadDashboard error: ${e.message}")
        }
    }

    fun fetchDietOverview() {
        viewModelScope.launch {
            _dietOverviewLoading.value = true
            val patient = prefs.getPatient()
            if (patient != null) {
                val result = AppDependencies.fetchDailyDietOverview()(patient.pid, patient.clientId)
                if (result is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                    // One entry per active plan; the app follows the newest.
                    _dietOverview.value = result.data.maxByOrNull { it.dietPlanId }
                }
            }
            _dietOverviewLoading.value = false
        }
    }

    private val _markingMedicines = MutableStateFlow<Set<Int>>(emptySet())
    val markingMedicines: StateFlow<Set<Int>> = _markingMedicines

    fun markMedicineIntake(medicine: com.critetiontech.ctvitalio.model.MedicineItem) {
        // Ignore repeat taps while this medicine is already being marked.
        if (medicine.id in _markingMedicines.value) return
        _markingMedicines.value += medicine.id
        viewModelScope.launch {
            Log.d("HomeViewModel", "🔵 markMedicineIntake called → id=${medicine.id}, scheduledDateTime=${medicine.scheduledDateTime}, medicineName=${medicine.medicineName}")
            try {
                val result = AppDependencies.markMedicineIntake()(medicine.id, medicine.scheduledDateTime)
                Log.d("HomeViewModel", "🔵 markMedicineIntake result → $result")
                if (result is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                    // Show it as taken right away, then sync with the server before clearing
                    // the loader — otherwise the stale unchecked card flashes back and invites a second tap.
                    val now = java.time.LocalDateTime.now().withNano(0).toString()
                    _dailyMedicines.value = _dailyMedicines.value.map {
                        if (it.id == medicine.id) it.copy(isTaken = true, takenDateTime = now) else it
                    }
                    Haptics.success()
                    // Refresh the card, and drop the alarm for the dose just taken using the same
                    // download (this used to fetch today's schedule twice).
                    refreshTodayMedicines(syncReminders = true)
                } else if (result is com.critetiontech.ctvitalio.domain.model.DomainResult.Error) {
                    Log.e("HomeViewModel", "🔴 markMedicineIntake API error: ${result.exception.message}", result.exception)
                    Haptics.error()
                    android.widget.Toast.makeText(MyApplication.appContext, MyApplication.appContext.getString(com.critetiontech.ctvitalio.R.string.home_failed_mark_medicine), android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "🔴 Error marking intake: ${e.message}", e)
            } finally {
                _markingMedicines.value = _markingMedicines.value - medicine.id
            }
        }
    }

    /**
     * Today's medicines for the card. With [syncReminders], the same download is handed to the
     * reminder scheduler so it doesn't fetch today's schedule again.
     */
    private suspend fun refreshTodayMedicines(syncReminders: Boolean = false) {
        val patient = prefs.getPatient() ?: return
        try {
            val today = java.time.LocalDate.now()
            val medsRes = AppDependencies.fetchMedicineIntake()(patient.pid, today.toString(), patient.clientId)
            if (medsRes is com.critetiontech.ctvitalio.domain.model.DomainResult.Success) {
                _dailyMedicines.value = medsRes.data.flatMap { it.medicineData }
                    .filter { it.medicineName.isNotBlank() }
                    .sortedBy { it.scheduledDateTime }
                    .withLocallyTaken(MyApplication.appContext)
                if (syncReminders) MedicineReminderScheduler.requestSync(MyApplication.appContext, today to medsRes.data)
            }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "refreshTodayMedicines error: ${e.message}")
        }
    }

    /** Same as [fetchDashboard] but skips the cache, both loads side by side. */
    fun fetchDashboardNoCache() {
        viewModelScope.launch {
            launch { loadDashboard(cache = false) }
            launch { refreshTodayMedicines() }
        }
    }


}