package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.config.ApiConfig

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.os.ParcelFileDescriptor
import android.util.Log
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ReportRepository
import com.critetiontech.ctvitalio.model.MediaItem
import com.critetiontech.ctvitalio.model.MediaResponse
import com.critetiontech.ctvitalio.model.PrescriptionResponseItem
import com.critetiontech.ctvitalio.ui.screens.ReportItem
import com.critetiontech.ctvitalio.utils.PrefsManager
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
private const val MAX_UPLOAD_SIDE_PX = 2000
private const val UPLOAD_JPEG_QUALITY = 80
// 745 KB passed, 948 KB got "Invalid Image Type"; stay safely under that.
private const val MAX_UPLOAD_BYTES = 700 * 1024
private const val MAX_PDF_PAGES = 3
private const val PDF_PAGE_WIDTH_PX = 1000

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

    // Concrete Content-Type for the multipart part instead of the wildcard "image/*" (matches iOS).
    private fun mimeTypeFor(file: File): String = when (file.extension.lowercase()) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        "pdf" -> "application/pdf"
        else -> "image/jpeg"
    }

    override suspend fun uploadMedia(uhid: String, clientId: String, category: String, dateTime: String, subCategory: String, remark: String, file: File): DomainResult<Unit> = try {
        val requestFile = file.asRequestBody(mimeTypeFor(file).toMediaType())
        val filePart = MultipartBody.Part.createFormData("formFile", file.name, requestFile)
        val response = ApiClients.module4082.uploadMedia(
            url = "${ApiConfig.MODULE_4082}api/PatientMediaData/AddMedia",
            uhid = uhid, category = category, dateTime = dateTime, clientId = clientId,
            subCategory = subCategory, remark = remark, formFile = filePart
        )
        if (response.isSuccessful) DomainResult.Success(Unit) else DomainResult.Error(Exception("API Error: ${response.code()}"))
    } catch (e: Exception) { DomainResult.Error(e) }

    /**
     * Prepares a file for the AI server (uploadLabreport/ and uploadPrescription/), which
     *  - answers {"response":"Invalid Image Type"} for heavy images: the same document fails as a
     *    948 KB / 2.8 MB JPEG (or 1.8 MB PNG) but reads fine as a 150–750 KB JPEG at quality 80;
     *  - answers 500 Internal Server Error for any PDF.
     * So images are re-encoded and PDFs are rendered to one JPEG. Anything that fails to
     * decode/render goes up unchanged.
     */
    private suspend fun compressForUpload(file: File): File = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = if (file.extension.equals("pdf", ignoreCase = true)) renderPdf(file) else decodeImage(file)
            bitmap ?: return@runCatching file
            val out = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            var quality = UPLOAD_JPEG_QUALITY
            do {
                out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, quality, it) }
                quality -= 15
            } while (out.length() > MAX_UPLOAD_BYTES && quality >= 40)
            bitmap.recycle()
            out
        }.getOrDefault(file)
    }

    /**
     * Renders the first [MAX_PDF_PAGES] pages of a PDF stacked top-to-bottom into one bitmap,
     * [PDF_PAGE_WIDTH_PX] wide. Tested: a 3-page stack at 1000–1200 px reads every value.
     */
    private fun renderPdf(file: File): Bitmap? {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
            PdfRenderer(fd).use { renderer ->
                val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
                if (pageCount == 0) return null
                val pages = (0 until pageCount).map { i ->
                    renderer.openPage(i).use { page ->
                        val height = (PDF_PAGE_WIDTH_PX.toFloat() * page.height / page.width).toInt()
                        Bitmap.createBitmap(PDF_PAGE_WIDTH_PX, height, Bitmap.Config.ARGB_8888).also {
                            it.eraseColor(Color.WHITE) // PDF pages are transparent by default
                            page.render(it, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        }
                    }
                }
                val stacked = Bitmap.createBitmap(PDF_PAGE_WIDTH_PX, pages.sumOf { it.height }, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(stacked)
                var top = 0f
                pages.forEach { canvas.drawBitmap(it, 0f, top, null); top += it.height; it.recycle() }
                return stacked
            }
        }
    }

    /** Decodes an image scaled to at most [MAX_UPLOAD_SIDE_PX] on its long side, upright. */
    private fun decodeImage(file: File): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_UPLOAD_SIDE_PX) sample *= 2
        val decoded = BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null

        // Camera photos are usually stored sideways with an EXIF rotation flag. Re-encoding drops
        // EXIF, so bake the rotation into the pixels or the AI gets a sideways page.
        val rotation = when (ExifInterface(file.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val scale = minOf(1f, MAX_UPLOAD_SIDE_PX.toFloat() / maxOf(decoded.width, decoded.height))
        if (scale >= 1f && rotation == 0f) return decoded
        val matrix = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
    }

    override suspend fun analyzeReport(file: File): DomainResult<String> = try {
        val original = file
        val file = compressForUpload(original)
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mimeTypeFor(file).toMediaTypeOrNull()))
            .build()

        val url = "${ApiConfig.AI_UPLOAD}uploadLabreport/"
        val request = Request.Builder().url(url).post(requestBody).build()

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "AI Analyze → POST $url")
            Log.d(TAG, "AI Analyze → file=${file.name}, size=${file.length()} bytes (original ${original.length()} bytes)")
        }

        val (isSuccessful, body) = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "AI Analyze ← [${response.code}] body=${ response.peekBody(Long.MAX_VALUE).string()}")
            }
            Pair(response.isSuccessful, response.body?.string())
        }

        if (isSuccessful && !body.isNullOrEmpty()) {
            // Success: {"response":[{...}]}. Failure: {"response":["msg"]} or {"response":"msg"}.
            val responseArray = JSONObject(body).opt("response")
            when {
                responseArray is JSONArray && responseArray.length() > 0 && responseArray.get(0) is JSONObject ->
                    DomainResult.Success(body)
                responseArray is JSONArray && responseArray.length() > 0 ->
                    DomainResult.Error(Exception(responseArray.getString(0)))
                responseArray is String -> DomainResult.Error(Exception(responseArray))
                else -> DomainResult.Error(Exception("Analysis failed"))
            }
        } else DomainResult.Error(Exception("Analysis failed"))
    } catch (e: Exception) { DomainResult.Error(e) }

    /**
     * Builds the InsertResult payload from the AI report JSON and POSTs it to
     * {MODULE_4090}api/InvestigationByPatient/InsertResult
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
        data class AiApiResponse(val response: List<com.critetiontech.ctvitalio.ui.screens.ResponseData> = emptyList())
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
            val rangeColor = com.critetiontech.ctvitalio.ui.screens.parseRangeColor(item.result, item.normal_values)
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
            Log.d(TAG, "InsertResult → POST ${ApiConfig.MODULE_4090}$endpointUrl")
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

    /**
     * Uploads a prescription image/PDF to the AI prescription reader.
     * Response: { "response": [ {patient, prescription} ] } on success,
     * or { "response": [ "<message>" ] } / { "response": "<message>" } when it could not be read.
     */
    override suspend fun uploadPrescription(file: File, mimeType: String): DomainResult<PrescriptionResponseItem> = try {
        val original = file
        val file = compressForUpload(original)
        // A re-encoded image is always JPEG; an untouched file (PDF) keeps the picker's type.
        val mimeType = if (file === original) mimeType else "image/jpeg"
        val client = OkHttpClient.Builder()
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.name, file.asRequestBody(mimeType.toMediaTypeOrNull()))
            .build()

        val url = "${ApiConfig.AI_UPLOAD}uploadPrescription/"
        val request = Request.Builder().url(url).post(requestBody).build()

        if (BuildConfig.DEBUG) Log.d(TAG, "UploadPrescription → POST $url file=${file.name}, size=${file.length()} bytes (original ${original.length()} bytes)")

        val (isSuccessful, body) = withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                Pair(response.isSuccessful, response.body?.string())
            }
        }

        if (BuildConfig.DEBUG) Log.d(TAG, "UploadPrescription ← body=$body")

        if (isSuccessful && !body.isNullOrEmpty()) {
            when (val response = JSONObject(body).opt("response")) {
                // Errors like "Invalid Image Type" / "Server is down…" come back as a bare string
                is String -> DomainResult.Error(Exception(response))
                is JSONArray -> when {
                    response.length() == 0 -> DomainResult.Error(Exception("No data returned from server"))
                    response.get(0) is JSONObject -> DomainResult.Success(
                        Gson().fromJson(response.getJSONObject(0).toString(), PrescriptionResponseItem::class.java)
                    )
                    else -> DomainResult.Error(Exception(response.getString(0)))
                }
                else -> DomainResult.Error(Exception("Prescription analysis failed"))
            }
        } else DomainResult.Error(Exception("Prescription analysis failed"))
    } catch (e: Exception) { DomainResult.Error(e) }
}

