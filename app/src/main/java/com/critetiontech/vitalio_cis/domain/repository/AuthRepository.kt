package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.utils.Patient

interface AuthRepository {
    suspend fun sendOtp(mobile: String): DomainResult<Boolean>
    suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient>
}