package com.critetiontech.vitalio_cis.voice

import android.util.Log
import com.critetiontech.vitalio_cis.utils.MyApplication
import com.critetiontech.vitalio_cis.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════════════════
//  EchoAIService
//  Android equivalent of the iOS EchoAIService.
//  Sends transcribed speech text to the Echo AI REST API and parses
//  the returned JSON into VoiceExtractedData (vitals + symptoms).
// ═══════════════════════════════════════════════════════════════════════════════

private const val ECHO_AI_URL = "http://food.shopright.ai:3478/api/echo/"

object EchoAIService {

    private val prefs by lazy { PrefsManager(MyApplication.appContext) }

    private fun currentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Sends [text] to the Echo AI endpoint and returns all extracted entities.
     * Runs on [Dispatchers.IO]. Throws on network / parse failures.
     */
    suspend fun parseSpeechToEntities(text: String): VoiceExtractedData =
        withContext(Dispatchers.IO) {
            try {
                val patient = prefs.getPatient()
                val uhid   = patient?.uhId ?: ""
                val userId = patient?.pid?.toString() ?: "1"

                // Build JSON payload matching the iOS structure exactly
                val medicationArray = org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("drugName", org.json.JSONArray())
                        put("medicationNameAndDate", org.json.JSONArray())
                    })
                }
                val textObj = JSONObject().apply {
                    put("text", text)
                    put("userid", userId)
                    put("uhid", uhid)
                    put("date", currentDate())
                    put("time", currentTime())
                    put("clientID", 1)
                    put("medication", medicationArray)
                }
                val payload = JSONObject().apply {
                    put("text", textObj)
                }

                val url = java.net.URL(ECHO_AI_URL)
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                connection.outputStream.use { it.write(payload.toString().toByteArray()) }

                if (connection.responseCode != 200) {
                    Log.e("EchoAI", "Server returned ${connection.responseCode}")
                    return@withContext VoiceExtractedData()
                }

                val responseStr = connection.inputStream.bufferedReader().readText()
                Log.d("EchoAI", "Response: $responseStr")

                parseResponse(responseStr)
            } catch (e: Exception) {
                Log.e("EchoAI", "parseSpeechToEntities failed: ${e.message}")
                VoiceExtractedData()
            }
        }

    // ── Response parser ───────────────────────────────────────────────────────

    private fun parseResponse(json: String): VoiceExtractedData {
        return try {
            val root = JSONObject(json)

            // Navigate: root["echo"]["myvital"] or root["myvital"] or root
            val myVital: JSONObject = when {
                root.has("echo") -> {
                    val echo = root.getJSONObject("echo")
                    if (echo.has("myvital")) echo.getJSONObject("myvital") else echo
                }
                root.has("myvital") -> root.getJSONObject("myvital")
                else -> root
            }

            // 1. Extract vitals
            val vitals = mutableMapOf<String, String>()
            fun extract(key: String) {
                if (!myVital.has(key)) return
                val raw = myVital.opt(key)
                val str = when (raw) {
                    is Number -> if (raw.toString() != "0") raw.toString() else null
                    is String -> if (raw.isNotEmpty() && raw != "0" && raw != "N/A") raw else null
                    else -> null
                }
                if (str != null) vitals[key] = str
            }

            extract("vmValueBPSys")
            extract("vmValueBPDias")
            extract("vmValuePulse")
            extract("vmValueSPO2")
            extract("vmValueTemperature")
            extract("vmValueHeartRate")
            extract("vmValueRespiratoryRate")
            extract("vmValueRbs")
            extract("weight")

            // 2. Extract symptoms
            val vitalKeywords = setOf(
                "heart", "heart rate", "pulse", "bp", "blood pressure",
                "temperature", "temp", "spo2", "respiratory rate", "weight"
            )

            val symptomsJson: org.json.JSONArray? = when {
                myVital.has("symptomsList") -> myVital.optJSONArray("symptomsList")
                myVital.has("symptoms")     -> myVital.optJSONArray("symptoms")
                myVital.has("problems")     -> myVital.optJSONArray("problems")
                else -> null
            }

            val symptoms = mutableListOf<VoiceSymptomItem>()
            symptomsJson?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    val name = item.optString("symptom")
                        .ifEmpty { item.optString("details") }
                        .ifEmpty { item.optString("problemName") }
                        .trim()

                    if (name.isEmpty()) continue
                    if (name.lowercase() in vitalKeywords) {
                        Log.w("EchoAI", "Filtering false-positive vital from symptoms: $name")
                        continue
                    }

                    val id = item.optInt("id", 0)
                        .takeIf { it != 0 }
                        ?: item.optInt("detailID", 0)
                        ?: item.optInt("problemId", 0)

                    val pmId = item.optInt("pdmId", 2)
                        .takeIf { it != 0 } ?: item.optInt("pmId", 2)

                    val date = item.optString("detailsDate")
                        .ifEmpty { "${currentDate()}T${currentTime()}" }

                    symptoms.add(VoiceSymptomItem(
                        detailID = id.toString(),
                        detailsDate = date,
                        details = name,
                        pmId = pmId
                    ))
                }
            }

            Log.d("EchoAI", "Entities extracted — Vitals: $vitals | Symptoms: ${symptoms.map { it.details }}")

            VoiceExtractedData(
                vitals = vitals.takeIf { it.isNotEmpty() },
                symptoms = symptoms.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            Log.e("EchoAI", "Parse failed: ${e.message}")
            VoiceExtractedData()
        }
    }

    // ── Backward-compat helpers ───────────────────────────────────────────────

    suspend fun parseSpeechToVitals(text: String): Map<String, String> =
        parseSpeechToEntities(text).vitals ?: emptyMap()

    suspend fun parseSpeechToSymptoms(text: String): List<VoiceSymptomItem> =
        parseSpeechToEntities(text).symptoms ?: emptyList()
}
