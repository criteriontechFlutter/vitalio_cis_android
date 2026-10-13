package com.critetiontech.ctvitalio.domain.usecase.medicine

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.MedicineRepository
import com.critetiontech.ctvitalio.model.AllMedicine
import com.critetiontech.ctvitalio.model.LoggedMedicine
import javax.inject.Inject

class FetchMedicineByDateUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>> =
        repository.fetchMedicineByDate(pid, givenDate, clientId)
}
