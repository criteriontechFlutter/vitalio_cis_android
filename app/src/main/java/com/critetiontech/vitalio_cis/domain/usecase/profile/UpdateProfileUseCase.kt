package com.critetiontech.vitalio_cis.domain.usecase.profile

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ProfileRepository
import com.critetiontech.vitalio_cis.utils.Patient
import java.io.File
import javax.inject.Inject

class UpdateProfileUseCase @Inject constructor(private val repository: ProfileRepository) {
    suspend operator fun invoke(patient: Patient, firstName: String, lastName: String, dob: String, mobileNo: String, emailId: String, address: String, zip: String, imageFile: File?): DomainResult<Unit> =
        repository.updateProfile(patient, firstName, lastName, dob, mobileNo, emailId, address, zip, imageFile)
}