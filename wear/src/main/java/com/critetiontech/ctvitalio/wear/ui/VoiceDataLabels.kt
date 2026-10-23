package com.critetiontech.ctvitalio.wear.ui

import com.critetiontech.ctvitalio.voice.VoiceExtractedData

/** One-line, watch-sized descriptions of everything extracted from speech. */
fun VoiceExtractedData.summaryLines(): List<String> = buildList {
    vitals?.let { v ->
        val sys = v["vmValueBPSys"]
        val dias = v["vmValueBPDias"]
        when {
            sys != null && dias != null -> add("BP $sys/$dias mmHg")
            sys != null -> add("BP systolic $sys mmHg")
            dias != null -> add("BP diastolic $dias mmHg")
        }
        v["vmValuePulse"]?.let { add("Pulse $it bpm") }
        v["vmValueHeartRate"]?.let { add("Heart rate $it bpm") }
        v["vmValueSPO2"]?.let { add("SpO₂ $it%") }
        v["vmValueTemperature"]?.let { add("Temp $it°") /* °C/°F normalised on save */ }
        v["vmValueRespiratoryRate"]?.let { add("Resp. rate $it /min") }
        v["vmValueRbs"]?.let { add("Blood sugar $it mg/dL") }
        (v["vmValueWeight"] ?: v["weight"])?.let { add("Weight $it kg") }
    }
    symptoms?.forEach { add("Symptom: ${it.details}") }
    fluidIntakes?.forEach { add("${it.name} ${it.quantity} ml") }
    fluidOutput?.let { add("Urine ${it.quantity} ml (${it.colourName})") }
    medicines?.forEach { add("Taken: ${it.medicineName}") }
}