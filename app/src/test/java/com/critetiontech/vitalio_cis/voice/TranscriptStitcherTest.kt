package com.critetiontech.ctvitalio.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TranscriptStitcherTest {

    /** Replays a backend message stream the way VoiceNavigationManager consumes it. */
    private fun stitch(messages: List<Pair<String, String>>): String {
        val partials = ArrayDeque<String>()
        val pieces = mutableListOf<Pair<String, Boolean>>()
        for ((type, raw) in messages) {
            when (type) {
                "partial" -> { partials.addFirst(raw); if (partials.size > 3) partials.removeLast() }
                "final"   -> pieces += raw.trim() to TranscriptStitcher.continuesWithoutSpace(raw, partials.toList())
            }
        }
        return TranscriptStitcher.join(pieces)
    }

    // Exact sequence from the Galaxy Tab S10 FE log (2026-10-19 13:22), partials trimmed to the last few
    private val deviceLog = listOf(
        "partial" to "Hello, hello. I have fever, my he",
        "partial" to " I have fever, my he",
        "final"   to "Hello, hello.",
        "partial" to " I have fever, my heart rate is 72",
        "partial" to "ever, my heart rate is 72",
        "final"   to "I have f",
        "partial" to "ever, my heart rate is 72, I took paracetamol",
        "partial" to "art rate is 72, I took paracetamol",
        "final"   to "ever, my he",
        "partial" to "art rate is 72, I took paracetamol and I drink 10",
        "partial" to ", I took paracetamol and I drink 100",
        "final"   to "art rate is 72",
        "partial" to ", I took paracetamol and I drink 100 ml of OT",
        "final"   to ", I took paracetamol and I drink 100 ml of water."
    )

    @Test fun `mid-word cuts are glued back`() {
        assertEquals(
            "Hello, hello. I have fever, my heart rate is 72, I took paracetamol and I drink 100 ml of water.",
            stitch(deviceLog)
        )
    }

    @Test fun `stitched device transcript yields fever, pulse, water and paracetamol`() {
        val meds = listOf(ScheduledMedicineRef(7, "paracetamol", "2026-10-19 17:47", "10", "mg", isTaken = false))
        val data = VoiceNlpParser.extract(stitch(deviceLog), VoiceExtractedData(), meds)
        assertEquals("72", data.vitals?.get("vmValuePulse"))
        assertEquals(listOf("Fever"), data.symptoms?.map { it.details })
        assertEquals(100, data.fluidIntakes?.single()?.quantity)
        assertEquals(listOf(7), data.medicines?.map { it.medicineId })
    }

    @Test fun `cut between words keeps the space`() {
        assertFalse(TranscriptStitcher.continuesWithoutSpace("Hello, hello.", listOf(" I have", "Hello, hello. I have")))
    }

    @Test fun `cut inside a word glues`() {
        assertTrue(TranscriptStitcher.continuesWithoutSpace("I have f", listOf("ever, my", " I have fever, my")))
    }

    @Test fun `no evidence keeps the space`() {
        assertFalse(TranscriptStitcher.continuesWithoutSpace("I have", emptyList()))
    }

    // Second device run (13:27): cuts inside "110/80" and "72", and "feverish" instead of "fever"
    @Test fun `second device run`() {
        val text = stitch(listOf(
            "partial" to "Hello, hello. My blood pressure is 110/80",
            "partial" to " My blood pressure is 110/80",
            "final"   to "Hello, hello.",
            "partial" to "My blood pressure is 110/80, my heart rate is 72",
            "partial" to "80, my heart rate is 72",
            "final"   to "My blood pressure is 110/",
            "partial" to "80, my heart rate is 72, and I'm feeling",
            "partial" to "2, and I'm feeling",
            "final"   to "80, my heart rate is 7",
            "partial" to "2, and I'm feeling feverish.",
            "partial" to " feverish. I have",
            "final"   to "2, and I'm feeling",
            "final"   to "feverish. I have"
        ))
        assertEquals("Hello, hello. My blood pressure is 110/80, my heart rate is 72, and I'm feeling feverish. I have", text)
        val data = VoiceNlpParser.extract(text)
        assertEquals("110", data.vitals?.get("vmValueBPSys"))
        assertEquals("80", data.vitals?.get("vmValueBPDias"))
        assertEquals("72", data.vitals?.get("vmValuePulse"))
        assertEquals(listOf("Fever"), data.symptoms?.map { it.details })
    }

    @Test fun `my heart is 72 is a pulse`() {
        assertEquals("72", VoiceNlpParser.extract("my heart is 72").vitals?.get("vmValuePulse"))
        assertEquals("80", VoiceNlpParser.extract("heart beat 80").vitals?.get("vmValuePulse"))
    }
}
