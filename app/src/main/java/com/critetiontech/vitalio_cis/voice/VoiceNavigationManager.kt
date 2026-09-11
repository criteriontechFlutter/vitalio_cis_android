package com.critetiontech.vitalio_cis.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.util.Base64
import android.util.Log
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceNavigationManager  (singleton)
//  Android equivalent of the iOS VoiceNavigationManager.
//
//  Session flow:
//   1. startListening() → connects WebSocket → starts AudioRecord at 16kHz
//   2. Audio is chunked into 512 Float32 samples → Base64 → sent over WebSocket
//   3. Backend responds with {original_text, translation} or legacy {text}
//   4. If translation is a navigation command → handleCommand()
//   5. Otherwise send translation to EchoAI → extract vitals + symptoms + fluids + medicines
//   6. Show UnifiedConfirmationCard if data was extracted
//   7. stopListening() / confirmAndSaveAllPendingData() / cancelPendingData()
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VoiceNavManager"

// WebSocket URL for the dictation backend (matching the iOS config)
private const val DICTATION_WS_BASE = "ws://182.156.200.178:8023/ws"

// Data indicators to prevent treating health data dictation as navigation commands
private val DATA_INDICATORS = listOf(
    "ml", "bpm", "mmhg", "drank", "drink", "had", "passed", "output", "intake",
    "record", "log", "piya", "paani", "glass", "cup", "fever", "pain", "headache",
    "cough", "pressure", "rate", "pulse", "spo2", "sugar", "rbs", "temperature", "weight"
)

// Navigation command keywords (mirrors iOS containsNavigationCommand)
private val NAV_KEYWORDS = listOf(
    "home", "dashboard", "vital", "fluid", "water", "medicine", "pill",
    "report", "symptom", "appointment", "doctor", "article", "research",
    "activity", "back", "profile", "dark mode", "appearance",
    "reminder", "lab", "diet", "allergy", "faq", "feedback",
    "emergency", "shared", "observer", "family"
)

object VoiceNavigationManager {

    // ── Exposed state flows ───────────────────────────────────────────────────

    private val _isListening           = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcript            = MutableStateFlow("")
    val transcript: StateFlow<String>  = _transcript.asStateFlow()

    private val _showListeningOverlay  = MutableStateFlow(false)
    val showListeningOverlay: StateFlow<Boolean> = _showListeningOverlay.asStateFlow()

    private val _showConfirmationCard  = MutableStateFlow(false)
    val showConfirmationCard: StateFlow<Boolean> = _showConfirmationCard.asStateFlow()

    private val _pendingData           = MutableStateFlow<VoiceExtractedData?>(null)
    val pendingData: StateFlow<VoiceExtractedData?> = _pendingData.asStateFlow()

    private val _isSavingData          = MutableStateFlow(false)
    val isSavingData: StateFlow<Boolean> = _isSavingData.asStateFlow()

    /** True while Echo AI network call is in progress */
    private val _isExtractingData      = MutableStateFlow(false)
    val isExtractingData: StateFlow<Boolean> = _isExtractingData.asStateFlow()

    /** Last transcript text being processed by Echo AI */
    private val _processingTranscript  = MutableStateFlow("")
    val processingTranscript: StateFlow<String> = _processingTranscript.asStateFlow()

    // Navigation command result – consumed once by the UI
    private val _navigationCommand     = MutableStateFlow<String?>(null)
    val navigationCommand: StateFlow<String?> = _navigationCommand.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioRecord: AudioRecord? = null
    private var audioJob: Job? = null
    private var aiExtractionJob: Job? = null
    private var aiDebounceJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isStarting = false
    private var lastOriginalText = ""
    private var lastTranslation = ""
    private var lastProcessedAIText = ""

