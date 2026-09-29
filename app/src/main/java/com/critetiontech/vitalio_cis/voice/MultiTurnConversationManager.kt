package com.critetiontech.vitalio_cis.voice

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// ═══════════════════════════════════════════════════════════════════════════════
//  MultiTurnConversationManager
//  Enables guided Q&A dialog when user gives incomplete vitals.
//  Example:
//    User: "BP record karo"
//    AI prompt: "Systolic value batao?"
//    User: "130"
//    AI prompt: "Diastolic?"
//    User: "85" → BP 130/85 recorded
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "MultiTurn"

enum class ConversationSlot {
    NONE,
    AWAITING_BP_SYS,
    AWAITING_BP_DIAS,
    AWAITING_TEMPERATURE,
    AWAITING_PULSE,
    AWAITING_SPO2,
    AWAITING_RBS,
    AWAITING_FLUID_TYPE,
    AWAITING_FLUID_QTY,
    COMPLETE
}

data class ConversationState(
    val slot: ConversationSlot = ConversationSlot.NONE,
    val promptMessage: String = "",
    val collectedData: MutableMap<String, String> = mutableMapOf()
) {
    val isActive: Boolean get() = slot != ConversationSlot.NONE && slot != ConversationSlot.COMPLETE
}

object MultiTurnConversationManager {

    private val _state = MutableStateFlow(ConversationState())
    val state: StateFlow<ConversationState> = _state.asStateFlow()

    val isActive: Boolean get() = _state.value.isActive
    val currentPrompt: String get() = _state.value.promptMessage

    // ── Intent detection ──────────────────────────────────────────────────────

    /**
     * Detects if the text is an intent without values (e.g. "record BP")
     * and enters a multi-turn dialog for that intent.
     * Returns a prompt string if multi-turn was triggered, null otherwise.
     */
    fun detectAndStart(text: String): String? {
        val lower = text.lowercase().trim()
        return when {
            // BP intent without numbers
            (lower.contains("bp") || lower.contains("blood pressure") || lower.contains("pressure record")) &&
            !lower.any { it.isDigit() } -> {
                startSlot(ConversationSlot.AWAITING_BP_SYS, "Systolic (upper) value kya hai? (e.g. 130)")
            }
            // Temperature intent without numbers
            (lower.contains("temperature") || lower.contains("temp") || lower.contains("bukhar")) &&
            !lower.any { it.isDigit() } -> {
                startSlot(ConversationSlot.AWAITING_TEMPERATURE, "Temperature kya hai? (e.g. 98.6)")
            }
            // Sugar intent without numbers
            (lower.contains("sugar") || lower.contains("rbs") || lower.contains("glucose")) &&
            !lower.any { it.isDigit() } -> {
                startSlot(ConversationSlot.AWAITING_RBS, "Blood sugar reading kya hai? (e.g. 120 mg/dL)")
            }
            // Pulse intent without numbers
            (lower.contains("pulse") || lower.contains("heart rate")) &&
            !lower.any { it.isDigit() } -> {
                startSlot(ConversationSlot.AWAITING_PULSE, "Pulse rate kya hai? (e.g. 72 bpm)")
            }
            else -> null
        }
    }

    /**
     * Processes user's reply to an active dialog slot.
     * Returns a VoiceExtractedData if collection is complete, null if still collecting.
     */
    fun processReply(text: String): Pair<String?, VoiceExtractedData?> {
        val current = _state.value
        if (!current.isActive) return Pair(null, null)

        val lower  = text.lowercase().trim()
        val number = extractFirstNumber(lower)

        return when (current.slot) {
            ConversationSlot.AWAITING_BP_SYS -> {
                if (number != null) {
                    current.collectedData["vmValueBPSys"] = number
                    val nextPrompt = "Diastolic (lower) value kya hai? (e.g. 85)"
                    _state.value = current.copy(slot = ConversationSlot.AWAITING_BP_DIAS, promptMessage = nextPrompt)
                    Pair(nextPrompt, null)
                } else {
                    Pair("Kripya number mein batao — Systolic value kya hai?", null)
                }
            }
            ConversationSlot.AWAITING_BP_DIAS -> {
                if (number != null) {
                    current.collectedData["vmValueBPDias"] = number
                    complete(current.collectedData)
                } else {
                    Pair("Kripya number mein batao — Diastolic value kya hai?", null)
                }
            }
            ConversationSlot.AWAITING_TEMPERATURE -> {
                if (number != null) {
                    current.collectedData["vmValueTemperature"] = number
                    complete(current.collectedData)
                } else {
                    Pair("Temperature number mein batao?", null)
                }
            }
            ConversationSlot.AWAITING_PULSE -> {
                if (number != null) {
                    current.collectedData["vmValuePulse"] = number
                    complete(current.collectedData)
                } else {
                    Pair("Pulse number mein batao?", null)
                }
            }
            ConversationSlot.AWAITING_RBS -> {
                if (number != null) {
                    current.collectedData["vmValueRbs"] = number
                    complete(current.collectedData)
                } else {
                    Pair("Blood sugar value number mein batao?", null)
                }
            }
            else -> { reset(); Pair(null, null) }
        }
    }

    fun reset() {
        _state.value = ConversationState()
        Log.d(TAG, "Multi-turn reset")
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun startSlot(slot: ConversationSlot, prompt: String): String {
        _state.value = ConversationState(slot = slot, promptMessage = prompt)
        Log.d(TAG, "Multi-turn started: $slot")
        return prompt
    }

    private fun complete(data: Map<String, String>): Pair<String, VoiceExtractedData> {
        val extracted = VoiceExtractedData(vitals = data.toMap())
        _state.value = ConversationState(slot = ConversationSlot.COMPLETE)
        Log.d(TAG, "Multi-turn complete: $data")
        return Pair("✅ Data recorded successfully!", extracted)
    }

    private fun extractFirstNumber(text: String): String? {
        val match = Regex("""(\d+\.?\d*)""").find(text)
        return match?.groupValues?.get(1)
    }
}
