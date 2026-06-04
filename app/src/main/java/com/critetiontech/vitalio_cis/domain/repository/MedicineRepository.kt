package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.model.MedicinePeriod

interface MedicineRepository {
    suspend fun fetchMedicineIntake(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>>
    suspend fun markIntake(id: Int, scheduledDateTime: String): DomainResult<Unit>
    suspend fun fetchMedicineByDate(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>>
}