    // Audio: 512 Float32 samples per packet → 2048 bytes → Base64
    private const val SAMPLE_RATE    = 16000
    private const val PACKET_SAMPLES = 512
    // ArrayDeque for O(1) removeFirst() — was mutableListOf which caused O(n²) jitter
    private val floatBuffer = ArrayDeque<Float>(PACKET_SAMPLES * 8)



    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // keep alive for WS
            .build()
    }

    private val prefs by lazy { AppDependencies.prefs }

    // VAD (Voice Activity Detection) state
    private var lastSpeechTime = 0L
    private var hasSpoken = false

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    fun startListening(onNavigationCommand: (String) -> Unit) {
        if (_isListening.value || isStarting) {
            stopListening()
            return
        }
        isStarting = true

        _transcript.value           = ""
        _processingTranscript.value = ""
        lastOriginalText            = ""
        lastTranslation             = ""
        lastProcessedAIText         = ""
        floatBuffer.clear()
        wsReconnectAttempts         = 0   // Fresh reconnect budget for every session
        
        lastSpeechTime = System.currentTimeMillis()
        hasSpoken = false


        _showListeningOverlay.value = true

        scope.launch {
            try {
                connectWebSocket(onNavigationCommand)
                startAudioCapture()

                withContext(Dispatchers.Main) { 
                    _isListening.value = true 
                }
                isStarting = false

            } catch (e: Exception) {
                Log.e(TAG, "startListening error: ${e.message}")
                isStarting = false
                stopListening()
            }
        }
    }

    fun stopListening() {
        // Flush any pending AI text before shutting down (mirrors iOS)
        val aiText = if (lastTranslation.isNotEmpty()) lastTranslation else lastOriginalText
        val cleanedAI = aiText.trim()
        if (cleanedAI.isNotEmpty() && cleanedAI != lastProcessedAIText && !containsNavigationCommand(cleanedAI.lowercase())) {
            aiDebounceJob?.cancel()
            triggerAIExtraction(cleanedAI)
        }

        cleanup()
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
            // Keep overlay open if reviewing confirmation card OR if actively extracting data
            if (!_showConfirmationCard.value && !_isExtractingData.value) {
                _showListeningOverlay.value = false
            }
        }
    }

    fun cancelPendingData() {
        aiExtractionJob?.cancel()
        aiDebounceJob?.cancel()
        _pendingData.value              = null
        _showConfirmationCard.value     = false
        _showListeningOverlay.value     = false
        _transcript.value               = ""
        _processingTranscript.value     = ""
        _isExtractingData.value         = false
        stopListening()
    }

    // ── REMOVE ITEM FUNCTIONS ─────────────────────────────────────────────────

    fun removeVital(keys: List<String>) {
        val data = _pendingData.value ?: return
        val newVitals = data.vitals?.toMutableMap()?.also { v -> keys.forEach { v.remove(it) } }
        val updated = data.copy(vitals = newVitals?.takeIf { it.isNotEmpty() })
        applyUpdatedData(updated)
    }

    fun removeSymptom(detailID: String, details: String) {
        val data = _pendingData.value ?: return
        val newSymptoms = data.symptoms?.filterNot {
            it.detailID == detailID || it.details.equals(details, ignoreCase = true)
        }
        val updated = data.copy(symptoms = newSymptoms?.takeIf { it.isNotEmpty() })
        applyUpdatedData(updated)
    }

    fun removeFluidIntake(id: String) {
        val data = _pendingData.value ?: return
        val newIntakes = data.fluidIntakes?.filterNot { it.id == id }
        val updated = data.copy(fluidIntakes = newIntakes?.takeIf { it.isNotEmpty() })
        applyUpdatedData(updated)
    }

    fun removeFluidOutput() {
        val data = _pendingData.value ?: return
        val updated = data.copy(fluidOutput = null)
        applyUpdatedData(updated)
    }

    fun removeMedicine(id: String) {
        val data = _pendingData.value ?: return
        val newMedicines = data.medicines?.filterNot { it.id == id }
        val updated = data.copy(medicines = newMedicines?.takeIf { it.isNotEmpty() })
        applyUpdatedData(updated)
    }

    private fun applyUpdatedData(updated: VoiceExtractedData) {
        if (updated.isEmpty) {
            _pendingData.value          = null
            _showConfirmationCard.value = false
            _showListeningOverlay.value = false
        } else {
            _pendingData.value = updated
        }
    }

    // ── SAVE ALL ──────────────────────────────────────────────────────────────

    fun confirmAndSaveAllPendingData() {
        val data = _pendingData.value ?: return
        if (_isSavingData.value) return
        _isSavingData.value = true

        scope.launch {
            val summaries = mutableListOf<String>()

            // 1. Save vitals
            data.vitals?.takeIf { it.isNotEmpty() }?.let { vitals ->
                try { summaries.add(saveVitals(vitals)) }
                catch (e: Exception) { Log.e(TAG, "Save vitals error: ${e.message}") }
            }

            // 2. Save symptoms
            data.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                try { summaries.add(saveSymptoms(symptoms)) }
                catch (e: Exception) { Log.e(TAG, "Save symptoms error: ${e.message}") }
            }

            // 3. Save fluid intakes
            data.fluidIntakes?.takeIf { it.isNotEmpty() }?.let { intakes ->
                try { summaries.add(saveFluidIntakes(intakes)) }
                catch (e: Exception) { Log.e(TAG, "Save fluid intakes error: ${e.message}") }
            }

            // 4. Save fluid output
            data.fluidOutput?.let { output ->
                try { summaries.add(saveFluidOutput(output)) }
                catch (e: Exception) { Log.e(TAG, "Save fluid output error: ${e.message}") }
            }

            // 5. Save medicines
            data.medicines?.takeIf { it.isNotEmpty() }?.let { medicines ->
                try { summaries.add(saveMedicines(medicines)) }
                catch (e: Exception) { Log.e(TAG, "Save medicines error: ${e.message}") }
            }

            withContext(Dispatchers.Main) {
                _isSavingData.value         = false
                _pendingData.value          = null
                _showConfirmationCard.value = false
                _transcript.value = if (summaries.isEmpty()) "Data saved successfully" else summaries.joinToString(" | ")

                delay(2000)
                stopListening()
            }
        }
    }

    /** Call when the navigation event has been consumed */
    fun clearNavigationCommand() {
        _navigationCommand.value = null
    }

    // ── AI EXTRACTION ─────────────────────────────────────────────────────────

    private fun triggerAIExtraction(text: String) {
        val cleaned = text.trim()
        if (cleaned.length < 2 || !cleaned.any { it.isLetter() }) return
        if (cleaned == lastProcessedAIText) return
        lastProcessedAIText = cleaned

        // Synchronously set state so stopListening() knows we are extracting
        _processingTranscript.value = cleaned
        _isExtractingData.value     = true

        aiExtractionJob?.cancel()
        aiExtractionJob = scope.launch {
            try {
                Log.d(TAG, "Sending to Echo AI: $cleaned")
                val extracted = EchoAIService.parseSpeechToEntities(cleaned)
                withContext(Dispatchers.Main) {
                    _isExtractingData.value = false
                    if (!extracted.isEmpty) {
                        val existing = _pendingData.value
                        _pendingData.value          = existing?.merge(extracted) ?: extracted
                        _showConfirmationCard.value = true
                    } else if (!_isListening.value) {
                        // AI found nothing and we stopped listening -> close overlay
                        _showListeningOverlay.value = false
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { 
                    _isExtractingData.value = false
                    if (!_isListening.value && !_showConfirmationCard.value) {
                        _showListeningOverlay.value = false
                    }
                }
                Log.e(TAG, "EchoAI extraction error: ${e.message}")
            }
        }
    }

    // ── WEBSOCKET ─────────────────────────────────────────────────────────────

    private var wsReconnectAttempts = 0
    private const val WS_MAX_RECONNECT = 3

    /**
     * Schedules a reconnect attempt with exponential backoff.
     * Extracted into a named function so Kotlin can resolve the type without
     * hitting the recursive inference problem caused by calling a suspend fun
     * from inside an anonymous WebSocketListener object literal.
     */
    private fun scheduleReconnect(onNavigationCommand: (String) -> Unit): Unit {
        wsReconnectAttempts++
        val delayMs: Long = 500L * (1 shl (wsReconnectAttempts - 1))   // 500, 1000, 2000 ms
        Log.d(TAG, "Reconnecting WebSocket in ${delayMs}ms (attempt $wsReconnectAttempts/$WS_MAX_RECONNECT)")
        scope.launch(Dispatchers.IO) {
            delay(delayMs)
            if (_isListening.value) {
                try { connectWebSocket(onNavigationCommand) }
                catch (e: Exception) { Log.e(TAG, "Reconnect failed: ${e.message}") }
            }
        }
    }

    private suspend fun connectWebSocket(onNavigationCommand: (String) -> Unit): Unit =
        withContext(Dispatchers.IO) {
            val patient  = prefs.getPatient()
            val userId   = patient?.pid?.toString() ?: "625"
            val language = "hindi_v2/drpdvoiceagent"
            val dept     = "GENERAL_MEDICINE"

            val wsUrl = "$DICTATION_WS_BASE/$language/$dept/$userId/"
            Log.d(TAG, "Connecting WebSocket: $wsUrl")

            val request = Request.Builder().url(wsUrl).build()
            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                    wsReconnectAttempts = 0  // Reset on successful connect
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleWebSocketMessage(text, onNavigationCommand)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    handleWebSocketMessage(bytes.utf8(), onNavigationCommand)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure (attempt $wsReconnectAttempts): ${t.message}")
                    if (_isListening.value && wsReconnectAttempts < WS_MAX_RECONNECT) {
                        // Call named helper — avoids recursive type-inference inside anonymous object
                        scheduleReconnect(onNavigationCommand)
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                }
            })
        }


    private fun handleWebSocketMessage(raw: String, onNavigationCommand: (String) -> Unit) {
        Log.d(TAG, "WS RAW: $raw")
        var originalText: String? = null
        var translation: String?  = null
        var isFinalSegment        = false

        try {
            val json = org.json.JSONObject(raw)
            if (json.optString("type") == "final") isFinalSegment = true
            originalText = json.optString("original_text").trim().takeIf { it.isNotEmpty() }
            translation  = json.optString("translation").trim().takeIf { it.isNotEmpty() }

            // Legacy fallback
            if (originalText == null && translation == null) {
                originalText = json.optString("text").trim().takeIf { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            if (!raw.startsWith("{")) originalText = raw.trim().takeIf { it.isNotEmpty() }
        }

        scope.launch(Dispatchers.Main) {
            originalText?.let { lastOriginalText = it; _transcript.value = it }
            translation?.let  { lastTranslation  = it }

            val uiText = if (lastOriginalText.isNotEmpty()) lastOriginalText else lastTranslation
            val aiText = if (lastTranslation.isNotEmpty())  lastTranslation  else lastOriginalText

            if (uiText.isEmpty() && aiText.isEmpty()) return@launch

            val loweredUI = uiText.lowercase()
            val loweredAI = aiText.lowercase()

            // 1. Navigation command detection
            if (containsNavigationCommand(loweredUI) || containsNavigationCommand(loweredAI)) {
                val cmdText = if (containsNavigationCommand(loweredUI)) loweredUI else loweredAI
                val route   = resolveNavigationRoute(cmdText)
                if (route != null) {
                    onNavigationCommand(route)
                    stopListening()
                }
                return@launch
            }

            // 2. EchoAI entity extraction – immediate on final, debounced 1s on partial
            if (aiText.isNotEmpty()) {
                if (isFinalSegment) {
                    aiDebounceJob?.cancel()
                    triggerAIExtraction(aiText)
                } else {
                    aiDebounceJob?.cancel()
                    aiDebounceJob = scope.launch {
                        delay(1_000)
                        triggerAIExtraction(aiText)
                    }
                }
            }
        }
    }

    // ── AUDIO CAPTURE ─────────────────────────────────────────────────────────

    @SuppressLint("MissingPermission")
    private fun startAudioCapture() {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufSize = maxOf(minBuf, PACKET_SAMPLES * 2 * 4)

        // Using VOICE_RECOGNITION instead of MIC for OS-level noise suppression and auto-gain
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        )

        // Attempt to explicitly enable hardware Noise Suppressor
        try {
            if (android.media.audiofx.NoiseSuppressor.isAvailable()) {
                android.media.audiofx.NoiseSuppressor.create(audioRecord!!.audioSessionId)?.enabled = true
            }
            if (android.media.audiofx.AutomaticGainControl.isAvailable()) {
                android.media.audiofx.AutomaticGainControl.create(audioRecord!!.audioSessionId)?.enabled = true
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AudioFX: ${e.message}")
        }

        audioRecord?.startRecording()

        audioJob = scope.launch(Dispatchers.IO) {
            val shortBuf = ShortArray(PACKET_SAMPLES)
            while (_isListening.value || isStarting) {
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: break
                if (read <= 0) continue

                // ── Local Energy-Based VAD ──
                var energySum = 0.0f
                val floats = FloatArray(read) { 
                    val f = shortBuf[it] / 32768.0f 
                    energySum += f * f
                    f 
                }
                
                val rms = kotlin.math.sqrt(energySum / read)
                val now = System.currentTimeMillis()

                // Raised threshold to 0.035f to ignore fan/AC background noise
                if (rms > 0.035f) {
                    lastSpeechTime = now
                    hasSpoken = true
                } else {
                    val silenceDuration = now - lastSpeechTime
                    val timeoutLimit = if (hasSpoken) 3000L else 10000L // 3s stop after speech, 10s stop initially
                    if (silenceDuration > timeoutLimit) {
                        Log.d(TAG, "VAD detected silence for ${silenceDuration}ms. Auto-stopping.")
                        scope.launch(Dispatchers.Main) { stopListening() }
                        break // exit audio loop
                    }
                }

                floatBuffer.addAll(floats.toList())

                // Send 512-sample packets using O(1) ArrayDeque.removeFirst()
                while (floatBuffer.size >= PACKET_SAMPLES) {
                    val packet = FloatArray(PACKET_SAMPLES) { floatBuffer.removeFirst() }

                    val bytes = ByteArray(PACKET_SAMPLES * 4)
                    packet.forEachIndexed { i, f ->
                        val bits = java.lang.Float.floatToRawIntBits(f)
                        bytes[i * 4 + 0] = (bits and 0xFF).toByte()
                        bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
                        bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
                        bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
                    }

                    val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    val msg = """{"type":"audio","data":"$b64"}"""
                    // Guard against null WebSocket during reconnect window
                    webSocket?.send(msg)
                }
            }
        }
    }

    // ── SAVE HELPERS ──────────────────────────────────────────────────────────

    private suspend fun saveVitals(vitals: Map<String, String>): String {
        val patient     = prefs.getPatient() ?: return "Vitals saved"
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm",      Locale.getDefault()).format(Date())
        val body = buildMap<String, Any> {
            putAll(vitals)
            put("uhid",          patient.uhId)
            put("userId",        patient.pid.toString())
            put("vitalDate",     currentDate)
            put("vitalTime",     currentTime)
            put("clientId",      patient.clientId.toString())
            put("isFromPatient", "true")
            put("isFromMachine", "0")
            put("positionId",    "0")
        }
        return when (AppDependencies.addVital()(body)) {
            is DomainResult.Success -> formatVitalSummary(vitals)
            is DomainResult.Error   -> "Error saving vitals"
        }
    }

    private suspend fun saveSymptoms(symptoms: List<VoiceSymptomItem>): String {
        val patient = prefs.getPatient() ?: return "Symptoms saved"
        val now     = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val dtDataTable = symptoms.map { s ->
            mapOf("detailID" to s.detailID, "detailsDate" to now, "details" to s.details, "pdmId" to s.pmId)
        }
        val body = mapOf(
            "uhId"          to patient.uhId,
            "jsonSymtoms"   to Gson().toJson(dtDataTable),
            "clientId"      to patient.clientId,
            "type"          to "Symptoms",
            "isFromPatient" to true
        )
        return when (AppDependencies.insertSymptoms()(body)) {
            is DomainResult.Success -> formatSymptomSummary(symptoms)
            is DomainResult.Error   -> "Error saving symptoms"
        }
    }

    private suspend fun saveFluidIntakes(intakes: List<VoiceFluidIntakeItem>): String {
        val patient  = prefs.getPatient() ?: return "Fluid intake saved"
        val foodDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            java.time.Instant.now().toString()
        else
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        val savedNames = mutableListOf<String>()
        for (intake in intakes) {
            val body = mapOf<String, Any>(
                "FoodDate"      to foodDate,
                "FoodQuantity"  to intake.quantity.toString(),
                "UserID"        to patient.pid,
                "foodId"        to intake.foodId.toString(),
                "clientId"      to patient.clientId,
                "FoodUnitId"    to 27,
                "uhid"          to patient.uhId,
                "isGiven"       to 1,
                "entryType"     to "N",
                "isFromPatient" to 1
            )
            when (AppDependencies.addIntake()(body)) {
                is DomainResult.Success -> savedNames.add("${intake.name} ${intake.quantity}ml")
                is DomainResult.Error   -> Log.e(TAG, "Error saving intake: ${intake.name}")
            }
        }
        return if (savedNames.isEmpty()) "Fluid intake saved" else "Logged Intake: ${savedNames.joinToString(", ")}"
    }

    private suspend fun saveFluidOutput(output: VoiceFluidOutputItem): String {
        val patient    = prefs.getPatient() ?: return "Fluid output saved"
        val outputDate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
        else
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val body = mapOf<String, Any>(
            "outputTypeID" to 51,
            "quantity"     to output.quantity,
            "unitID"       to 1,
            "outputDate"   to outputDate,
            "colour"       to output.colourHex,
            "userID"       to patient.pid,
            "clientId"     to patient.clientId,
            "uhid"         to patient.uhId
        )
        return when (AppDependencies.addOutput()(body)) {
            is DomainResult.Success -> "Logged Output: ${output.quantity}ml (${output.colourName})"
            is DomainResult.Error   -> "Error saving fluid output"
        }
    }

    private suspend fun saveMedicines(medicines: List<VoiceMedicineItem>): String {
        val savedNames = mutableListOf<String>()
        for (med in medicines) {
            when (AppDependencies.markMedicineIntake()(med.medicineId, med.scheduledDateTime)) {
                is DomainResult.Success -> savedNames.add(med.medicineName)
                is DomainResult.Error   -> Log.e(TAG, "Error marking medicine: ${med.medicineName}")
            }
        }
        return if (savedNames.isEmpty()) "Medication recorded" else "Taken: ${savedNames.joinToString(", ")}"
    }

    // ── FORMAT HELPERS ────────────────────────────────────────────────────────

    private fun formatVitalSummary(vitals: Map<String, String>): String {
        val parts = mutableListOf<String>()
        val sys   = vitals["vmValueBPSys"]
        val dias  = vitals["vmValueBPDias"]
        if (sys != null && dias != null) parts.add("BP $sys/$dias")
        else if (sys  != null) parts.add("BP Sys $sys")
        vitals["vmValuePulse"]?.let         { parts.add("Pulse $it") }
        vitals["vmValueSPO2"]?.let          { parts.add("SpO2 $it%") }
        vitals["vmValueTemperature"]?.let   { parts.add("Temp $it°") }
        vitals["vmValueHeartRate"]?.let     { parts.add("HR $it") }
        vitals["weight"]?.let               { parts.add("Weight ${it}kg") }
        vitals["vmValueRbs"]?.let           { parts.add("RBS ${it}mg/dL") }
        return if (parts.isEmpty()) "Vitals saved" else "Logged: ${parts.joinToString(", ")}"
    }

    private fun formatSymptomSummary(symptoms: List<VoiceSymptomItem>): String {
        val names = symptoms.map { it.details }
        return if (names.isEmpty()) "Symptoms saved" else "Logged: ${names.joinToString(", ")}"
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun containsNavigationCommand(text: String): Boolean {
        // If text has digits or data-context words, it's NOT a navigation command
        val hasDataIndicator = DATA_INDICATORS.any { text.contains(it) }
        val hasDigits        = text.any { it.isDigit() }
        if (hasDigits || hasDataIndicator) return false
        return NAV_KEYWORDS.any { text.contains(it) }
    }

    private fun resolveNavigationRoute(cmd: String): String? {
        return when {
            cmd.contains("home") || cmd.contains("dashboard")     -> "dashboard"
            cmd.contains("vital")                                  -> "vitals"
            cmd.contains("fluid output") || cmd.contains("urine") -> "FluidOutputHistoryScreen"
            cmd.contains("fluid") || cmd.contains("water")        -> "FluidDataInputScreen"
            cmd.contains("medicine") || cmd.contains("pill")      -> "medicine"
            cmd.contains("report")                                 -> "labReports"
            cmd.contains("symptom")                                -> "symptomsTracker"
            cmd.contains("appointment") || cmd.contains("doctor") -> "findDoctor"
            cmd.contains("article") || cmd.contains("research")   -> "ResearchArticlesScreen"
            cmd.contains("profile")                                -> "MedicalProfileScreen"
            cmd.contains("faq")                                    -> "FAQScreen"
            cmd.contains("feedback")                               -> "FeedbackScreen"
            cmd.contains("reminder")                               -> "RemindersScreen"
            cmd.contains("diet")                                   -> "DietChecklistScreen"
            cmd.contains("allergy")                                -> "AllergiesScreen"
            cmd.contains("emergency")                              -> "EmergencyContactsScreen"
            cmd.contains("shared")                                 -> "SharedAccountScreen"
            cmd.contains("family")                                 -> "FamilyHealthScreen"
            cmd.contains("prescription")                           -> "PrescriptionScreen"
            cmd.contains("observer")                               -> "MyObserversScreen"
            cmd.contains("interaction")                            -> "InteractionCheckerScreen"
            cmd.contains("ai report") || cmd.contains("smart report") -> "AiReportScreen"
            cmd.contains("upload") || cmd.contains("lab result")  -> "AddLabResultsScreen"
            cmd.contains("watch")                                  -> "ConnectWatchScreen"
            cmd.contains("drawer") || cmd.contains("menu")        -> "drawer"
            else -> null
        }
    }

    // ── CLEANUP ───────────────────────────────────────────────────────────────

    private fun cleanup() {
        audioJob?.cancel();   audioJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "Done")
        webSocket = null
        floatBuffer.clear()
        isStarting = false
    }
}
