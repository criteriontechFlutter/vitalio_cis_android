package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.MediaItem
import java.io.File

interface ReportRepository {
    suspend fun fetchMedia(uhid: String, clientId: String): DomainResult<List<MediaItem>>
    suspend fun uploadMedia(uhid: String, clientId: String, category: String, dateTime: String, subCategory: String, remark: String, file: File): DomainResult<Unit>
    suspend fun analyzeReport(file: File): DomainResult<String>
}