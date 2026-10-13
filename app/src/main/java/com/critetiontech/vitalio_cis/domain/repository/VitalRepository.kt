package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.Vital
import com.critetiontech.ctvitalio.model.VitalGraphEntry

interface VitalRepository {
    suspend fun fetchLastVital(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>>
    suspend fun addVital(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchVitalAnalytics(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>>
}