package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import com.critetiontech.ctvitalio.model.SymptomItem
import javax.inject.Inject

class FetchSymptomsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<SymptomItem>> =
        repository.fetchSymptoms(uhid, clientId)
}