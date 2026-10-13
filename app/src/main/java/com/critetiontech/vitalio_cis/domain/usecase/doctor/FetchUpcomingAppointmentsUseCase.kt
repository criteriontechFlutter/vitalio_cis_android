package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.UpcomingAppointmentItem
import javax.inject.Inject

class FetchUpcomingAppointmentsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(pid: String, clientId: String): DomainResult<List<UpcomingAppointmentItem>> =
        repository.fetchUpcomingAppointments(pid, clientId)
}