package com.critetiontech.vitalio_cis.domain.usecase.report

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import java.io.File
import javax.inject.Inject

class UploadMediaUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, category: String, dateTime: String, subCategory: String, remark: String, file: File): DomainResult<Unit> =
        repository.uploadMedia(uhid, clientId, category, dateTime, subCategory, remark, file)
}