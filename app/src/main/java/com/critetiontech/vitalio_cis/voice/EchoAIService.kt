package com.critetiontech.ctvitalio.voice

import android.util.Log
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════════════════════
//  EchoAIService
//  Android equivalent of the iOS EchoAIService.
//  Sends transcribed speech text to the Echo AI REST API and parses
//  the returned JSON into VoiceExtractedData (vitals + symptoms + fluids + medicines).
//  Includes NLP fallbacks for RBS, BP, Fluid Intake, and Fluid Output.
// ═══════════════════════════════════════════════════════════════════════════════

private const val ECHO_AI_URL = "http://food.shopright.ai:3478/api/echo/"

/** Past this the turn falls back to the on-device result instead of keeping the user waiting. */
const val ECHO_AI_TIMEOUT_MS = 8_000L

private val JSON = "application/json".toMediaType()

object EchoAIService {

    private val prefs by lazy { PrefsManager(MyApplication.appContext) }

    private fun currentDate(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private fun currentTime(): String =
        SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    // ── Main entry point ──────────────────────────────────────────────────────

    /**
     * Instant on-device extraction (no network). Shown on the confirmation card while
     * [parseSpeechToEntities] is still waiting for the AI.
     */
    fun parseLocally(text: String, pendingMedicines: List<ScheduledMedicineRef>): VoiceExtractedData =
        VoiceNlpParser.extract(text, VoiceExtractedData(), pendingMedicines)

    /**
     * Sends [text] to the Echo AI endpoint and returns all extracted entities, with the
     * on-device rules filling whatever the AI missed and implausible values dropped.
     * On network / parse failure or after [timeoutMs], returns the on-device result alone.
     * [pendingMedicines] are today's untaken doses from the session context (no extra API call).
     */
    suspend fun parseSpeechToEntities(
        text: String,
        pendingMedicines: List<ScheduledMedicineRef> = emptyList(),
        timeoutMs: Long = ECHO_AI_TIMEOUT_MS
    ): VoiceExtractedData =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.length < 2 || !trimmed.any { it.isLetter() }) return@withContext VoiceExtractedData()

            val aiResult = try {
                val patient  = prefs.getPatient()
                val uhid     = patient?.uhId ?: ""
                val userId   = patient?.pid?.toString() ?: "1"
                val clientId = patient?.clientId ?: 1  // Use actual clientId — critical for multi-tenant / enterprise

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
                    put("clientID", clientId)  // ✅ Fixed: was hardcoded to 1
                    put("medication", medicationArray)
                }
                val payload = JSONObject().apply {
                    put("text", textObj)
                }

                Log.d("EchoAI", "Request → $ECHO_AI_URL")
                Log.d("EchoAI", "Body: $payload")

