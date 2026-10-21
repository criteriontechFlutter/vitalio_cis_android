package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.BookingException
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

    /** True when the doctor's profile couldn't be loaded, so the screen can offer a retry. */
    private val _profileError = MutableStateFlow(false)
    val profileError: StateFlow<Boolean> = _profileError

    /** Message when slots couldn't be loaded (as opposed to the day simply having none). */
    private val _slotsError = MutableStateFlow<String?>(null)
    val slotsError: StateFlow<String?> = _slotsError

    /** True while the next token number for the picked shift is loading. */
    private val _tokenLoading = MutableStateFlow(false)
    val tokenLoading: StateFlow<Boolean> = _tokenLoading

    /** Message when the token number couldn't be fetched. */
    private val _tokenError = MutableStateFlow<String?>(null)
    val tokenError: StateFlow<String?> = _tokenError

    /** Visit number of the appointment just booked, if the server sent one. */
    private val _visitNo = MutableStateFlow<String?>(null)
    val visitNo: StateFlow<String?> = _visitNo

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

    /** Minutes per appointment from the doctor's shift setup; 0 until it loads. */
    private val _slotSize = MutableStateFlow(0)
    val slotSize: StateFlow<Int> = _slotSize

    fun checkShiftType(doctorId: String) {
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: return@launch
            when (val r = deps.doctorRepository.fetchDoctorShiftConfig(doctorId, patient.clientId.toString())) {
                is DomainResult.Success -> {
                    val config = r.data
                    _shiftConfig.value = config
                    _slotSize.value = config?.appointmentSlotSize ?: 0
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
            _profileError.value = false
            val patient = prefs.getPatient() ?: run { _loading.value = false; _profileError.value = true; return@launch }
            when (val r = deps.fetchDoctorProfile()(patient.clientId.toString(), id)) {
                is DomainResult.Success -> {
                    // The server pads some names ("Dr Ahmad ").
                    _doctor.value = r.data?.let { it.copy(name = it.name.trim()) }
                    _profileError.value = r.data == null
                    // Book under the doctor's own department.
                    r.data?.departmentId?.takeIf { it != 0 }?.let { _departmentId.value = it.toString() }
                }
                is DomainResult.Error -> {
                    _profileError.value = _doctor.value == null
                    Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
                }
            }
            _loading.value = false
        }
    }

    // Slot-based Schedule
    fun fetchAvailableSlots(scheduleDate: String, doctorId: String = _doctorId.value.orEmpty()) {
        val id = doctorId.ifEmpty { _doctorId.value.orEmpty() }
        viewModelScope.launch {
            _slotList.value = emptyList(); _slotsError.value = null; _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchAvailableSlots()(id, scheduleDate, patient.clientId.toString())) {
                // Ignore an answer for a date the patient has since moved away from.
                is DomainResult.Success -> if (_slotDate.value == scheduleDate) _slotList.value = r.data
                is DomainResult.Error -> {
                    if (_slotDate.value == scheduleDate) _slotsError.value = "Couldn't load the available slots. Check your connection and try again."
                    Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
                }
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
        // Drop the previous shift's number right away so it never shows against the new shift.
        _latestToken.value = null
        _tokenError.value = null
        _tokenLoading.value = true
        viewModelScope.launch {
            val patient = prefs.getPatient() ?: run { _tokenLoading.value = false; return@launch }
            val r = deps.doctorRepository.getLatestToken(id, scheduleDate, shiftId, depId, patient.clientId.toString())
            // Only the latest pick counts if the patient tapped another shift meanwhile.
            if (_shiftId.value != shiftId || _slotDate.value != scheduleDate) return@launch
            when (r) {
                is DomainResult.Success -> {
                    _latestToken.value = r.data.firstOrNull()
                    if (_latestToken.value == null) _tokenError.value = "No token is available for this shift. Try another shift or day."
                }
                is DomainResult.Error -> {
                    _tokenError.value = "Couldn't get your token number. Tap the shift to try again."
                    Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
                }
            }
            _tokenLoading.value = false
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

    /** [consentGiven]: the patient ticked the prescription-sharing consent; only then is it recorded. */
    fun bookAppointment(did: String, shiftId: Int, sTime: String, appointmentDate: String, queueNo: Int? = null, consentGiven: Boolean = false) {
        viewModelScope.launch {
            _bookingLoading.value = true; _bookingError.value = null
            val patient = prefs.getPatient() ?: run {
                _bookingError.value = "Your session has expired. Please log in again to book."
                _bookingLoading.value = false
                return@launch
            }
            
            if (consentGiven && _consentList.value.isNotEmpty()) {
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
            
            // Slot bookings send the slot; token bookings send the shift start plus their queue number.
            params["slotTime"] = sTime
            queueNo?.let { params["queueNo"] = it }

            when (val r = deps.bookAppointment()(params)) {
                is DomainResult.Success -> { _visitNo.value = r.data; _bookingSuccess.value = true; Haptics.success() }
                is DomainResult.Error -> {
                    _bookingError.value = (r.exception as? BookingException)?.message
                        ?: "Couldn't book the appointment. Please try again."
                    Haptics.error()
                    Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
                }
            }
            _bookingLoading.value = false
        }
    }
}
