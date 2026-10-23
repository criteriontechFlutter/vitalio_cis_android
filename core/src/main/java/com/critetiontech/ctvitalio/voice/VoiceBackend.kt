package com.critetiontech.ctvitalio.voice

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceBackend
//  Platform port for the shared voice pipeline. The phone app implements it with
//  PrefsManager + use cases; the watch implements it with data synced from the phone.
//  Register an implementation in Application.onCreate() via VoiceCore.backend.
// ═══════════════════════════════════════════════════════════════════════════════

/** Logged-in patient identity needed by the voice pipeline. */
data class VoicePatient(
    val pid: Int,
    val clientId: Int,
    val uhId: String
)

interface VoiceBackend {
    /** Currently logged-in patient, or null when signed out. */
    fun currentPatient(): VoicePatient?

    /** Medicines scheduled on [date] (yyyy-MM-dd) that are not yet marked as taken. */
    suspend fun pendingMedicines(date: String): List<ScheduledMedicineRef>

    /** Latest recorded vitals as (vitalName, value) pairs, e.g. ("Pulse", 72.0). */
    suspend fun lastVitalReadings(): List<Pair<String, Double>>

    // ── Save calls — bodies are built by VoiceNavigationManager; return true on success ──
    suspend fun addVital(body: Map<String, Any>): Boolean
    suspend fun insertSymptoms(body: Map<String, Any>): Boolean
    suspend fun addIntake(body: Map<String, Any>): Boolean
    suspend fun addOutput(body: Map<String, Any>): Boolean
    suspend fun markMedicineTaken(scheduleId: Int, scheduledDateTime: String): Boolean
}

object VoiceCore {
    lateinit var backend: VoiceBackend
}