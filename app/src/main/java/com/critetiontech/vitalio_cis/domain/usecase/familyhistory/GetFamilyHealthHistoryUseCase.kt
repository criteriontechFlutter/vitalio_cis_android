package com.critetiontech.ctvitalio.domain.usecase.familyhistory

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.model.FamilyRelation
import com.critetiontech.ctvitalio.domain.repository.FamilyHealthHistoryRepository

/**
 * The server sends a row per disease + relation (and repeats a relation if it was added twice); this
 * folds them into one entry per disease. Names come from the disease list, as the rows only carry ids.
 */
class GetFamilyHealthHistoryUseCase(private val repository: FamilyHealthHistoryRepository) {
    suspend operator fun invoke(pid: Int, clientId: Int): DomainResult<List<FamilyHealthCondition>> {
        val rows = when (val r = repository.fetchHistory(pid, clientId)) {
            is DomainResult.Success -> r.data
            is DomainResult.Error -> return r
        }
        // Without the disease list the entries still show, just without a name.
        val names = (repository.fetchDiseases() as? DomainResult.Success)?.data?.associate { it.id to it.name }.orEmpty()
        val conditions = rows
            .groupBy { it.diseaseId }
            .map { (diseaseId, group) ->
                FamilyHealthCondition(
                    diseaseId = diseaseId,
                    diseaseName = group.firstNotNullOfOrNull { it.diseaseName?.trim()?.takeIf(String::isNotEmpty) }
                        ?: names[diseaseId] ?: "Unknown condition",
                    relations = group.mapNotNull { FamilyRelation.fromId(it.relationId) }.distinct().sortedBy { it.ordinal },
                    recordId = group.firstNotNullOfOrNull { it.id },
                )
            }
            .filter { it.relations.isNotEmpty() }
        return DomainResult.Success(conditions)
    }
}
