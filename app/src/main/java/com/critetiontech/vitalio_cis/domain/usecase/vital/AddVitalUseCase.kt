package com.critetiontech.ctvitalio.domain.usecase.vital

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.VitalRepository
import javax.inject.Inject

class AddVitalUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addVital(body)
}