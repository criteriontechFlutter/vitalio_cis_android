package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.CallJoin

/** Video consultation: the doctor rings the patient, who joins, declines or ends the call. */
interface ConsultationRepository {
    suspend fun join(roomName: String, userId: String, displayName: String, avatarUrl: String?): DomainResult<CallJoin>
    suspend fun decline(roomName: String, patientUserId: String, reason: String): DomainResult<Unit>
    suspend fun end(roomName: String, endedByUserId: String, reason: String): DomainResult<Unit>
}
