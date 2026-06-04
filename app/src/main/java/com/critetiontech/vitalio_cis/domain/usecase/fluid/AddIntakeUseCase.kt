package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import javax.inject.Inject

class AddIntakeUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(body: Map<String, Any>): DomainResult<Unit> = repository.addIntake(body)
}