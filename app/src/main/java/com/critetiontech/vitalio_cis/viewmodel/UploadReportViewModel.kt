package com.critetiontech.vitalio_cis.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.model.MediaItem
import com.critetiontech.vitalio_cis.model.MediaResponse
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class UploadReportViewModel @Inject constructor() : ViewModel() {

    // -------------------------------------------------
    // LOADING
    // -------------------------------------------------

    private val _loading =
        MutableStateFlow(false)

    val loading: StateFlow<Boolean> =
        _loading

    // -------------------------------------------------
    // MEDIA LIST
    // -------------------------------------------------

    private val _mediaList =
        MutableStateFlow<List<MediaItem>>(emptyList())

    val mediaList:
            StateFlow<List<MediaItem>> =
        _mediaList

    // -------------------------------------------------
    // RESPONSE STRING
    // -------------------------------------------------

    // -------------------------------------------------
    // FETCH MEDIA
    // -------------------------------------------------

    fun fetchMedia(
        context: Context
    ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache =
                PrefsManager(context)

            try {

                val queryParams = mapOf(

                    "uhid" to
                            prefsCache.getPatient()
                                ?.uhId.toString(),

                    "ClientId" to
                            prefsCache.getPatient()
                                ?.clientId.toString()
                )

                val result: String? =
                    prefsCache.getData(

                        key =
                            ApiEndPointCorporateModule()
                                .fetchMedia,

                        shouldSave = true

                    ) {

                        val response =
                            ApiHelper().callApi(

                                context,

                                ApiEndPointCorporateModule()
                                    .fetchMedia,

                                showNoConnectionDialog =
                                    false

                            ) { url ->

                                ApiClients.module4082
                                    .dynamicGet(

                                        url = url,

                                        params =
                                            queryParams
                                    )
                            }

                        if (response.isSuccessful) {

                            response.body()?.string()

                        } else {

                            throw Exception(
                                "API Error: ${response.code()}"
                            )
                        }
                    }

                if (!result.isNullOrEmpty()) {

                    val apiResponse =
                        Gson().fromJson(

                            result,

                            MediaResponse::class.java
                        )

                    _mediaList.value =
                        apiResponse.responseValue
                            ?: emptyList()
                }

            } catch (e: Exception) {

                Log.e(
                    "FETCH_MEDIA",
                    "Error = ${e.message}",
                    e
                )

            } finally {

                _loading.value = false
            }
        }
    }

    // -------------------------------------------------
    // AI REPORT API
    // -------------------------------------------------

    private val _responseString =
        MutableStateFlow("")

    val responseString:
            StateFlow<String> =
        _responseString

    // -------------------------------------------------
    // AI REPORT API
    // -------------------------------------------------

    fun aiReport(
        file: File,
        navController: NavController
    ) {

        viewModelScope.launch {

            try {

                _loading.value = true

                Log.d(
                    "AI_REPORT",
                    "Attempt started"
                )

                val client =
                    OkHttpClient.Builder()

                        .connectTimeout(
                            60,
                            TimeUnit.SECONDS
                        )

                        .readTimeout(
                            60,
                            TimeUnit.SECONDS
                        )

                        .writeTimeout(
                            60,
                            TimeUnit.SECONDS
                        )

                        .build()

                // -----------------------------------------
                // MULTIPART
                // -----------------------------------------

                val requestBody =
                    MultipartBody.Builder()

                        .setType(
                            MultipartBody.FORM
                        )

                        .addFormDataPart(

                            "file",

                            file.name,

                            file.asRequestBody(
                                "image/*"
                                    .toMediaTypeOrNull()
                            )
                        )

                        .build()

                // -----------------------------------------
                // REQUEST
                // -----------------------------------------

                val request =
                    Request.Builder()

                        .url(
                            "http://182.156.200.178:8016/uploadLabreport/"
                        )

                        .post(requestBody)

                        .build()

                // -----------------------------------------
                // API CALL
                // -----------------------------------------

                val response: Response =

                    withContext(
                        Dispatchers.IO
                    ) {

                        client.newCall(request)
                            .execute()
                    }

                // -----------------------------------------
                // RESPONSE STRING
                // -----------------------------------------

                val responseString =
                    response.body?.string()

                Log.d(
                    "AI_REPORT",
                    "Response = $responseString"
                )

                // -----------------------------------------
                // HANDLE RESPONSE
                // -----------------------------------------

                if (
                    response.isSuccessful &&
                    !responseString.isNullOrEmpty()
                ) {

                    try {

                        val jsonObject =
                            JSONObject(responseString)

                        val responseArray =
                            jsonObject.getJSONArray(
                                "response"
                            )

                        if (
                            responseArray.length() > 0 &&
                            responseArray.get(0)
                                    is JSONObject
                        ) {

                            // -----------------------------------------
                            // SAVE RESPONSE
                            // -----------------------------------------

                            _responseString.value =
                                responseString

                            Log.d(
                                "AI_REPORT",
                                "String Saved Successfully"
                            )

                            // -----------------------------------------
                            // NAVIGATE
                            // -----------------------------------------

                            withContext(
                                Dispatchers.Main
                            ) {

                                navController.navigate(
                                    Routes.AIREPORT
                                )
                            }

                        } else {

                            val message =
                                responseArray
                                    .getString(0)

                            Log.e(
                                "AI_REPORT",
                                "API MESSAGE = $message"
                            )
                        }

                    } catch (e: Exception) {

                        Log.e(
                            "AI_REPORT",
                            "Parsing Error = ${e.message}",
                            e
                        )
                    }
                }

            } catch (e: Exception) {

                Log.e(
                    "AI_REPORT",
                    "Error = ${e.message}",
                    e
                )

            } finally {

                _loading.value = false
            }
        }
    }
    // -------------------------------------------------
    // ADD MEDIA
    // -------------------------------------------------

    fun AddMedia(
        context: Context,
        file: File
    ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache =
                PrefsManager(context)

            try {

                val requestFile =
                    file.asRequestBody(
                        "image/*".toMediaType()
                    )

                val filePart =
                    MultipartBody.Part
                        .createFormData(

                            "formFile",

                            file.name,

                            requestFile
                        )

                val response =
                    ApiClients.module4082
                        .uploadMedia(

                            url =
                                "https://vitaliocis.vitalio.care:4082/api/PatientMediaData/AddMedia",

                            uhid =
                                prefsCache.getPatient()
                                    ?.uhId ?: "",

                            category =
                                "Investigation",

                            dateTime =
                                "2026-02-20",

                            clientId =
                                prefsCache.getPatient()
                                    ?.clientId.toString(),

                            subCategory =
                                "LFT",

                            remark =
                                "test",

                            formFile =
                                filePart
                        )

                if (response.isSuccessful) {

                    val bodyString =
                        response.body()?.string()

                    Log.d(
                        "UPLOAD_API",
                        "SUCCESS = $bodyString"
                    )

                } else {

                    Log.e(
                        "UPLOAD_API",
                        "Error = ${response.code()} ${response.message()}"
                    )

                    Log.e(
                        "UPLOAD_API",
                        "ErrorBody = ${
                            response.errorBody()?.string()
                        }"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "UPLOAD_API",
                    "Exception = ${e.message}",
                    e
                )

            } finally {

                _loading.value = false
            }
        }
    }

