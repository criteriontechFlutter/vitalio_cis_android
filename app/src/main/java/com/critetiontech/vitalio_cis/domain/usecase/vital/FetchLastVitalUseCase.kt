package com.critetiontech.ctvitalio.domain.usecase.vital

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.VitalRepository
import com.critetiontech.ctvitalio.model.Vital
import javax.inject.Inject

class FetchLastVitalUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>> =
        repository.fetchLastVital(uhid, clientId, userId)
}