package com.critetiontech.vitalio_cis.domain.usecase.vital

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import javax.inject.Inject

class FetchVitalAnalyticsUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>> =
        repository.fetchVitalAnalytics(params)
}