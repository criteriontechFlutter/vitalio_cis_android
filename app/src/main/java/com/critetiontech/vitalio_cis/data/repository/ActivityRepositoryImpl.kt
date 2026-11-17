package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ActivityRepository
import com.critetiontech.ctvitalio.model.ActivityMasterResponse
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest
import com.critetiontech.ctvitalio.model.PatientActivityItem
import com.critetiontech.ctvitalio.model.PatientActivityResponse
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import org.json.JSONObject

class ActivityRepositoryImpl(
    private val context: Context,
    @Suppress("unused") private val prefs: PrefsManager,
) : ActivityRepository {

    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchActivityMaster(): DomainResult<List<ActivityModel>> = try {
        val response = ApiHelper().callApi(context, endpoints.getAllActivityMaster, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = emptyMap())
        }
        if (!response.isSuccessful) throw Exception("API Error: ${response.code()}")
        val list = Gson().fromJson(response.body()?.string().orEmpty(), ActivityMasterResponse::class.java)?.responseValue.orEmpty()
        DomainResult.Success(list)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun addPatientActivity(request: AddPatientActivityRequest): DomainResult<ActivitySaveResult> = try {
        val body = mapOf(
            "uhid" to request.uhid,
            "activityId" to request.activityId,
            "timeFrom" to request.timeFrom,
            "totalTimeInMinutes" to request.totalTimeInMinutes,
            "remark" to request.remark,
            "userId" to request.userId,
            "clientID" to request.clientID,
        )
        val response = ApiHelper().callApi(context, endpoints.addPatientActivity, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        // Server answers 500 {"status":0,"responseValue":"Already Exists!"} for an activity it already has
        val raw = (if (response.isSuccessful) response.body() else response.errorBody())?.string().orEmpty()
        val json = runCatching { JSONObject(raw) }.getOrNull()
        when {
            json?.optString("responseValue")?.contains("already exist", ignoreCase = true) == true ->
                DomainResult.Success(ActivitySaveResult.ALREADY_EXISTS)
            response.isSuccessful && json?.optInt("status", 1) != 0 -> DomainResult.Success(ActivitySaveResult.SAVED)
            else -> DomainResult.Error(Exception("AddPatientActivity rejected: ${response.code()} $raw"))
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun fetchPatientActivity(uhid: String, date: String, clientId: Int): DomainResult<List<PatientActivityItem>> = try {
        val params = mapOf("uhid" to uhid, "date" to date, "clientId" to clientId)
        val response = ApiHelper().callApi(context, endpoints.fetchPatientActivity, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        if (response.isSuccessful) {
            DomainResult.Success(parsePatientActivities(response.body()?.string().orEmpty()))
        } else {
            // A day with nothing logged comes back as 400 {"status":0,"responseValue":"No record found"}.
            val raw = response.errorBody()?.string().orEmpty()
            if (isNoRecord(raw)) DomainResult.Success(emptyList())
            else throw Exception("API Error: ${response.code()} $raw")
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun removePatientActivity(id: Int, clientId: Int, userId: Int): DomainResult<Unit> = try {
        val params = mapOf("id" to id, "clientId" to clientId, "userId" to userId)
        val response = ApiHelper().callApi(context, endpoints.removePatientActivity, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicDelete(url = url, params = params)
        }
        val raw = (if (response.isSuccessful) response.body() else response.errorBody())?.string().orEmpty()
        val status = runCatching { JSONObject(raw).optInt("status", 1) }.getOrDefault(1)
        if (response.isSuccessful && status != 0) DomainResult.Success(Unit)
        else DomainResult.Error(Exception("RemovePatientActivity rejected: ${response.code()} $raw"))
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    companion object {
        fun isNoRecord(body: String): Boolean =
            runCatching {
                com.google.gson.JsonParser.parseString(body).asJsonObject.get("responseValue")
                    ?.takeIf { it.isJsonPrimitive }?.asString
            }.getOrNull()?.contains("no record", ignoreCase = true) == true

        /** The server has answered both a bare JSON array and the usual {status, responseValue} wrapper. */
        fun parsePatientActivities(body: String): List<PatientActivityItem> {
            val trimmed = body.trim()
            if (trimmed.isEmpty()) return emptyList()
            return if (trimmed.startsWith("[")) {
                Gson().fromJson(trimmed, Array<PatientActivityItem>::class.java)?.toList().orEmpty()
            } else {
                Gson().fromJson(trimmed, PatientActivityResponse::class.java)?.responseValue.orEmpty()
            }
        }
    }
}
