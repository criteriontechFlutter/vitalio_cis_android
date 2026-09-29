package com.critetiontech.vitalio_cis.voice

import android.util.Log

// ═══════════════════════════════════════════════════════════════════════════════
//  VitalUnitNormalizer
//  Detects and converts units spoken by users into the canonical units
//  expected by the Vitalio API.
//  Handles Hindi + English mixed speech.
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "UnitNormalizer"

object VitalUnitNormalizer {

    // ── Temperature ──────────────────────────────────────────────────────────

    /**
     * Normalizes temperature to Fahrenheit (API stores in °F).
     * Detects °C keywords and converts. Defaults to °F if ambiguous.
     */
    fun normalizeTemperature(value: String, contextText: String): String {
        val num = value.toDoubleOrNull() ?: return value
        val lower = contextText.lowercase()
        val isCelsius = lower.contains("celsius") || lower.contains("centigrade") ||
                lower.contains("°c") || lower.contains(" c ") ||
                // Heuristic: values < 45 are almost certainly Celsius
                (num < 45.0 && !lower.contains("fahrenheit") && !lower.contains("°f"))
        return if (isCelsius) {
            val fahrenheit = (num * 9.0 / 5.0) + 32.0
            Log.d(TAG, "Temperature: ${num}°C → ${fahrenheit}°F")
            String.format("%.1f", fahrenheit)
        } else {
            value
        }
    }

    // ── Blood Sugar ───────────────────────────────────────────────────────────

    /**
     * Normalizes blood sugar to mg/dL (API standard).
     * Converts mmol/L if detected.
     * mmol/L range: 2–30. mg/dL range: 40–600.
     */
    fun normalizeSugar(value: String, contextText: String): String {
        val num = value.toDoubleOrNull() ?: return value
        val lower = contextText.lowercase()
        val isMmol = lower.contains("mmol") ||
                // Heuristic: tiny values like 6.5, 7.2 → likely mmol/L
                (num < 30.0 && !lower.contains("mg"))
        return if (isMmol) {
            val mgDl = (num * 18.016).toInt()
            Log.d(TAG, "Sugar: ${num} mmol/L → $mgDl mg/dL")
            mgDl.toString()
        } else {
            num.toInt().toString()
        }
    }

    // ── Fluid Volume ──────────────────────────────────────────────────────────

    /**
     * Converts spoken fluid quantity words into ml.
     * Handles: glass, cup, bottle, liter/litre, ml, oz
     * Also handles Hindi: gilas, botal, paav
     */
    fun normalizeFluidToMl(quantity: Int, contextText: String): Int {
        val lower = contextText.lowercase()
        return when {
            // Liters
            lower.contains("liter") || lower.contains("litre") ||
            lower.contains("ltr") || lower.contains("litre")        -> quantity * 1000
            // Half-liter
            lower.contains("half liter") || lower.contains("half litre") -> 500
            // Bottle
            lower.contains("bottle") || lower.contains("botal")     -> quantity * 500
            // Glass / gilas
            lower.contains("glass") || lower.contains("gilas")      -> quantity * 250
            // Cup
            lower.contains("cup") || lower.contains("mug")          -> quantity * 240
            // Ounce
            lower.contains("oz") || lower.contains("ounce")         -> (quantity * 29.5735).toInt()
            // Paav (quarter litre in Hindi)
            lower.contains("paav") || lower.contains("pav")         -> quantity * 250
            // Already in ml
            lower.contains("ml") || lower.contains("milliliter")    -> quantity
            // Default: assume ml if value > 50, otherwise assume glasses
            else -> if (quantity > 50) quantity else quantity * 250
        }.also { result ->
            if (result != quantity) Log.d(TAG, "Fluid: $quantity (raw) → $result ml")
        }
    }

    // ── Weight ────────────────────────────────────────────────────────────────

    /**
     * Normalizes weight to kg (API standard).
     * Converts lbs/pounds if detected.
     */
    fun normalizeWeight(value: String, contextText: String): String {
        val num = value.toDoubleOrNull() ?: return value
        val lower = contextText.lowercase()
        val isLbs = lower.contains("lbs") || lower.contains("pound") || lower.contains("lb ")
        return if (isLbs) {
            val kg = num * 0.453592
            Log.d(TAG, "Weight: ${num} lbs → $kg kg")
            String.format("%.1f", kg)
        } else {
            value
        }
    }

    /**
     * Master normalizer — applies all unit normalizations to a full vitals map.
     * Call this AFTER AI/NLP extraction and BEFORE saving.
     */
    fun normalizeAll(vitals: Map<String, String>, contextText: String): Map<String, String> {
        val result = vitals.toMutableMap()
        result["vmValueTemperature"]?.let {
            result["vmValueTemperature"] = normalizeTemperature(it, contextText)
        }
        result["vmValueRbs"]?.let {
            result["vmValueRbs"] = normalizeSugar(it, contextText)
        }
        // Normalize weight — handle both "weight" (NLP) and "vmValueWeight" (remapped) keys
        result["weight"]?.let {
            result["weight"] = normalizeWeight(it, contextText)
        }
        result["vmValueWeight"]?.let {
            result["vmValueWeight"] = normalizeWeight(it, contextText)
        }
        return result
    }
}
