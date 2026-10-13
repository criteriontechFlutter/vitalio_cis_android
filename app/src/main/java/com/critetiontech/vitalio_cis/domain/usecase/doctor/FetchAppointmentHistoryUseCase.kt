package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.AppointmentHistoryItem
import javax.inject.Inject

class FetchAppointmentHistoryUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(pid: String, clientId: String, userId: String = "0"): DomainResult<List<AppointmentHistoryItem>> =
        repository.fetchAppointmentHistory(pid, clientId, userId)
}