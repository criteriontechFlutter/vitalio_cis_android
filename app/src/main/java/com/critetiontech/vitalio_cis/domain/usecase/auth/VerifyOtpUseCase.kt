package com.critetiontech.ctvitalio.domain.usecase.auth

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AuthRepository
import com.critetiontech.ctvitalio.utils.Patient
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
        repository.verifyOtp(uhid, otp, deviceToken)
}