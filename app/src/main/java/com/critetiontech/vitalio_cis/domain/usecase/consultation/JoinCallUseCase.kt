package com.critetiontech.ctvitalio.domain.usecase.consultation

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ConsultationRepository
import com.critetiontech.ctvitalio.model.CallJoin

class JoinCallUseCase(private val repository: ConsultationRepository) {
    suspend operator fun invoke(roomName: String, userId: String, displayName: String, avatarUrl: String?): DomainResult<CallJoin> =
        repository.join(roomName, userId, displayName, avatarUrl)
}
