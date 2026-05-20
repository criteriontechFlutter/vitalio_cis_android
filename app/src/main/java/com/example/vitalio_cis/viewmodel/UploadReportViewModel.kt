package com.example.vitalio_cis.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.example.vitalio_cis.model.MediaItem
import com.example.vitalio_cis.model.MediaResponse
import com.example.vitalio_cis.model.Vital
import com.example.vitalio_cis.model.VitalApiResponse
import com.example.vitalio_cis.utils.PrefsManager
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import javax.inject.Inject

class UploadReportViewModel  @Inject constructor() : ViewModel() {

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading
    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList

    fun fetchMedia(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "ClientId" to prefsCache.getPatient()?.clientId.toString(),
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().fetchMedia,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().fetchMedia,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module44374.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {

                    val apiResponse = Gson().fromJson(
                        result,
                        MediaResponse::class.java
                    )

                    // STORE FULL LIST

                    _mediaList.value = apiResponse.responseValue


                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }


    fun uploadLabreportUrl(context: Context,   ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {


                val queryParams = mapOf(

                    "uhid" to prefsCache.getPatient()?.uhId.toString(),
                    "ClientId" to prefsCache.getPatient()?.clientId.toString(),
                    "userId" to "1354",
                )

                val result: String? = prefsCache.getData(
                    key =  ApiEndPointCorporateModule().uploadLabreportUrl,

                    shouldSave = true
                ) {

                    val response = ApiHelper().callApi(
                        context,
                        ApiEndPointCorporateModule().uploadLabreportUrl,
                        showNoConnectionDialog = false
                    ) { url ->
                        ApiClients.module4082.dynamicGet(
                            url = url,
                            params = queryParams,
                        )
                    }

                    if (response.isSuccessful) {

                        val bodyString = response.body()?.string()

                        Log.d("LoginViewModel", "API Response: $bodyString")

                        bodyString   // ✅ FULL RESPONSE SAVE HOGA
                    } else {
                        throw Exception("API Error: ${response.code()}")
                    }
                }

                // ✅ RESULT HANDLE
                if (!result.isNullOrEmpty()) {




                } else {
                    Log.d("LoginViewModel", "OTP Success (API/Cache): $result")
                }

            } catch (e: Exception) {
                Log.e("LoginViewModel", "Error: ${e.message}", e)

            } finally {

                _loading.value = false
                Log.d("LoginViewModel", "Loading finished")
            }
        }
    }


    fun AddMedia(
        context: Context,
        file: File
    ) {

        viewModelScope.launch {

            _loading.value = true

            val prefsCache = PrefsManager(context)

            try {

                // ------------------------------------------------
                // FILE PART
                // ------------------------------------------------

                val requestFile = file.asRequestBody(
                    "image/*".toMediaType()
                )

                val filePart = MultipartBody.Part.createFormData(

                    "file",

                    file.name,

                    requestFile
                )

                // ------------------------------------------------
                // API CALL
                // ------------------------------------------------

                val response = ApiClients.module44374.uploadMedia(

                    url = "http://182.156.200.178:4082/api/PatientMediaData/InsertPatientMediaData",

                    uhid = prefsCache.getPatient()?.uhId.toString(),

                    category = "Investigation",

                    dateTime = "2026-02-20 16:45",

                    clientId = prefsCache.getPatient()?.clientId.toString(),

                    subCategory = "LFT",

                    remark = "est",

                    file = filePart
                )

                // ------------------------------------------------
                // RESPONSE
                // ------------------------------------------------

                if (response.isSuccessful) {

                    val bodyString = response.body()?.string()

                    Log.d(
                        "UPLOAD_API",
                        bodyString.toString()
                    )

                } else {

                    Log.e(
                        "UPLOAD_API",
                        "Error = ${response.code()}"
                    )
                }

            } catch (e: Exception) {

                Log.e(
                    "UPLOAD_API",
                    "Exception = ${e.message}"
                )

            } finally {

                _loading.value = false
            }
        }
    }


}