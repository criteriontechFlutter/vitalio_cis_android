package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.os.Build
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.FluidSummaryItem
import com.critetiontech.ctvitalio.model.IntakeItem
import com.critetiontech.ctvitalio.model.ManualFoodAssignItem
import com.critetiontech.ctvitalio.model.OutputItem
import com.critetiontech.ctvitalio.model.OutputSummaryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.data.offline.PendingEntries
import kotlinx.coroutines.flow.drop

class IntakeOutputViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    var searchText by mutableStateOf(""); private set

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _outputList = MutableStateFlow<List<OutputItem>>(emptyList())
    val outputList: StateFlow<List<OutputItem>> = _outputList

    private val _outputSummaryList = MutableStateFlow<List<OutputSummaryItem>>(emptyList())
    val outputSummaryList: StateFlow<List<OutputSummaryItem>> = _outputSummaryList

    private val _outputLoading = MutableStateFlow(false)
    val outputLoading: StateFlow<Boolean> = _outputLoading

    private val _intakeList = MutableStateFlow<List<IntakeItem>>(emptyList())
    val intakeList: StateFlow<List<IntakeItem>> = _intakeList

    // Latest fluidRecommended from FetchIntake; kept when a day has no entries so the target doesn't drop back to default
    private val _fluidRecommended = MutableStateFlow<Int?>(null)
    val fluidRecommended: StateFlow<Int?> = _fluidRecommended

    /** Day the intake and output lists were last loaded for, so they can reload after a sync. */
    private var intakeListDate: String? = null
    private var outputListDate: String? = null

    private fun setIntakeList(list: List<IntakeItem>, date: String) {
        val names = _manualFoodList.value.associate { it.foodID to it.foodName }
        _intakeList.value = list + PendingEntries.intake(date, names)
        list.firstNotNullOfOrNull { it.fluidRecommendedMl }?.let { _fluidRecommended.value = it }
    }

    init {
        // Offline logs just uploaded: reload so the server's copies replace the pending ones.
        viewModelScope.launch {
            OfflineOutbox.lastSyncedAt.drop(1).collect {
                intakeListDate?.let { fetchIntakeItems(it) }
                outputListDate?.let { fetchOutput(it) }
            }
        }
    }

    private val _fluidSummaryList =MutableStateFlow<List<FluidSummaryItem>>(emptyList())
    val fluidSummaryList: StateFlow<List<FluidSummaryItem>> = _fluidSummaryList

    private val _intakeLoading = MutableStateFlow(false)
    val intakeLoading: StateFlow<Boolean> = _intakeLoading

    private val _intakeSuccess = MutableStateFlow(false)
    val intakeSuccess: StateFlow<Boolean> = _intakeSuccess

    private val _outputSuccess = MutableStateFlow(false)
    val outputSuccess: StateFlow<Boolean> = _outputSuccess

    fun resetIntakeSuccess() { _intakeSuccess.value = false }
    fun resetOutputSuccess() { _outputSuccess.value = false }

    private val _manualFoodList = MutableStateFlow<List<ManualFoodAssignItem>>(emptyList())
    val manualFoodList: StateFlow<List<ManualFoodAssignItem>> = _manualFoodList

    // Both calls below take ~20 s on the server: show the cached copy right away and only show the
    // spinner when there's nothing cached yet. The network result replaces it when it arrives.
    fun getManualFoodAssignList() {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            val cached = deps.fluidRepository.cachedManualFoodList(patient.uhId)
            if (cached.isNotEmpty()) _manualFoodList.value = cached
            _intakeLoading.value = cached.isEmpty()
            when (val r = deps.getManualFoodList()(patient.uhId)) {
                is DomainResult.Success -> _manualFoodList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchIntakeItems(fromDate: String) {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: run { _intakeList.value = emptyList(); return@launch }
            intakeListDate = fromDate
            val cached = deps.fluidRepository.cachedIntakeItems(patient.uhId, fromDate)
            setIntakeList(cached, fromDate)
            _intakeLoading.value = cached.isEmpty()
            when (val r = deps.fetchIntake()(patient.uhId, patient.clientId.toString(), fromDate)) {
                is DomainResult.Success -> setIntakeList(r.data, fromDate)
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun addIntake(value: String, fId: String) {
        viewModelScope.launch {
            _intakeLoading.value = true
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            // Local wall-clock time, no offset: the server stores the string as given, so a UTC
            // instant ("…Z") showed up 5½ hours early in the history.
            val foodDate = LocalDateTime.now().withNano(0).toString()
            val body = mapOf<String, Any>("FoodDate" to foodDate, "FoodQuantity" to value, "UserID" to patient.pid, "foodId" to fId, "clientId" to patient.clientId, "FoodUnitId" to 27, "uhid" to patient.uhId, "isGiven" to 1, "entryType" to "N", "isFromPatient" to 1)
            when (val r = deps.addIntake()(body)) {
                is DomainResult.Success -> {
                    Log.d("IntakeOutputVM", "addIntake success")
                    _intakeSuccess.value = true
                    Haptics.success()
                    // Refresh today's intake bar chart data
                    fetchIntake()
                }
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchOutput(date: String) {
        viewModelScope.launch {
            outputListDate = date
            _outputLoading.value = true; _outputList.value = PendingEntries.output(date)
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            when (val r = deps.fetchOutput()(patient.uhId, patient.clientId.toString(), patient.pid.toString(), date)) {
                is DomainResult.Success -> _outputList.value = r.data + PendingEntries.output(date)
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun addOutput(quantity: Int, colour: String) {
        viewModelScope.launch {
            _outputLoading.value = true
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            val outputDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) else java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
            val body = mapOf<String, Any>("outputTypeID" to 51, "quantity" to quantity, "unitID" to 1, "outputDate" to outputDate, "colour" to colour, "userID" to patient.pid, "clientId" to patient.clientId, "uhid" to patient.uhId)
            when (val r = deps.addOutput()(body)) {
                is DomainResult.Success -> {
                    Log.d("IntakeOutputVM", "addOutput success")
                    _outputSuccess.value = true
                    Haptics.success()
                    // Refresh the count/volume (includes the entry if it was queued offline)
                    fetchOutput(outputListDate ?: LocalDate.now().toString())
                }
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun fetchOutputSummary(fromDate: String, toDate: String) {
        viewModelScope.launch {
            _outputLoading.value = true; _outputSummaryList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _outputLoading.value = false; return@launch }
            when (val r = deps.fetchOutputSummary()(patient.uhId, patient.clientId.toString(), fromDate, toDate)) {
                is DomainResult.Success -> _outputSummaryList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _outputLoading.value = false
        }
    }

    fun fetchFluidSummary(fromDate: String, toDate: String) {
        viewModelScope.launch {
            _intakeLoading.value = true; _fluidSummaryList.value = emptyList()
            val patient = prefs.getPatient() ?: run { _intakeLoading.value = false; return@launch }
            when (val r = deps.fetchFluidSummary()(patient.uhId, fromDate, toDate)) {
                is DomainResult.Success -> _fluidSummaryList.value = r.data
                is DomainResult.Error -> Log.e("IntakeOutputVM", r.exception.message.orEmpty())
            }
            _intakeLoading.value = false
        }
    }

    fun fetchIntake() {
        val today = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) LocalDate.now().toString() else java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        fetchIntakeItems(today)
    }
}
