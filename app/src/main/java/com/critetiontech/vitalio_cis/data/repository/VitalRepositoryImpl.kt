package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.data.offline.SendResult
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

    /** Saves now, or keeps it on the phone to upload later when there's no network. */
    override suspend fun addVital(body: Map<String, Any>): DomainResult<Unit> =
        OfflineOutbox.submit(OfflineOutbox.Kind.VITAL, body, ::sendVital)

    suspend fun sendVital(body: Map<String, Any>): SendResult = try {
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
            response.isSuccessful && (status == null || status == 1) -> SendResult.Sent
            SendResult.isNetworkFailure(response, raw) -> SendResult.Offline
            else -> SendResult.Rejected(reason ?: "Couldn't save the vital right now. Please try again.")
        }
    } catch (e: Exception) {
        SendResult.Rejected("Couldn't save the vital right now. Please try again.")
    }

    override suspend fun fetchVitalAnalytics(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>> = try {
        // Offline, the last result for the same range and vitals is shown instead.
        val key = "${endpoints.fetchVitalAnalytics}?" + params.toSortedMap().entries.joinToString("&") { "${it.key}=${it.value}" }
        val body = prefs.getData(key = key, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchVitalAnalytics, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        when {
            body == null -> DomainResult.Error(Exception("Couldn't load vital history"))
            body.isEmpty() -> DomainResult.Success(emptyList())
            else -> {
                val parsed = Gson().fromJson(body, VitalAnalyticsResponse::class.java)
                val entries = if (parsed.status == 1) {
                    parsed.responseValue.patientGraph.map { item ->
                        val details = try { Gson().fromJson(item.vitalDetails, Array<VitalGraphDetail>::class.java).toList() } catch (e: Exception) { emptyList() }
                        VitalGraphEntry(item.vitalDateTime, details)
                    }
                } else emptyList()
                DomainResult.Success(entries)
            }
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
