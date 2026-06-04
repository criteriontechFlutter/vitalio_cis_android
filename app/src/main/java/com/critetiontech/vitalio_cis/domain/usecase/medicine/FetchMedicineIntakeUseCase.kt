package com.critetiontech.vitalio_cis.domain.usecase.medicine

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import javax.inject.Inject

class FetchMedicineIntakeUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>> =
        repository.fetchMedicineIntake(pid, givenDate, clientId)
}