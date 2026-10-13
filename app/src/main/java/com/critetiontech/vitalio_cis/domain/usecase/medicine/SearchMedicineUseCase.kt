package com.critetiontech.ctvitalio.domain.usecase.medicine

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.MedicineRepository
import com.critetiontech.ctvitalio.model.SearchMedicineItem
import javax.inject.Inject

class SearchMedicineUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(alphabet: String): DomainResult<List<SearchMedicineItem>> =
        repository.searchMedicine(alphabet)
}
