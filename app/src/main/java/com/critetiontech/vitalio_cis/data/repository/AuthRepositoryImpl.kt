package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AuthRepository
import com.critetiontech.ctvitalio.utils.Patient
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.utils.VerifyOtpResponse
import com.critetiontech.ctvitalio.utils.bloodGroupNameFor
import com.critetiontech.ctvitalio.utils.genderNameFor
import com.critetiontech.ctvitalio.utils.profileImageUrl
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class SendOtpDto(
    val status: Int = 0,
    val message: String = "",
    val responseValue: SendOtpResponseValue = SendOtpResponseValue()
)

data class SendOtpResponseValue(
    val uhid: String? = null,
    val mobileNo: String? = null,
    val otp: Int? = null,
    val patientName: String? = null,
    val clientId: Int? = null,
    val isRegisterd: Int? = null
)

@Singleton
class AuthRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : AuthRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun sendOtp(mobile: String): DomainResult<Boolean> = try {
        val params = mapOf("key" to mobile, "ifLoggedOutFromAllDevices" to true)
        val response = ApiHelper().callApi(context, endpoints.corporateEmployeeLogin, showNoConnectionToast = true) { url ->
            ApiClients.module4082.queryDynamicRawPost(url = url, params = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string()
            val parsed = Gson().fromJson(body, SendOtpDto::class.java)
            DomainResult.Success(parsed.responseValue.isRegisterd == 1)
        } else {
            DomainResult.Error(Exception("API Error: ${response.code()}"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> = try {
        val params = mapOf("key" to uhid, "otp" to otp, "deviceToken" to deviceToken, "ifLoggedOutFromAllDevices" to true)
        val response = ApiHelper().callApi(context, endpoints.verifyLogInOTPForSHFCApp, showNoConnectionToast = true) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string()
            val parsed = Gson().fromJson(body, VerifyOtpResponse::class.java)
            if (parsed.status == 1 && parsed.responseValue != null) {
                val p = parsed.responseValue
                val patient = Patient(
                    pid = p.pid, uhId = p.uhId, firstName = p.firstName, lastName = p.lastName,
                    dob = p.dob, genderId = p.genderId, mobileNo = p.mobileNo, emailAddress = p.emailId,
                    age = p.age, address = p.address, cityId = p.cityId, stateId = p.stateId,
                    bloodGroupId = p.bloodGroupId, clientId = p.clientId, countryCallingCode = "",
                    guardianRelationId = 0, guardianName = "", countryId = 0, imageUrl = profileImageUrl(p.profileURL),
                    // City/state come as ids only; Personal Info looks their names up once
                    zip = "", isActive = true, departmentName = null, cityName = "", stateName = "",
                    countryName = "", genderName = genderNameFor(p.genderId), guardianRelationName = null,
                    bloodGroupName = bloodGroupNameFor(p.bloodGroupId),
                    crNo = p.crNo,  // ← capture crNo from login response
                    height = p.height.takeIf { it.isNotBlank() },
                    weight = p.weight.takeIf { it.isNotBlank() }
                )
                prefs.savePatient(patient)
                DomainResult.Success(patient)
            } else {
                DomainResult.Error(Exception(parsed.message.ifEmpty { "OTP verification failed" }))
            }
        } else {
            DomainResult.Error(Exception("API Error: ${response.code()}"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
