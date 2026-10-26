package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.EmergencyContact

interface EmergencyContactRepository {
    suspend fun fetchContacts(pid: Int, clientId: Int): DomainResult<List<EmergencyContact>>
    /** Inserts when [contact] has no id yet, otherwise updates it. */
    suspend fun saveContact(contact: EmergencyContact): DomainResult<Unit>
    suspend fun removeContact(id: Int): DomainResult<Unit>
}
