package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.FluidSummaryItem
import com.critetiontech.vitalio_cis.model.IntakeItem
import com.critetiontech.vitalio_cis.model.ManualFoodAssignItem
import com.critetiontech.vitalio_cis.model.OutputItem
import com.critetiontech.vitalio_cis.model.OutputSummaryItem

interface FluidRepository {
    suspend fun getManualFoodList(uhid: String): DomainResult<List<ManualFoodAssignItem>>
    suspend fun fetchIntakeItems(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>>
    suspend fun addIntake(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchOutput(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>>
    suspend fun addOutput(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchFluidSummary(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>>
    suspend fun fetchOutputSummary(uhid: String, clientId: String, fromDate: String, toDate: String): DomainResult<List<OutputSummaryItem>>
}
