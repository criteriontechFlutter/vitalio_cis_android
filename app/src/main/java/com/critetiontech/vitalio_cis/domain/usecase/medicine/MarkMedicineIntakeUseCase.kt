package com.critetiontech.vitalio_cis.domain.usecase.medicine

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import javax.inject.Inject

class MarkMedicineIntakeUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(id: Int, scheduledDateTime: String): DomainResult<Unit> =
        repository.markIntake(id, scheduledDateTime)
}