package com.critetiontech.ctvitalio.voice

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.Log
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
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
//  VoiceNavigationManager  (singleton)  — v2.0 Advanced
//  Android equivalent of the iOS VoiceNavigationManager.
//
//  Session flow:
//   1. startListening() → loads session context → connects WebSocket → starts AudioRecord at 16kHz
//   2. Audio is chunked into 512 Float32 samples → Base64 → sent over WebSocket
//   3. Backend responds with {original_text, translation} or legacy {text}
//   4. If translation is a navigation command → handleCommand()
//   5. Multi-turn dialog intercepts incomplete vitals (e.g. "record BP") → guided Q&A
//   6. Otherwise send translation to EchoAI → extract vitals + symptoms + fluids + medicines
//   7. Unit normalization (°C→°F, lbs→kg, mmol→mg/dL) applied automatically
//   8. Anomaly detection runs before save → CRITICAL alerts shown to user
//   9. Show UnifiedConfirmationCard with trend deltas from last session
//  10. TTS confirmation spoken after successful save
//  11. Session analytics logged throughout for enterprise dashboards
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VoiceNavManager"

// WebSocket URL for the dictation backend (matching the iOS config)
private const val DICTATION_WS_BASE = "ws://food.shopright.ai:3479/ws"

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

    // ── NEW v2.0 Advanced State Flows ─────────────────────────────────────────

    /** Clinical anomaly alerts for current pending data — shown before save */
    private val _anomalyAlerts         = MutableStateFlow<List<VitalAlert>>(emptyList())
    val anomalyAlerts: StateFlow<List<VitalAlert>> = _anomalyAlerts.asStateFlow()

    /** Vital trends (current vs last session) for confirmation card delta display */
    private val _vitalTrends           = MutableStateFlow<Map<String, VitalTrend>>(emptyMap())
    val vitalTrends: StateFlow<Map<String, VitalTrend>> = _vitalTrends.asStateFlow()

    /** Active nurse/observer session config — null means patient self-recording */
    private val _sessionConfig         = MutableStateFlow<VoiceSessionConfig?>(null)
    val sessionConfig: StateFlow<VoiceSessionConfig?> = _sessionConfig.asStateFlow()

    /** Multi-turn guided prompt shown to user (e.g. "Diastolic value batao?") */
    private val _multiTurnPrompt       = MutableStateFlow("")
    val multiTurnPrompt: StateFlow<String> = _multiTurnPrompt.asStateFlow()

    /** Whether TTS audio confirmation is enabled */
    private val _ttsEnabled            = MutableStateFlow(true)
    val ttsEnabled: StateFlow<Boolean> = _ttsEnabled.asStateFlow()

    // ── Internal state ────────────────────────────────────────────────────────

    private var scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var audioRecord: AudioRecord? = null
    private var audioJob: Job? = null
    private var aiExtractionJob: Job? = null
    private var aiDebounceJob: Job? = null
    private var webSocket: WebSocket? = null

    // Thread-safe flags — accessed from Main, IO, and Default dispatchers
    private val isStarting  = java.util.concurrent.atomic.AtomicBoolean(false)
    private val isCleaning  = java.util.concurrent.atomic.AtomicBoolean(false) // guard against double-cleanup

    private var lastOriginalText       = ""
    private var lastTranslation        = ""
    private var lastProcessedAIText    = ""

    // Session context loaded once at session start for context-aware AI
    private var sessionContext: VoiceSessionContext = VoiceSessionContext()

    // TTS engine — lazy init on first use
    private var ttsEngine: TextToSpeech? = null
    private var ttsReady = false

    // Personalized medicine keywords loaded from patient's schedule
    private var personalizedMedKeywords: Map<String, Int> = emptyMap() // name → scheduleId

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

    /**
     * Initialize TTS engine. Call from MainActivity/Application once.
     * Required before TTS confirmations will speak.
     */
    fun initTts(context: Context) {
        if (ttsEngine != null) return
        ttsEngine = TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                ttsEngine?.language = Locale.getDefault()
                Log.d(TAG, "TTS engine ready")
            } else {
                Log.e(TAG, "TTS init failed: $status")
            }
        }
    }

    /** Toggle TTS audio confirmations on/off */
    fun setTtsEnabled(enabled: Boolean) { _ttsEnabled.value = enabled }

    /**
     * Set nurse/observer mode — call before startListening().
     * Pass null to revert to patient self-recording mode.
     */
    fun setSessionConfig(config: VoiceSessionConfig?) {
        _sessionConfig.value = config
        Log.d(TAG, "Session config set: ${config?.auditLabel ?: "Patient mode"}")
    }

    fun startListening(onNavigationCommand: (String) -> Unit) {
        // compareAndSet(false, true) atomically returns true only for the FIRST caller
        if (_isListening.value || !isStarting.compareAndSet(false, true)) {
            stopListening()
            return
        }

        _transcript.value           = ""
        _processingTranscript.value = ""
        _multiTurnPrompt.value      = ""
        _anomalyAlerts.value        = emptyList()
        lastOriginalText            = ""
        lastTranslation             = ""
        lastProcessedAIText         = ""
        floatBuffer.clear()
        wsReconnectAttempts         = 0   // Fresh reconnect budget for every session

        lastSpeechTime = System.currentTimeMillis()
        hasSpoken = false

        _showListeningOverlay.value = true

        // Reset multi-turn dialog for fresh session
        MultiTurnConversationManager.reset()

        // Start session analytics
        val patient = prefs.getPatient()
        VoiceSessionAnalytics.startSession(
            patientId = patient?.pid?.toString() ?: "unknown",
            nurseMode = _sessionConfig.value?.isNurseMode == true
        )

        scope.launch {
            // Load session context (last vitals + scheduled medicines) in background
            loadSessionContext()

            try {
                connectWebSocket(onNavigationCommand)

                // ✅ Reset VAD timer RIGHT BEFORE audio capture starts.
                // loadSessionContext() is a network call and may take several seconds.
                // If lastSpeechTime was set before that call (line ~226), the initial
                // 10-second silence timeout fires immediately — causing the loop to exit
                // before the user even speaks. Reset here so the 10s clock starts fresh.
                lastSpeechTime = System.currentTimeMillis()
                hasSpoken = false

                startAudioCapture()

                withContext(Dispatchers.Main) {
                    _isListening.value = true
                }
                isStarting.set(false)

            } catch (e: Exception) {
                Log.e(TAG, "startListening error: ${e.message}")
                isStarting.set(false)
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
        _multiTurnPrompt.value          = ""
        _anomalyAlerts.value            = emptyList()
        _isExtractingData.value         = false
        MultiTurnConversationManager.reset()
        VoiceSessionAnalytics.endSession(cancelled = true)
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
        var data = _pendingData.value ?: return
        if (_isSavingData.value) return
        _isSavingData.value = true

        scope.launch {
            val summaries   = mutableListOf<String>()
            var savedCount  = 0
            var failedCount = 0

            // 0. Apply unit normalization to vitals before saving
            val lastText = if (lastTranslation.isNotEmpty()) lastTranslation else lastOriginalText
            val normalizedVitals = data.vitals?.let {
                VitalUnitNormalizer.normalizeAll(it, lastText)
            }
            if (normalizedVitals != null && normalizedVitals != data.vitals) {
                Log.d(TAG, "Unit normalization applied to vitals")
                data = data.copy(vitals = normalizedVitals)
            }

            // 1. Save vitals
            data.vitals?.takeIf { it.isNotEmpty() }?.let { vitals ->
                try {
                    summaries.add(saveVitals(vitals))
                    savedCount += vitals.size
                } catch (e: Exception) {
                    Log.e(TAG, "Save vitals error: ${e.message}")
                    failedCount++
                }
            }

            // 2. Save symptoms
            data.symptoms?.takeIf { it.isNotEmpty() }?.let { symptoms ->
                try {
                    summaries.add(saveSymptoms(symptoms))
                    savedCount += symptoms.size
                } catch (e: Exception) {
                    Log.e(TAG, "Save symptoms error: ${e.message}")
                    failedCount++
                }
            }

            // 3. Save fluid intakes
            data.fluidIntakes?.takeIf { it.isNotEmpty() }?.let { intakes ->
                try {
                    summaries.add(saveFluidIntakes(intakes))
                    savedCount += intakes.size
                } catch (e: Exception) {
                    Log.e(TAG, "Save fluid intakes error: ${e.message}")
                    failedCount++
                }
            }

            // 4. Save fluid output
            data.fluidOutput?.let { output ->
                try {
                    summaries.add(saveFluidOutput(output))
                    savedCount++
                } catch (e: Exception) {
                    Log.e(TAG, "Save fluid output error: ${e.message}")
                    failedCount++
                }
            }

            // 5. Save medicines
            data.medicines?.takeIf { it.isNotEmpty() }?.let { medicines ->
                try {
                    summaries.add(saveMedicines(medicines))
                    savedCount += medicines.size
                } catch (e: Exception) {
                    Log.e(TAG, "Save medicines error: ${e.message}")
                    failedCount++
                }
            }

            // 6. Update analytics
            VoiceSessionAnalytics.onItemSaved(savedCount)
            if (failedCount > 0) VoiceSessionAnalytics.onItemFailed(failedCount)
            VoiceSessionAnalytics.endSession(cancelled = false)

            val summary = if (summaries.isEmpty()) "Data saved successfully" else summaries.joinToString(" | ")

            // 7. TTS confirmation — speak what was saved
            if (_ttsEnabled.value && ttsReady && summaries.isNotEmpty()) {
                val nursePrefix = _sessionConfig.value?.let { " for ${it.recordingForPatientName}" } ?: ""
                speakTts("Recorded$nursePrefix: ${summaries.joinToString(", ")}")
            }

            withContext(Dispatchers.Main) {
                _isSavingData.value         = false
                _pendingData.value          = null
                _showConfirmationCard.value = false
                _anomalyAlerts.value        = emptyList()
                _vitalTrends.value          = emptyMap()
                _transcript.value           = summary

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

        // Track analytics
        VoiceSessionAnalytics.onTranscriptReceived(cleaned)

        aiExtractionJob?.cancel()
        aiExtractionJob = scope.launch {
            val aiStart = VoiceSessionAnalytics.onAiCallStart()
            try {
                Log.d(TAG, "Sending to Echo AI: $cleaned")
                val extracted = EchoAIService.parseSpeechToEntities(cleaned)
                VoiceSessionAnalytics.onAiCallSuccess(aiStart, extracted.let {
                    (it.vitals?.size ?: 0) + (it.symptoms?.size ?: 0) +
                    (it.fluidIntakes?.size ?: 0) + (if (it.fluidOutput != null) 1 else 0) +
                    (it.medicines?.size ?: 0)
                })

                withContext(Dispatchers.Main) {
                    _isExtractingData.value = false
                    if (!extracted.isEmpty) {
                        val existing = _pendingData.value
                        val merged   = existing?.merge(extracted) ?: extracted
                        _pendingData.value          = merged

                        // Run anomaly detection on new vitals
                        merged.vitals?.let { vitals ->
                            val alerts = VitalAnomalyChecker.check(vitals)
                            _anomalyAlerts.value = alerts
                            if (alerts.isNotEmpty()) {
                                VoiceSessionAnalytics.onAnomalyDetected(alerts)
                                Log.w(TAG, "Anomalies: ${alerts.map { it.vitalLabel }}")
                            }
                            // Compute trends against last session vitals
                            if (sessionContext.lastVitals.isNotEmpty()) {
                                _vitalTrends.value = computeTrends(vitals, sessionContext.lastVitals)
                            }
                        }

                        _showConfirmationCard.value = true
                    } else if (!_isListening.value) {
                        // AI found nothing and we stopped listening → close overlay
                        _showListeningOverlay.value = false
                    }
                }
            } catch (e: Exception) {
                VoiceSessionAnalytics.onAiCallFail(aiStart)
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
    private const val WS_MAX_RECONNECT = 5   // ✅ Upgraded from 3 → 5 (500, 1000, 2000, 4000, 8000ms)

    /**
     * Schedules a reconnect attempt with exponential backoff.
     * Extracted into a named function so Kotlin can resolve the type without
     * hitting the recursive inference problem caused by calling a suspend fun
     * from inside an anonymous WebSocketListener object literal.
     */
    private fun scheduleReconnect(onNavigationCommand: (String) -> Unit): Unit {
        wsReconnectAttempts++
        VoiceSessionAnalytics.onWsReconnect()
        val delayMs: Long = 500L * (1 shl (wsReconnectAttempts - 1))   // 500, 1000, 2000, 4000, 8000 ms
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

            // 2. Multi-turn conversation check — if user gave an intent without values
            if (MultiTurnConversationManager.isActive) {
                // Feed reply to active dialog
                val (prompt, extracted) = MultiTurnConversationManager.processReply(aiText)
                if (prompt != null) _multiTurnPrompt.value = prompt
                if (extracted != null && !extracted.isEmpty) {
                    _multiTurnPrompt.value = ""
                    val existing = _pendingData.value
                    _pendingData.value         = existing?.merge(extracted) ?: extracted
                    _showConfirmationCard.value = true
                }
                return@launch
            } else {
                // Check if this text is an intent-only statement (e.g. "record BP")
                val intentPrompt = MultiTurnConversationManager.detectAndStart(aiText)
                if (intentPrompt != null) {
                    _multiTurnPrompt.value = intentPrompt
                    _transcript.value = intentPrompt
                    return@launch
                }
            }

            // 3. EchoAI entity extraction – immediate on final, debounced 1s on partial
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
            while (_isListening.value || isStarting.get()) {
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

        // ✅ Match manual VitalDetailViewModel exactly: send all keys, missing ones as empty strings.
        // NLP may emit "weight"; remap to "vmValueWeight" for API compatibility.
        val body = buildMap<String, Any> {
            put("vmValueBPSys",           vitals["vmValueBPSys"]           ?: "")
            put("vmValueBPDias",          vitals["vmValueBPDias"]          ?: "")
            put("vmValueSPO2",            vitals["vmValueSPO2"]            ?: "")
            put("vmValueRespiratoryRate", vitals["vmValueRespiratoryRate"] ?: "")
            put("vmValueHeartRate",       vitals["vmValueHeartRate"]       ?: "")
            put("vmValuePulse",           vitals["vmValuePulse"]           ?: "")
            put("vmValueRbs",             vitals["vmValueRbs"]             ?: "")
            put("vmValueTemperature",     vitals["vmValueTemperature"]     ?: "")
            // NLP emits "weight"; API key is "vmValueWeight"
            put("vmValueWeight",          vitals["vmValueWeight"] ?: vitals["weight"] ?: "")
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
        // ✅ Use ISO date format matching SymptomTrackerViewModel (was "yyyy-MM-dd HH:mm:ss.SSS")
        val now = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)
            java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
        else
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

        // ✅ Match SymptomTrackerViewModel body exactly:
        //   - detailID as Int (was String — API rejects it)
        //   - Added isProvisionalDiagnosis, isChronic, isStillFeeling (were missing)
        val dtDataTable = symptoms.map { s ->
            mapOf(
                "detailID"               to (s.detailID.toIntOrNull() ?: 0),
                "detailsDate"            to now,
                "details"                to s.details,
                "pdmId"                  to s.pmId,
                "isProvisionalDiagnosis" to 0,
                "isChronic"              to 0,
                "isStillFeeling"         to true
            )
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

    // ── SESSION CONTEXT LOADER ────────────────────────────────────────────────

    /**
     * Loads last vital readings + today's scheduled medicines from API
     * into [sessionContext] so EchoAI and multi-turn can use it.
     * Runs on IO dispatcher at session start — fully safe to call from any coroutine.
     */
    private suspend fun loadSessionContext() {
        withContext(Dispatchers.IO) {
            try {
                val patient = prefs.getPatient() ?: return@withContext
                val today   = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                // ── Last Vitals — for trend computation ──────────────────────
                // FetchLastVitalUseCase.invoke(uhid: String, clientId: String, userId: String)
                val lastVitalsMap = mutableMapOf<String, String>()
                try {
                    val vitalRes = AppDependencies.fetchLastVital()(
                        patient.uhId,
                        patient.clientId.toString(),
                        patient.pid.toString()
                    )
                    if (vitalRes is DomainResult.Success) {
                        // Vital is a flat list: each entry has vitalName + vitalValue.
                        // Map vitalName → canonical voice key used throughout the voice system.
                        vitalRes.data.forEach { vital ->
                            val key = when (vital.vitalName.lowercase().trim()) {
                                "systolic", "bp systolic", "bpsys", "vmvaluebpsys"         -> "vmValueBPSys"
                                "diastolic", "bp diastolic", "bpdias", "vmvaluebpdias"     -> "vmValueBPDias"
                                "pulse", "pulse rate", "vmvaluepulse"                       -> "vmValuePulse"
                                "spo2", "oxygen", "oxygen saturation", "vmvaluespo2"        -> "vmValueSPO2"
                                "temperature", "temp", "vmvaluetemperature"                 -> "vmValueTemperature"
                                "heart rate", "heartrate", "vmvalueheartrate"               -> "vmValueHeartRate"
                                "respiratory rate", "rr", "vmvaluerespiratoryrate"          -> "vmValueRespiratoryRate"
                                "rbs", "blood sugar", "sugar", "glucose", "vmvaluerbs"      -> "vmValueRbs"
                                "weight", "body weight", "vmvalueweight"                    -> "weight"
                                else -> null
                            }
                            if (key != null && vital.vitalValue != 0.0) {
                                lastVitalsMap[key] = vital.vitalValue.toString()
                            }
                        }
                        Log.d(TAG, "Last vitals loaded for trend: ${lastVitalsMap.keys}")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load last vitals for context: ${e.message}")
                }

                // ── Scheduled Medicines — for personalized keyword matching ──
                val scheduledMeds = mutableListOf<ScheduledMedicineRef>()
                val medKeywords   = mutableMapOf<String, Int>()
                try {
                    val medRes = AppDependencies.fetchMedicineIntake()(
                        patient.pid,
                        today,
                        patient.clientId
                    )
                    if (medRes is DomainResult.Success) {
                        medRes.data.flatMap { it.medicineData }.filter { !it.isTaken }.forEach { med ->
                            scheduledMeds.add(ScheduledMedicineRef(
                                id                = med.id,
                                name              = med.medicineName,
                                scheduledDateTime = med.scheduledDateTime,
                                dosageStrength    = med.dosageStrength.toString(),
                                unit              = med.unit,
                                isTaken           = med.isTaken
                            ))
                            medKeywords[med.medicineName.lowercase().trim()] = med.id
                        }
                        personalizedMedKeywords = medKeywords
                        Log.d(TAG, "Loaded ${scheduledMeds.size} pending medicines for keyword matching")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not load medicines for context: ${e.message}")
                }

                sessionContext = VoiceSessionContext(
                    lastVitals         = lastVitalsMap,
                    scheduledMedicines = scheduledMeds
                )
                Log.d(TAG, "Session context ready: ${lastVitalsMap.size} vitals, ${scheduledMeds.size} medicines")
            } catch (e: Exception) {
                Log.e(TAG, "loadSessionContext failed: ${e.message}")
            }
        }
    }

    // ── TTS HELPER ────────────────────────────────────────────────────────────

    /** Speaks [text] using the TTS engine if ready and enabled. Non-blocking. */
    private fun speakTts(text: String) {
        if (!ttsReady || ttsEngine == null) return
        ttsEngine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "vitalio_confirm")
    }

    // ── CLEANUP ───────────────────────────────────────────────────────────────

    private fun cleanup() {
        // isCleaning guard: ensures stop()+release() on AudioRecord is never called twice.
        // Double-call causes IllegalStateException (AudioRecord in wrong state).
        if (!isCleaning.compareAndSet(false, true)) {
            Log.d(TAG, "cleanup() skipped — already cleaning up")
            return
        }
        try {
            audioJob?.cancel();   audioJob = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            webSocket?.close(1000, "Done")
            webSocket  = null
            floatBuffer.clear()
            isStarting.set(false)
        } finally {
            isCleaning.set(false)  // Reset so next session can clean up properly
        }
    }

    /** Call once when the app is destroyed — releases TTS engine resources */
    fun releaseTts() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        ttsEngine = null
        ttsReady  = false
    }
}
