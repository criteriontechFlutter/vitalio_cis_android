package com.critetiontech.vitalio_cis.domain.usecase.doctor

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.Department
import javax.inject.Inject

class FetchDepartmentsUseCase @Inject constructor(private val repository: DoctorRepository) {
    suspend operator fun invoke(clientId: String): DomainResult<List<Department>> =
        repository.fetchDepartments(clientId)
}
