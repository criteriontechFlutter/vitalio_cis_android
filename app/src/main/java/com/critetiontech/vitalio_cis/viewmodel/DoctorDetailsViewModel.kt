package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.ConsentItem
import com.critetiontech.ctvitalio.model.DoctorDetails
import com.critetiontech.ctvitalio.model.DoctorSchedule
import com.critetiontech.ctvitalio.model.LatestToken
import com.critetiontech.ctvitalio.model.ShiftData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DoctorDetailsViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _bookingLoading = MutableStateFlow(false)
    val bookingLoading: StateFlow<Boolean> = _bookingLoading

    private val _bookingSuccess = MutableStateFlow(false)
    val bookingSuccess: StateFlow<Boolean> = _bookingSuccess

    private val _bookingError = MutableStateFlow<String?>(null)
    val bookingError: StateFlow<String?> = _bookingError

    private val _doctorId = MutableLiveData("")
    private val _departmentId = MutableLiveData("")
    
    private val _slotTime = MutableStateFlow("")
    val slotTime: StateFlow<String> = _slotTime

    private val _slotDate = MutableStateFlow("")
    val slotDate: StateFlow<String> = _slotDate

    private val _shiftId = MutableStateFlow("")
    val shiftId: StateFlow<String> = _shiftId

    fun setSlotTime(value: String) { _slotTime.value = value }
    fun setSlotDate(value: String) { 
        _slotDate.value = value 
        extractShiftsForDate(value)
    }
    fun setShiftId(value: String) { _shiftId.value = value }

    /** Call this when user picks a new date — routes to slots or shifts automatically. */
    fun onDateSelected(dateStr: String, doctorId: String = _doctorId.value.orEmpty()) {
        setSlotDate(dateStr)
        if (_isSlotBased.value) {
            fetchAvailableSlots(dateStr, doctorId)
        }
        // For token-wise, extractShiftsForDate is already called inside setSlotDate
    }

    private val _doctor = MutableStateFlow<DoctorDetails?>(null)
    val doctor: StateFlow<DoctorDetails?> = _doctor

    // Slot Based State
    private val _isSlotBased = MutableStateFlow(true)
    val isSlotBased: StateFlow<Boolean> = _isSlotBased
    private val _slotList = MutableStateFlow<List<ShiftData>>(emptyList())
    val slotList: StateFlow<List<ShiftData>> = _slotList

    // Token Based State
    private val _scheduleList = MutableStateFlow<List<DoctorSchedule>>(emptyList())
    val scheduleList: StateFlow<List<DoctorSchedule>> = _scheduleList
    
    private val _latestToken = MutableStateFlow<LatestToken?>(null)
    val latestToken: StateFlow<LatestToken?> = _latestToken

    // Consent State
    private val _consentList = MutableStateFlow<List<ConsentItem>>(emptyList())
    val consentList: StateFlow<List<ConsentItem>> = _consentList

    fun setDoctorId(id: String) { _doctorId.value = id }
    fun setDepartmentId(value: String) { _departmentId.value = value }
    fun resetBookingState() { _bookingSuccess.value = false; _bookingError.value = null }

    fun initDoctorConfiguration(doctorId: String) {
        setDoctorId(doctorId)
        getDoctorProfile(doctorId)
        checkShiftType(doctorId)
        fetchConsent()
    }

    private val _shiftConfig = MutableStateFlow<com.critetiontech.ctvitalio.model.Doctor?>(null)

    fun checkShiftType(doctorId: String) {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            when (val r = deps.doctorRepository.fetchDoctorShiftConfig(doctorId, patient.clientId.toString())) {
                is DomainResult.Success -> {
                    val config = r.data
                    _shiftConfig.value = config
                    val isSlot = config?.bookingType?.equals("Slot Wise", ignoreCase = true) != false
                    _isSlotBased.value = isSlot
                    // If we already have a selected date, extract shifts for its day
                    if (!isSlot && _slotDate.value.isNotEmpty()) {
                        extractShiftsForDate(_slotDate.value)
                    }
                }
                is DomainResult.Error -> _isSlotBased.value = true // default to slot
            }
        }
    }

    fun extractShiftsForDate(dateStr: String) {
        if (_isSlotBased.value) return
        val config = _shiftConfig.value ?: return
        try {
            // dateStr is usually "yyyy/MM/dd" or "yyyy-M-d"
            val parts = dateStr.replace("-", "/").split("/")
            if (parts.size == 3) {
                val cal = java.util.Calendar.getInstance()
                cal.set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
                val dayOfWeek = cal.get(java.util.Calendar.DAY_OF_WEEK)
                // In Calendar, Sunday is 1, Monday is 2. In DB, it might be Monday=1, Sunday=7.
                // Let's assume dayId matches Calendar (Sunday=1). Or maybe Monday=1?
                // Wait, in sample: dayId=3 is Wednesday. In Calendar, Wednesday is 4.
                // So dayId is likely 1=Monday, 2=Tuesday, 3=Wednesday, 4=Thursday, 5=Friday, 6=Saturday, 7=Sunday.
                val dbDayId = if (dayOfWeek == java.util.Calendar.SUNDAY) 7 else dayOfWeek - 1
                
                val workingDay = config.workingDays.find { it.dayId == dbDayId }
                val newSchedules = workingDay?.shifts?.map {
                    DoctorSchedule(
                        id = it.shiftId,
                        doctorId = config.doctorId,
                        shiftId = it.shiftId,
                        shiftName = it.shiftName,
                        timeInterval = "",
                        dayId = dbDayId,
                        dayName = workingDay.dayName,
                        shiftTimeFrom = it.shiftTimeFrom,
                        shiftTimeTo = it.shiftTimeTo,
                        userId = 0,
                        doctorName = config.doctorName ?: ""
                    )
                } ?: emptyList()
                
                _scheduleList.value = newSchedules
            }
        } catch (e: Exception) {
            _scheduleList.value = emptyList()
        }
    }

    fun getDoctorProfile(doctorId: String = _doctorId.value.orEmpty()) {
        val id = doctorId.ifEmpty { _doctorId.value.orEmpty() }
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchDoctorProfile()(patient.clientId.toString(), id)) {
                is DomainResult.Success -> _doctor.value = r.data
                is DomainResult.Error -> Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    // Slot-based Schedule
    fun fetchAvailableSlots(scheduleDate: String, doctorId: String = _doctorId.value.orEmpty()) {
        val id = doctorId.ifEmpty { _doctorId.value.orEmpty() }
        viewModelScope.launch {
            _slotList.value = emptyList(); _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchAvailableSlots()(id, scheduleDate, patient.clientId.toString())) {
                is DomainResult.Success -> _slotList.value = r.data
                is DomainResult.Error -> Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    // Token-based Schedule
    fun fetchDoctorSchedule(dayId: String, doctorId: String = _doctorId.value.orEmpty()) {
        val id = doctorId.ifEmpty { _doctorId.value.orEmpty() }
        viewModelScope.launch {
            _scheduleList.value = emptyList(); _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.doctorRepository.fetchSchedule(id, patient.clientId.toString(), dayId)) {
                is DomainResult.Success -> _scheduleList.value = r.data
                is DomainResult.Error -> Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun fetchLatestToken(scheduleDate: String, shiftId: String, doctorId: String = _doctorId.value.orEmpty()) {
        val id = doctorId.ifEmpty { _doctorId.value.orEmpty() }
        val depId = _departmentId.value.orEmpty().ifEmpty { "0" }
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            when (val r = deps.doctorRepository.getLatestToken(id, scheduleDate, shiftId, depId, patient.clientId.toString())) {
                is DomainResult.Success -> _latestToken.value = r.data.firstOrNull()
                is DomainResult.Error -> _latestToken.value = null
            }
        }
    }

    private fun fetchConsent() {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            when (val r = deps.doctorRepository.fetchConsent(patient.clientId.toString())) {
                is DomainResult.Success -> _consentList.value = r.data
                is DomainResult.Error -> _consentList.value = emptyList()
            }
        }
    }

    fun bookAppointment(did: String, shiftId: Int, sTime: String, appointmentDate: String, queueNo: Int? = null) {
        viewModelScope.launch {
            _bookingLoading.value = true; _bookingError.value = null
            val patient = prefs.getPatient() ?: run { _bookingLoading.value = false; return@launch }
            
            // Check if consent is required
            if (_consentList.value.isNotEmpty()) {
                val consentParams = mapOf(
                    "patientId" to patient.pid,
                    "consentMasterId" to _consentList.value.first().id,
                    "clientId" to patient.clientId.toString()
                )
                deps.doctorRepository.addConsent(consentParams) // Fire and forget or handle wait
            }

            val params = mutableMapOf<String, Any>(
                "departmentId" to (_departmentId.value?.toIntOrNull() ?: 3), 
                "doctorId" to did, 
                "uhId" to patient.uhId, 
                "isOnline" to true, 
                "clientId" to patient.clientId.toString(), 
                "shiftId" to shiftId, 
                "appointmentDate" to appointmentDate
            )
            
            if (_isSlotBased.value) {
                params["slotTime"] = sTime
            } else {
                params["slotTime"] = sTime // For token, it passes shiftTimeFrom
                queueNo?.let { params["queueNo"] = it }
            }

            when (val r = deps.bookAppointment()(params)) {
                is DomainResult.Success -> { _bookingSuccess.value = true; Haptics.success() }
                is DomainResult.Error -> { _bookingError.value = "Booking failed."; Haptics.error(); Log.e("DoctorDetailsVM", r.exception.message.orEmpty()) }
            }
            _bookingLoading.value = false
        }
    }
}
