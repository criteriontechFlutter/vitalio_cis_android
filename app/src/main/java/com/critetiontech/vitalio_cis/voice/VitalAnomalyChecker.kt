package com.critetiontech.vitalio_cis.voice

import android.util.Log

// ═══════════════════════════════════════════════════════════════════════════════
//  VitalAnomalyChecker
//  Clinical range validation for all voice-recorded vitals.
//  Fires WARNING or CRITICAL alerts before saving — patient safety first.
//  Ranges sourced from WHO / standard clinical guidelines.
// ═══════════════════════════════════════════════════════════════════════════════

private const val TAG = "VitalAnomaly"

enum class AnomalySeverity { WARNING, CRITICAL }

data class VitalAlert(
    val vitalKey: String,
    val vitalLabel: String,
    val value: String,
    val severity: AnomalySeverity,
    val message: String,
    val emoji: String = "⚠️"
)

object VitalAnomalyChecker {

    /**
     * Checks all vitals in the map against clinical safe ranges.
     * Returns a list of VitalAlert — empty means all values are normal.
     */
    fun check(vitals: Map<String, String>): List<VitalAlert> {
        val alerts = mutableListOf<VitalAlert>()

        // ── Blood Pressure ────────────────────────────────────────────────────
        val sys  = vitals["vmValueBPSys"]?.toDoubleOrNull()
        val dias = vitals["vmValueBPDias"]?.toDoubleOrNull()

        if (sys != null) {
            when {
                sys >= 180 -> alerts.add(VitalAlert("vmValueBPSys", "Systolic BP", "${sys.toInt()} mmHg",
                    AnomalySeverity.CRITICAL, "Hypertensive Crisis! Immediate medical attention needed.", "🚨"))
                sys >= 140 -> alerts.add(VitalAlert("vmValueBPSys", "Systolic BP", "${sys.toInt()} mmHg",
                    AnomalySeverity.WARNING, "Stage 2 Hypertension. Consult doctor soon.", "⚠️"))
                sys < 90   -> alerts.add(VitalAlert("vmValueBPSys", "Systolic BP", "${sys.toInt()} mmHg",
                    AnomalySeverity.WARNING, "Low blood pressure detected. Monitor for dizziness.", "⚠️"))
            }
        }
        if (dias != null) {
            when {
                dias >= 120 -> alerts.add(VitalAlert("vmValueBPDias", "Diastolic BP", "${dias.toInt()} mmHg",
                    AnomalySeverity.CRITICAL, "Hypertensive Crisis! Immediate medical attention needed.", "🚨"))
                dias >= 90  -> alerts.add(VitalAlert("vmValueBPDias", "Diastolic BP", "${dias.toInt()} mmHg",
                    AnomalySeverity.WARNING, "High diastolic pressure. Monitor closely.", "⚠️"))
                dias < 60   -> alerts.add(VitalAlert("vmValueBPDias", "Diastolic BP", "${dias.toInt()} mmHg",
                    AnomalySeverity.WARNING, "Low diastolic pressure.", "⚠️"))
            }
        }

        // ── Heart Rate / Pulse ────────────────────────────────────────────────
        val pulse = (vitals["vmValuePulse"] ?: vitals["vmValueHeartRate"])?.toDoubleOrNull()
        if (pulse != null) {
            when {
                pulse >= 150 -> alerts.add(VitalAlert("vmValuePulse", "Heart Rate", "${pulse.toInt()} bpm",
                    AnomalySeverity.CRITICAL, "Severe tachycardia! Seek immediate attention.", "🚨"))
                pulse >= 100 -> alerts.add(VitalAlert("vmValuePulse", "Heart Rate", "${pulse.toInt()} bpm",
                    AnomalySeverity.WARNING, "Tachycardia detected.", "⚠️"))
                pulse < 40  -> alerts.add(VitalAlert("vmValuePulse", "Heart Rate", "${pulse.toInt()} bpm",
                    AnomalySeverity.CRITICAL, "Severe bradycardia! Seek immediate attention.", "🚨"))
                pulse < 60  -> alerts.add(VitalAlert("vmValuePulse", "Heart Rate", "${pulse.toInt()} bpm",
                    AnomalySeverity.WARNING, "Low heart rate (bradycardia).", "⚠️"))
            }
        }

        // ── SpO2 ──────────────────────────────────────────────────────────────
        val spo2 = vitals["vmValueSPO2"]?.toDoubleOrNull()
        if (spo2 != null) {
            when {
                spo2 < 85 -> alerts.add(VitalAlert("vmValueSPO2", "SpO2", "${spo2.toInt()}%",
                    AnomalySeverity.CRITICAL, "Critical hypoxia! Oxygen needed immediately.", "🚨"))
                spo2 < 92 -> alerts.add(VitalAlert("vmValueSPO2", "SpO2", "${spo2.toInt()}%",
                    AnomalySeverity.WARNING, "Low oxygen saturation. Consider supplemental oxygen.", "⚠️"))
                spo2 > 100 -> alerts.add(VitalAlert("vmValueSPO2", "SpO2", "${spo2.toInt()}%",
                    AnomalySeverity.WARNING, "SpO2 value above 100% — likely a recording error.", "⚠️"))
            }
        }

        // ── Temperature (stored as °F) ─────────────────────────────────────────
        val temp = vitals["vmValueTemperature"]?.toDoubleOrNull()
        if (temp != null) {
            when {
                temp >= 104.0 -> alerts.add(VitalAlert("vmValueTemperature", "Temperature", "${temp}°F",
                    AnomalySeverity.CRITICAL, "High fever (≥104°F). Seek medical attention immediately.", "🚨"))
                temp >= 100.4 -> alerts.add(VitalAlert("vmValueTemperature", "Temperature", "${temp}°F",
                    AnomalySeverity.WARNING, "Fever detected (${temp}°F). Monitor closely.", "⚠️"))
                temp < 96.8  -> alerts.add(VitalAlert("vmValueTemperature", "Temperature", "${temp}°F",
                    AnomalySeverity.WARNING, "Hypothermia risk. Temperature below normal.", "⚠️"))
            }
        }

        // ── Blood Sugar (RBS) ─────────────────────────────────────────────────
        val rbs = vitals["vmValueRbs"]?.toDoubleOrNull()
        if (rbs != null) {
            when {
                rbs >= 400 -> alerts.add(VitalAlert("vmValueRbs", "Blood Sugar (RBS)", "${rbs.toInt()} mg/dL",
                    AnomalySeverity.CRITICAL, "Dangerously high blood sugar! Risk of diabetic ketoacidosis.", "🚨"))
                rbs >= 200 -> alerts.add(VitalAlert("vmValueRbs", "Blood Sugar (RBS)", "${rbs.toInt()} mg/dL",
                    AnomalySeverity.WARNING, "Hyperglycemia detected. Consult doctor.", "⚠️"))
                rbs < 54   -> alerts.add(VitalAlert("vmValueRbs", "Blood Sugar (RBS)", "${rbs.toInt()} mg/dL",
                    AnomalySeverity.CRITICAL, "Severe hypoglycemia! Immediate glucose intake needed.", "🚨"))
                rbs < 70   -> alerts.add(VitalAlert("vmValueRbs", "Blood Sugar (RBS)", "${rbs.toInt()} mg/dL",
                    AnomalySeverity.WARNING, "Low blood sugar. Take glucose soon.", "⚠️"))
            }
        }

        // ── Respiratory Rate ──────────────────────────────────────────────────
        val rr = vitals["vmValueRespiratoryRate"]?.toDoubleOrNull()
        if (rr != null) {
            when {
                rr >= 30 -> alerts.add(VitalAlert("vmValueRespiratoryRate", "Respiratory Rate", "${rr.toInt()}/min",
                    AnomalySeverity.CRITICAL, "Severe respiratory distress! Seek help immediately.", "🚨"))
                rr >= 20 -> alerts.add(VitalAlert("vmValueRespiratoryRate", "Respiratory Rate", "${rr.toInt()}/min",
                    AnomalySeverity.WARNING, "Elevated respiratory rate (tachypnea).", "⚠️"))
                rr < 8   -> alerts.add(VitalAlert("vmValueRespiratoryRate", "Respiratory Rate", "${rr.toInt()}/min",
                    AnomalySeverity.CRITICAL, "Critically low respiratory rate! Seek immediate help.", "🚨"))
                rr < 12  -> alerts.add(VitalAlert("vmValueRespiratoryRate", "Respiratory Rate", "${rr.toInt()}/min",
                    AnomalySeverity.WARNING, "Low respiratory rate (bradypnea).", "⚠️"))
            }
        }

        // ── Weight (sanity check only — no clinical range) ────────────────────
        val weight = vitals["weight"]?.toDoubleOrNull()
        if (weight != null && (weight !in 1.0..500.0)) {
            alerts.add(VitalAlert("weight", "Body Weight", "$weight kg",
                AnomalySeverity.WARNING, "Weight value looks unusual — please verify.", "⚠️"))
        }

        if (alerts.isNotEmpty()) {
            Log.w(TAG, "Anomalies detected: ${alerts.map { it.vitalLabel + " " + it.severity }}")
        }
        return alerts
    }

    /** Convenience: true if any CRITICAL alert exists */
    fun hasCritical(alerts: List<VitalAlert>): Boolean =
        alerts.any { it.severity == AnomalySeverity.CRITICAL }
}
