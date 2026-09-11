package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AppointmentHistoryItem
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.model.ShiftData
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentItem
import com.critetiontech.vitalio_cis.model.Department

interface DoctorRepository {
    suspend fun fetchDoctors(clientId: String, departmentId: String = "0"): DomainResult<List<Doctor>>
    suspend fun fetchDepartments(clientId: String): DomainResult<List<Department>>
    suspend fun fetchDoctorProfile(clientId: String, doctorId: String): DomainResult<DoctorDetails?>
    suspend fun fetchAvailableSlots(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>>
    suspend fun bookAppointment(params: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchUpcomingAppointments(pid: String, clientId: String): DomainResult<List<UpcomingAppointmentItem>>
    suspend fun fetchAppointmentHistory(pid: String, clientId: String, userId: String): DomainResult<List<AppointmentHistoryItem>>
    
    // Token & Slot booking additions
    suspend fun fetchShiftTypeSetting(doctorId: String, clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.ShiftTypeSetting>>
    suspend fun fetchDoctorShiftConfig(doctorId: String, clientId: String): DomainResult<com.critetiontech.vitalio_cis.model.Doctor?>
    suspend fun fetchSchedule(doctorId: String, clientId: String, dayId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.DoctorSchedule>>
    suspend fun getLatestToken(doctorId: String, scheduleDate: String, shiftId: String, departmentId: String, clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.LatestToken>>
    suspend fun fetchConsent(clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.ConsentItem>>
    suspend fun addConsent(params: Map<String, Any>): DomainResult<String?>
}
