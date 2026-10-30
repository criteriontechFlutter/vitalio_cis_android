package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.data.offline.OfflineOutbox
import com.critetiontech.ctvitalio.data.offline.SendResult
import okhttp3.ResponseBody
import retrofit2.Response
import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.FluidRepository
import com.critetiontech.ctvitalio.model.FluidSummaryItem
import com.critetiontech.ctvitalio.model.FluidSummaryResponse
import com.critetiontech.ctvitalio.model.IntakeItem
import com.critetiontech.ctvitalio.model.IntakeResponse
import com.critetiontech.ctvitalio.model.ManualFoodAssignItem
import com.critetiontech.ctvitalio.model.ManualFoodAssignResponse
import com.critetiontech.ctvitalio.model.OutputItem
import com.critetiontech.ctvitalio.model.OutputResponse
import com.critetiontech.ctvitalio.model.OutputSummaryItem
import com.critetiontech.ctvitalio.model.OutputSummaryResponse
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.utils.PrefsManager
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

    // These two endpoints take ~20 s on the server, so the default 20 s read timeout cut them off
    // every time and nothing ever got cached. Give them more room.
    private val slowHeaders = mapOf(RetrofitInstance.READ_TIMEOUT_HEADER to "60")

    // Cache keys include uhid so a different patient logging in on the same device never sees
    // the previous patient's data.
    private fun manualFoodKey(uhid: String) = "${endpoints.getManualFoodAssignList}_$uhid"
    private fun intakeKey(uhid: String, fromDate: String) = "${endpoints.fetchIntake}_${uhid}_$fromDate"

    private fun parseManualFood(json: String?): List<ManualFoodAssignItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val parsed = Gson().fromJson(json, ManualFoodAssignResponse::class.java)
        return if (parsed.status == 1) parsed.responseValue?.filter { !it.foodName.isNullOrBlank() } ?: emptyList() else emptyList()
    }

    private fun parseIntake(json: String?): List<IntakeItem> {
        if (json.isNullOrEmpty()) return emptyList()
        val parsed = Gson().fromJson(json, IntakeResponse::class.java)
        return if (parsed.status == 1) parsed.responseValue ?: emptyList() else emptyList()
    }

    override fun cachedManualFoodList(uhid: String): List<ManualFoodAssignItem> =
        runCatching { parseManualFood(prefs.get(manualFoodKey(uhid), String::class.java)) }.getOrDefault(emptyList())

    override fun cachedIntakeItems(uhid: String, fromDate: String): List<IntakeItem> =
        runCatching { parseIntake(prefs.get(intakeKey(uhid, fromDate), String::class.java)) }.getOrDefault(emptyList())

    override suspend fun getManualFoodList(uhid: String): DomainResult<List<ManualFoodAssignItem>> = try {
        val params = mapOf("Uhid" to uhid, "intervalTimeInHour" to 24)
        val json = prefs.getData(key = manualFoodKey(uhid), shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getManualFoodAssignList, showNoConnectionToast = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, headers = slowHeaders, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        DomainResult.Success(parseManualFood(json))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchIntakeItems(uhid: String, clientId: String, fromDate: String): DomainResult<List<IntakeItem>> = try {
        val params = mapOf("Uhid" to uhid, "entryType" to "N", "clientId" to clientId, "fromDate" to fromDate)
        val json = prefs.getData(key = intakeKey(uhid, fromDate), shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchIntake, showNoConnectionToast = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, headers = slowHeaders, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        DomainResult.Success(parseIntake(json))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun addIntake(body: Map<String, Any>): DomainResult<Unit> =
        OfflineOutbox.submit(OfflineOutbox.Kind.INTAKE, body, ::sendIntake)

    suspend fun sendIntake(body: Map<String, Any>): SendResult = post(endpoints.addIntake, body) { url ->
        ApiClients.module4094.dynamicRawPost(url = url, body = body)
    }

    private suspend fun post(endpoint: String, body: Map<String, Any>, call: suspend (String) -> Response<ResponseBody>): SendResult = try {
        val response = ApiHelper().callApi(context, endpoint, showNoConnectionToast = false) { url -> call(url) }
        val raw = if (response.isSuccessful) null else response.errorBody()?.string()
        when {
            response.isSuccessful -> SendResult.Sent
            SendResult.isNetworkFailure(response, raw) -> SendResult.Offline
            else -> SendResult.Rejected("API Error: ${response.code()}")
        }
    } catch (e: Exception) { SendResult.Rejected(e.message ?: "API Error") }

    override suspend fun fetchOutput(uhid: String, clientId: String, userId: String, fromDate: String): DomainResult<List<OutputItem>> = try {
        val params = mapOf("uhid" to uhid, "clientId" to clientId, "userId" to userId, "fromdate" to fromDate)
        val json = prefs.getData(key = "${endpoints.fetchOutput}_$fromDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchOutput, showNoConnectionToast = false) { url ->
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

    override suspend fun addOutput(body: Map<String, Any>): DomainResult<Unit> =
        OfflineOutbox.submit(OfflineOutbox.Kind.OUTPUT, body, ::sendOutput)

    suspend fun sendOutput(body: Map<String, Any>): SendResult = post(endpoints.addOutput, body) { url ->
        ApiClients.module4082.dynamicRawPost(url = url, body = body)
    }

    override suspend fun fetchFluidSummary(uhid: String, fromDate: String, toDate: String): DomainResult<List<FluidSummaryItem>> = try {
        val params = mapOf("Uhid" to uhid, "fromDate" to fromDate, "toDate" to toDate)
        val json = prefs.getData(key = "${endpoints.fluidSummaryByDateRange}_${fromDate}_$toDate", shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fluidSummaryByDateRange, showNoConnectionToast = false) { url ->
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
            val response = ApiHelper().callApi(context, endpoints.outputSummaryByDateRange, showNoConnectionToast = false) { url ->
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
