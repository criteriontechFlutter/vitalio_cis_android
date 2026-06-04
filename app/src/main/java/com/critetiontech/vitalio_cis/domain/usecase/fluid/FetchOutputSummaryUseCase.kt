package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.OutputSummaryItem
import javax.inject.Inject

class FetchOutputSummaryUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, fromDate: String, toDate: String): DomainResult<List<OutputSummaryItem>> =
        repository.fetchOutputSummary(uhid, clientId, fromDate, toDate)
}