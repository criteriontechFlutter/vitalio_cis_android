package com.critetiontech.ctvitalio.domain.usecase.familyhistory

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.repository.FamilyHealthHistoryRepository

class RemoveFamilyHealthHistoryUseCase(private val repository: FamilyHealthHistoryRepository) {
    /**
     * The server keeps one row per disease + relation, and the delete API removes one row by id, so every
     * row behind the disease is deleted. Stops at the first failure.
     */
    suspend operator fun invoke(condition: FamilyHealthCondition, pid: Int, clientId: Int): DomainResult<Unit> {
        // The delete API needs record ids, which GetPatientFamilyHealthHistory doesn't return yet.
        if (condition.recordIds.isEmpty()) {
            return DomainResult.Error(IllegalStateException("No record id for disease ${condition.diseaseId}"))
        }
        for (id in condition.recordIds) {
            val r = repository.removeHistory(id, clientId, userId = pid)
            if (r is DomainResult.Error) return r
        }
        return DomainResult.Success(Unit)
    }
}
