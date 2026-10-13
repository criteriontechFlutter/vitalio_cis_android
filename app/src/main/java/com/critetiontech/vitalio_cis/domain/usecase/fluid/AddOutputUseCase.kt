package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import javax.inject.Inject

class AddOutputUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addOutput(body)
}