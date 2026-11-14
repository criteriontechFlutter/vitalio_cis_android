package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.AllergyItem

interface AllergyRepository {
    suspend fun fetchAllergies(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>>
    /** POST AddAllergies — [typeAllergy] ("FoodAllergy" | "MedicineAllergy") is a query param per swagger, not a body field. */
    suspend fun addAllergy(typeAllergy: String, body: Map<String, Any>): DomainResult<Unit>
}
