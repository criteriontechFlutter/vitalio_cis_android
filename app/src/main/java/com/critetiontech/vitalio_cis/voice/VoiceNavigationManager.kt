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
//  Session flow (logging only — the whole utterance is analysed once, at the end):
//   1. startListening() → loads session context → connects WebSocket → starts AudioRecord at 16kHz
//   2. Audio is chunked into 512 Float32 samples → Base64 → sent over WebSocket
//   3. Backend streams per-segment partial/final {original_text, translation}; segments are
//      accumulated so nothing said earlier is lost
//   4. VAD (adaptive mic energy + live transcript activity) ends the turn after a pause,
//      or the user taps stop; trailing finals/translations are awaited briefly
//   5. The full transcript is sent to EchoAI once → vitals + symptoms + fluids + medicines
//      (an open multi-turn question, e.g. "Systolic value?", consumes the reply instead)
//   7. Unit normalization (°C→°F, lbs→kg, mmol→mg/dL) applied automatically
//   8. Anomaly detection runs before save → CRITICAL alerts shown to user
//   9. Show UnifiedConfirmationCard with trend deltas from last session
//  10. TTS confirmation spoken after successful save
//  11. Session analytics logged throughout for enterprise dashboards
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VoiceNavManager"

// WebSocket URL for the dictation backend (matching the iOS config)
private const val DICTATION_WS_BASE = "ws://food.shopright.ai:3479/ws"

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

    // VAD (Voice Activity Detection) state — written from the audio and WebSocket threads
    @Volatile private var lastSpeechTime = 0L
    @Volatile private var hasSpoken = false
    private var noiseFloor = 0.01f
    private var loudFrames = 0

    private const val VAD_MIN_THRESHOLD     = 0.012f  // RMS floor so silence never counts as speech
    private const val VAD_NOISE_MULTIPLIER  = 3f      // speech must be this much louder than room noise
    private const val VAD_SPEECH_FRAMES     = 3       // ~96ms of loud audio before we call it speech (ignores clicks)
    private const val VAD_END_SILENCE_MS    = 3_500L  // pause that ends the turn once the user has spoken
    private const val VAD_NO_SPEECH_MS      = 8_000L  // give up if nothing is said at all
    private const val MAX_SESSION_MS        = 120_000L
    private const val TRAILING_RESULT_WAIT_MS = 2_500L

    // Transcript accumulated across the backend's per-segment messages, in arrival order.
    // English speech arrives as final originals only; Hindi arrives as an original followed
    // by a separate translation that stands in for the Hindi segments before it.
    private class Segment(val text: String, val needsTranslation: Boolean, var translated: Boolean = false)
    private val segments = mutableListOf<Segment>()
    @Volatile private var currentPartial = ""
    private val transcriptLock = Any()

    private val isFinalizing = java.util.concurrent.atomic.AtomicBoolean(false)
    // Bumped whenever a turn starts or ends, so a slow start can tell it was cancelled meanwhile
    @Volatile private var sessionToken = 0

    /** Shown on the listening screen when a turn produced nothing to log. */
    private val _statusMessage = MutableStateFlow("")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()


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

    /**
     * Starts a dictation turn. Calling it while listening ends the turn (and analyses it).
     * Pending items from earlier turns are kept, so "Speak again" adds to the same log.
     * [onNavigationCommand] is kept for API compatibility; the assistant now only logs data.
     */
    @Suppress("UNUSED_PARAMETER")
    fun startListening(onNavigationCommand: (String) -> Unit = {}) {
        if (isFinalizing.get() || _isExtractingData.value || _isSavingData.value) return
        // compareAndSet(false, true) atomically returns true only for the FIRST caller
        if (_isListening.value || !isStarting.compareAndSet(false, true)) {
            stopListening()
            return
        }

        _transcript.value           = ""
        _processingTranscript.value = ""
        _statusMessage.value        = ""
        lastOriginalText            = ""
        lastTranslation             = ""
        lastProcessedAIText         = ""
        resetTranscriptBuffers()
        floatBuffer.clear()
        wsReconnectAttempts         = 0   // Fresh reconnect budget for every session

        lastSpeechTime = System.currentTimeMillis()
        hasSpoken = false

        _showListeningOverlay.value = true
        val token = ++sessionToken

        // Keep an open guided question alive so this turn can answer it
        if (!MultiTurnConversationManager.isActive) {
            MultiTurnConversationManager.reset()
            _multiTurnPrompt.value = ""
        }

        // Start session analytics
        val patient = prefs.getPatient()
        VoiceSessionAnalytics.startSession(
            patientId = patient?.pid?.toString() ?: "unknown",
            nurseMode = _sessionConfig.value?.isNurseMode == true
        )

        scope.launch {
            // Context (last vitals + scheduled medicines) is only needed once the turn is
            // analysed, so load it alongside instead of making the user wait for the mic.
            launch { loadSessionContext() }

            try {
                if (token != sessionToken) return@launch   // stopped before we got going
                connectWebSocket()
                if (token != sessionToken) { closeWebSocket(); return@launch }

                // Reset the VAD clock right before capture: loadSessionContext() is a network
                // call and can take seconds, which would otherwise eat the no-speech timeout.
                lastSpeechTime = System.currentTimeMillis()
                hasSpoken = false

                // Flip the flag before the audio loop starts so it doesn't exit immediately
                withContext(Dispatchers.Main) { _isListening.value = true }
                startAudioCapture()
                isStarting.set(false)

            } catch (e: Exception) {
                Log.e(TAG, "startListening error: ${e.message}")
                isStarting.set(false)
                finishSession(analyze = false)
                withContext(Dispatchers.Main) {
                    if (_pendingData.value == null) _showListeningOverlay.value = false
                }
            }
        }
    }

    /** Ends the current turn (user tap or VAD) and analyses everything said in it. */
    fun stopListening() = finishSession(analyze = true)

    fun cancelPendingData() {
        aiExtractionJob?.cancel()
        aiDebounceJob?.cancel()
        finishSession(analyze = false)
        _pendingData.value              = null
        _showConfirmationCard.value     = false
        _showListeningOverlay.value     = false
        _transcript.value               = ""
        _processingTranscript.value     = ""
        _multiTurnPrompt.value          = ""
        _statusMessage.value            = ""
        _anomalyAlerts.value            = emptyList()
        _isExtractingData.value         = false
        MultiTurnConversationManager.reset()
        VoiceSessionAnalytics.endSession(cancelled = true)
    }

    /**
     * Stops capture, waits briefly for the backend's trailing final/translation, closes the
     * socket and — when [analyze] — runs a single AI pass over the full transcript.
     */
    private fun finishSession(analyze: Boolean) {
        if (!isFinalizing.compareAndSet(false, true)) return
        val wasActive = _isListening.value || isStarting.get()
        sessionToken++
        _isListening.value = false
        stopAudioCapture()

        scope.launch {
            try {
                if (analyze && wasActive) {
                    withContext(Dispatchers.Main) {
                        _isExtractingData.value     = true
                        _processingTranscript.value = _transcript.value
                    }
                    // A little silence helps the backend close the last segment
                    sendTrailingSilence()
                    val deadline = System.currentTimeMillis() + TRAILING_RESULT_WAIT_MS
                    while (System.currentTimeMillis() < deadline && awaitingTrailingResults()) delay(100)
                }
                closeWebSocket()
                isStarting.set(false)

                if (analyze && wasActive) analyzeTurn()
            } finally {
                isFinalizing.set(false)
            }
        }
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
            val lastText = lastProcessedAIText
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
                finishSession(analyze = false)
                _showListeningOverlay.value = false
                _transcript.value           = ""
            }
        }
    }

    /** Call when the navigation event has been consumed */
    fun clearNavigationCommand() {
        _navigationCommand.value = null
    }

    // ── AI EXTRACTION ─────────────────────────────────────────────────────────

    /** Runs once per turn with everything the user said. */
    private suspend fun analyzeTurn() {
        val text = pickTextForAnalysis()
        Log.d(TAG, "Turn ended. Analysing: \"$text\"")

        if (text.length < 2 || !text.any { it.isLetter() }) {
            withContext(Dispatchers.Main) {
                _isExtractingData.value = false
                if (_pendingData.value == null) {
                    _statusMessage.value = "Didn't catch that. Tap Speak again and say your readings."
                }
            }
            return
        }

        // An open guided question ("Systolic value?") takes this reply first
        if (MultiTurnConversationManager.isActive) {
            val (prompt, extracted) = MultiTurnConversationManager.processReply(text)
            withContext(Dispatchers.Main) {
                _isExtractingData.value = false
                if (extracted != null && !extracted.isEmpty) {
                    _multiTurnPrompt.value = ""
                    mergePending(extracted)
                } else if (prompt != null) {
                    _multiTurnPrompt.value = prompt
                }
            }
            return
        }

        lastProcessedAIText         = text
        _processingTranscript.value = text
        VoiceSessionAnalytics.onTranscriptReceived(text)

        val aiStart = VoiceSessionAnalytics.onAiCallStart()
        val extracted = try {
            EchoAIService.parseSpeechToEntities(text).also { result ->
                VoiceSessionAnalytics.onAiCallSuccess(aiStart,
                    (result.vitals?.size ?: 0) + (result.symptoms?.size ?: 0) +
                    (result.fluidIntakes?.size ?: 0) + (if (result.fluidOutput != null) 1 else 0) +
                    (result.medicines?.size ?: 0))
            }
        } catch (e: Exception) {
            VoiceSessionAnalytics.onAiCallFail(aiStart)
            Log.e(TAG, "EchoAI extraction error: ${e.message}")
            VoiceExtractedData()
        }

        withContext(Dispatchers.Main) {
            _isExtractingData.value = false
            if (!extracted.isEmpty) {
                mergePending(extracted)
                return@withContext
            }
            // Nothing extracted: maybe an intent without values ("record my BP")
            val prompt = MultiTurnConversationManager.detectAndStart(text)
            when {
                prompt != null -> _multiTurnPrompt.value = prompt
                _pendingData.value == null ->
                    _statusMessage.value = "No health readings found in what you said. Tap Speak again to try."
            }
        }
    }

    private fun mergePending(extracted: VoiceExtractedData) {
        val merged = _pendingData.value?.merge(extracted) ?: extracted
        _pendingData.value   = merged
        _statusMessage.value = ""

        merged.vitals?.let { vitals ->
            val alerts = VitalAnomalyChecker.check(vitals)
            _anomalyAlerts.value = alerts
            if (alerts.isNotEmpty()) {
                VoiceSessionAnalytics.onAnomalyDetected(alerts)
                Log.w(TAG, "Anomalies: ${alerts.map { it.vitalLabel }}")
            }
            if (sessionContext.lastVitals.isNotEmpty()) {
                _vitalTrends.value = computeTrends(vitals, sessionContext.lastVitals)
            }
        }
        _showConfirmationCard.value = true
    }

    // ── TRANSCRIPT ASSEMBLY ───────────────────────────────────────────────────

    private fun resetTranscriptBuffers() = synchronized(transcriptLock) {
        segments.clear()
        currentPartial = ""
    }

    private fun isDevanagari(text: String) = text.any { it in '\u0900'..'\u097F' }

    /** What the user sees: everything they said, in their own words. */
    private fun fullOriginalText(): String = synchronized(transcriptLock) {
        (segments.filter { it.needsTranslation || !it.translated }.map { it.text } + currentPartial)
            .map { it.trim() }.filter { it.isNotEmpty() && it != "." }.joinToString(" ")
    }

    /**
     * What the AI gets: English segments as spoken, Hindi segments replaced by their
     * translation once it arrives (or kept in Hindi if it never does).
     */
    private fun textForAnalysis(): String = synchronized(transcriptLock) {
        (segments.map { seg ->
            // A mixed segment ("I have fever, मेरा सर घूम रहा है") is translated only for its
            // Hindi part, so keep the English words alongside the translation.
            if (seg.needsTranslation && seg.translated) latinPart(seg.text) else seg.text
        } + currentPartial)
            .map { it.trim() }.filter { it.isNotEmpty() && it != "." }.joinToString(" ")
    }

    private fun latinPart(text: String): String =
        text.split(Regex("\\s+"))
            .filterNot { isDevanagari(it) }
            .joinToString(" ")
            .trim()
            .trimEnd(',', '।', '.')
            .takeIf { part -> part.any { it.isLetterOrDigit() } }
            ?.let { "$it," }
            .orEmpty()

    /** Still waiting on the backend if a segment is open or a Hindi segment awaits translation. */
    private fun awaitingTrailingResults(): Boolean = synchronized(transcriptLock) {
        currentPartial.isNotBlank() || segments.any { it.needsTranslation && !it.translated }
    }

    private fun pickTextForAnalysis(): String {
        lastOriginalText = fullOriginalText()
        lastTranslation  = textForAnalysis()
        return lastTranslation
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
    private fun scheduleReconnect(): Unit {
        wsReconnectAttempts++
        VoiceSessionAnalytics.onWsReconnect()
        val delayMs: Long = 500L * (1 shl (wsReconnectAttempts - 1))   // 500, 1000, 2000, 4000, 8000 ms
        Log.d(TAG, "Reconnecting WebSocket in ${delayMs}ms (attempt $wsReconnectAttempts/$WS_MAX_RECONNECT)")
        scope.launch(Dispatchers.IO) {
            delay(delayMs)
            if (_isListening.value) {
                try { connectWebSocket() }
                catch (e: Exception) { Log.e(TAG, "Reconnect failed: ${e.message}") }
            }
        }
    }

    private suspend fun connectWebSocket(): Unit =
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
                    handleWebSocketMessage(text)
                }

                override fun onMessage(ws: WebSocket, bytes: ByteString) {
                    handleWebSocketMessage(bytes.utf8())
                }

                override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure (attempt $wsReconnectAttempts): ${t.message}")
                    if (_isListening.value && wsReconnectAttempts < WS_MAX_RECONNECT) {
                        // Call named helper — avoids recursive type-inference inside anonymous object
                        scheduleReconnect()
                    }
                }

                override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                    Log.d(TAG, "WebSocket closed: $reason")
                }
            })
        }


    /**
     * Collects streamed transcript segments. Nothing is analysed here — that happens once
     * when the turn ends — so the user can keep talking for as long as they like.
     */
    private fun handleWebSocketMessage(raw: String) {
        Log.d(TAG, "WS RAW: $raw")
        val json = try { org.json.JSONObject(raw) } catch (e: Exception) {
            if (!raw.startsWith("{") && raw.isNotBlank()) {
                synchronized(transcriptLock) { currentPartial = raw.trim() }
                markSpeechActivity()
                publishTranscript()
            }
            return
        }

        val type        = json.optString("type")
        val original    = json.optString("original_text").trim()
            .ifEmpty { json.optString("text").trim() }          // legacy payload
        val translation = json.optString("translation").trim()

        synchronized(transcriptLock) {
            when (type) {
                "final" -> {
                    if (original.isNotEmpty()) {
                        segments += Segment(original, needsTranslation = isDevanagari(original))
                        currentPartial = ""
                    }
                    if (translation.isNotEmpty()) {
                        // Stands in for every Hindi segment not yet translated
                        segments.filter { it.needsTranslation && !it.translated }.forEach { it.translated = true }
                        segments += Segment(translation, needsTranslation = false, translated = true)
                    }
                }
                "partial", "" -> if (original.isNotEmpty()) currentPartial = original
                else -> return   // session_started etc.
            }
        }
        if (original.isNotEmpty() || translation.isNotEmpty()) {
            markSpeechActivity()
            publishTranscript()
        }
    }

    /** Text from the backend proves the user is speaking even if the mic level looks low. */
    private fun markSpeechActivity() {
        if (!_isListening.value) return
        lastSpeechTime = System.currentTimeMillis()
        hasSpoken = true
    }

    private fun publishTranscript() {
        val text = fullOriginalText()
        scope.launch(Dispatchers.Main) { _transcript.value = text }
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
            val shortBuf  = ShortArray(PACKET_SAMPLES)
            val startedAt = System.currentTimeMillis()
            noiseFloor = 0.01f
            loudFrames = 0
            while (_isListening.value) {
                val read = audioRecord?.read(shortBuf, 0, shortBuf.size) ?: break
                if (read <= 0) continue

                // ── Adaptive energy VAD ──
                var energySum = 0.0f
                val floats = FloatArray(read) {
                    val f = shortBuf[it] / 32768.0f
                    energySum += f * f
                    f
                }
                val rms = kotlin.math.sqrt(energySum / read)
                val now = System.currentTimeMillis()

                val threshold = maxOf(VAD_MIN_THRESHOLD, noiseFloor * VAD_NOISE_MULTIPLIER)
                if (rms > threshold) {
                    loudFrames++
                    if (loudFrames >= VAD_SPEECH_FRAMES) {
                        lastSpeechTime = now
                        hasSpoken = true
                    }
                } else {
                    loudFrames = 0
                    // Track room noise only while quiet, so speech doesn't raise the bar
                    noiseFloor = noiseFloor * 0.95f + rms * 0.05f
                }

                val silence = now - lastSpeechTime
                val limit   = if (hasSpoken) VAD_END_SILENCE_MS else VAD_NO_SPEECH_MS
                if (silence > limit || now - startedAt > MAX_SESSION_MS) {
                    Log.d(TAG, "VAD: ${silence}ms silence (spoken=$hasSpoken). Ending turn.")
                    scope.launch(Dispatchers.Main) { stopListening() }
                    break
                }

                floats.forEach { floatBuffer.addLast(it) }
                while (floatBuffer.size >= PACKET_SAMPLES) {
                    sendPacket(FloatArray(PACKET_SAMPLES) { floatBuffer.removeFirst() })
                }
            }
        }
    }

    private fun sendPacket(packet: FloatArray) {
        val bytes = ByteArray(packet.size * 4)
        packet.forEachIndexed { i, f ->
            val bits = java.lang.Float.floatToRawIntBits(f)
            bytes[i * 4 + 0] = (bits and 0xFF).toByte()
            bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
            bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
            bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
        }
        val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        // Guard against null WebSocket during reconnect window
        webSocket?.send("""{"type":"audio","data":"$b64"}""")
    }

    /** ~0.6s of silence so the backend's endpointing closes the last open segment. */
    private fun sendTrailingSilence() {
        val silent = FloatArray(PACKET_SAMPLES)
        repeat(20) { sendPacket(silent) }
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

    private fun stopAudioCapture() {
        // isCleaning guard: stop()+release() on AudioRecord must never run twice
        // (IllegalStateException when it is already released).
        if (!isCleaning.compareAndSet(false, true)) return
        try {
            audioJob?.cancel(); audioJob = null
            try { audioRecord?.stop() } catch (e: IllegalStateException) { Log.w(TAG, "AudioRecord stop: ${e.message}") }
            audioRecord?.release()
            audioRecord = null
            floatBuffer.clear()
        } finally {
            isCleaning.set(false)
        }
    }

    private fun closeWebSocket() {
        webSocket?.close(1000, "Done")
        webSocket = null
    }

    /** Call once when the app is destroyed — releases TTS engine resources */
    fun releaseTts() {
        ttsEngine?.stop()
        ttsEngine?.shutdown()
        ttsEngine = null
        ttsReady  = false
    }
}
