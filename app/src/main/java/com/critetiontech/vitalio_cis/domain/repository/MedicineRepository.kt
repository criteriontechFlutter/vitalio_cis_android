package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.InsertMedicineRequest
import com.critetiontech.vitalio_cis.model.InsertMedicineResponse
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import com.critetiontech.vitalio_cis.model.SearchMedicineItem
import com.critetiontech.vitalio_cis.model.DrugInteractionItem

interface MedicineRepository {
    suspend fun fetchMedicineIntake(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>>
    suspend fun markIntake(id: Int, scheduledDateTime: String): DomainResult<Unit>
    suspend fun fetchMedicineByDate(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>>
    suspend fun searchMedicine(alphabet: String): DomainResult<List<SearchMedicineItem>>
    suspend fun insertMedicine(request: InsertMedicineRequest): DomainResult<InsertMedicineResponse>
    suspend fun getDrugInteraction(brandIds: String): DomainResult<List<DrugInteractionItem>>
}
