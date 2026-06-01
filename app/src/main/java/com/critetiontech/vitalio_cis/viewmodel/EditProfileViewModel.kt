package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.utils.PrefsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class EditProfileViewModel : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    fun resetSuccess() {
        _updateSuccess.value = false
    }

    fun updateProfile(
        context: Context,
        firstName: String,
        lastName: String,
        dob: String,
        mobileNo: String,
        emailId: String,
        address: String,
        zip: String,
        imageFile: File?
    ) {
        viewModelScope.launch {
            _loading.value = true
            val prefs = PrefsManager(context)
            val patient = prefs.getPatient() ?: run {
                Toast.makeText(context, "Session expired, please login again", Toast.LENGTH_SHORT).show()
                _loading.value = false
                return@launch
            }

            try {
                val parts = mutableListOf<MultipartBody.Part>()

                fun addPart(name: String, value: String) {
                    parts.add(MultipartBody.Part.createFormData(name, value))
                }

                addPart("FirstName", firstName)
                addPart("LastName", lastName)
                addPart("Dob", dob)
                addPart("MobileNo", mobileNo)
                addPart("EmailID", emailId)
                addPart("Address", address)
                addPart("Zip", zip)
                addPart("Pid", patient.pid.toString())
                addPart("ClientId", patient.clientId.toString())
                addPart("UserId", patient.pid.toString())
                addPart("GenderId", patient.genderId.toString())
                addPart("BloodGroupId", patient.bloodGroupId.toString())
                addPart("CityId", patient.cityId.toString())
                addPart("StateId", patient.stateId.toString())
                addPart("CountryId", patient.countryId.toString())
                val ageYears = Regex("^(\\d+)").find(patient.age.trim())?.value ?: "0"
                addPart("Age", ageYears)
                addPart("AgeUnitId", "1")

                if (imageFile != null && imageFile.exists()) {
                    val requestFile = imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                    parts.add(
                        MultipartBody.Part.createFormData("FormFile", imageFile.name, requestFile)
                    )
                }

                val response = ApiClients.module4082.dynamicMultipartPut(
                    url = ApiEndPointCorporateModule().updatePatientProfile,
                    parts = parts
                )

                if (response.isSuccessful) {
                    val body = response.body()?.string()
                    Log.d("EditProfile", "Success: $body")

                    val updatedPatient = patient.copy(
                        firstName = firstName,
                        lastName = lastName,
                        dob = dob,
                        mobileNo = mobileNo,
                        emailAddress = emailId,
                        address = address,
                        zip = zip
                    )
                    prefs.savePatient(updatedPatient)
                    _updateSuccess.value = true
                } else {
                    val error = response.errorBody()?.string() ?: "Update failed (${response.code()})"
                    Log.e("EditProfile", "Error: $error")
                    Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                Log.e("EditProfile", "Exception: ${e.message}", e)
                Toast.makeText(context, e.message ?: "Something went wrong", Toast.LENGTH_LONG).show()
            } finally {
                _loading.value = false
            }
        }
    }
}