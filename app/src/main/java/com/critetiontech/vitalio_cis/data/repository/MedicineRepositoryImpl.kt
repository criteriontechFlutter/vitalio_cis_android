package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.model.MedicineIntakeByDateResponse
import com.critetiontech.vitalio_cis.model.MedicineIntakeResponse
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicineRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : MedicineRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchMedicineIntake(pid: Int, givenDate: String, clientId: Int): DomainResult<List<MedicinePeriod>> = try {
        val params = mapOf("pid" to pid, "givenDate" to givenDate, "clientId" to clientId)
        val json = prefs.getData(key = "${endpoints.fetchPatientMedicineIntake}?pid=$pid&givenDate=$givenDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchPatientMedicineIntake, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, MedicineIntakeResponse::class.java)
            if (parsed.status == 1) parsed.responseValue else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun markIntake(id: Int, scheduledDateTime: String): DomainResult<Unit> = try {
        val body = mapOf("id" to id, "ScheduledDateTime" to scheduledDateTime)
        val response = ApiHelper().callApi(context, endpoints.insertMedicineIntake, showNoConnectionToast = false) { url ->
            ApiClients.module4082.queryRawPostApi(url = url, params = body, body = body)
        }
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchMedicineByDate(pid: Int, givenDate: String, clientId: Int): DomainResult<Pair<List<LoggedMedicine>, List<AllMedicine>>> = try {
        val params = mapOf("pid" to pid, "givenDate" to givenDate, "clientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.fetchPatientMedicineIntakeByDate, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string()
            if (!body.isNullOrEmpty()) {
                val parsed = Gson().fromJson(body, MedicineIntakeByDateResponse::class.java)
                if (parsed.status == 1) DomainResult.Success(Pair(parsed.responseValue.loggedMedicines, parsed.responseValue.allMedicines))
                else DomainResult.Success(Pair(emptyList(), emptyList()))
            } else DomainResult.Success(Pair(emptyList(), emptyList()))
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }
}
