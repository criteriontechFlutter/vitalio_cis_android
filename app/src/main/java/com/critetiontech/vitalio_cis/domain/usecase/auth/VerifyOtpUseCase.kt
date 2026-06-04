package com.critetiontech.vitalio_cis.domain.usecase.auth

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AuthRepository
import com.critetiontech.vitalio_cis.utils.Patient
import javax.inject.Inject

class VerifyOtpUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
        repository.verifyOtp(uhid, otp, deviceToken)
}