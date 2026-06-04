package com.critetiontech.vitalio_cis.domain.usecase.auth

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AuthRepository
import javax.inject.Inject

class
SendOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(mobile: String): DomainResult<Boolean> = repository.sendOtp(mobile)
}