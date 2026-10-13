package com.critetiontech.ctvitalio.voice

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceSessionContext
//  Patient context passed to EchoAI each session for smarter extraction.
//  Includes recent vitals (for trend comparison), known conditions, and
//  current scheduled medicines (for NLP medicine matching).
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Snapshot of patient state loaded once at voice session start.
 * Passed to EchoAIService to enable context-aware extraction.
 */
data class VoiceSessionContext(
    /** Previous vitals for delta/trend calculation (key → value string) */
    val lastVitals: Map<String, String> = emptyMap(),
    /** Known chronic conditions / diagnoses (for symptom filtering) */
    val knownConditions: List<String> = emptyList(),
    /** Today's scheduled medicines — name + schedule id */
    val scheduledMedicines: List<ScheduledMedicineRef> = emptyList(),
    /** Patient's preferred unit system */
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    /** Ward / bed info for enterprise nurse mode */
    val wardInfo: WardInfo? = null
) {
    val isEmpty: Boolean get() = lastVitals.isEmpty() && scheduledMedicines.isEmpty()
}

data class ScheduledMedicineRef(
    val id: Int,
    val name: String,
    val scheduledDateTime: String,
    val dosageStrength: String,
    val unit: String,
    val isTaken: Boolean
)

data class WardInfo(
    val wardName: String,
    val bedNumber: String,
    val nurseId: String,
    val nurseName: String
)

enum class UnitSystem { METRIC, IMPERIAL }

/**
 * Nurse / Observer mode config.
 * When set, voice recordings are attributed to the patient
 * but logged against the nurse who recorded them.
 */
data class VoiceSessionConfig(
    val recordingForPatientId: Int,
    val recordingForPatientName: String,
    val recordedByUserId: Int,
    val recordedByName: String,
    val role: RecorderRole = RecorderRole.PATIENT,
    val ward: String = "",
    val bedNumber: String = ""
) {
    val isNurseMode: Boolean get() = role == RecorderRole.NURSE || role == RecorderRole.DOCTOR
    val auditLabel: String get() = when (role) {
        RecorderRole.NURSE  -> "Recorded by Nurse $recordedByName"
        RecorderRole.DOCTOR -> "Recorded by Dr. $recordedByName"
        RecorderRole.PATIENT -> "Self-recorded"
    }
}

enum class RecorderRole { PATIENT, NURSE, DOCTOR }

/**
 * Trend delta between current and last value.
 * Used in the confirmation card to show ↑ / ↓ changes.
 */
data class VitalTrend(
    val key: String,
    val currentValue: Double,
    val previousValue: Double,
    val delta: Double = currentValue - previousValue,
    val direction: TrendDirection = when {
        delta > 0 -> TrendDirection.UP
        delta < 0 -> TrendDirection.DOWN
        else      -> TrendDirection.STABLE
    }
)

enum class TrendDirection { UP, DOWN, STABLE }

/** Computes VitalTrend map from current and previous vitals */
fun computeTrends(
    current: Map<String, String>,
    previous: Map<String, String>
): Map<String, VitalTrend> {
    val trends = mutableMapOf<String, VitalTrend>()
    for ((key, value) in current) {
        val curr = value.toDoubleOrNull() ?: continue
        val prev = previous[key]?.toDoubleOrNull() ?: continue
        trends[key] = VitalTrend(
            key           = key,
            currentValue  = curr,
            previousValue = prev
        )
    }
    return trends
}
