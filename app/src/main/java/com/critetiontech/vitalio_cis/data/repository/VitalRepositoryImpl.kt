package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.model.VitalAnalyticsResponse
import com.critetiontech.vitalio_cis.model.VitalApiResponse
import com.critetiontech.vitalio_cis.model.VitalGraphDetail
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import com.critetiontech.vitalio_cis.utils.PrefsManager
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
        if (response.isSuccessful) DomainResult.Success(Unit)
        else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) {
        DomainResult.Error(e)
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
