package com.critetiontech.vitalio_cis.domain.usecase.report

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import javax.inject.Inject

class InsertInvestigationResultUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(
        responseJson: String,
        uhid: String,
        clientId: Int
    ): DomainResult<Unit> = repository.insertInvestigationResult(responseJson, uhid, clientId)
}
