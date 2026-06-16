package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentItem
import javax.inject.Inject

class FetchUpcomingAppointmentsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(pid: String, clientId: String): DomainResult<List<UpcomingAppointmentItem>> =
        repository.fetchUpcomingAppointments(pid, clientId)
}