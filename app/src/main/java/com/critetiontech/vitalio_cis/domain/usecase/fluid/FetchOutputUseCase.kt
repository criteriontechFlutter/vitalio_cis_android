package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.OutputItem
import javax.inject.Inject

class FetchOutputUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>> =
        repository.fetchOutput(uhid, clientId, userId, fromDate)
}