package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.MediaItem
import com.critetiontech.ctvitalio.model.PrescriptionResponseItem
import java.io.File

interface ReportRepository {
    suspend fun fetchMedia(uhid: String, clientId: String): DomainResult<List<MediaItem>>
    suspend fun uploadMedia(uhid: String, clientId: String, category: String, dateTime: String, subCategory: String, remark: String, file: File): DomainResult<Unit>
    suspend fun analyzeReport(file: File): DomainResult<String>
    suspend fun insertInvestigationResult(responseJson: String, uhid: String, clientId: Int): DomainResult<Unit>
    suspend fun uploadPrescription(file: File, mimeType: String): DomainResult<PrescriptionResponseItem>
}