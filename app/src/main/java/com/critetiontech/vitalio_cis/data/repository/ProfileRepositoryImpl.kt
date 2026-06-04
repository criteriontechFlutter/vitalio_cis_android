package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ProfileRepository
import com.critetiontech.vitalio_cis.utils.Patient
import com.critetiontech.vitalio_cis.utils.PrefsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : ProfileRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun updateProfile(patient: Patient, firstName: String, lastName: String, dob: String, mobileNo: String, emailId: String, address: String, zip: String, imageFile: File?): DomainResult<Unit> = try {
        val parts = mutableListOf<MultipartBody.Part>().apply {
            fun add(name: String, value: String) = add(MultipartBody.Part.createFormData(name, value))
            add("FirstName", firstName); add("LastName", lastName); add("Dob", dob)
            add("MobileNo", mobileNo); add("EmailID", emailId); add("Address", address); add("Zip", zip)
            add("Pid", patient.pid.toString()); add("ClientId", patient.clientId.toString())
            add("UserId", patient.pid.toString()); add("GenderId", patient.genderId.toString())
            add("BloodGroupId", patient.bloodGroupId.toString()); add("CityId", patient.cityId.toString())
            add("StateId", patient.stateId.toString()); add("CountryId", patient.countryId.toString())
            val ageYears = Regex("^(\\d+)").find(patient.age.trim())?.value ?: "0"
            add("Age", ageYears); add("AgeUnitId", "1")
            if (imageFile != null && imageFile.exists()) {
                add(MultipartBody.Part.createFormData("FormFile", imageFile.name, imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())))
            }
        }
        val response = ApiClients.module4082.dynamicMultipartPut(url = endpoints.updatePatientProfile, parts = parts)
        if (response.isSuccessful) {
            prefs.savePatient(patient.copy(firstName = firstName, lastName = lastName, dob = dob, mobileNo = mobileNo, emailAddress = emailId, address = address, zip = zip))
            DomainResult.Success(Unit)
        } else DomainResult.Error(Exception("Update failed (${response.code()})"))
    } catch (e: Exception) { DomainResult.Error(e) }
}
