package com.critetiontech.vitalio_cis.domain.usecase.fluid

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.ManualFoodAssignItem
import javax.inject.Inject

class GetManualFoodListUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String): DomainResult<List<ManualFoodAssignItem>> =
        repository.getManualFoodList(uhid)
}