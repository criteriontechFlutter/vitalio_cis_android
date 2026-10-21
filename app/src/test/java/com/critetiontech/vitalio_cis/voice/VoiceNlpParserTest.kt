package com.critetiontech.ctvitalio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNlpParserTest {

    private fun parse(text: String, meds: List<ScheduledMedicineRef> = emptyList()) =
        VoiceNlpParser.extract(text, VoiceExtractedData(), meds)

    private fun symptoms(text: String) = parse(text).symptoms.orEmpty().map { it.details }
    private fun intakes(text: String) = parse(text).fluidIntakes.orEmpty().associate { it.name to it.quantity }

    // ── Whole-word matching (false positives from substring matching) ─────────

    @Test fun `multivitamin is not vomiting`() {
        assertFalse("Vomiting" in symptoms("maine multivitamin li"))
    }

    @Test fun `hypertension is not anxiety`() {
        assertFalse("Anxiety" in symptoms("mujhe hypertension hai"))
    }

    @Test fun `rash is not juice`() {
        val data = parse("skin rash hai")
        assertNull(data.fluidIntakes)
        assertEquals(listOf("Skin Rash"), data.symptoms?.map { it.details })
    }

    @Test fun `instead is not tea`() {
        assertNull(parse("I took the tablet instead of syrup").fluidIntakes)
    }

    @Test fun `cold water is not a cold`() {
        val data = parse("cold water piya")
        assertFalse("Cold" in data.symptoms.orEmpty().map { it.details })
        assertEquals(DEFAULT_GLASS, data.fluidIntakes?.single()?.quantity)
    }

    @Test fun `a real cold is still detected`() {
        assertTrue("Cold" in symptoms("I have a cold and cough"))
    }

    @Test fun `speed is not urine output`() {
        assertNull(parse("internet speed slow hai").fluidOutput)
    }

    @Test fun `pani pee liya means drank, not urine`() {
        val data = parse("maine 2 glass pani pee liya")
        assertNull(data.fluidOutput)
        assertEquals(500, data.fluidIntakes?.single()?.quantity)
    }

    @Test fun `red needs to be a word for urine colour`() {
        assertEquals("Light Yellow", VoiceNlpParser.buildUrineOutput(200, "urine 200 ml, I covered it").colourName)
        assertEquals("Red", VoiceNlpParser.buildUrineOutput(200, "urine 200 ml red colour").colourName)
    }

    // ── Quantities ────────────────────────────────────────────────────────────

    @Test fun `glasses and cups convert to ml`() {
        assertEquals(500, intakes("2 glass water")["Water"])
        assertEquals(300, intakes("2 cups of tea")["Tea"])
    }

    @Test fun `liter applies only to its own number`() {
        val q = intakes("1 liter water aur 200 ml milk")
        assertEquals(1000, q["Water"])
        assertEquals(200, q["Milk"])
    }

    @Test fun `quantity after the drink`() {
        assertEquals(300, intakes("water 300 ml")["Water"])
        assertEquals(1500, intakes("pani 1.5 litre")["Water"])
    }

    @Test fun `urine quantity`() {
        assertEquals(300, parse("urine output 300 ml dark yellow").fluidOutput?.quantity)
        assertEquals("Dark Yellow", parse("urine output 300 ml dark yellow").fluidOutput?.colourName)
    }

    // ── Vitals ────────────────────────────────────────────────────────────────

    @Test fun `bp with keyword or separator`() {
        assertEquals("120" to "80", bp("bp 120 80"))
        assertEquals("130" to "85", bp("blood pressure is 130 by 85"))
        assertEquals("140" to "90", bp("140/90"))
        assertEquals("125" to "82", bp("125 over 82"))
    }

    @Test fun `two unrelated numbers are not bp`() {
        val v = parse("pulse 98 spo2 97").vitals.orEmpty()
        assertNull(v["vmValueBPSys"])
        assertEquals("98", v["vmValuePulse"])
        assertEquals("97", v["vmValueSPO2"])
    }

    @Test fun `duration is not pulse`() {
        assertNull(parse("3 hr 30 min baad dawai li").vitals?.get("vmValuePulse"))
        assertEquals("76", parse("hr 76").vitals?.get("vmValuePulse"))
    }

    @Test fun `sugar temperature spo2`() {
        val v = parse("sugar 140, temperature 99.5 and oxygen 96").vitals.orEmpty()
        assertEquals("140", v["vmValueRbs"])
        assertEquals("99.5", v["vmValueTemperature"])
        assertEquals("96", v["vmValueSPO2"])
    }

    @Test fun `mixed sentence`() {
        val data = parse("BP 120/80, pulse 72, 2 glass water piya aur sir dard hai")
        assertEquals("120" to "80", data.vitals?.let { it["vmValueBPSys"] to it["vmValueBPDias"] })
        assertEquals("72", data.vitals?.get("vmValuePulse"))
        assertEquals(500, data.fluidIntakes?.single()?.quantity)
        assertEquals(listOf("Headache"), data.symptoms?.map { it.details })
    }

    // ── Validation of AI values ───────────────────────────────────────────────

    @Test fun `implausible ai values are dropped`() {
        val ai = VoiceExtractedData(vitals = mapOf(
            "vmValueSPO2" to "980", "vmValuePulse" to "72", "vmValueTemperature" to "abc"
        ))
        val (clean, rejected) = VoiceNlpParser.sanitize(ai)
        assertEquals(mapOf("vmValuePulse" to "72"), clean.vitals)
        assertEquals(2, rejected.size)
    }

    @Test fun `inverted bp from ai is dropped`() {
        val ai = VoiceExtractedData(vitals = mapOf("vmValueBPSys" to "80", "vmValueBPDias" to "120"))
        assertNull(VoiceNlpParser.sanitize(ai).first.vitals)
    }

    @Test fun `bad ai value is replaced by the spoken one`() {
        val ai = VoiceExtractedData(vitals = mapOf("vmValueSPO2" to "980"))
        assertEquals("98", VoiceNlpParser.extract("spo2 98", ai).vitals?.get("vmValueSPO2"))
    }

    @Test fun `celsius temperature is kept`() {
        val ai = VoiceExtractedData(vitals = mapOf("vmValueTemperature" to "38.5"))
        assertEquals("38.5", VoiceNlpParser.sanitize(ai).first.vitals?.get("vmValueTemperature"))
    }

    // ── Medicines ─────────────────────────────────────────────────────────────

    private val meds = listOf(
        ScheduledMedicineRef(11, "Dolo 650", "2026-10-19 09:00", "650", "mg", isTaken = false),
        ScheduledMedicineRef(12, "D3", "2026-10-19 09:00", "60000", "IU", isTaken = false),
        ScheduledMedicineRef(13, "Metformin", "2026-10-19 08:00", "500", "mg", isTaken = true)
    )

    @Test fun `medicine matched by whole name`() {
        val found = parse("maine dolo 650 le li", meds).medicines.orEmpty().map { it.medicineId }
        assertEquals(listOf(11), found)
    }

    @Test fun `short medicine name needs whole word`() {
        assertNull(parse("room d3a mein hoon", meds).medicines)
    }

    @Test fun `taken medicine is not matched again`() {
        assertNull(parse("metformin le li", meds).medicines)
    }

    // ── Data helpers ──────────────────────────────────────────────────────────

    @Test fun `without keeps only new or changed items`() {
        val local = parse("bp 120/80 and 2 glass water")
        val ai = local.merge(VoiceExtractedData(vitals = mapOf("vmValuePulse" to "72")))
        val diff = ai.without(local)
        assertEquals(mapOf("vmValuePulse" to "72"), diff.vitals)
        assertNull(diff.fluidIntakes)
        assertEquals(1, diff.itemCount)
    }

    private fun bp(text: String) = parse(text).vitals?.let { it["vmValueBPSys"] to it["vmValueBPDias"] }

    private companion object {
        const val DEFAULT_GLASS = 250
    }
}
