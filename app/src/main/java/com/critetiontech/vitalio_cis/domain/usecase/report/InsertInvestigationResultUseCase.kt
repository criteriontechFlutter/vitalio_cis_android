package com.critetiontech.ctvitalio.domain.usecase.report

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ReportRepository
import javax.inject.Inject

class InsertInvestigationResultUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(
        responseJson: String,
        uhid: String,
        clientId: Int,
        testName: String,
        dateTime: String,
    ): DomainResult<Unit> = repository.insertInvestigationResult(responseJson, uhid, clientId, testName, dateTime)
}
