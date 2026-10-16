package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.AppointmentHistoryItem
import com.critetiontech.ctvitalio.model.Doctor
import com.critetiontech.ctvitalio.model.DoctorDetails
import com.critetiontech.ctvitalio.model.ShiftData
import com.critetiontech.ctvitalio.model.UpcomingAppointmentItem
import com.critetiontech.ctvitalio.model.Department

/** A booking that didn't go through; [message] is ready to show the patient. */
class BookingException(message: String) : Exception(message)

interface DoctorRepository {
    suspend fun fetchDoctors(clientId: String, departmentId: String = "0"): DomainResult<List<Doctor>>
    suspend fun fetchDepartments(clientId: String): DomainResult<List<Department>>
    suspend fun fetchDoctorProfile(clientId: String, doctorId: String): DomainResult<DoctorDetails?>
    suspend fun fetchAvailableSlots(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>>
    /**
     * Books the appointment; succeeds with its visit number (e.g. "VNO2604500640") when the server sends one.
     * Fails with [BookingException] carrying a patient-facing reason (server refusal, no internet, …).
     */
    suspend fun bookAppointment(params: Map<String, Any>): DomainResult<String?>
    suspend fun fetchUpcomingAppointments(pid: String, clientId: String): DomainResult<List<UpcomingAppointmentItem>>
    suspend fun fetchAppointmentHistory(pid: String, clientId: String, userId: String): DomainResult<List<AppointmentHistoryItem>>
    
    // Token & Slot booking additions
    suspend fun fetchShiftTypeSetting(doctorId: String, clientId: String): DomainResult<List<com.critetiontech.ctvitalio.model.ShiftTypeSetting>>
    suspend fun fetchDoctorShiftConfig(doctorId: String, clientId: String): DomainResult<com.critetiontech.ctvitalio.model.Doctor?>
    suspend fun fetchSchedule(doctorId: String, clientId: String, dayId: String): DomainResult<List<com.critetiontech.ctvitalio.model.DoctorSchedule>>
    suspend fun getLatestToken(doctorId: String, scheduleDate: String, shiftId: String, departmentId: String, clientId: String): DomainResult<List<com.critetiontech.ctvitalio.model.LatestToken>>
    suspend fun fetchConsent(clientId: String): DomainResult<List<com.critetiontech.ctvitalio.model.ConsentItem>>
    suspend fun addConsent(params: Map<String, Any>): DomainResult<String?>
}
