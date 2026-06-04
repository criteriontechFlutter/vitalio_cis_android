package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.DoctorDetails
import javax.inject.Inject

class FetchDoctorProfileUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String, doctorId: String): DomainResult<DoctorDetails?> =
        repository.fetchDoctorProfile(clientId, doctorId)
}
