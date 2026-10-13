package com.critetiontech.ctvitalio.domain.usecase.auth

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AuthRepository
import javax.inject.Inject

class
SendOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(mobile: String): DomainResult<Boolean> = repository.sendOtp(mobile)
}