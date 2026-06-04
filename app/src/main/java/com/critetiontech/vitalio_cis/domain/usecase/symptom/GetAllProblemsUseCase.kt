package com.critetiontech.vitalio_cis.domain.usecase.symptom

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.SymptomRepository
import com.critetiontech.vitalio_cis.model.Problem
import javax.inject.Inject

class GetAllProblemsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(query: String): DomainResult<List<Problem>> =
        repository.getAllProblems(query)
}