package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.OutputSummaryItem
import javax.inject.Inject

class FetchOutputSummaryUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, fromDate: String, toDate: String): DomainResult<List<OutputSummaryItem>> =
        repository.fetchOutputSummary(uhid, clientId, fromDate, toDate)
}