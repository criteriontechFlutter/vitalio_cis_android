package com.critetiontech.ctvitalio.wear.ui

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.critetiontech.ctvitalio.voice.VoiceExtractedData
import com.critetiontech.ctvitalio.wear.R

/** Visual category of an extracted item: icon + accent colour on the confirm screen. */
enum class ItemKind(@param:DrawableRes val icon: Int, val accent: Color) {
    HEART(R.drawable.ic_heart, Color(0xFFFF8FA3)),
    OXYGEN(R.drawable.ic_drop, Color(0xFF7CC8FF)),
    TEMPERATURE(R.drawable.ic_thermo, Color(0xFFFFC266)),
    SUGAR(R.drawable.ic_drop, Color(0xFFFFB38A)),
    WEIGHT(R.drawable.ic_weight, Color(0xFF6FE3C8)),
    FLUID(R.drawable.ic_drop, Color(0xFF7CC8FF)),
    URINE(R.drawable.ic_drop, Color(0xFFFFE08A)),
    MEDICINE(R.drawable.ic_pill, Color(0xFFC4A7FF)),
    SYMPTOM(R.drawable.ic_note, Color(0xFFB4BCCB)),
}

/** One watch-sized row: small label over a prominent value. */
data class SummaryItem(val kind: ItemKind, val label: String, val value: String)

/** Everything extracted from speech, as rows for the confirm screen. */
fun VoiceExtractedData.summaryItems(): List<SummaryItem> = buildList {
    vitals?.let { v ->
        val sys = v["vmValueBPSys"]
        val dias = v["vmValueBPDias"]
        when {
            sys != null && dias != null -> add(SummaryItem(ItemKind.HEART, "Blood pressure", "$sys/$dias mmHg"))
            sys != null -> add(SummaryItem(ItemKind.HEART, "BP systolic", "$sys mmHg"))
            dias != null -> add(SummaryItem(ItemKind.HEART, "BP diastolic", "$dias mmHg"))
        }
        v["vmValuePulse"]?.let { add(SummaryItem(ItemKind.HEART, "Pulse", "$it bpm")) }
        v["vmValueHeartRate"]?.let { add(SummaryItem(ItemKind.HEART, "Heart rate", "$it bpm")) }
        v["vmValueSPO2"]?.let { add(SummaryItem(ItemKind.OXYGEN, "Oxygen (SpO₂)", "$it%")) }
        v["vmValueTemperature"]?.let { add(SummaryItem(ItemKind.TEMPERATURE, "Temperature", "$it°")) } // °C/°F normalised on save
        v["vmValueRespiratoryRate"]?.let { add(SummaryItem(ItemKind.OXYGEN, "Breathing rate", "$it /min")) }
        v["vmValueRbs"]?.let { add(SummaryItem(ItemKind.SUGAR, "Blood sugar", "$it mg/dL")) }
        (v["vmValueWeight"] ?: v["weight"])?.let { add(SummaryItem(ItemKind.WEIGHT, "Weight", "$it kg")) }
    }
    symptoms?.forEach { add(SummaryItem(ItemKind.SYMPTOM, "Symptom", it.details)) }
    fluidIntakes?.forEach { add(SummaryItem(ItemKind.FLUID, it.name, "${it.quantity} ml")) }
    fluidOutput?.let { add(SummaryItem(ItemKind.URINE, "Urine output", "${it.quantity} ml · ${it.colourName}")) }
    medicines?.forEach { add(SummaryItem(ItemKind.MEDICINE, "Medicine taken", it.medicineName)) }
}
