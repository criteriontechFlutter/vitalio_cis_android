package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.FluidSummaryItem
import javax.inject.Inject

class FetchFluidSummaryUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>> =
        repository.fetchFluidSummary(uhid, fromDate, toDate)
}
