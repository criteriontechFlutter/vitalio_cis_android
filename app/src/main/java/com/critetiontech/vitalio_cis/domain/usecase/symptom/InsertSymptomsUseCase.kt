package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import javax.inject.Inject

class InsertSymptomsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> =
        repository.insertSymptoms(body)
}