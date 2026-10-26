package com.critetiontech.ctvitalio.domain.usecase.familyhistory

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.model.FamilyRelation
import com.critetiontech.ctvitalio.domain.repository.FamilyHealthHistoryRepository

/**
 * Records [relations] as having the disease. Insert appends rows, so for a disease already on record only
 * the relations not saved yet are sent; nothing new counts as done.
 */
class AddFamilyHealthHistoryUseCase(private val repository: FamilyHealthHistoryRepository) {
    suspend operator fun invoke(
        pid: Int,
        clientId: Int,
        diseaseId: Int,
        relations: Collection<FamilyRelation>,
        existing: FamilyHealthCondition?,
    ): DomainResult<Unit> {
        val newIds = relations.filterNot { existing?.relations?.contains(it) == true }.map { it.id }
        return if (newIds.isEmpty()) DomainResult.Success(Unit)
        else repository.addHistory(pid, clientId, diseaseId, newIds)
    }
}
