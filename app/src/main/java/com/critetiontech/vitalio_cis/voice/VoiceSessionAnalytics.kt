package com.critetiontech.vitalio_cis.voice

import android.util.Log
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceSessionAnalytics
//  Tracks per-session metrics for enterprise dashboards and debugging.
//  Logs: duration, words spoken, items extracted/saved/failed, latencies.
//  Thread-safe singleton — call from VoiceNavigationManager.
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VoiceAnalytics"

data class VoiceSessionLog(
    val sessionId: String           = UUID.randomUUID().toString(),
    val startTimeMs: Long           = System.currentTimeMillis(),
    var endTimeMs: Long             = 0L,
    var wordsSpoken: Int            = 0,
    var aiCallCount: Int            = 0,
    var aiSuccessCount: Int         = 0,
    var aiFailCount: Int            = 0,
    var totalAiLatencyMs: Long      = 0L,
    var wsReconnectCount: Int       = 0,
    var itemsExtracted: Int         = 0,
    var itemsSaved: Int             = 0,
    var itemsFailed: Int            = 0,
    var anomaliesDetected: Int      = 0,
    var criticalAnomalies: Int      = 0,
    var sessionCancelled: Boolean   = false,
    var nurseMode: Boolean          = false,
    var patientId: String           = ""
) {
    val durationMs: Long get()      = if (endTimeMs > 0) endTimeMs - startTimeMs else System.currentTimeMillis() - startTimeMs
    val avgAiLatencyMs: Long get()  = if (aiCallCount > 0) totalAiLatencyMs / aiCallCount else 0L
    val successRate: Float get()    = if (itemsExtracted > 0) itemsSaved.toFloat() / itemsExtracted else 0f
}

object VoiceSessionAnalytics {

    @Volatile
    private var current: VoiceSessionLog? = null

    private val history = ArrayDeque<VoiceSessionLog>(50)

    // ── Session lifecycle ─────────────────────────────────────────────────────

    fun startSession(patientId: String, nurseMode: Boolean = false) {
        current = VoiceSessionLog(patientId = patientId, nurseMode = nurseMode)
        Log.d(TAG, "Session started: ${current?.sessionId}")
    }

    fun endSession(cancelled: Boolean = false) {
        current?.let { log ->
            log.endTimeMs = System.currentTimeMillis()
            log.sessionCancelled = cancelled
            history.addFirst(log)
            if (history.size > 50) history.removeLast()
            logSummary(log)
        }
        current = null
    }

    // ── Event tracking ────────────────────────────────────────────────────────

    fun onTranscriptReceived(text: String) {
        current?.wordsSpoken = (current?.wordsSpoken ?: 0) + text.trim().split("\\s+".toRegex()).size
    }

    fun onAiCallStart(): Long = System.currentTimeMillis()

    fun onAiCallSuccess(startMs: Long, itemCount: Int) {
        current?.apply {
            aiCallCount++
            aiSuccessCount++
            totalAiLatencyMs += (System.currentTimeMillis() - startMs)
            itemsExtracted += itemCount
        }
    }

    fun onAiCallFail(startMs: Long) {
        current?.apply {
            aiCallCount++
            aiFailCount++
            totalAiLatencyMs += (System.currentTimeMillis() - startMs)
        }
    }

    fun onWsReconnect() {
        current?.wsReconnectCount = (current?.wsReconnectCount ?: 0) + 1
    }

    fun onItemSaved(count: Int = 1) {
        current?.itemsSaved = (current?.itemsSaved ?: 0) + count
    }

    fun onItemFailed(count: Int = 1) {
        current?.itemsFailed = (current?.itemsFailed ?: 0) + count
    }

    fun onAnomalyDetected(alerts: List<VitalAlert>) {
        current?.apply {
            anomaliesDetected += alerts.size
            criticalAnomalies += alerts.count { it.severity == AnomalySeverity.CRITICAL }
        }
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    fun currentSession(): VoiceSessionLog? = current

    /** Returns last N completed sessions */
    fun recentSessions(n: Int = 10): List<VoiceSessionLog> = history.take(n)

    // ── Summary log ──────────────────────────────────────────────────────────

    private fun logSummary(log: VoiceSessionLog) {
        Log.i(TAG, buildString {
            appendLine("═══ Voice Session Summary ═══")
            appendLine("Session ID  : ${log.sessionId}")
            appendLine("Patient     : ${log.patientId}")
            appendLine("Duration    : ${log.durationMs}ms")
            appendLine("Words spoken: ${log.wordsSpoken}")
            appendLine("AI calls    : ${log.aiCallCount} (${log.aiSuccessCount} ok, ${log.aiFailCount} fail)")
            appendLine("Avg AI lat  : ${log.avgAiLatencyMs}ms")
            appendLine("WS reconnect: ${log.wsReconnectCount}")
            appendLine("Extracted   : ${log.itemsExtracted}")
            appendLine("Saved       : ${log.itemsSaved}")
            appendLine("Failed      : ${log.itemsFailed}")
            appendLine("Anomalies   : ${log.anomaliesDetected} (${log.criticalAnomalies} critical)")
            appendLine("Cancelled   : ${log.sessionCancelled}")
            appendLine("Nurse mode  : ${log.nurseMode}")
        })
    }
}
