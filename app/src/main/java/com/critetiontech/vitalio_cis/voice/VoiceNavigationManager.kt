package com.critetiontech.vitalio_cis.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Base64
import android.util.Log
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
//   5. Otherwise send translation to EchoAI → extract vitals + symptoms
//   6. Show UnifiedConfirmationCard if data was extracted
//   7. stopListening() / confirmAndSaveAllPendingData() / cancelPendingData()
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VoiceNavManager"

// WebSocket URL for the dictation backend (matching the iOS config)
private const val DICTATION_WS_BASE = "ws://172.16.61.15:8023/ws"

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

    private val _isListening        = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _transcript         = MutableStateFlow("")
    val transcript: StateFlow<String> = _transcript.asStateFlow()

    private val _showListeningOverlay = MutableStateFlow(false)
    val showListeningOverlay: StateFlow<Boolean> = _showListeningOverlay.asStateFlow()

    private val _showConfirmationCard = MutableStateFlow(false)
    val showConfirmationCard: StateFlow<Boolean> = _showConfirmationCard.asStateFlow()

    private val _pendingData        = MutableStateFlow<VoiceExtractedData?>(null)
    val pendingData: StateFlow<VoiceExtractedData?> = _pendingData.asStateFlow()

    private val _isSavingData       = MutableStateFlow(false)
    val isSavingData: StateFlow<Boolean> = _isSavingData.asStateFlow()

    // Navigation command result – consumed once by the UI
    private val _navigationCommand  = MutableStateFlow<String?>(null)
    val navigationCommand: StateFlow<String?> = _navigationCommand.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioRecord: AudioRecord? = null
    private var audioJob: Job? = null
    private var aiExtractionJob: Job? = null
    private var webSocket: WebSocket? = null
    private var isStarting = false
    private var lastOriginalText = ""
    private var lastTranslation = ""

    // Audio: 512 Float32 samples per packet → 2048 bytes → Base64
    private val SAMPLE_RATE = 16000
    private val PACKET_SAMPLES = 512
    private val floatBuffer = mutableListOf<Float>()

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // keep alive for WS
            .build()
    }

    private val prefs by lazy { AppDependencies.prefs }

    // ── PUBLIC API ────────────────────────────────────────────────────────────

    fun startListening(onNavigationCommand: (String) -> Unit) {
        if (_isListening.value || isStarting) {
            stopListening()
            return
        }
        isStarting = true

        _transcript.value = ""
        lastOriginalText = ""
        lastTranslation = ""
        floatBuffer.clear()

        _showListeningOverlay.value = true

        scope.launch {
            try {
                connectWebSocket(onNavigationCommand)
                startAudioCapture()

                withContext(Dispatchers.Main) { _isListening.value = true }
                isStarting = false

                // Auto-stop after 15 s (matching iOS)
                delay(15_000)
                if (_isListening.value) stopListening()

            } catch (e: Exception) {
                Log.e(TAG, "startListening error: ${e.message}")
                isStarting = false
                stopListening()
            }
        }
    }

    fun stopListening() {
        cleanup()
        scope.launch(Dispatchers.Main) {
            _isListening.value = false
            if (!_showConfirmationCard.value) {
                _showListeningOverlay.value = false
            }
        }
    }

    fun cancelPendingData() {
        _pendingData.value = null
        _showConfirmationCard.value = false
        _showListeningOverlay.value = false
        _transcript.value = ""
        stopListening()
    }

    fun removeVital(keys: List<String>) {
        val data = _pendingData.value ?: return
        val newVitals = data.vitals?.toMutableMap()?.also { v -> keys.forEach { v.remove(it) } }
        val updated = data.copy(vitals = newVitals?.takeIf { it.isNotEmpty() })
        if (updated.isEmpty) {
            _pendingData.value = null
            _showConfirmationCard.value = false
            _showListeningOverlay.value = false
        } else {
            _pendingData.value = updated
        }
    }

    fun removeSymptom(detailID: String, details: String) {
        val data = _pendingData.value ?: return
        val newSymptoms = data.symptoms?.filterNot {
            it.detailID == detailID || it.details.equals(details, ignoreCase = true)
        }
        val updated = data.copy(symptoms = newSymptoms?.takeIf { it.isNotEmpty() })
        if (updated.isEmpty) {
            _pendingData.value = null
            _showConfirmationCard.value = false
            _showListeningOverlay.value = false
        } else {
            _pendingData.value = updated
        }
    }

    fun confirmAndSaveAllPendingData() {
        val data = _pendingData.value ?: return
        if (_isSavingData.value) return
        _isSavingData.value = true

        scope.launch {
            val summaries = mutableListOf<String>()

            // Save vitals
            data.vitals?.takeIf { it.isNotEmpty() }?.let { vitals ->
                try {
                    val result = saveVitals(vitals)
                    summaries.add(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Save vitals error: ${e.message}")
                }
            }

            // Save symptoms
            data.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                try {
                    val result = saveSymptoms(symptoms)
                    summaries.add(result)
                } catch (e: Exception) {
                    Log.e(TAG, "Save symptoms error: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                _isSavingData.value = false
                _pendingData.value = null
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

    // ── WEBSOCKET ─────────────────────────────────────────────────────────────

    private suspend fun connectWebSocket(onNavigationCommand: (String) -> Unit) =
        withContext(Dispatchers.IO) {
            val patient = prefs.getPatient()
            val userId = patient?.pid?.toString() ?: "625"
            val language = "hindi_v2/drpdvoiceagent"
            val department = "GENERAL_MEDICINE"

            val wsUrl = "$DICTATION_WS_BASE/$language/$department/$userId/"
            Log.d(TAG, "Connecting WebSocket: $wsUrl")

            val request = Request.Builder().url(wsUrl).build()
            webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(ws: WebSocket, response: Response) {
                    Log.d(TAG, "WebSocket connected")
                }

                override fun onMessage(ws: WebSocket, text: String) {
                    handleWebSocketMessage(text, onNavigationCommand)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    handleWebSocketMessage(bytes.utf8(), onNavigationCommand)
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}")
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                }
            })
        }

    private fun handleWebSocketMessage(raw: String, onNavigationCommand: (String) -> Unit) {
        Log.d(TAG, "WS RAW: $raw")
        var originalText: String? = null
        var translation: String? = null

        try {
            val json = org.json.JSONObject(raw)
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
            translation?.let { lastTranslation = it }

            val uiText = if (lastOriginalText.isNotEmpty()) lastOriginalText else lastTranslation
            val aiText = if (lastTranslation.isNotEmpty()) lastTranslation else lastOriginalText

            if (uiText.isEmpty() && aiText.isEmpty()) return@launch

            val loweredUI = uiText.lowercase()
            val loweredAI = aiText.lowercase()

            // 1. Navigation command detection
            if (containsNavigationCommand(loweredUI) || containsNavigationCommand(loweredAI)) {
                val cmdText = if (containsNavigationCommand(loweredUI)) loweredUI else loweredAI
                val route = resolveNavigationRoute(cmdText)
                if (route != null) {
                    onNavigationCommand(route)
                    stopListening()
                }
                return@launch
            }

            // 2. EchoAI entity extraction
            if (aiText.isNotEmpty()) {
                aiExtractionJob?.cancel()
                aiExtractionJob = scope.launch {
                    try {
                        Log.d(TAG, "Sending to Echo AI: $aiText")
                        val extracted = EchoAIService.parseSpeechToEntities(aiText)
                        if (!extracted.isEmpty) {
                            withContext(Dispatchers.Main) {
                                val existing = _pendingData.value
                                _pendingData.value = existing?.merge(extracted) ?: extracted
                                _showConfirmationCard.value = true
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "EchoAI extraction error: ${e.message}")
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

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize
        ).also { it.startRecording() }

        audioJob = scope.launch(Dispatchers.IO) {
            val shortBuf = ShortArray(PACKET_SAMPLES)
            while (_isListening.value || isStarting) {
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: break
                if (read <= 0) continue

                // Convert PCM16 → Float32 [-1.0, 1.0]
                val floats = FloatArray(read) { shortBuf[it] / 32768.0f }
                floatBuffer.addAll(floats.toList())

                // Send 512-sample packets
                while (floatBuffer.size >= PACKET_SAMPLES) {
                    val packet = floatBuffer.take(PACKET_SAMPLES)
                    repeat(PACKET_SAMPLES) { floatBuffer.removeAt(0) }

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
                    webSocket?.send(msg)
                }
            }
        }
    }

    // ── SAVE HELPERS ──────────────────────────────────────────────────────────

    private suspend fun saveVitals(vitals: Map<String, String>): String {
        val patient = prefs.getPatient() ?: return "Vitals saved"
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
        val body = buildMap<String, Any> {
            putAll(vitals)
            put("uhid", patient.uhId)
            put("userId", patient.pid.toString())
            put("vitalDate", currentDate)
            put("vitalTime", currentTime)
            put("clientId", patient.clientId.toString())
            put("isFromPatient", "true")
            put("isFromMachine", "0")
            put("positionId", "0")
        }
        return when (AppDependencies.addVital()(body)) {
            is DomainResult.Success -> formatVitalSummary(vitals)
            is DomainResult.Error   -> "Error saving vitals"
        }
    }

    private suspend fun saveSymptoms(symptoms: List<VoiceSymptomItem>): String {
        val patient = prefs.getPatient() ?: return "Symptoms saved"
        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val dtDataTable = symptoms.map { s ->
            mapOf("detailID" to s.detailID, "detailsDate" to now, "details" to s.details, "pdmId" to s.pmId)
        }
        val body = mapOf(
            "uhId" to patient.uhId,
            "jsonSymtoms" to Gson().toJson(dtDataTable),
            "clientId" to patient.clientId,
            "type" to "Symptoms",
            "isFromPatient" to true
        )
        return when (AppDependencies.insertSymptoms()(body)) {
            is DomainResult.Success -> formatSymptomSummary(symptoms)
            is DomainResult.Error   -> "Error saving symptoms"
        }
    }

    private fun formatVitalSummary(vitals: Map<String, String>): String {
        val parts = mutableListOf<String>()
        val sys  = vitals["vmValueBPSys"]
        val dias = vitals["vmValueBPDias"]
        if (sys != null && dias != null) parts.add("BP $sys/$dias")
        else if (sys != null) parts.add("BP Sys $sys")
        vitals["vmValuePulse"]?.let         { parts.add("Pulse $it") }
        vitals["vmValueSPO2"]?.let          { parts.add("SpO2 $it%") }
        vitals["vmValueTemperature"]?.let   { parts.add("Temp $it°") }
        vitals["vmValueHeartRate"]?.let     { parts.add("HR $it") }
        vitals["weight"]?.let               { parts.add("Weight ${it}kg") }
        return if (parts.isEmpty()) "Vitals saved" else "Logged: ${parts.joinToString(", ")}"
    }

    private fun formatSymptomSummary(symptoms: List<VoiceSymptomItem>): String {
        val names = symptoms.map { it.details }
        return if (names.isEmpty()) "Symptoms saved" else "Logged: ${names.joinToString(", ")}"
    }

    // ── NAVIGATION ────────────────────────────────────────────────────────────

    private fun containsNavigationCommand(text: String): Boolean =
        NAV_KEYWORDS.any { text.contains(it) }

    private fun resolveNavigationRoute(cmd: String): String? {
        return when {
            cmd.contains("home") || cmd.contains("dashboard") -> "dashboard"
            cmd.contains("vital")                              -> "vitals"
            cmd.contains("fluid output") || cmd.contains("urine") -> "FluidOutputHistoryScreen"
            cmd.contains("fluid") || cmd.contains("water")    -> "FluidDataInputScreen"
            cmd.contains("medicine") || cmd.contains("pill")  -> "medicine"
            cmd.contains("report")                             -> "labReports"
            cmd.contains("symptom")                            -> "symptomsTracker"
            cmd.contains("appointment") || cmd.contains("doctor") -> "findDoctor"
            cmd.contains("article") || cmd.contains("research") -> "ResearchArticlesScreen"
            cmd.contains("profile")                            -> "MedicalProfileScreen"
            cmd.contains("faq")                                -> "FAQScreen"
            cmd.contains("feedback")                           -> "FeedbackScreen"
            cmd.contains("reminder")                           -> "RemindersScreen"
            cmd.contains("diet")                               -> "DietChecklistScreen"
            cmd.contains("allergy")                            -> "AllergiesScreen"
            cmd.contains("emergency")                          -> "EmergencyContactsScreen"
            cmd.contains("shared")                             -> "SharedAccountScreen"
            cmd.contains("family")                             -> "FamilyHealthScreen"
            cmd.contains("prescription")                       -> "PrescriptionScreen"
            cmd.contains("observer")                           -> "MyObserversScreen"
            cmd.contains("interaction")                        -> "InteractionCheckerScreen"
            cmd.contains("ai report") || cmd.contains("smart report") -> "AiReportScreen"
            cmd.contains("upload") || cmd.contains("lab result") -> "AddLabResultsScreen"
            cmd.contains("watch")                              -> "ConnectWatchScreen"
            cmd.contains("drawer") || cmd.contains("menu")    -> "drawer"
            else -> null
        }
    }

    // ── CLEANUP ───────────────────────────────────────────────────────────────

    private fun cleanup() {
        audioJob?.cancel()
        audioJob = null
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        webSocket?.close(1000, "Done")
        webSocket = null
        floatBuffer.clear()
        isStarting = false
    }
}
