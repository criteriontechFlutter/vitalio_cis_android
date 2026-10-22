package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import android.util.Log
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ConsultationRepository
import com.critetiontech.ctvitalio.model.CallJoin
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class ConsultationRepositoryImpl(
    private val context: Context,
    @Suppress("unused") private val prefs: PrefsManager,
) : ConsultationRepository {

    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun join(roomName: String, userId: String, displayName: String, avatarUrl: String?): DomainResult<CallJoin> = try {
        val body = buildMap<String, Any> {
            put("roomName", roomName)
            put("userId", userId)
            put("displayName", displayName)
            put("role", "patient")
            avatarUrl?.takeIf { it.isNotBlank() }?.let { put("avatarUrl", it) }
        }
        val json = post(endpoints.consultationJoin, body)
        // Field names vary between LiveKit backends; accept the common spellings
        val token = json.firstString("token", "accessToken", "participantToken", "jwt")
        val url = json.firstString("serverUrl", "url", "wsUrl", "livekitUrl", "liveKitUrl", "host")
        if (token == null || url == null) DomainResult.Error(Exception("The server didn't send call details"))
        else DomainResult.Success(CallJoin(serverUrl = url, token = token))
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun decline(roomName: String, patientUserId: String, reason: String): DomainResult<Unit> = try {
        post(endpoints.consultationDecline, mapOf("roomName" to roomName, "patientUserId" to patientUserId, "reason" to reason))
        DomainResult.Success(Unit)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun end(roomName: String, endedByUserId: String, reason: String): DomainResult<Unit> = try {
        post(endpoints.consultationEnd, mapOf("roomName" to roomName, "endedByUserId" to endedByUserId, "reason" to reason))
        DomainResult.Success(Unit)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    /** Posts [body]; throws with a message fit to show the patient when the server refuses or can't be reached. */
    private suspend fun post(endpoint: String, body: Map<String, Any>): JsonObject {
        val response = ApiHelper().callApi(context, endpoint, showNoConnectionToast = false) { url ->
            ApiClients.consultation.dynamicRawPost(url = url, body = body)
        }
        val raw = (if (response.isSuccessful) response.body() else response.errorBody())?.string().orEmpty()
        if (!response.isSuccessful) {
            Log.w(TAG, "$endpoint failed: ${response.code()} $raw")
            // ApiHelper turns "offline" into 504 and network failures into 500 "Exception: …"
            if (response.code() == 504 && raw.startsWith("No Connection")) throw Exception("No internet connection. Check your network and try again.")
            if (raw.startsWith("Exception:")) throw Exception("Couldn't reach the server. Check your internet connection.")
            val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull()
            throw Exception(json?.firstString("detail", "error", "message", "title") ?: "Server error ${response.code()}")
        }
        val json = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrDefault(JsonObject())
        // Some responses wrap the payload in "data"
        return json.get("data")?.takeIf { it.isJsonObject }?.asJsonObject ?: json
    }

    private fun JsonObject.firstString(vararg keys: String): String? = keys.firstNotNullOfOrNull { key ->
        entrySet().firstOrNull { it.key.equals(key, ignoreCase = true) }?.value
            ?.takeIf { it.isJsonPrimitive }?.asString?.takeIf { it.isNotBlank() }
    }

    private companion object { const val TAG = "Consultation" }
}
