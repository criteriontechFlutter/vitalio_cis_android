package com.critetiontech.ctvitalio.domain.usecase.allergy

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AllergyRepository
import javax.inject.Inject

class AddAllergyUseCase @Inject constructor(private val repository: AllergyRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addAllergy(body)
}