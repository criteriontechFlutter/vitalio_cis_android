package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.Disease
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FamilyHealthHistoryRepository
import com.critetiontech.ctvitalio.model.DiseaseItem
import com.critetiontech.ctvitalio.model.FamilyHealthHistoryRow
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Response

class FamilyHealthHistoryRepositoryImpl(
    private val context: Context,
    private val prefs: PrefsManager,
) : FamilyHealthHistoryRepository {
    private val endpoints = ApiEndPointCorporateModule()

    /** The disease master list (~3000 names) is fetched once per process; the cache covers offline starts. */
    @Volatile private var diseases: List<Disease>? = null

    override suspend fun fetchHistory(pid: Int, clientId: Int): DomainResult<List<FamilyHealthHistoryRow>> = try {
        // Entries change from this screen, so always ask the server instead of a cached copy.
        val response = ApiHelper().callApi(context, endpoints.getFamilyHealthHistory, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = mapOf("pid" to pid, "clientId" to clientId))
        }
        val json = response.json()
        val value = json?.get("responseValue")
        when {
            response.isSuccessful && value != null && value.isJsonArray ->
                DomainResult.Success(Gson().fromJson(value, Array<FamilyHealthHistoryRow>::class.java).toList())
            // Nothing recorded yet: {"status":0,"responseValue":"No record found"}.
            json?.status() == 0 && value?.takeIf { it.isJsonPrimitive }?.asString
                ?.contains("no record", ignoreCase = true) == true -> DomainResult.Success(emptyList())
            else -> DomainResult.Error(Exception(json.serverMessage() ?: "API Error: ${response.code()}"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun fetchDiseases(): DomainResult<List<Disease>> = try {
        diseases?.let { return DomainResult.Success(it) }
        val body = prefs.getData(key = endpoints.getAllDiseases, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getAllDiseases, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = emptyMap())
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        if (body.isNullOrEmpty()) throw Exception("Couldn't load diseases")
        val value = JsonParser.parseString(body).asJsonObject.get("responseValue")
        val list = if (value != null && value.isJsonArray) {
            Gson().fromJson(value, Array<DiseaseItem>::class.java)
                .mapNotNull { d -> d.problemName?.trim()?.takeIf { it.isNotEmpty() }?.let { Disease(d.id, it) } }
        } else emptyList()
        diseases = list
        DomainResult.Success(list)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun addHistory(pid: Int, clientId: Int, diseaseId: Int, relationIds: List<Int>): DomainResult<Unit> = try {
        val body = mapOf<String, Any>(
            "pid" to pid,
            "diseaseId" to diseaseId,
            "relationJson" to Gson().toJson(relationIds),
            "userId" to pid,
            "clientId" to clientId,
        )
        val response = ApiHelper().callApi(context, endpoints.insertFamilyHealthHistory, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        response.toUnitResult()
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun removeHistory(id: Int, clientId: Int, userId: Int): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.deleteFamilyHealthHistory, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicDelete(url = url, params = mapOf("id" to id, "clientId" to clientId, "userId" to userId))
        }
        response.toUnitResult()
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    private fun Response<ResponseBody>.json(): JsonObject? {
        val text = (if (isSuccessful) body() else errorBody())?.string()
        return text?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
    }

    /** Success only when HTTP succeeded and the body doesn't report status 0. */
    private fun Response<ResponseBody>.toUnitResult(): DomainResult<Unit> {
        val json = json()
        return if (isSuccessful && json?.status() != 0) DomainResult.Success(Unit)
        else DomainResult.Error(Exception(json.serverMessage() ?: "API Error: ${code()}"))
    }

    private fun JsonObject.status(): Int? =
        get("status")?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }

    /** The server's reason, e.g. responseValue "No record found"; "failure" alone says nothing. */
    private fun JsonObject?.serverMessage(): String? {
        if (this == null) return null
        val value = get("responseValue")?.takeIf { it.isJsonPrimitive }?.asString
        val message = get("message")?.takeIf { it.isJsonPrimitive }?.asString
        return listOf(value, message).firstOrNull { !it.isNullOrBlank() && !it.equals("failure", ignoreCase = true) }
    }
}
