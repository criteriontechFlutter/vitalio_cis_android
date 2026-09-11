package com.critetiontech.vitalio_cis.domain.usecase.medicine

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.SearchMedicineItem
import javax.inject.Inject

class SearchMedicineUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(alphabet: String): DomainResult<List<SearchMedicineItem>> =
        repository.searchMedicine(alphabet)
}
