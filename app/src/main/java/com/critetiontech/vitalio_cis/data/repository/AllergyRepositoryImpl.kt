package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AllergyRepository
import com.critetiontech.vitalio_cis.model.AllergyApiResponse
import com.critetiontech.vitalio_cis.model.AllergyItem
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AllergyRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : AllergyRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchAllergies(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId.toString(), "typeAllergy" to typeAllergy)
        val json = prefs.getData(key = "${endpoints.fetchAllergies}_$typeAllergy", shouldSave = false) {
            val response = ApiHelper().callApi(context, endpoints.fetchAllergies, showNoConnectionDialog = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string()
            else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, AllergyApiResponse::class.java)
            if (parsed.status == 1) parsed.responseValue else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) {
        DomainResult.Error(e)
    }

    override suspend fun addAllergy(body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.addAllergies, showNoConnectionDialog = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) DomainResult.Success(Unit)
        else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
