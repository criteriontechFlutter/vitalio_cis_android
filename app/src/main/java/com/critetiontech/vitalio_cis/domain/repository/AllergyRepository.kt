package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.AllergyItem

interface AllergyRepository {
    suspend fun fetchAllergies(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>>
    suspend fun addAllergy(body: Map<String, Any>): DomainResult<Unit>
}
