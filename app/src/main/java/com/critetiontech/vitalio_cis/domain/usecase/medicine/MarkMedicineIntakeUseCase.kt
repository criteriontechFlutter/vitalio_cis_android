package com.critetiontech.ctvitalio.domain.usecase.medicine

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.MedicineRepository
import javax.inject.Inject
class MarkMedicineIntakeUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(id: Int, scheduledDateTime: String): DomainResult<Unit> =
        repository.markIntake(id, scheduledDateTime)
}