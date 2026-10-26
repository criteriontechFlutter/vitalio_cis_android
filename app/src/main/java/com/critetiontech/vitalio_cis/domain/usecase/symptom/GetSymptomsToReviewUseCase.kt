package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.SymptomToReview
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository

/**
 * Symptoms the patient still has, one question each (newest first). The server lists a symptom once
 * per visit; all of those rows are kept on the question so the answer reaches each of them.
 */
class GetSymptomsToReviewUseCase(private val repository: SymptomRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<SymptomToReview>> =
        when (val r = repository.fetchSymptoms(uhid, clientId)) {
            is DomainResult.Success -> DomainResult.Success(
                r.data
                    .filter { it.details.isNotBlank() && it.isStillFeeling }
                    // ISO dates sort correctly as text
                    .sortedByDescending { it.detailsDate }
                    .groupBy { it.detailId }
                    .map { (detailId, rows) -> SymptomToReview(detailId, rows.first().details.trim(), rows) }
            )
            is DomainResult.Error -> r
        }
}
