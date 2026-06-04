package com.critetiontech.vitalio_cis.domain.usecase.report

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import com.critetiontech.vitalio_cis.model.MediaItem
import javax.inject.Inject

class FetchMediaUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<MediaItem>> =
        repository.fetchMedia(uhid, clientId)
}
