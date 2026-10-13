package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.Problem
import com.critetiontech.ctvitalio.model.SymptomItem

interface SymptomRepository {
    suspend fun fetchSymptoms(uhid: String, clientId: String): DomainResult<List<SymptomItem>>
    suspend fun insertSymptoms(body: Map<String, Any>): DomainResult<Unit>
    suspend fun getProblemsWithIcon(): DomainResult<List<Problem>>
    suspend fun getAllProblems(query: String): DomainResult<List<Problem>>
}
