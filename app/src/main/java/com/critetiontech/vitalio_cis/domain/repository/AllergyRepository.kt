package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllergyItem

interface AllergyRepository {
    suspend fun fetchAllergies(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>>
    suspend fun addAllergy(body: Map<String, Any>): DomainResult<Unit>
}
