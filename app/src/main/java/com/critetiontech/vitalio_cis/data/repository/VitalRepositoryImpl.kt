package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.VitalRepository
import com.critetiontech.ctvitalio.model.Vital
import com.critetiontech.ctvitalio.model.VitalAnalyticsResponse
import com.critetiontech.ctvitalio.model.VitalApiResponse
import com.critetiontech.ctvitalio.model.VitalGraphDetail
import com.critetiontech.ctvitalio.model.VitalGraphEntry
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VitalRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : VitalRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchLastVital(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>> = try {
        val params = mapOf("uhid" to uhid, "ClientId" to clientId, "userId" to userId)
        val json = prefs.getData(key = endpoints.fetchLastVital, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchLastVital, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string()
            else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, VitalApiResponse::class.java).responseValue.lastVital else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun addVital(body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.addVital, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        // The server may answer 200 or 500 with {"status":0,"responseValue":"<reason>"} when it refuses.
        val raw = if (response.isSuccessful) response.body()?.string() else response.errorBody()?.string()
        val body = runCatching { org.json.JSONObject(raw.orEmpty()) }.getOrNull()
        val status = body?.opt("status")?.toString()?.toIntOrNull()
        val generic = setOf("", "null", "failure", "failed", "error", "success")
        val reason = (body?.opt("responseValue") as? String)?.trim()?.takeIf { it.lowercase() !in generic }
            ?: body?.optString("message")?.trim()?.takeIf { it.lowercase() !in generic }
        when {
            response.isSuccessful && (status == null || status == 1) -> DomainResult.Success(Unit)
            response.code() == 504 && raw.orEmpty().startsWith("No Connection") ->
                DomainResult.Error(Exception("No internet connection. Check your network and try again."))
            raw.orEmpty().startsWith("Exception:") ->
                DomainResult.Error(Exception("Couldn't reach the server. Check your connection and try again."))
            else -> DomainResult.Error(Exception(reason ?: "Couldn't save the vital right now. Please try again."))
        }
    } catch (e: Exception) {
        DomainResult.Error(Exception("Couldn't reach the server. Check your connection and try again."))
    }

    override suspend fun fetchVitalAnalytics(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>> = try {
        val response = ApiHelper().callApi(context, endpoints.fetchVitalAnalytics, showNoConnectionToast = false) { url ->
            ApiClients.module4082.dynamicGet(url = url, params = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string() ?: return DomainResult.Success(emptyList())
            val parsed = Gson().fromJson(body, VitalAnalyticsResponse::class.java)
            val entries = if (parsed.status == 1) {
                parsed.responseValue.patientGraph.map { item ->
                    val details = try { Gson().fromJson(item.vitalDetails, Array<VitalGraphDetail>::class.java).toList() } catch (e: Exception) { emptyList() }
                    VitalGraphEntry(item.vitalDateTime, details)
                }
            } else emptyList()
            DomainResult.Success(entries)
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
