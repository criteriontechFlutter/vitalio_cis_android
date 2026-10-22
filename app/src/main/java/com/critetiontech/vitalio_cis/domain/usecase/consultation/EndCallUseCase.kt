package com.critetiontech.ctvitalio.domain.usecase.consultation

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ConsultationRepository

class EndCallUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(roomName: String, endedByUserId: String, reason: String): DomainResult<Unit> =
        repository.end(roomName, endedByUserId, reason)
}
