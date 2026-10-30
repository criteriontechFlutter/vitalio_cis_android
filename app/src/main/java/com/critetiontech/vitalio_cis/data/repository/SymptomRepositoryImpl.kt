package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.data.offline.SendResult
import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.SymptomRepository
import com.critetiontech.ctvitalio.model.Problem
import com.critetiontech.ctvitalio.model.ProblemResponse
import com.critetiontech.ctvitalio.model.SymptomApiResponse
import com.critetiontech.ctvitalio.model.SymptomItem
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

private val PayloadDateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

@Singleton
class SymptomRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : SymptomRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchSymptoms(uhid: String, clientId: String): DomainResult<List<SymptomItem>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId, "type" to "Symptoms")
        val json = prefs.getData(key = endpoints.getSymptoms, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getSymptoms, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, headers = mapOf("Cache-Control" to "no-cache"), params = params)
            }
            if (response.isSuccessful) response.body()?.string()
            else {
                // A patient with nothing recorded may get 4xx {"status":0,..."No record found"}: that's
                // an empty list, not a failure.
                val error = response.errorBody()?.string()
                if (error?.contains("no record", ignoreCase = true) == true) error
                else throw Exception("API Error: ${response.code()}")
            }
        }
        // Null here means the call failed and nothing was cached, so the caller can offer a retry.
        if (json.isNullOrEmpty()) throw Exception("Couldn't load symptoms")
        val parsed = Gson().fromJson(json, SymptomApiResponse::class.java)
        DomainResult.Success(if (parsed.status == 1) parsed.responseValue.orEmpty() else emptyList())
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun insertSymptoms(body: Map<String, Any>): DomainResult<Unit> =
        OfflineOutbox.submit(OfflineOutbox.Kind.SYMPTOMS, body, ::sendSymptoms)

    suspend fun sendSymptoms(body: Map<String, Any>): SendResult = try {
        val response = ApiHelper().callApi(context, endpoints.insertSymtoms, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        val raw = if (response.isSuccessful) null else response.errorBody()?.string()
        when {
            response.isSuccessful -> SendResult.Sent
            SendResult.isNetworkFailure(response, raw) -> SendResult.Offline
            else -> SendResult.Rejected("API Error: ${response.code()}")
        }
    } catch (e: Exception) { SendResult.Rejected(e.message ?: "API Error") }

    override suspend fun saveSymptoms(uhid: String, clientId: Int, symptoms: List<SymptomItem>): DomainResult<Unit> {
        val now = LocalDateTime.now().format(PayloadDateFormat)
        // The backend reads these keys in this order (same as iOS). "pdmId" carries the visit's pmId
        // for a symptom already on record, which is how the backend finds the row to update;
        // a newly picked symptom has none and sends 2.
        val rows = symptoms.map {
            linkedMapOf(
                "detailID" to it.detailId,
                "detailsDate" to payloadDate(it.detailsDate, now),
                "details" to it.details,
                "pdmId" to (it.pmId.takeIf { id -> id > 0 } ?: 2),
                "isProvisionalDiagnosis" to 0,
                "isChronic" to 0,
                "isStillFeeling" to it.isStillFeeling,
            )
        }
        val body = mapOf(
            "uhId" to uhid,
            "jsonSymtoms" to Gson().toJson(rows),
            "clientId" to clientId,
            "type" to "Symptoms",
            "isFromPatient" to true,
        )
        return insertSymptoms(body)
    }

    /** "2026-10-16T11:12:51.743" or "2026-10-16 11:12:51" → "2026-10-16T11:12:51"; anything unreadable → [now]. */
    private fun payloadDate(raw: String, now: String): String {
        val trimmed = raw.trim()
        if (trimmed.length < 10 || !trimmed.take(4).all { it.isDigit() }) return now
        return trimmed.replace(' ', 'T').substringBefore('.')
    }

    override suspend fun getProblemsWithIcon(): DomainResult<List<Problem>> = try {
        val params = mapOf("problemName" to "", "languageId" to "1")
        val json = prefs.getData(key = endpoints.getProblemsWithIcon, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getProblemsWithIcon, showNoConnectionToast = false) { url ->
                ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, ProblemResponse::class.java).responseValue ?: emptyList() else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun getAllProblems(query: String): DomainResult<List<Problem>> = try {
        val params = mapOf("alphabet" to query, "language" to 1)
        val body = prefs.getData(key = "${endpoints.getAllProblems}?alphabet=${query.trim().lowercase()}", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getAllProblems, showNoConnectionToast = false) { url ->
                ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        if (body != null) {
            val list = if (body.isNotEmpty()) Gson().fromJson(body, ProblemResponse::class.java).responseValue ?: emptyList() else emptyList()
            DomainResult.Success(list)
        } else {
            // Offline and never searched this term: search the common-problems list we do have.
            val common = (getProblemsWithIcon() as? DomainResult.Success)?.data.orEmpty()
            if (common.isEmpty()) DomainResult.Error(Exception("Couldn't search symptoms"))
            else DomainResult.Success(common.filter { it.problemName.orEmpty().contains(query.trim(), ignoreCase = true) })
        }
    } catch (e: Exception) { DomainResult.Error(e) }
}
