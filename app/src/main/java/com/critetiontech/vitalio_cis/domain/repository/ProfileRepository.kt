package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.Patient
import com.critetiontech.ctvitalio.model.LocationItem
import java.io.File

interface ProfileRepository {
    suspend fun updateProfile(
        patient: Patient,
        firstName: String,
        lastName: String,
        dob: String,
        mobileNo: String,
        emailId: String,
        address: String,
        zip: String,
        imageFile: File?
    ): DomainResult<Unit>

    /** Returns list of countries (id, name) */
    suspend fun getCountries(): List<LocationItem>

    /** Returns list of states for a given countryId */
    suspend fun getStates(countryId: Int): List<LocationItem>

    /** Returns list of cities for a given stateId */
    suspend fun getCities(stateId: Int): List<LocationItem>
}
