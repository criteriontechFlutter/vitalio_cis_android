package com.critetiontech.vitalio_cis.domain.usecase.medicine

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.InsertMedicineRequest
import com.critetiontech.vitalio_cis.model.InsertMedicineResponse
import javax.inject.Inject

class InsertMedicineUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(request: InsertMedicineRequest): DomainResult<InsertMedicineResponse> =
        repository.insertMedicine(request)
}
