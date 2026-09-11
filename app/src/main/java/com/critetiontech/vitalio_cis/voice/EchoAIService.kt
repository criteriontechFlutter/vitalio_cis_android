package com.critetiontech.vitalio_cis.voice

import android.util.Log
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.utils.MyApplication
import com.critetiontech.vitalio_cis.utils.PrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

// ═══════════════════════════════════════════════════════════════════════════════
//  EchoAIService
//  Android equivalent of the iOS EchoAIService.
//  Sends transcribed speech text to the Echo AI REST API and parses
//  the returned JSON into VoiceExtractedData (vitals + symptoms + fluids + medicines).
//  Includes NLP fallbacks for RBS, BP, Fluid Intake, and Fluid Output.
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
     * Runs on [Dispatchers.IO]. Returns empty data on network / parse failures.
     * Applies NLP rule-based fallbacks on top of AI result (mirrors iOS hybrid approach).
     */
    suspend fun parseSpeechToEntities(text: String): VoiceExtractedData =
        withContext(Dispatchers.IO) {
            val trimmed = text.trim()
            if (trimmed.length < 2 || !trimmed.any { it.isLetter() }) return@withContext VoiceExtractedData()

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

                Log.d("EchoAI", "Request → $ECHO_AI_URL")
                Log.d("EchoAI", "Body: $payload")

                val url = java.net.URL(ECHO_AI_URL)
                val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout    = 10_000
                }

                connection.outputStream.use { it.write(payload.toString().toByteArray()) }

                if (connection.responseCode != 200) {
                    Log.e("EchoAI", "Server returned ${connection.responseCode}")
                    return@withContext applyNlpFallbacks(text, VoiceExtractedData())
                }

                val responseStr = connection.inputStream.bufferedReader().readText()
                Log.d("EchoAI", "Response: $responseStr")

                val aiResult = parseResponse(responseStr, text)
                // Apply NLP fallbacks on top of AI result for better coverage
                applyNlpFallbacks(text, aiResult)

            } catch (e: Exception) {
                Log.e("EchoAI", "parseSpeechToEntities failed: ${e.message}")
                // Even if network fails, try NLP-only extraction
                applyNlpFallbacks(text, VoiceExtractedData())
            }
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
                fluidOutput = buildUrineOutput(urineQtyFromAI, originalText.lowercase())
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

    // ── NLP Fallbacks (mirrors iOS rule-based extraction) ─────────────────────

    private suspend fun applyNlpFallbacks(text: String, existing: VoiceExtractedData): VoiceExtractedData {
        val lowerText = text.lowercase()
        var result = existing

        // --- RBS / Blood Sugar NLP fallback ---
        if (result.vitals?.containsKey("vmValueRbs") != true) {
            val rbsPatterns = listOf(
                Pattern.compile("(?:rbs|blood\\s*sugar|random\\s*blood\\s*sugar|sugar|glucose)\\s*(?:level|value|reading|is|was|=|:)?\\s*(\\d{2,3})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(\\d{2,3})\\s*(?:mg\\s*/?\\s*dl)?\\s*(?:rbs|blood\\s*sugar|sugar|glucose)", Pattern.CASE_INSENSITIVE)
            )
            for (pat in rbsPatterns) {
                val m = pat.matcher(lowerText)
                if (m.find()) {
                    val valStr  = m.group(1)?.trim()
                    val valInt  = valStr?.toIntOrNull()
                    if (valInt != null && valInt in 30..600) {
                        val newVitals = (result.vitals?.toMutableMap() ?: mutableMapOf()).also { it["vmValueRbs"] = valStr!! }
                        result = result.copy(vitals = newVitals)
                        Log.d("EchoAI", "RBS NLP fallback: $valStr")
                        break
                    }
                }
            }
        }

        // --- BP NLP fallback (e.g. "120 by 80", "120/80", "bp 130 90") ---
        if (result.vitals?.containsKey("vmValueBPSys") != true || !result.vitals.containsKey("vmValueBPDias")) {
            val bpPat = Pattern.compile("(?:bp|blood\\s*pressure)?\\s*(\\d{2,3})\\s*(?:/|\\s*by\\s*|\\s+over\\s+|\\s+and\\s+|\\s+)(\\d{2,3})", Pattern.CASE_INSENSITIVE)
            val m = bpPat.matcher(lowerText)
            if (m.find()) {
                val sys     = m.group(1)?.trim()
                val dias    = m.group(2)?.trim()
                val sysInt  = sys?.toIntOrNull()
                val diasInt = dias?.toIntOrNull()
                if (sysInt != null && diasInt != null && sysInt > diasInt && sysInt <= 300 && diasInt >= 30) {
                    val newVitals = (result.vitals?.toMutableMap() ?: mutableMapOf())
                    if (!newVitals.containsKey("vmValueBPSys")  && sys  != null) newVitals["vmValueBPSys"]  = sys
                    if (!newVitals.containsKey("vmValueBPDias") && dias != null) newVitals["vmValueBPDias"] = dias
                    result = result.copy(vitals = newVitals.takeIf { it.isNotEmpty() })
                }
            }
        }

        // --- Fluid Intake NLP fallback ---
        data class FluidSpec(val foodId: Int, val name: String, val keywords: List<String>)
        val fluidSpecs = listOf(
            FluidSpec(97694, "Water",  listOf("water", "pani", "paani")),
            FluidSpec(66,    "Juice",  listOf("juice", "ras", "sharbat")),
            FluidSpec(76,    "Milk",   listOf("milk", "doodh", "dudh")),
            FluidSpec(114973,"Tea",   listOf("tea", "chai")),
            FluidSpec(168,   "Coffee", listOf("coffee", "kafi", "cappuccino", "latte", "espresso"))
        )
        val currentFoodIds = result.fluidIntakes?.map { it.foodId }?.toSet() ?: emptySet()
        val newIntakes = (result.fluidIntakes?.toMutableList() ?: mutableListOf())

        for (spec in fluidSpecs) {
            if (spec.foodId in currentFoodIds) continue
            for (kw in spec.keywords) {
                if (!lowerText.contains(kw)) continue
                var quantity = 250 // default glass size

                // Try to extract explicit quantity near keyword
                val quantityPat = Pattern.compile(
                    "(?:(\\d+)\\s*(?:ml|milliliters?|ltr|liters?|glass(?:es)?|cups?)?\\s*(?:of\\s*)?$kw)|(?:$kw\\s*(?:of\\s*)?(\\d+)\\s*(?:ml|milliliters?|ltr|liters?|glass(?:es)?|cups?)?)",
                    Pattern.CASE_INSENSITIVE
                )
                val m = quantityPat.matcher(lowerText)
                if (m.find()) {
                    val g1  = m.group(1)?.toIntOrNull()
                    val g2  = m.group(2)?.toIntOrNull()
                    val num = g1 ?: g2
                    if (num != null) {
                        quantity = if (lowerText.contains("ltr") || lowerText.contains("liter")) num * 1000 else num
                    }
                }

                newIntakes.add(VoiceFluidIntakeItem(foodId = spec.foodId, name = spec.name, quantity = quantity))
                break
            }
        }
        if (newIntakes.isNotEmpty()) result = result.copy(fluidIntakes = newIntakes)

        // --- Fluid Output NLP fallback ---
        if (result.fluidOutput == null) {
            val urineKeywords = listOf("urine", "urination", "output", "su su", "susu", "peshab", "mutra", "peed")
            val hasUrineKeyword = urineKeywords.any { lowerText.contains(it) }
            if (hasUrineKeyword) {
                var urineQty: Int? = null
                val urinePat = Pattern.compile(
                    "(?:(\\d+)\\s*(?:ml|milliliters?)?\\s*(?:of\\s*)?(?:urine|output|peshab|urination|pee))|(?:(?:urine|output|peshab|urination|peed?)\\s*(?:of\\s*)?(\\d+)\\s*(?:ml|milliliters?)?)",
                    Pattern.CASE_INSENSITIVE
                )
                val m = urinePat.matcher(lowerText)
                if (m.find()) {
                    urineQty = (m.group(1) ?: m.group(2))?.toIntOrNull()
                }
                if (urineQty == null) urineQty = 250 // default
                result = result.copy(fluidOutput = buildUrineOutput(urineQty, lowerText))
            }
        }

        // --- Medicine NLP Fallback ---
        // If AI missed medicine, we extract it directly from pending medicines
        try {
            val patient = prefs.getPatient()
            if (patient != null) {
                val dateStr = currentDate()
                val medsRes =
                    AppDependencies.fetchMedicineIntake()(patient.pid, dateStr, patient.clientId)
                if (medsRes is DomainResult.Success) {
                    val pendingMeds = medsRes.data.flatMap { it.medicineData }.filter { !it.isTaken }
                    val currentMeds = result.medicines?.toMutableList() ?: mutableListOf()
                    val existingIds = currentMeds.map { it.medicineId }.toSet()
                    
                    var changed = false
                    for (med in pendingMeds) {
                        if (med.id in existingIds) continue
                        // Check if medicine name is spoken
                        val medNameLower = med.medicineName.lowercase().trim()
                        if (medNameLower.isNotEmpty() && lowerText.contains(medNameLower)) {
                            currentMeds.add(VoiceMedicineItem(
                                medicineId = med.id, // We use schedule id as medicineId for markIntake!
                                medicineName = med.medicineName,
                                scheduledDateTime = med.scheduledDateTime,
                                instructions = med.dosageStrength.toString(),
                                frequency = med.unit
                            ))
                            changed = true
                        }
                    }
                    if (changed) {
                        result = result.copy(medicines = currentMeds)
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("EchoAI", "Medicine NLP Fallback failed: ${e.message}")
        }

        return result
    }

    private fun buildUrineOutput(qty: Int, lowerText: String): VoiceFluidOutputItem {
        val (hex, name) = when {
            lowerText.contains("dark yellow")  -> Pair("#FFA500", "Dark Yellow")
            lowerText.contains("light yellow") -> Pair("#FFF9CC", "Light Yellow")
            lowerText.contains("yellow")       -> Pair("#FFFF00", "Yellow")
            lowerText.contains("amber")        -> Pair("#FFB347", "Amber")
            lowerText.contains("brown")        -> Pair("#A52A2A", "Brown")
            lowerText.contains("red")          -> Pair("#FF0000", "Red")
            else                               -> Pair("#FFF9CC", "Light Yellow")
        }
        return VoiceFluidOutputItem(quantity = qty, colourHex = hex, colourName = name)
    }
}
