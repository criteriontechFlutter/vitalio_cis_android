package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.AllergyRepository
import com.critetiontech.ctvitalio.model.AllergyApiResponse
import com.critetiontech.ctvitalio.model.AllergyItem
import com.critetiontech.ctvitalio.utils.PrefsManager
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
            val response = ApiHelper().callApi(context, endpoints.fetchAllergies, showNoConnectionToast = false) { url ->
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

    override suspend fun addAllergy(typeAllergy: String, body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.addAllergies, showNoConnectionToast = false) { url ->
            // typeAllergy must be in the query string — the server ignores it in the body and
            // falls back to FoodAllergy, which filed medicine allergies under food.
            ApiClients.module4082.dynamicRawPost(
                url = "$url?typeAllergy=${android.net.Uri.encode(typeAllergy)}",
                body = body
            )
        }
        val status = if (response.isSuccessful) {
            runCatching {
                com.google.gson.JsonParser.parseString(response.body()?.string().orEmpty())
                    .asJsonObject.get("status")?.asInt
            }.getOrNull()
        } else null
        when {
            !response.isSuccessful -> DomainResult.Error(Exception("API Error: ${response.code()}"))
            status == 0 -> DomainResult.Error(Exception("AddAllergies returned status 0"))
            else -> DomainResult.Success(Unit)
        }
    } catch (e: Exception) {
        DomainResult.Error(e)
    }
}
