package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.ShiftData
import javax.inject.Inject

class FetchAvailableSlotsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>> =
        repository.fetchAvailableSlots(doctorId, scheduleDate, clientId)
}