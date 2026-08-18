package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import android.util.Log
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.BuildConfig
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import com.critetiontech.vitalio_cis.model.MediaItem
import com.critetiontech.vitalio_cis.model.MediaResponse
import com.critetiontech.vitalio_cis.ui.screens.ReportItem
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ReportRepository"

@Singleton
class ReportRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PrefsManager
) : ReportRepository {
    private val endpoints = ApiEndPointCorporateModule()

    override suspend fun fetchMedia(uhid: String, clientId: String): DomainResult<List<MediaItem>> = try {
        val params = mapOf("uhid" to uhid, "ClientId" to clientId)
        val json = prefs.getData(key = endpoints.fetchMedia, shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.fetchMedia, showNoConnectionToast = false) { url ->
                ApiClients.module4082.dynamicGet(url = url, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        val list = if (!json.isNullOrEmpty()) Gson().fromJson(json, MediaResponse::class.java).responseValue ?: emptyList() else emptyList()
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun uploadMedia(uhid: String, clientId: String, category: String, dateTime: String, subCategory: String, remark: String, file: File): DomainResult<Unit> = try {
        val requestFile = file.asRequestBody("image/*".toMediaType())
        val filePart = MultipartBody.Part.createFormData("formFile", file.name, requestFile)
        val response = ApiClients.module4082.uploadMedia(
            url = "https://vitaliocis.vitalio.care:4082/api/PatientMediaData/AddMedia",
            uhid = uhid, category = category, dateTime = dateTime, clientId = clientId,
            subCategory = subCategory, remark = remark, formFile = filePart
        )
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun analyzeReport(file: File): DomainResult<String> = try {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
            .build()

        val url = "http://182.156.200.178:8016/uploadLabreport/"
        val request = Request.Builder().url(url).post(requestBody).build()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "AI Analyze → POST $url")
            Log.d(TAG, "AI Analyze → file=${file.name}, size=${file.length()} bytes")
        }

        val (isSuccessful, body) = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AI Analyze ← [${response.code}] body=${ response.peekBody(Long.MAX_VALUE).string()}")
            }
            Pair(response.isSuccessful, response.body?.string())
        }

        if (isSuccessful && !body.isNullOrEmpty()) {
            val jsonObject = JSONObject(body)
            val responseArray = jsonObject.getJSONArray("response")
            if (responseArray.length() > 0 && responseArray.get(0) is JSONObject) DomainResult.Success(body)
            else DomainResult.Error(Exception(responseArray.getString(0)))
        } else DomainResult.Error(Exception("Analysis failed"))
    } catch (e: Exception) { DomainResult.Error(e) }

    /**
     * Builds the InsertResult payload from the AI report JSON and POSTs it to
     * https://vitaliocis.vitalio.care:4090/api/InvestigationByPatient/InsertResult
     *
     * Payload structure:
     * {
     *   "userId": 0,
     *   "uhid": "<uhid>",
     *   "investigationDetailsJson": "[{receiptNo, itemName, labName, itemId, resultDateTime}]",
     *   "investigationResultJson": "[{result, resultDateTime, subTestId, unit, range, isNormal, subTestName}]",
     *   "clientId": <clientId>
     * }
     */
    override suspend fun insertInvestigationResult(
        responseJson: String,
        uhid: String,
        clientId: Int
    ): DomainResult<Unit> = try {
        val gson = Gson()

        // Parse the AI response to extract report items
        data class AiApiResponse(val response: List<com.critetiontech.vitalio_cis.ui.screens.ResponseData> = emptyList())
        val aiResponse = gson.fromJson(responseJson, AiApiResponse::class.java)
        val responseData = aiResponse.response.firstOrNull()
        val reportItems: List<ReportItem> = responseData?.report ?: emptyList()
        val patientDetails = responseData?.patient_details

        // Build investigationDetailsJson — one entry per distinct lab/item group
        val investigationDetails = JSONArray().apply {
            put(JSONObject().apply {
                put("receiptNo", "")
                put("itemName", patientDetails?.let { "${it.lab_name} Report".trim() } ?: "Lab Report")
                put("labName", patientDetails?.lab_name ?: "")
                put("itemId", "")
                put("resultDateTime", patientDetails?.collection_date ?: "")
            })
        }

        // Build investigationResultJson — one entry per ReportItem / sub-test
        val investigationResults = JSONArray()
        reportItems.forEach { item ->
            // Determine isNormal: 1 = normal (green), 0 = abnormal
            val rangeColor = com.critetiontech.vitalio_cis.ui.screens.parseRangeColor(item.result, item.normal_values)
            val isNormal = if (rangeColor == null || rangeColor == androidx.compose.ui.graphics.Color(0xFF16A34A)) "1" else "0"

            investigationResults.put(JSONObject().apply {
                put("result", item.result)
                put("resultDateTime", "")
                put("subTestId", item.id ?: "")
                put("unit", item.unit ?: "")
                put("range", item.normal_values ?: "")
                put("isNormal", isNormal)
                put("subTestName", item.test_name)
            })
        }

        // Final payload
        val payload = mapOf<String, Any>(
            "userId" to 0,
            "uhid" to uhid,
            "investigationDetailsJson" to investigationDetails.toString(),
            "investigationResultJson" to investigationResults.toString(),
            "clientId" to clientId
        )

        val endpointUrl = "api/InvestigationByPatient/InsertResult"

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "InsertResult → POST https://vitaliocis.vitalio.care:4090/$endpointUrl")
            Log.d(TAG, "InsertResult → payload=${gson.toJson(payload)}")
        }

        val response = ApiClients.module4090.dynamicRawPost(
            url = endpointUrl,
            body = payload
        )

        val responseBodyStr = response.body()?.string()
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "InsertResult ← [${response.code()}] body=$responseBodyStr")
        }

        if (response.isSuccessful) DomainResult.Success(Unit)
        else DomainResult.Error(Exception("InsertResult API Error: ${response.code()} $responseBodyStr"))
    } catch (e: Exception) {
        if (BuildConfig.DEBUG) Log.e(TAG, "InsertResult exception: ${e.message}", e)
        DomainResult.Error(e)
    }
}

