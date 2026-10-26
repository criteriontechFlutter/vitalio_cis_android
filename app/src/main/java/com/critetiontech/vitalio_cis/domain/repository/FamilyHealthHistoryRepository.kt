package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.Disease
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.FamilyHealthHistoryRow

interface FamilyHealthHistoryRepository {
    suspend fun fetchHistory(pid: Int, clientId: Int): DomainResult<List<FamilyHealthHistoryRow>>
    suspend fun fetchDiseases(): DomainResult<List<Disease>>
    suspend fun addHistory(pid: Int, clientId: Int, diseaseId: Int, relationIds: List<Int>): DomainResult<Unit>
    suspend fun removeHistory(id: Int, clientId: Int, userId: Int): DomainResult<Unit>
}