//    fun insertInvestigation(
//
//        context: Context,
//
//        reportData: ApiResponse
//
//    ) {
//
//        viewModelScope.launch {
//
//            try {
//
//                _loading.value = true
//
//                // -----------------------------------------
//                // TEMP LIST
//                // -----------------------------------------
//
//                val tempPatientData =
//                    mutableListOf<Map<String, Any?>>()
//
//                val tempReportData =
//                    mutableListOf<Map<String, Any?>>()
//
//                // -----------------------------------------
//                // RESPONSE LOOP
//                // -----------------------------------------
//
//                val responseList =
//                    reportData.response
//
//                responseList.forEach { response ->
//
//                    // -----------------------------------------
//                    // PATIENT DATA
//                    // -----------------------------------------
//
//                    tempPatientData.add(
//
//                        mapOf(
//
//                            "itemName" to "",
//
//                            "itemId" to "",
//
//                            "labName" to
//                                    response.patient_details.lab_name,
//
//                            "receiptNo" to "",
//
//                            "resultDateTime" to
//                                    "2024-12-10 13:11"
//                        )
//                    )
//
//                    // -----------------------------------------
//                    // REPORT DATA
//                    // -----------------------------------------
//
//                    response.report.forEach { item ->
//
//                        tempReportData.add(
//
//                            mapOf(
//
//                                "subTestId" to
//                                        item.id.toString(),
//
//                                "subTestName" to
//                                        item.test_name,
//
//                                "range" to
//                                        item.normal_values,
//
//                                "resultDateTime" to
//                                        response.patient_details.collection_date,
//
//                                // -----------------------------------------
//                                // EDITED VALUE
//                                // -----------------------------------------
//
//                                "result" to
//                                        item.result,
//
//                                "unit" to
//                                        item.unit,
//
//                                "isNormal" to
//                                        "1"
//                            )
//                        )
//                    }
//                }
//
//                // -----------------------------------------
//                // PREFS
//                // -----------------------------------------
//
//                val prefs =
//                    PrefsManager(context)
//
//                // -----------------------------------------
//                // BODY
//                // -----------------------------------------
//
//                val body = hashMapOf(
//
//                    "uhid" to
//                            prefs.getPatient()
//                                ?.uhId.toString(),
//
//                    "investigationDetailsJson" to
//                            Gson().toJson(
//                                tempPatientData
//                            ),
//
//                    "clientId" to
//                            prefs.getPatient()
//                                ?.clientId.toString(),
//
//                    "investigationResultJson" to
//                            Gson().toJson(
//                                tempReportData
//                            )
//                )
//
//                // -----------------------------------------
//                // LOG BODY
//                // -----------------------------------------
//
//                Log.d(
//
//                    "INSERT_API_BODY",
//
//                    Gson().toJson(body)
//                )
//
//                // -----------------------------------------
//                // API CALL
//                // -----------------------------------------
//
//                val response =
//
//                    ApiClients.module4082
//                        .rawPost(
//
//                            url =
//                                "api/InvestigationByPatient/InsertResult",
//
//                            body = body
//                        )
//
//                // -----------------------------------------
//                // RESPONSE
//                // -----------------------------------------
//
//                if (response.isSuccessful) {
//
//                    val responseBody =
//                        response.body()?.string()
//
//                    Log.d(
//                        "INSERT_API",
//                        "SUCCESS = $responseBody"
//                    )
//
//                } else {
//
//                    Log.e(
//                        "INSERT_API",
//                        "FAILED = ${response.code()}"
//                    )
//
//                    Log.e(
//                        "INSERT_API",
//                        response.errorBody()
//                            ?.string()
//                            .toString()
//                    )
//                }
//
//            } catch (e: Exception) {
//
//                Log.e(
//                    "INSERT_API",
//                    e.message.toString(),
//                    e
//                )
//
//            } finally {
//
//                _loading.value = false
//            }
//        }
//    }
}

// -------------------------------------------------
// FILE UTILS
// -------------------------------------------------

object FileUtils {

    fun getFile(
        context: Context,
        uri: Uri
    ): File {

        val inputStream =
            context.contentResolver
                .openInputStream(uri)

        val file = File(
            context.cacheDir,
            "upload.jpg"
        )

        inputStream?.use { input ->

            file.outputStream().use { output ->

                input.copyTo(output)
            }
        }

        return file
    }
}