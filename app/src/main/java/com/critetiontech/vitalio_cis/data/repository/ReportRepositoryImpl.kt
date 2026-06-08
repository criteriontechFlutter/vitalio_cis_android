package com.critetiontech.vitalio_cis.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.ReportRepository
import com.critetiontech.vitalio_cis.model.MediaItem
import com.critetiontech.vitalio_cis.model.MediaResponse
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
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

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
            .connectTimeout(60, TimeUnit.SECONDS).readTimeout(60, TimeUnit.SECONDS).writeTimeout(60, TimeUnit.SECONDS).build()
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody("image/*".toMediaTypeOrNull())).build()
        val request = Request.Builder().url("http://182.156.200.178:8016/uploadLabreport/").post(requestBody).build()
        val (isSuccessful, body) = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            Pair(response.isSuccessful, response.body?.string())
        }
        if (isSuccessful && !body.isNullOrEmpty()) {
            val jsonObject = JSONObject(body)
            val responseArray = jsonObject.getJSONArray("response")
            if (responseArray.length() > 0 && responseArray.get(0) is JSONObject) DomainResult.Success(body)
            else DomainResult.Error(Exception(responseArray.getString(0)))
        } else DomainResult.Error(Exception("Analysis failed"))
    } catch (e: Exception) { DomainResult.Error(e) }
}
