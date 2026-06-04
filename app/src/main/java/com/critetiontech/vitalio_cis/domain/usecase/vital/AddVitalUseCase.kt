package com.critetiontech.vitalio_cis.domain.usecase.vital

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import javax.inject.Inject

class AddVitalUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addVital(body)
}