package com.critetiontech.vitalio_cis.domain.repository

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.utils.Patient
import java.io.File

interface ProfileRepository {
    suspend fun updateProfile(patient: Patient, firstName: String, lastName: String, dob: String, mobileNo: String, emailId: String, address: String, zip: String, imageFile: File?): DomainResult<Unit>
}
