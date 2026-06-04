package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.FluidSummaryItem
import javax.inject.Inject

class FetchFluidSummaryUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>> =
        repository.fetchFluidSummary(uhid, fromDate, toDate)
}
