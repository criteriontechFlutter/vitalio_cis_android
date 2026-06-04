package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.Problem
import com.critetiontech.vitalio_cis.model.SymptomItem

interface SymptomRepository {
    suspend fun fetchSymptoms(uhid: String, clientId: String): DomainResult<List<SymptomItem>>
    suspend fun insertSymptoms(body: Map<String, Any>): DomainResult<Unit>
    suspend fun getProblemsWithIcon(): DomainResult<List<Problem>>
    suspend fun getAllProblems(query: String): DomainResult<List<Problem>>
}
