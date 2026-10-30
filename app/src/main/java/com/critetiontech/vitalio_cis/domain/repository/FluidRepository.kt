package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.FluidSummaryItem
import com.critetiontech.ctvitalio.model.IntakeItem
import com.critetiontech.ctvitalio.model.ManualFoodAssignItem
import com.critetiontech.ctvitalio.model.OutputItem
import com.critetiontech.ctvitalio.model.OutputSummaryItem

interface FluidRepository {
    suspend fun getManualFoodList(uhid: String): DomainResult<List<ManualFoodAssignItem>>
    suspend fun fetchIntakeItems(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>>

    // Last saved responses, read straight from local cache (no network) so the UI can render
    // instantly while the slow (~20 s) server call refreshes in the background.
    fun cachedManualFoodList(uhid: String): List<ManualFoodAssignItem>
    fun cachedIntakeItems(uhid: String, fromDate: String): List<IntakeItem>
    suspend fun addIntake(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchOutput(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>>
    suspend fun addOutput(body: Map<String, Any>): DomainResult<Unit>
    suspend fun fetchFluidSummary(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>>
    suspend fun fetchOutputSummary(uhid: String, clientId: String, fromDate: String, toDate: String): DomainResult<List<OutputSummaryItem>>
}
