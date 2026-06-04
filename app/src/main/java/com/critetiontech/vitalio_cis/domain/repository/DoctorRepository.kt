package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.model.ShiftData

interface DoctorRepository {
    suspend fun fetchDoctors(clientId: String): DomainResult<List<Doctor>>
    suspend fun fetchDoctorProfile(clientId: String, doctorId: String): DomainResult<DoctorDetails?>
    suspend fun fetchAvailableSlots(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>>
    suspend fun bookAppointment(params: Map<String, Any>): DomainResult<Unit>
}
