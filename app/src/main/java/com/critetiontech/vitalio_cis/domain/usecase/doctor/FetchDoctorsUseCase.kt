package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.Doctor
import javax.inject.Inject

class FetchDoctorsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String, departmentId: String = "0"): DomainResult<List<Doctor>> =
        repository.fetchDoctors(clientId, departmentId)
}
