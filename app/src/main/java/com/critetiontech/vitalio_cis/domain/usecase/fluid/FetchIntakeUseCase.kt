package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.IntakeItem
import javax.inject.Inject

class FetchIntakeUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>> =
        repository.fetchIntakeItems(uhid, clientId, fromDate)
}