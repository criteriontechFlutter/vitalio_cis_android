package com.critetiontech.vitalio_cis.domain.usecase.allergy

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AllergyRepository
import javax.inject.Inject

class AddAllergyUseCase @Inject constructor(private val repository: AllergyRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addAllergy(body)
}