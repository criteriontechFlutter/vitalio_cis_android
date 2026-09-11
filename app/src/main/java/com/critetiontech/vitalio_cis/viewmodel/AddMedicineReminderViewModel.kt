package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.InsertMedicineRequest
import com.critetiontech.vitalio_cis.model.SearchMedicineItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class TimeSlot(val id: Long = System.currentTimeMillis(), var time: Date = Date(), var isSelected: Boolean = false)

class AddMedicineReminderViewModel : ViewModel() {

    private val searchMedicineUseCase = AppDependencies.searchMedicine()
    private val insertMedicineUseCase = AppDependencies.insertMedicine()
    private val prefs = AppDependencies.prefs

    // ── Search ────────────────────────────────────────────────────────────────
    var medicineName by mutableStateOf("")
    var searchResults by mutableStateOf<List<SearchMedicineItem>>(emptyList())
    var selectedMedicine by mutableStateOf<SearchMedicineItem?>(null)
    var isSearching by mutableStateOf(false)

    private val searchCache = mutableMapOf<String, List<SearchMedicineItem>>()
    private var debounceJob: Job? = null

    fun onMedicineNameChange(value: String) {
        medicineName = value
        if (selectedMedicine?.brandName == value) return
        selectedMedicine = null
        searchMedicineDebounced(value)
    }

    fun clearMedicineSelection() {
        selectedMedicine = null
        medicineName = ""
        searchResults = emptyList()
    }

    fun selectMedicine(item: SearchMedicineItem) {
        selectedMedicine = item
        medicineName = item.brandName
        searchResults = emptyList()
    }

    private fun searchMedicineDebounced(query: String) {
        debounceJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length < 2) { searchResults = emptyList(); isSearching = false; return }
        val key = trimmed.lowercase()
        searchCache[key]?.let { searchResults = it; isSearching = false; return }
        debounceJob = viewModelScope.launch {
            delay(300)
            isSearching = true
            when (val r = searchMedicineUseCase(trimmed)) {
                is DomainResult.Success -> {
                    searchCache[key] = r.data
                    searchResults = r.data
                }
                is DomainResult.Error -> Log.e("AddMedicineVM", r.exception.message.orEmpty())
            }
            isSearching = false
        }
    }

    // ── Frequency ─────────────────────────────────────────────────────────────
    var frequency by mutableStateOf("")
    var frequencyValue by mutableStateOf(2)
    var selectedDays by mutableStateOf<Set<String>>(emptySet())
    var selectedDates by mutableStateOf<List<Int>>(emptyList())

    val apiFrequencyValue: String get() = if (frequency == "Every x day") "$frequencyValue" else "0"
    val apiWeekDays: String
        get() {
            if (frequency != "Every week") return ""
            val dayMapping = listOf(
                "Sunday" to "SU", "Monday" to "MO", "Tuesday" to "TU",
                "Wednesday" to "WE", "Thursday" to "TH", "Friday" to "FR", "Saturday" to "SA"
            )
            return dayMapping.filter { (id, _) -> selectedDays.contains(id) }
                .joinToString(",") { it.second }
        }
    val apiMonthlyDates: String
        get() {
            if (selectedDates.isEmpty()) return ""
            return selectedDates.sorted().joinToString(", ") { formatOrdinal(it) }
        }

    private fun formatOrdinal(n: Int): String {
        val suffix = when {
            n in 11..13 -> "th"
            n % 10 == 1 -> "st"
            n % 10 == 2 -> "nd"
            n % 10 == 3 -> "rd"
            else -> "th"
        }
        return "$n$suffix"
    }

    // ── Instructions ──────────────────────────────────────────────────────────
    var instructions by mutableStateOf("")

    // ── Dates ─────────────────────────────────────────────────────────────────
    var startDate by mutableStateOf(Date())
    var endDate by mutableStateOf(Date())

    val dateFormatter = SimpleDateFormat("MM/dd/yyyy", Locale.US)
    val apiDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val startDateFormatted: String get() = dateFormatter.format(startDate)
    val endDateFormatted: String get() = dateFormatter.format(endDate)

    // ── Time Slots ────────────────────────────────────────────────────────────
    var timeSlots by mutableStateOf(listOf(TimeSlot()))
    var showTimePicker by mutableStateOf(false)
    var activeSlotIndex by mutableStateOf(0)

    fun addTimeSlot() { timeSlots = timeSlots + TimeSlot() }
    fun removeTimeSlot(index: Int) {
        if (timeSlots.size > 1 && index > 0)
            timeSlots = timeSlots.toMutableList().also { it.removeAt(index) }
    }
    fun updateSlotTime(index: Int, time: Date) {
        timeSlots = timeSlots.toMutableList().also { list ->
            list[index] = list[index].copy(time = time, isSelected = true)
        }
    }

    // ── Insert ────────────────────────────────────────────────────────────────
    private val _isInserting = MutableStateFlow(false)
    val isInserting: StateFlow<Boolean> = _isInserting

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _insertSuccess = MutableStateFlow(false)
    val insertSuccess: StateFlow<Boolean> = _insertSuccess

    fun clearError() { _errorMessage.value = null }
    fun clearSuccess() { _insertSuccess.value = false }

    fun insertMedicineReminder() {
        viewModelScope.launch {
            val medicine = selectedMedicine ?: run { _errorMessage.value = "No medicine selected"; return@launch }
            _isInserting.value = true
            _errorMessage.value = null

            val timeFmt = SimpleDateFormat("HH:mm", Locale.US)
            val jsonArray = JSONArray()
            timeSlots.forEach { slot ->
                val obj = JSONObject()
                obj.put("timeSlot", timeFmt.format(slot.time))
                jsonArray.put(obj)
            }

            val patient = prefs.getPatient()
            val request = InsertMedicineRequest(
                pid = "${patient?.pid ?: 0}",
                medicineId = medicine.medicineID,
                dosageType = medicine.dosageFormName,
                dosageStrength = medicine.doseStrength.toInt(),
                frequency = frequency,
                frequencyValue = apiFrequencyValue,
                weekDays = apiWeekDays,
                monthDates = apiMonthlyDates,
                instructions = instructions,
                timeSlotsJson = jsonArray.toString(),
                startdate = apiDateFormatter.format(startDate),
                enddate = apiDateFormatter.format(endDate),
                clientId = "${patient?.clientId ?: 0}"
            )

            Log.d("AddMedicineVM", "Request: $request")

            when (val r = insertMedicineUseCase(request)) {
                is DomainResult.Success -> {
                    if (r.data.status == 1) {
                        _insertSuccess.value = true
                    } else {
                        _errorMessage.value = r.data.responseValue ?: r.data.message
                    }
                }
                is DomainResult.Error -> {
                    _errorMessage.value = r.exception.message ?: "Unknown error"
                    Log.e("AddMedicineVM", r.exception.message.orEmpty())
                }
            }
            _isInserting.value = false
        }
    }

    fun resetFields() {
        medicineName = ""
        selectedMedicine = null
        searchResults = emptyList()
        frequency = ""
        frequencyValue = 2
        selectedDays = emptySet()
        selectedDates = emptyList()
        instructions = ""
        startDate = Date()
        endDate = Date()
        timeSlots = listOf(TimeSlot())
        showTimePicker = false
        activeSlotIndex = 0
        _errorMessage.value = null
        _insertSuccess.value = false
    }
}
