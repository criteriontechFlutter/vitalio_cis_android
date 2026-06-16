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

    override suspend fun fetchDoctors(clientId: String): DomainResult<List<Doctor>> = try {
        val params = mapOf("clientID" to clientId, "departmentId" to "0")
        val json = prefs.getData(key = endpoints.fetchDoctorsAvalability, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchDoctorsAvalability, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, DoctorResponse::class.java).responseValue else emptyList()
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
            ApiClients.module44374.dynamicGet(url = url, params = params)
        }
        val json = if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, AppointmentHistoryResponse::class.java).responseValue else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }
}
