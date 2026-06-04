package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.SymptomRepository
import com.critetiontech.vitalio_cis.model.Problem
import com.critetiontech.vitalio_cis.model.ProblemResponse
import com.critetiontech.vitalio_cis.model.SymptomApiResponse
import com.critetiontech.vitalio_cis.model.SymptomItem
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SymptomRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : SymptomRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchSymptoms(uhid: String, clientId: String): DomainResult<List<SymptomItem>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId, "type" to "Symptoms")
        val json = prefs.getData(key = endpoints.getSymptoms, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getSymptoms, showNoConnectionDialog = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, SymptomApiResponse::class.java)
            if (parsed.status == 1) parsed.responseValue else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun insertSymptoms(body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.insertSymtoms, showNoConnectionDialog = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun getProblemsWithIcon(): DomainResult<List<Problem>> = try {
        val params = mapOf("problemName" to "", "languageId" to "1")
        val json = prefs.getData(key = endpoints.getProblemsWithIcon, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getProblemsWithIcon, showNoConnectionDialog = false) { url ->
                ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, ProblemResponse::class.java).responseValue ?: emptyList() else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun getAllProblems(query: String): DomainResult<List<Problem>> = try {
        val params = mapOf("alphabet" to query, "language" to 1)
        val response = ApiHelper().callApi(context, endpoints.getAllProblems, showNoConnectionDialog = false) { url ->
            ApiClients.digidoctor_BaseURL.dynamicRawPost(url = url, body = params)
        }
        if (response.isSuccessful) {
            val body = response.body()?.string()
            val list = if (!body.isNullOrEmpty()) Gson().fromJson(body, ProblemResponse::class.java).responseValue ?: emptyList() else emptyList()
            DomainResult.Success(list)
        } else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }
}
