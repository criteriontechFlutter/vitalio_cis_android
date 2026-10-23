package com.critetiontech.ctvitalio.domain.usecase.consultation

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ConsultationRepository

class DeclineCallUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(roomName: String, patientUserId: String, reason: String): DomainResult<Unit> =
        repository.decline(roomName, patientUserId, reason)
}
