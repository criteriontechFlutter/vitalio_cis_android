package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.IntakeItem
import javax.inject.Inject

class FetchIntakeUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>> =
        repository.fetchIntakeItems(uhid, clientId, fromDate)
}