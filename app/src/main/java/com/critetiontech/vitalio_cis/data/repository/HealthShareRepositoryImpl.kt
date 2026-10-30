package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.offline.SendResult
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.HealthShareRepository
import com.critetiontech.ctvitalio.model.HealthShareCreated
import com.critetiontech.ctvitalio.model.HealthShareLink
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Response

/** Thrown when the request never reached the server, so the screen can say "you're offline". */
class HealthShareOfflineException : Exception("No connection")

class HealthShareRepositoryImpl(
    private val context: Context,
) : HealthShareRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchLinks(pid: Int, clientId: Int): DomainResult<List<HealthShareLink>> = try {
        val params = mapOf("pid" to pid, "clientId" to clientId)
        // Links are created and revoked from this screen, so always ask the server instead of a cached copy.
        val response = ApiHelper().callApi(context, endpoints.getAllHealthShareLinks, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val (json, raw) = response.parse()
        val value = json?.get("responseValue")
        when {
            response.isSuccessful && json?.status() != 0 && value != null && value.isJsonArray ->
                DomainResult.Success(Gson().fromJson(value, Array<HealthShareLink>::class.java).toList())
            // No links yet: an empty or missing list, or a "no record" reply.
            response.isSuccessful && (value == null || value.isJsonNull) -> DomainResult.Success(emptyList())
            json?.serverMessage()?.contains("no record", ignoreCase = true) == true -> DomainResult.Success(emptyList())
            else -> response.failure(json, raw)
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun createLink(
        uhid: String, pid: Int, userId: Int, clientId: Int, recipientEmail: String, patientName: String,
    ): DomainResult<HealthShareCreated> = try {
        val body = mapOf<String, Any>(
            "uhid" to uhid,
            "pId" to pid,
            "userId" to userId,
            "clientId" to clientId,
            "recipientEmail" to recipientEmail,
            "patientName" to patientName,
        )
        val response = ApiHelper().callApi(context, endpoints.generateHealthShareLink, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        val (json, raw) = response.parse()
        val value = json?.get("responseValue")
        if (response.isSuccessful && json?.status() == 1) {
            val created = if (value != null && value.isJsonObject) Gson().fromJson(value, HealthShareCreated::class.java) else HealthShareCreated()
            DomainResult.Success(created)
        } else response.failure(json, raw)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun revokeLink(id: Int, clientId: Int): DomainResult<Unit> = try {
        // Query parameters with an empty POST body, as the API expects.
        val response = ApiHelper().callApi(context, endpoints.revokeHealthShareLink, showNoConnectionToast = false) { url ->
            ApiClients.module4082.queryDynamicRawPost(url = url, params = mapOf("id" to id, "clientId" to clientId))
        }
        val (json, raw) = response.parse()
        if (response.isSuccessful && json?.status() != 0) DomainResult.Success(Unit) else response.failure(json, raw)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    /** The body (or error body) as JSON, plus the raw text. */
    private fun Response<ResponseBody>.parse(): Pair<JsonObject?, String?> {
        val text = (if (isSuccessful) body() else errorBody())?.string()
        return text?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() } to text
    }

    private fun <T> Response<ResponseBody>.failure(json: JsonObject?, raw: String?): DomainResult<T> =
        if (SendResult.isNetworkFailure(this, raw)) DomainResult.Error(HealthShareOfflineException())
        else DomainResult.Error(Exception(json.serverMessage() ?: "API Error: ${code()}"))

    private fun JsonObject.status(): Int? =
        get("status")?.takeIf { it.isJsonPrimitive }?.let { runCatching { it.asInt }.getOrNull() }

    /** The server's reason, e.g. message "A valid recipient email address is required."; "failure" alone says nothing. */
    private fun JsonObject?.serverMessage(): String? {
        if (this == null) return null
        val value = get("responseValue")?.takeIf { it.isJsonPrimitive }?.asString
        val message = get("message")?.takeIf { it.isJsonPrimitive }?.asString
        return listOf(message, value).firstOrNull { !it.isNullOrBlank() && !it.equals("failure", ignoreCase = true) }
    }
}
