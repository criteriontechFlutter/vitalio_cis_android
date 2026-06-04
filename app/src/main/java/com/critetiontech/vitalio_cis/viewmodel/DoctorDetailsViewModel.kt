package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.model.ShiftData
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
    val doctorId: androidx.lifecycle.LiveData<String> = _doctorId

    private val _slotTime = MutableLiveData("")
    val slotTime: androidx.lifecycle.LiveData<String> = _slotTime

    private val _slotDate = MutableLiveData("")
    val slotDate: androidx.lifecycle.LiveData<String> = _slotDate

    private val _shiftId = MutableLiveData("")
    val shiftId: androidx.lifecycle.LiveData<String> = _shiftId

    private val _departmentId = MutableLiveData("")
    val departmentId: androidx.lifecycle.LiveData<String> = _departmentId

    private val _doctor = MutableStateFlow<DoctorDetails?>(null)
    val doctor: StateFlow<DoctorDetails?> = _doctor

    private val _slotList = MutableStateFlow<List<ShiftData>>(emptyList())
    val slotList: StateFlow<List<ShiftData>> = _slotList

    fun setDoctorId(id: String) { _doctorId.value = id }
    fun setSlotTime(value: String) { _slotTime.value = value }
    fun setSlotDate(value: String) { _slotDate.value = value }
    fun setShiftId(value: String) { _shiftId.value = value }
    fun setDepartmentId(value: String) { _departmentId.value = value }
    fun resetBookingState() { _bookingSuccess.value = false; _bookingError.value = null }

    fun getDoctorProfile() {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchDoctorProfile()(patient.clientId.toString(), _doctorId.value.orEmpty())) {
                is DomainResult.Success -> _doctor.value = r.data
                is DomainResult.Error -> Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun fetchAvailableSlots(scheduleDate: String) {
        viewModelScope.launch {
            _slotList.value = emptyList(); _loading.value = true
            val patient = prefs.getPatient() ?: run { _loading.value = false; return@launch }
            when (val r = deps.fetchAvailableSlots()(_doctorId.value.orEmpty(), scheduleDate, patient.clientId.toString())) {
                is DomainResult.Success -> _slotList.value = r.data
                is DomainResult.Error -> Log.e("DoctorDetailsVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    fun bookAppointment(did: String, sTime: String, appointmentDate: String) {
        viewModelScope.launch {
            _bookingLoading.value = true; _bookingError.value = null
            val patient = prefs.getPatient() ?: run { _bookingLoading.value = false; return@launch }
            val params = mapOf<String, Any>("departmentId" to 3, "doctorId" to did, "uhId" to patient.uhId, "isOnline" to true, "clientId" to patient.clientId.toString(), "shiftId" to 6, "slotTime" to sTime, "appointmentDate" to appointmentDate)
            when (val r = deps.bookAppointment()(params)) {
                is DomainResult.Success -> _bookingSuccess.value = true
                is DomainResult.Error -> { _bookingError.value = "Booking failed."; Log.e("DoctorDetailsVM", r.exception.message.orEmpty()) }
            }
            _bookingLoading.value = false
        }
    }
}
