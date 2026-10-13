package com.critetiontech.ctvitalio.domain.usecase.medicine

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.MedicineRepository
import com.critetiontech.ctvitalio.model.InsertMedicineRequest
import com.critetiontech.ctvitalio.model.InsertMedicineResponse
import javax.inject.Inject

class InsertMedicineUseCase @Inject constructor(private val repository: MedicineRepository) {
    suspend operator fun invoke(request: InsertMedicineRequest): DomainResult<InsertMedicineResponse> =
        repository.insertMedicine(request)
}
