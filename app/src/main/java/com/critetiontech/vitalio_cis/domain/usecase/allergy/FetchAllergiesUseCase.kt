package com.critetiontech.vitalio_cis.domain.usecase.allergy

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AllergyRepository
import com.critetiontech.vitalio_cis.model.AllergyItem
import javax.inject.Inject

class FetchAllergiesUseCase @Inject constructor(private val repository: AllergyRepository) {
    suspend operator fun invoke(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>> =
        repository.fetchAllergies(uhid, clientId, typeAllergy)
}