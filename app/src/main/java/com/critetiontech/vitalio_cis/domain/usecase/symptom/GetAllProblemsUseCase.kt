package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import com.critetiontech.ctvitalio.model.Problem
import javax.inject.Inject

class GetAllProblemsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(query: String): DomainResult<List<Problem>> =
        repository.getAllProblems(query)
}