package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import javax.inject.Inject

class BookAppointmentUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(params: Map<String, Any>): DomainResult<Unit> =
        repository.bookAppointment(params)
}