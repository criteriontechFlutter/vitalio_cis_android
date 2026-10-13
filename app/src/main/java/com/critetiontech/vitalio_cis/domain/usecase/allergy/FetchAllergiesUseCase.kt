package com.critetiontech.ctvitalio.domain.usecase.allergy

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AllergyRepository
import com.critetiontech.ctvitalio.model.AllergyItem
import javax.inject.Inject

class FetchAllergiesUseCase @Inject constructor(private val repository: AllergyRepository) {
    suspend operator fun invoke(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>> =
        repository.fetchAllergies(uhid, clientId, typeAllergy)
}