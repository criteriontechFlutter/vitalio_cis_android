package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.FluidRepository
import com.critetiontech.vitalio_cis.model.FluidSummaryItem
import com.critetiontech.vitalio_cis.model.FluidSummaryResponse
import com.critetiontech.vitalio_cis.model.IntakeItem
import com.critetiontech.vitalio_cis.model.IntakeResponse
import com.critetiontech.vitalio_cis.model.ManualFoodAssignItem
import com.critetiontech.vitalio_cis.model.ManualFoodAssignResponse
import com.critetiontech.vitalio_cis.model.OutputItem
import com.critetiontech.vitalio_cis.model.OutputResponse
import com.critetiontech.vitalio_cis.model.OutputSummaryItem
import com.critetiontech.vitalio_cis.model.OutputSummaryResponse
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FluidRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : FluidRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun getManualFoodList(uhid: String): DomainResult<List<ManualFoodAssignItem>> = try {
        val params = mapOf("Uhid" to uhid, "intervalTimeInHour" to 24)
        val json = prefs.getData(key = endpoints.getManualFoodAssignList, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getManualFoodAssignList, showNoConnectionDialog = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, ManualFoodAssignResponse::class.java)
            if (parsed.status == 1) parsed.responseValue?.filter { !it.foodName.isNullOrBlank() } ?: emptyList() else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchIntakeItems(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>> = try {
        val params = mapOf("Uhid" to uhid, "entryType" to "N", "clientId" to clientId, "fromDate" to fromDate)
        val json = prefs.getData(key = "${endpoints.fetchIntake}_$fromDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchIntake, showNoConnectionDialog = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, IntakeResponse::class.java)
            if (parsed.status == 1) parsed.responseValue ?: emptyList() else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun addIntake(body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.addIntake, showNoConnectionDialog = false) { url ->
            ApiClients.module4094.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchOutput(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId, "userId" to userId, "fromdate" to fromDate)
        val json = prefs.getData(key = "${endpoints.fetchOutput}_$fromDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchOutput, showNoConnectionDialog = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, OutputResponse::class.java)
            if (parsed.status == 1) parsed.responseValue else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun addOutput(body: Map<String, Any>): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoints.addOutput, showNoConnectionDialog = false) { url ->
            ApiClients.module4082.dynamicRawPost(url = url, body = body)
        }
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchFluidSummary(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>> = try {
        val params = mapOf("Uhid" to uhid, "fromDate" to fromDate, "toDate" to toDate)
        val json = prefs.getData(key = "${endpoints.fluidSummaryByDateRange}_${fromDate}_$toDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fluidSummaryByDateRange, showNoConnectionDialog = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, FluidSummaryResponse::class.java)
            if (parsed.status == 1) parsed.responseValue ?: emptyList() else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchOutputSummary(uhid: String, clientId: String, fromDate: String, toDate: String): DomainResult<List<OutputSummaryItem>> = try {
        val params = mapOf("Uhid" to uhid, "clientId" to clientId, "fromDate" to fromDate, "toDate" to toDate)
        val json = prefs.getData(key = "${endpoints.outputSummaryByDateRange}_${fromDate}_$toDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.outputSummaryByDateRange, showNoConnectionDialog = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) {
            val parsed = Gson().fromJson(json, OutputSummaryResponse::class.java)
            if (parsed.status == 1) parsed.responseValue else emptyList()
        } else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }
}
