package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.OutputItem
import javax.inject.Inject

class FetchOutputUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>> =
        repository.fetchOutput(uhid, clientId, userId, fromDate)
}