package com.critetiontech.ctvitalio.domain.usecase.report

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ReportRepository
import com.critetiontech.ctvitalio.model.MediaItem
import javax.inject.Inject

class FetchMediaUseCase @Inject constructor(private val repository: ReportRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<MediaItem>> =
        repository.fetchMedia(uhid, clientId)
}
