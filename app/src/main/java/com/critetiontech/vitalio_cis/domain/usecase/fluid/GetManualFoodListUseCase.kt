package com.critetiontech.ctvitalio.domain.usecase.fluid

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.ManualFoodAssignItem
import javax.inject.Inject

class GetManualFoodListUseCase @Inject constructor(private val repository: FluidRepository) {
    suspend operator fun invoke(uhid: String): DomainResult<List<ManualFoodAssignItem>> =
        repository.getManualFoodList(uhid)
}