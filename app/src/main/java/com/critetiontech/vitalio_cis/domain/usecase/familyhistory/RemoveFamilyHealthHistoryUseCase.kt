package com.critetiontech.ctvitalio.domain.usecase.familyhistory

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.repository.FamilyHealthHistoryRepository

class RemoveFamilyHealthHistoryUseCase(private val repository: FamilyHealthHistoryRepository) {
    suspend operator fun invoke(condition: FamilyHealthCondition, pid: Int, clientId: Int): DomainResult<Unit> {
        // The delete API takes the record id, which GetPatientFamilyHealthHistory doesn't return yet.
        val id = condition.recordId
            ?: return DomainResult.Error(IllegalStateException("No record id for disease ${condition.diseaseId}"))
        return repository.removeHistory(id, clientId, userId = pid)
    }
}
