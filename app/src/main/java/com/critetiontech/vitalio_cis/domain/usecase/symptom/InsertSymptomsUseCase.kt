package com.critetiontech.vitalio_cis.domain.usecase.symptom

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.SymptomRepository
import javax.inject.Inject

class InsertSymptomsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> =
        repository.insertSymptoms(body)
}