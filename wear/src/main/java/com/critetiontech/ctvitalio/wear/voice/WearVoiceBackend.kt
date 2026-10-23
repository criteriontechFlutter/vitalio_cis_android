package com.critetiontech.ctvitalio.wear.voice

import android.util.Log
import com.critetiontech.ctvitalio.voice.ScheduledMedicineRef
import com.critetiontech.ctvitalio.voice.VoiceBackend
import com.critetiontech.ctvitalio.voice.VoicePatient
import com.critetiontech.ctvitalio.wear.session.WatchSessionStore
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Watch implementation of [VoiceBackend]: calls the same Vitalio APIs as the phone
 * (see RetrofitInstance / ApiEndPointCorporateModule in :app) using the synced session.
 */
object WearVoiceBackend : VoiceBackend {

    private const val TAG = "WearVoiceBackend"

    private const val BASE_4082 = "https://vitaliocis.vitalio.care:4082/"
    private const val BASE_4096 = "https://vitaliocis.vitalio.care:4096/"

    private const val FETCH_LAST_VITAL = "api/PatientVital/FetchLastVital"
    private const val ADD_VITAL = "api/PatientVital/AddVital"
    private const val ADD_SYMPTOMS = "api/PatientPrescription/AddSymtomsOrDiagnosis"
    private const val ADD_INTAKE = "api/FoodIntake/AddIntake"
    private const val ADD_OUTPUT = "api/output/AddOutput"
    private const val FETCH_MEDICINE_INTAKE = "api/PatientMedicineIntake/FetchPatientMedicineIntake"
    private const val MARK_MEDICINE_INTAKE = "api/PatientMedicineIntake/InsertEmployeeMedicineIntake"

    private val gson = Gson()
    private val jsonType = "application/json".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    override fun currentPatient(): VoicePatient? =
        WatchSessionStore.session.value?.let { VoicePatient(it.pid, it.clientId, it.uhId) }

    override suspend fun pendingMedicines(date: String): List<ScheduledMedicineRef> {
        val patient = currentPatient() ?: return emptyList()
        val json = get(
            BASE_4082 + FETCH_MEDICINE_INTAKE,
            mapOf("pid" to patient.pid, "givenDate" to date, "clientId" to patient.clientId)
        ) ?: return emptyList()
        return try {
            val root = JSONObject(json)
            if (root.optInt("status") != 1) return emptyList()
            val periods = root.optJSONArray("responseValue") ?: return emptyList()
            buildList {
                for (i in 0 until periods.length()) {
                    val meds = periods.getJSONObject(i).optJSONArray("medicineData") ?: continue
                    for (j in 0 until meds.length()) {
                        val med = meds.getJSONObject(j)
                        if (med.optBoolean("isTaken")) continue
                        add(
                            ScheduledMedicineRef(
                                id = med.optInt("id"),
                                name = med.optString("medicineName"),
                                scheduledDateTime = med.optString("scheduledDateTime"),
                                dosageStrength = med.optDouble("dosageStrength", 0.0).toString(),
                                unit = med.optString("unit"),
                                isTaken = false
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "pendingMedicines parse failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun lastVitalReadings(): List<Pair<String, Double>> {
        val patient = currentPatient() ?: return emptyList()
        val json = get(
            BASE_4082 + FETCH_LAST_VITAL,
            mapOf("uhid" to patient.uhId, "ClientId" to patient.clientId, "userId" to patient.pid)
        ) ?: return emptyList()
        return try {
            val vitals = JSONObject(json).optJSONObject("responseValue")?.optJSONArray("lastVital")
                ?: return emptyList()
            (0 until vitals.length()).map { i ->
                val v = vitals.getJSONObject(i)
                v.optString("vitalName") to v.optDouble("vitalValue", 0.0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "lastVitalReadings parse failed: ${e.message}")
            emptyList()
        }
    }

    override suspend fun addVital(body: Map<String, Any>) = post((BASE_4082 + ADD_VITAL).toHttpUrl(), body)

    override suspend fun insertSymptoms(body: Map<String, Any>) = post((BASE_4082 + ADD_SYMPTOMS).toHttpUrl(), body)

    override suspend fun addIntake(body: Map<String, Any>) = post((BASE_4096 + ADD_INTAKE).toHttpUrl(), body)

    override suspend fun addOutput(body: Map<String, Any>) = post((BASE_4082 + ADD_OUTPUT).toHttpUrl(), body)

    override suspend fun markMedicineTaken(scheduleId: Int, scheduledDateTime: String): Boolean {
        // Same as the phone: values sent both as query params and JSON body
        val body = mapOf("id" to scheduleId, "ScheduledDateTime" to scheduledDateTime)
        val url = (BASE_4082 + MARK_MEDICINE_INTAKE).toHttpUrl().newBuilder()
            .addQueryParameter("id", scheduleId.toString())
            .addQueryParameter("ScheduledDateTime", scheduledDateTime)
            .build()
        return post(url, body)
    }

    // ── HTTP helpers ──────────────────────────────────────────────────────────

    private suspend fun get(url: String, params: Map<String, Any>): String? = withContext(Dispatchers.IO) {
        try {
            val httpUrl = url.toHttpUrl().newBuilder().apply {
                params.forEach { (k, v) -> addQueryParameter(k, v.toString()) }
            }.build()
            val request = Request.Builder().url(httpUrl).header("Content-Type", "application/json").get().build()
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body.string()
                else { Log.w(TAG, "GET ${httpUrl.encodedPath} → ${response.code}"); null }
            }
        } catch (e: Exception) {
            Log.w(TAG, "GET $url failed: ${e.message}")
            null
        }
    }

    private suspend fun post(url: HttpUrl, body: Map<String, Any>): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(url)
                .post(gson.toJson(body).toRequestBody(jsonType))
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) Log.w(TAG, "POST ${url.encodedPath} → ${response.code}")
                response.isSuccessful
            }
        } catch (e: Exception) {
            Log.w(TAG, "POST ${url.encodedPath} failed: ${e.message}")
            false
        }
    }
}