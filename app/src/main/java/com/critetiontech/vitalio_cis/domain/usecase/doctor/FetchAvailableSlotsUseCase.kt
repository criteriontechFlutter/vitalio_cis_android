package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.ShiftData
import javax.inject.Inject

class FetchAvailableSlotsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>> =
        repository.fetchAvailableSlots(doctorId, scheduleDate, clientId)
}