package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.Patient

interface AuthRepository {
    suspend fun sendOtp(mobile: String): DomainResult<Boolean>
    suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient>
}