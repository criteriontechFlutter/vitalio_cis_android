package com.critetiontech.vitalio_cis.domain.usecase.medicine

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import javax.inject.Inject

class FetchMedicineByDateUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>> =
        repository.fetchMedicineByDate(pid, givenDate, clientId)
}
