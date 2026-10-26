package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.SymptomHistoryDay
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import java.time.LocalDate

/** Every recorded symptom grouped by the day it was recorded, newest day first. */
class GetSymptomHistoryUseCase(private val repository: SymptomRepository) {
    suspend operator fun invoke(uhid: String, clientId: String): DomainResult<List<SymptomHistoryDay>> =
        when (val r = repository.fetchSymptoms(uhid, clientId)) {
            is DomainResult.Success -> DomainResult.Success(
                r.data
                    .filter { it.details.isNotBlank() }
                    .sortedByDescending { it.detailsDate }
                    .groupBy { runCatching { LocalDate.parse(it.detailsDate.take(10)) }.getOrNull() }
                    .map { (date, symptoms) -> SymptomHistoryDay(date, symptoms) }
                    // Undated rows go last
                    .sortedWith(compareByDescending<SymptomHistoryDay, LocalDate?>(nullsFirst()) { it.date })
            )
            is DomainResult.Error -> r
        }
}
