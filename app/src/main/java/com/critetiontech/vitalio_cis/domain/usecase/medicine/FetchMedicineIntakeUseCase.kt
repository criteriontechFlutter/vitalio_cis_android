package com.critetiontech.ctvitalio.domain.usecase.medicine

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.MedicineRepository
import com.critetiontech.ctvitalio.model.MedicinePeriod
import javax.inject.Inject

class FetchMedicineIntakeUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>> =
        repository.fetchMedicineIntake(pid, givenDate, clientId)
}