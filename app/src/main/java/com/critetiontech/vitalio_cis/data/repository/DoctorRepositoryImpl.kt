package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.DoctorRepository
import com.critetiontech.vitalio_cis.model.AppointmentHistoryItem
import com.critetiontech.vitalio_cis.model.AppointmentHistoryResponse
import com.critetiontech.vitalio_cis.model.Doctor
import com.critetiontech.vitalio_cis.model.DoctorDetails
import com.critetiontech.vitalio_cis.model.DoctorResponse
import com.critetiontech.vitalio_cis.model.DoctorResponsedata
import com.critetiontech.vitalio_cis.model.ShiftData
import com.critetiontech.vitalio_cis.model.ShiftResponse
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentItem
import com.critetiontech.vitalio_cis.model.UpcomingAppointmentsResponse
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DoctorRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : DoctorRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchDoctors(clientId: String, departmentId: String): DomainResult<List<Doctor>> = try {
        val params = mutableMapOf<String, Any>("clientId" to clientId)
        if (departmentId != "0" && departmentId.isNotEmpty()) {
            params["departmentId"] = departmentId
        }
        val json = prefs.getData(key = endpoints.fetchDoctorsAvalability, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchDoctorsAvalability, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val response = Gson().fromJson(json, DoctorResponse::class.java)
            response.responseValue.map { it.copy(assignedUserId = if (it.assignedUserId != 0) it.assignedUserId else it.doctorId) }
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchDepartments(clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.Department>> = try {
        val params = mapOf("clientId" to clientId)
        // 4119 is module 4119, wait, we don't have ApiClients.module4119 mapped, but ApiClients might have dynamicGet we can just use module4082 and let URL handle it if base URL includes port? 
        // No, ApiClients use fixed base URLs. We should probably use the same module4082 for now, wait, in Android, there is no port specified in the endpoint, the URL builder might handle it or the base URL.
        val json = prefs.getData(key = endpoints.getAllDepartmentMaster, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getAllDepartmentMaster, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.DepartmentResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchDoctorProfile(clientId: String, doctorId: String): DomainResult<DoctorDetails?> = try {
        val params = mapOf("clientID" to clientId, "doctorId" to doctorId)
        val json = prefs.getData(key = endpoints.getDoctorProfile, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getDoctorProfile, showNoConnectionToast = false) { url ->
                ApiClients.module4084.dynamicGet(url = url, params = params as Map<String, @JvmSuppressWildcards Any>)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val doctor = if (!json.isNullOrEmpty()) Gson().fromJson(json, DoctorResponsedata::class.java).responseValue.firstOrNull() else null
        DomainResult.Success(doctor)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchAvailableSlots(doctorId: String, scheduleDate: String, clientId: String): DomainResult<List<ShiftData>> = try {
        val params = mapOf("doctorId" to doctorId, "scheduleDate" to scheduleDate, "userId" to "1362", "clientID" to clientId)
        val json = prefs.getData(key = endpoints.fetchAvailableSlots, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchAvailableSlots, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, ShiftResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun bookAppointment(params: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.bookAppointment, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = params)
        }
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchUpcomingAppointments(pid: String, clientId: String): DomainResult<List<UpcomingAppointmentItem>> = try {
        val params = mapOf("pid" to pid, "ClientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.fetchUpcomingAppointmentList, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, UpcomingAppointmentsResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchAppointmentHistory(pid: String, clientId: String, userId: String): DomainResult<List<AppointmentHistoryItem>> = try {
        val params = mapOf("pid" to pid, "clientId" to clientId, "userId" to userId)
        val response = ApiHelper().callApi(context, endpoints.appointmentHistory, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, AppointmentHistoryResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchShiftTypeSetting(doctorId: String, clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.ShiftTypeSetting>> = try {
        val params = mapOf("assigneduserId" to doctorId, "clientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.fetchShiftTypeSetting, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.ShiftTypeSettingResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchDoctorShiftConfig(doctorId: String, clientId: String): DomainResult<com.critetiontech.vitalio_cis.model.Doctor?> = try {
        val params = mapOf("doctorId" to doctorId, "clientId" to clientId)
        val json = prefs.getData(key = "${endpoints.fetchDoctorShiftConfig}_$doctorId", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchDoctorShiftConfig, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val doctor = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.SingleDoctorResponse::class.java).responseValue else null
        DomainResult.Success(doctor)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchSchedule(doctorId: String, clientId: String, dayId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.DoctorSchedule>> = try {
        val params = mapOf("doctorId" to doctorId, "clientId" to clientId, "dayId" to dayId)
        val response = ApiHelper().callApi(context, endpoints.fetchSchedule, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.ScheduleResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun getLatestToken(doctorId: String, scheduleDate: String, shiftId: String, departmentId: String, clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.LatestToken>> = try {
        val params = mapOf("doctorId" to doctorId, "targetDate" to scheduleDate, "shiftId" to shiftId, "clientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.getLatestToken, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.LatestTokenResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchConsent(clientId: String): DomainResult<List<com.critetiontech.vitalio_cis.model.ConsentItem>> = try {
        val params = mapOf("clientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.fetchConsent, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.ConsentResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun addConsent(params: Map<String, Any>): DomainResult<String?> = try {
        val response = ApiHelper().callApi(context, endpoints.addConsent, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val msg = if (!json.isNullOrEmpty()) Gson().fromJson(json, com.critetiontech.vitalio_cis.model.AddConsentResponse::class.java).message else null
        DomainResult.Success(msg)
    } catch (e: Exception) { DomainResult.Error(e) }
}
