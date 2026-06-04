package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.model.VitalGraphEntry

interface VitalRepository {
    suspend fun fetchLastVital(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>>
    suspend fun addVital(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchVitalAnalytics(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>>
}