package com.critetiontech.ctvitalio.domain.usecase.symptom

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import com.critetiontech.ctvitalio.model.SymptomItem

/** Saves the patient's answers or new picks; nothing to save counts as done. */
class SaveSymptomsUseCase(private val repository: SymptomRepository) {
    suspend operator fun invoke(uhid: String, clientId: Int, symptoms: List<SymptomItem>): DomainResult<Unit> =
        if (symptoms.isEmpty()) DomainResult.Success(Unit)
        else repository.saveSymptoms(uhid, clientId, symptoms)
}
