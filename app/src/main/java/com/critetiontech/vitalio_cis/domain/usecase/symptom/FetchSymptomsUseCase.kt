package com.critetiontech.vitalio_cis.domain.usecase.symptom

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.SymptomRepository
import com.critetiontech.vitalio_cis.model.SymptomItem
import javax.inject.Inject

class FetchSymptomsUseCase @Inject constructor(private val repository: SymptomRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<SymptomItem>> =
        repository.fetchSymptoms(uhid, clientId)
}