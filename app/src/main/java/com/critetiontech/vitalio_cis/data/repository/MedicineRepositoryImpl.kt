package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.MedicineRepository
import com.critetiontech.vitalio_cis.model.AllMedicine
import com.critetiontech.vitalio_cis.model.InsertMedicineRequest
import com.critetiontech.vitalio_cis.model.InsertMedicineResponse
import com.critetiontech.vitalio_cis.model.LoggedMedicine
import com.critetiontech.vitalio_cis.model.MedicineIntakeByDateResponse
import com.critetiontech.vitalio_cis.model.MedicineIntakeResponse
import com.critetiontech.vitalio_cis.model.MedicinePeriod
import com.critetiontech.vitalio_cis.model.SearchMedicineItem
import com.critetiontech.vitalio_cis.model.SearchMedicineResponse
import com.critetiontech.vitalio_cis.model.DrugInteractionItem
import com.critetiontech.vitalio_cis.model.DrugInteractionResponse
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
        android.util.Log.d("MedicineRepo", "🔵 markIntake request → id=$id, ScheduledDateTime=$scheduledDateTime, body=$body")
        val response = ApiHelper().callApi(context, endpoints.insertMedicineIntake, showNoConnectionToast = false) { url ->
            android.util.Log.d("MedicineRepo", "🔵 markIntake URL → $url")
            ApiClients.module4082.queryRawPostApi(url = url, params = body, body = body)
        }
        android.util.Log.d("MedicineRepo", "🔵 markIntake response → code=${response.code()}, isSuccessful=${response.isSuccessful}, body=${response.body()?.string()}")
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) {
        android.util.Log.e("MedicineRepo", "🔴 markIntake exception", e)
        DomainResult.Error(e)
    }

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

    override suspend fun searchMedicine(alphabet: String): DomainResult<List<SearchMedicineItem>> = try {
        val params = mapOf("alphabet" to alphabet)
        val response = ApiHelper().callApi(context, "api/KnowMedApis/GetBrandList", showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string()
            if (!body.isNullOrEmpty()) {
                val parsed = Gson().fromJson(body, SearchMedicineResponse::class.java)
                DomainResult.Success(parsed.responseValue.take(50))
            } else DomainResult.Success(emptyList())
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun insertMedicine(request: InsertMedicineRequest): DomainResult<InsertMedicineResponse> = try {
        val body: Map<String, Any> = mapOf(
            "pid" to request.pid,
            "medicineId" to request.medicineId,
            "dosageType" to request.dosageType,
            "dosageStrength" to request.dosageStrength,
            "frequency" to request.frequency,
            "frequencyValue" to request.frequencyValue,
            "weekDays" to request.weekDays,
            "monthDates" to request.monthDates,
            "instructions" to request.instructions,
            "timeSlotsJson" to request.timeSlotsJson,
            "startdate" to request.startdate,
            "enddate" to request.enddate,
            "clientId" to request.clientId
        )
        val response = ApiHelper().callApi(context, endpoints.insertMedicineIntake, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) {
            val bodyStr = response.body()?.string()
            if (!bodyStr.isNullOrEmpty()) {
                val parsed = Gson().fromJson(bodyStr, InsertMedicineResponse::class.java)
                DomainResult.Success(parsed)
            } else DomainResult.Error(Exception("Empty response"))
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }
    override suspend fun getDrugInteraction(brandIds: String): DomainResult<List<DrugInteractionItem>> = try {
        val body = mapOf("brandId" to brandIds, "medicineName" to "")
        val response = ApiHelper().callApi(context, "api/ADRReport/getDrugIntraction", showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) {
            val bodyStr = response.body()?.string()
            if (!bodyStr.isNullOrEmpty()) {
                val parsed = Gson().fromJson(bodyStr, DrugInteractionResponse::class.java)
                DomainResult.Success(parsed.responseValue ?: emptyList())
            } else DomainResult.Error(Exception("Empty response"))
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }
}

