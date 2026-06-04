package com.critetiontech.vitalio_cis.domain.usecase.vital

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import com.critetiontech.vitalio_cis.model.Vital
import javax.inject.Inject

class FetchLastVitalUseCase @Inject constructor(private val repository: VitalRepository) {
    suspend operator fun invoke(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>> =
        repository.fetchLastVital(uhid, clientId, userId)
}