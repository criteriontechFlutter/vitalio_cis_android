package com.critetiontech.ctvitalio.domain.usecase.doctor

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DoctorRepository
import com.critetiontech.ctvitalio.model.Department
import javax.inject.Inject

class FetchDepartmentsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String): DomainResult<List<Department>> =
        repository.fetchDepartments(clientId)
}
