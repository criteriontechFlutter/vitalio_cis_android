package com.critetiontech.ctvitalio.domain.usecase.report

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject

class AiReportUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(file: File): DomainResult<String> = repository.analyzeReport(file)
}