                val request = Request.Builder()
                    .url(ECHO_AI_URL)
                    .post(payload.toString().toRequestBody(JSON))
                    .build()
                val client = VoiceHttp.client.newBuilder()
                    .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)   // the base client's 10s default would cut the LLM off first
                    .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                    .build()

                client.newCall(request).await().use { response ->
                    if (response.code != 200) {
                        Log.e("EchoAI", "Server returned ${response.code}")
                        null
                    } else {
                        val responseStr = response.body.string()
                        Log.d("EchoAI", "Response: $responseStr")
                        parseResponse(responseStr, text)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("EchoAI", "parseSpeechToEntities failed: ${e.message}")
                null
            }

            // AI values are range-checked too; the rules fill whatever the AI missed
            aiResult?.let { ai ->
                val rejected = VoiceNlpParser.sanitize(ai).second
                if (rejected.isNotEmpty()) Log.w("EchoAI", "Dropped implausible AI values: $rejected")
            }
            VoiceNlpParser.extract(text, aiResult ?: VoiceExtractedData(), pendingMedicines)
        }

    // ── Response parser ───────────────────────────────────────────────────────

    private fun parseResponse(json: String, originalText: String): VoiceExtractedData {
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
            fun extract(vararg keys: String) {
                for (key in keys) {
                    if (!myVital.has(key)) continue
                    val str = when (val raw = myVital.opt(key)) {
                        is Number -> if (raw.toString() != "0") raw.toString() else null
                        is String -> if (raw.isNotEmpty() && raw != "0" && raw != "N/A") raw else null
                        else -> null
                    }
                    if (str != null) { vitals[keys[0]] = str; break }
                }
            }

            extract("vmValueBPSys")
            extract("vmValueBPDias")
            extract("vmValuePulse")
            extract("vmValueSPO2")
            extract("vmValueTemperature")
            extract("vmValueHeartRate", "heartRate")
            extract("vmValueRespiratoryRate", "respiratoryRate")
            extract("vmValueRbs", "rbs", "vmValueRBS", "sugar", "bloodSugar", "blood_sugar", "glucose", "randomBloodSugar")
            extract("weight", "vmValueWeight")

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

                    val pmId = item.optInt("pdmId", 2)
                        .takeIf { it != 0 } ?: item.optInt("pmId", 2)

                    val date = item.optString("detailsDate")
                        .ifEmpty { "${currentDate()}T${currentTime()}" }

                    symptoms.add(VoiceSymptomItem(
                        detailID    = id.toString(),
                        detailsDate = date,
                        details     = name,
                        pmId        = pmId
                    ))
                }
            }

            // 3. Extract fluid intakes from AI response fluidValue
            val fluidIntakes = mutableListOf<VoiceFluidIntakeItem>()
            val detectedIntakeFoodIds = mutableSetOf<Int>()

            myVital.optJSONObject("fluidValue")?.let { fluidValue ->
                fluidValue.keys().forEach { key ->
                    val lowerKey = key.lowercase()
                    val raw = fluidValue.opt(key)
                    val qty = when (raw) {
                        is Number -> raw.toInt()
                        is String -> raw.toDoubleOrNull()?.toInt() ?: 0
                        else -> 0
                    }
                    if (qty > 0) {
                        when {
                            lowerKey.contains("water") || lowerKey.contains("pani") -> {
                                fluidIntakes.add(VoiceFluidIntakeItem(foodId = 97694, name = "Water", quantity = qty))
                                detectedIntakeFoodIds.add(97694)
                            }
                            lowerKey.contains("juice") || lowerKey.contains("ras") -> {
                                fluidIntakes.add(VoiceFluidIntakeItem(foodId = 66, name = "Juice", quantity = qty))
                                detectedIntakeFoodIds.add(66)
                            }
                            lowerKey.contains("milk") || lowerKey.contains("doodh") || lowerKey.contains("dudh") -> {
                                fluidIntakes.add(VoiceFluidIntakeItem(foodId = 76, name = "Milk", quantity = qty))
                                detectedIntakeFoodIds.add(76)
                            }
                            lowerKey.contains("tea") || lowerKey.contains("chai") -> {
                                fluidIntakes.add(VoiceFluidIntakeItem(foodId = 114973, name = "Tea", quantity = qty))
                                detectedIntakeFoodIds.add(114973)
                            }
                            lowerKey.contains("coffee") || lowerKey.contains("kafi") -> {
                                fluidIntakes.add(VoiceFluidIntakeItem(foodId = 168, name = "Coffee", quantity = qty))
                                detectedIntakeFoodIds.add(168)
                            }
                        }
                    }
                }
            }

            // 4. Extract fluid output from AI response
            var fluidOutput: VoiceFluidOutputItem? = null
            val urineRaw = myVital.opt("vmValueUrineOutput")
            val urineQtyFromAI = when (urineRaw) {
                is Number -> if (urineRaw.toInt() > 0) urineRaw.toInt() else null
                is String -> urineRaw.toIntOrNull()?.takeIf { it > 0 }
                else -> null
            }
            if (urineQtyFromAI != null) {
                fluidOutput = VoiceNlpParser.buildUrineOutput(urineQtyFromAI, originalText)
            }

            // 5. Extract medicines
            val medicines = mutableListOf<VoiceMedicineItem>()
            val medicationArray = root.optJSONObject("echo")?.optJSONArray("medication") ?: root.optJSONArray("medication")
            if (medicationArray != null && medicationArray.length() > 0) {
                val medObj = medicationArray.optJSONObject(0)
                if (medObj != null) {
                    val parsedMeds = medObj.optJSONArray("medicationNameAndDate")
                    if (parsedMeds != null) {
                        for (i in 0 until parsedMeds.length()) {
                            val pm = parsedMeds.optJSONObject(i) ?: continue
                            // AI might return it in drugName or medicineName
                            val medName = pm.optString("drugName", "").ifEmpty { pm.optString("medicineName", "") }
                            val medId = pm.optInt("medicineID", 0)
                            if (medId > 0 && medName.isNotEmpty()) {
                                medicines.add(VoiceMedicineItem(
                                    medicineId = medId,
                                    medicineName = medName,
                                    scheduledDateTime = pm.optString("scheduledDateTime", ""),
                                    instructions = pm.optString("dose", ""),
                                    frequency = pm.optString("unit", "")
                                ))
                            }
                        }
                    }
                }
            }

            Log.d("EchoAI", "Entities extracted — Vitals: $vitals | Symptoms: ${symptoms.map { it.details }} | " +
                    "Intakes: ${fluidIntakes.map { "${it.name} ${it.quantity}ml" }} | " +
                    "Output: ${fluidOutput?.let { "${it.quantity}ml (${it.colourName})" } ?: "None"} | " +
                    "Medicines: ${medicines.map { it.medicineName }}")

            VoiceExtractedData(
                vitals       = vitals.takeIf { it.isNotEmpty() },
                symptoms     = symptoms.takeIf { it.isNotEmpty() },
                fluidIntakes = fluidIntakes.takeIf { it.isNotEmpty() },
                fluidOutput  = fluidOutput,
                medicines    = medicines.takeIf { it.isNotEmpty() }
            )
        } catch (e: Exception) {
            Log.e("EchoAI", "Parse failed: ${e.message}")
            VoiceExtractedData()
        }
    }
}
