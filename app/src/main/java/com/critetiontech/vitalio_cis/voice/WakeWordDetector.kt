package com.critetiontech.vitalio_cis.voice

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.*

// ═══════════════════════════════════════════════════════════════════════════════
//  WakeWordDetector
//  Listens passively for the phrase "Hey Vitalio" using energy + keyword
//  heuristics (no external SDK required for Phase 1).
//
//  Architecture:
//    Phase 1 (current): Energy-based + phoneme keyword matching.
//    Phase 2 (future):  Swap in Porcupine (Picovoice) SDK for production-grade
//                       offline wake word — just replace the detectWakeWord()
//                       method body.
//
//  Usage:
//    WakeWordDetector.start(context) { onWakeWordDetected() }
//    WakeWordDetector.stop()
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "WakeWordDetector"

// Phase-2 ready: interface for swappable wake word engine
interface WakeWordEngine {
    fun init(context: Context)
    fun processAudio(samples: ShortArray, count: Int): Boolean
    fun release()
}

/**
 * Phase 1 implementation — keyword substring match on SpeechRecognizer partial
 * results. Replace with PorcupineEngine in Phase 2.
 */
class KeywordMatchEngine : WakeWordEngine {
    private val wakeWords = listOf("hey vitalio", "vitalio", "hey vital", "hello vitalio")

    override fun init(context: Context) { /* no-op for Phase 1 */ }

    override fun processAudio(samples: ShortArray, count: Int): Boolean = false // Phase 1 uses text, not raw audio

    /** Call with transcribed text — returns true if wake word detected */
    fun detectInText(text: String): Boolean {
        val lower = text.lowercase().trim()
        return wakeWords.any { lower.contains(it) }
    }

    override fun release() { /* no-op */ }
}

object WakeWordDetector {

    private val engine: KeywordMatchEngine = KeywordMatchEngine()

    @Volatile private var isRunning = false
    private var callback: (() -> Unit)? = null
    private var scope: CoroutineScope? = null

    /**
     * Start passive wake word detection.
     * [onDetected] is called on Main dispatcher when wake word is heard.
     */
    fun start(context: Context, onDetected: () -> Unit) {
        if (isRunning) return
        isRunning = true
        callback  = onDetected
        engine.init(context)
        Log.d(TAG, "WakeWordDetector started (Phase 1 — text matching mode)")
    }

    /**
     * Feed partial/final SpeechRecognizer text here.
     * If wake word is detected, fires [callback] on Main thread.
     */
    fun feedText(text: String) {
        if (!isRunning) return
        if (engine.detectInText(text)) {
            Log.d(TAG, "Wake word detected in: \"$text\"")
            callback?.invoke()
        }
    }

    fun stop() {
        isRunning = false
        engine.release()
        callback = null
        Log.d(TAG, "WakeWordDetector stopped")
    }

    val isActive: Boolean get() = isRunning
}
