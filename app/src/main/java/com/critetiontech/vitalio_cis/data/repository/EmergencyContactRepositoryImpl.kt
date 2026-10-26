package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.EmergencyContactRepository
import com.critetiontech.ctvitalio.model.EmergencyContact
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import okhttp3.ResponseBody
import retrofit2.Response

class EmergencyContactRepositoryImpl(
    private val context: Context,
) : EmergencyContactRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchContacts(pid: Int, clientId: Int): DomainResult<List<EmergencyContact>> = try {
        val params = mapOf("pid" to pid, "clientId" to clientId)
        // Contacts change from this screen, so always ask the server instead of a cached copy.
        val response = ApiHelper().callApi(context, endpoints.getEmergencyContacts, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        val body = (if (response.isSuccessful) response.body() else response.errorBody())?.string()
        val json = body?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
        val value = json?.get("responseValue")
        when {
            response.isSuccessful && value != null && value.isJsonArray ->
                DomainResult.Success(Gson().fromJson(value, Array<EmergencyContact>::class.java).toList())
            // A patient without contacts gets 400 {"status":0,"responseValue":"No record found"}.
            json != null && json.status() == 0 && value?.takeIf { it.isJsonPrimitive }?.asString
                ?.contains("no record", ignoreCase = true) == true -> DomainResult.Success(emptyList())
            else -> DomainResult.Error(Exception(json.serverMessage() ?: "API Error: ${response.code()}"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun saveContact(contact: EmergencyContact): DomainResult<Unit> = try {
        val body = mapOf<String, Any>(
            "id" to contact.id,
            "pid" to contact.pid,
            "contactName" to contact.contactName.orEmpty(),
            "contactNumber" to contact.contactNumber.orEmpty(),
            "relationship" to contact.relationship.orEmpty(),
            "userId" to contact.userId,
            "clientId" to contact.clientId,
        )
        val isNew = contact.id == 0
        val endpoint = if (isNew) endpoints.insertEmergencyContact else endpoints.updateEmergencyContact
        val response = ApiHelper().callApi(context, endpoint, showNoConnectionToast = false) { url ->
            if (isNew) ApiClients.module4082.dynamicRawPost(url = url, body = body)
            else ApiClients.module4082.dynamicRawPut(url = url, body = body)
        }
        response.toUnitResult()
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun removeContact(id: Int): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.removeEmergencyContact, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicDelete(url = url, params = mapOf("id" to id))
        }
        response.toUnitResult()
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    /** Success only when HTTP succeeded and the body doesn't report status 0. */
    private fun Response<ResponseBody>.toUnitResult(): DomainResult<Unit> {
        val text = (if (isSuccessful) body() else errorBody())?.string()
        val json = text?.let { runCatching { JsonParser.parseString(it).asJsonObject }.getOrNull() }
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
