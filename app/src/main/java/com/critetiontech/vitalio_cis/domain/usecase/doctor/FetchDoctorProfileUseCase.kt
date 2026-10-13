package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.DoctorDetails
import javax.inject.Inject

class FetchDoctorProfileUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String, doctorId: String): DomainResult<DoctorDetails?> =
        repository.fetchDoctorProfile(clientId, doctorId)
}
