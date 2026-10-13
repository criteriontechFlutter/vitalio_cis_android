package com.critetiontech.ctvitalio.domain.usecase.vital

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.VitalRepository
import com.critetiontech.ctvitalio.model.VitalGraphEntry
import javax.inject.Inject

class FetchVitalAnalyticsUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>> =
        repository.fetchVitalAnalytics(params)
}