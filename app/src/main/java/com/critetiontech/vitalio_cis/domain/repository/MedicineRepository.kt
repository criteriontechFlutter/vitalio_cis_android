package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.AllMedicine
import com.critetiontech.ctvitalio.model.InsertMedicineRequest
import com.critetiontech.ctvitalio.model.InsertMedicineResponse
import com.critetiontech.ctvitalio.model.LoggedMedicine
import com.critetiontech.ctvitalio.model.MedicinePeriod
import com.critetiontech.ctvitalio.model.SearchMedicineItem
import com.critetiontech.ctvitalio.model.DrugInteractionItem

interface MedicineRepository {
    suspend fun fetchMedicineIntake(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>>
    suspend fun markIntake(id: Int, scheduledDateTime: String): DomainResult<Unit>
    suspend fun fetchMedicineByDate(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>>
    suspend fun searchMedicine(alphabet: String): DomainResult<List<SearchMedicineItem>>
    suspend fun insertMedicine(request: InsertMedicineRequest): DomainResult<InsertMedicineResponse>
    suspend fun getDrugInteraction(brandIds: String): DomainResult<List<DrugInteractionItem>>
}
