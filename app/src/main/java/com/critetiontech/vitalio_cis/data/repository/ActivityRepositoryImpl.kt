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
}
