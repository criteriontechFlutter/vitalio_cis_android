package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.InsertMedicineRequest
import com.critetiontech.ctvitalio.model.PrescriptionMedicine
import com.critetiontech.ctvitalio.model.PrescriptionResponseItem
import com.critetiontech.ctvitalio.model.withLocalId
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class UploadPrescriptionViewModel : ViewModel() {

    private val uploadPrescriptionUseCase = AppDependencies.uploadPrescription()
    private val insertMedicineUseCase = AppDependencies.insertMedicine()
    private val prefs = AppDependencies.prefs

    // ── Selected file ─────────────────────────────────────────────────────────
    private val _selectedFile = MutableStateFlow<File?>(null)
    val selectedFile: StateFlow<File?> = _selectedFile
    private var selectedMimeType: String = "image/jpeg"

    fun setSelectedFile(file: File?, mimeType: String = "image/jpeg") {
        _selectedFile.value = file
        selectedMimeType = mimeType
    }

    // ── Upload / analysis ─────────────────────────────────────────────────────
    private val _uploadingFile = MutableStateFlow(false)
    val uploadingFile: StateFlow<Boolean> = _uploadingFile

    private val _result = MutableStateFlow<PrescriptionResponseItem?>(null)
    val result: StateFlow<PrescriptionResponseItem?> = _result

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    // ── Medicine selection / saving ───────────────────────────────────────────
    private val _selectedMedicineIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedMedicineIds: StateFlow<Set<String>> = _selectedMedicineIds

    private val _isSavingMedicines = MutableStateFlow(false)
    val isSavingMedicines: StateFlow<Boolean> = _isSavingMedicines

    private val _medicinesSavedSuccessfully = MutableStateFlow(false)
    val medicinesSavedSuccessfully: StateFlow<Boolean> = _medicinesSavedSuccessfully

    private val _showSessionAlert = MutableStateFlow(false)
    val showSessionAlert: StateFlow<Boolean> = _showSessionAlert

    fun dismissSessionAlert() { _showSessionAlert.value = false }

    fun toggleMedicineSelection(id: String) {
        _selectedMedicineIds.value = _selectedMedicineIds.value.let { if (id in it) it - id else it + id }
    }

    fun selectAllMedicines(medicines: List<PrescriptionMedicine>) {
        _selectedMedicineIds.value = medicines.filter { it.isInsertable }.mapNotNull { it.localId }.toSet()
    }

    fun deselectAllMedicines() { _selectedMedicineIds.value = emptySet() }

    /** Selected medicines if any are selected, otherwise all insertable medicines. */
    fun resolvedMedicinesToInsert(medicines: List<PrescriptionMedicine>): List<PrescriptionMedicine> {
        val insertable = medicines.filter { it.isInsertable }
        val selected = _selectedMedicineIds.value
        return if (selected.isEmpty()) insertable else insertable.filter { it.localId in selected }
    }

    fun uploadPrescriptionToAnalyse() {
        val file = _selectedFile.value ?: run {
            viewModelScope.launch { _toastEvent.emit("Please select a prescription document first") }
            return
        }
        viewModelScope.launch {
            _uploadingFile.value = true
            when (val r = uploadPrescriptionUseCase(file, selectedMimeType)) {
                is DomainResult.Success -> {
                    val item = r.data
                    _selectedMedicineIds.value = emptySet()
                    _medicinesSavedSuccessfully.value = false
                    _result.value = item.copy(
                        prescription = item.prescription?.let { p ->
                            p.copy(medicines = p.medicines?.map { it.withLocalId() })
                        }
                    )
                    _navigationEvent.emit(Routes.PRESCRIPTIONANALYSIS)
                }
                is DomainResult.Error -> {
                    Log.e("UploadPrescriptionVM", r.exception.message.orEmpty())
                    _toastEvent.emit(r.exception.message ?: "Upload failed")
                }
            }
            _uploadingFile.value = false
        }
    }

    // ── Batch insert medicines into Medication Schedule ───────────────────────
    fun insertPrescriptionMedicines(medicines: List<PrescriptionMedicine>) {
        viewModelScope.launch {
            if (medicines.isEmpty()) { _toastEvent.emit("No medicines to add"); return@launch }

            val patient = prefs.getPatient()
            if (patient == null || patient.pid <= 0 || patient.clientId <= 0) {
                _showSessionAlert.value = true
                return@launch
            }

            val insertable = medicines.filter { it.isInsertable }
            val unmappedCount = medicines.size - insertable.size
            if (insertable.isEmpty()) {
                _toastEvent.emit("All ${medicines.size} medicines require manual addition to reminders.")
                return@launch
            }

            _isSavingMedicines.value = true
            val now = Date()
            val apiDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val formattedStart = apiDate.format(now)

            val results = insertable.map { med ->
                async {
                    val request = InsertMedicineRequest(
                        pid = "${patient.pid}",
                        medicineId = med.medicineID ?: 0,
                        dosageType = med.dosageFormName?.takeIf { it.isNotEmpty() }
                            ?: med.form?.takeIf { it.isNotEmpty() } ?: "Tablet",
                        dosageStrength = (med.doseStrength ?: 0.0).toInt(),
                        frequency = "Every day",
                        frequencyValue = "0",
                        weekDays = "",
                        monthDates = "",
                        instructions = normalizeInstructions(med.timing),
                        timeSlotsJson = resolveTimeSlotsJson(med.timeSlotsJson, med.frequency),
                        startdate = formattedStart,
                        enddate = calculateEndDate(now, med.duration),
                        clientId = "${patient.clientId}"
                    )
                    when (val r = insertMedicineUseCase(request)) {
                        is DomainResult.Success -> (r.data.status == 1).also {
                            if (!it) Log.w("UploadPrescriptionVM", "Failed to insert ${med.displayName}: ${r.data.message}")
                        }
                        is DomainResult.Error -> {
                            Log.e("UploadPrescriptionVM", "Error inserting ${med.displayName}: ${r.exception.message}")
                            false
                        }
                    }
                }
            }.awaitAll()

            _isSavingMedicines.value = false
            val successCount = results.count { it }
            if (successCount > 0) {
                _medicinesSavedSuccessfully.value = true
                Haptics.success()
                val manualNote = if (unmappedCount > 0) " ($unmappedCount require manual addition)" else ""
                _toastEvent.emit("$successCount ${if (successCount == 1) "medicine" else "medicines"} added to reminders!$manualNote")
            } else {
                Haptics.error()
                _toastEvent.emit("Failed to add medicines. Please try again.")
            }
        }
    }

    // ── Formatting helpers ────────────────────────────────────────────────────
    fun formattedTiming(timing: String?): String {
        val t = timing?.trim().orEmpty()
        if (t.isEmpty()) return "As Prescribed"
        return when (t.lowercase().replace("-", "_")) {
            "after_meals", "after_meal", "aftermeal", "aftermeals" -> "After Meals"
            "before_meals", "before_meal", "beforemeal", "beforemeals" -> "Before Meals"
            "empty_stomach" -> "Empty Stomach"
            "with_food" -> "With Food"
            "at_bedtime", "bedtime" -> "At Bedtime"
            else -> t.replace("_", " ").split(" ").joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
        }
    }

    fun formattedDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Recent"
        val out = SimpleDateFormat("dd MMM yyyy", Locale.US)
        listOf("yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "dd/MM/yyyy", "d/M/yyyy").forEach { fmt ->
            try {
                val parser = SimpleDateFormat(fmt, Locale.US).apply { isLenient = false }
                parser.parse(dateString)?.let { return out.format(it) }
            } catch (_: Exception) { }
        }
        return dateString
    }

    fun formattedTimeSlot(slot: String): String {
        val trimmed = slot.trim()
        return try {
            SimpleDateFormat("HH:mm", Locale.US).parse(trimmed)
                ?.let { SimpleDateFormat("hh:mm a", Locale.US).format(it) } ?: trimmed
        } catch (_: Exception) { trimmed }
    }

    private fun normalizeInstructions(timing: String?): String {
        val t = timing?.trim().orEmpty()
        if (t.isEmpty()) return "As Prescribed"
        return when (t.lowercase().replace("-", "_")) {
            "after_meals", "after_meal", "aftermeal", "aftermeals" -> "After meal"
            "before_meals", "before_meal", "beforemeal", "beforemeals" -> "Before meal"
            "empty_stomach" -> "Empty stomach"
            "with_food" -> "With food"
            "at_bedtime", "bedtime", "before_bed" -> "Before bed"
            else -> t.replace("_", " ").split(" ").joinToString(" ") { w -> w.lowercase().replaceFirstChar { it.uppercase() } }
        }
    }

    private fun resolveTimeSlotsJson(timeSlotsJson: String?, frequency: String?): String {
        val json = timeSlotsJson?.trim()
        if (json != null && json != "[]" && json.contains("timeSlot")) return json
        return when (frequency?.trim().orEmpty()) {
            "1-0-1", "2-0-2" -> "[{\"timeSlot\":\"08:00\"},{\"timeSlot\":\"21:00\"}]"
            "1-1-1" -> "[{\"timeSlot\":\"08:00\"},{\"timeSlot\":\"14:00\"},{\"timeSlot\":\"21:00\"}]"
            "0-0-1" -> "[{\"timeSlot\":\"21:00\"}]"
            "0-1-0" -> "[{\"timeSlot\":\"14:00\"}]"
            else -> "[{\"timeSlot\":\"08:00\"}]"
        }
    }

    private fun calculateEndDate(startDate: Date, duration: String?): String {
        val cal = Calendar.getInstance().apply { time = startDate }
        val d = duration?.trim()?.lowercase().orEmpty()
        val number = d.split(Regex("\\s+")).firstOrNull()?.toIntOrNull()
        when {
            number != null && d.contains("day") -> cal.add(Calendar.DAY_OF_YEAR, number)
            number != null && d.contains("week") -> cal.add(Calendar.DAY_OF_YEAR, number * 7)
            number != null && d.contains("month") -> cal.add(Calendar.MONTH, number)
            else -> cal.add(Calendar.DAY_OF_YEAR, 30)
        }
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }
}