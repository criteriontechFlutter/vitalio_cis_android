package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import javax.inject.Inject

class BookAppointmentUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(params: Map<String, Any>): DomainResult<Unit> =
        repository.bookAppointment(params)
}