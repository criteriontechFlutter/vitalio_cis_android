package com.critetiontech.ctvitalio.domain.usecase.report

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ReportRepository
import com.critetiontech.ctvitalio.model.PrescriptionResponseItem
import java.io.File
import javax.inject.Inject

class UploadPrescriptionUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(file: File, mimeType: String): DomainResult<PrescriptionResponseItem> =
        repository.uploadPrescription(file, mimeType)
}