package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.HealthShareCreated
import com.critetiontech.ctvitalio.model.HealthShareLink

interface HealthShareRepository {
    suspend fun fetchLinks(pid: Int, clientId: Int): DomainResult<List<HealthShareLink>>
    /** Emails [recipientEmail] a link to the patient's health records and an access code to open it. */
    suspend fun createLink(uhid: String, pid: Int, userId: Int, clientId: Int, recipientEmail: String, patientName: String): DomainResult<HealthShareCreated>
    suspend fun revokeLink(id: Int, clientId: Int): DomainResult<Unit>
}
