package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.Doctor
import javax.inject.Inject

class FetchDoctorsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String): DomainResult<List<Doctor>> =
        repository.fetchDoctors(clientId)
}
