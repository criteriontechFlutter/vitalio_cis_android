package com.critetiontech.ctvitalio

import com.critetiontech.ctvitalio.omron.OmronManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

/** Data shaped like "Sample Data from Library" in the OMRON Implementation Guide (Appendix 4). */
class OmronParseTest {

    private fun bp(sys: Int, dia: Int, pulse: Int, time: Long, seq: Int, user: Int = 1) = hashMapOf<String, Any>(
        "OMRONVitalDataSystolicKey" to sys,
        "OMRONVitalDataDiastolicKey" to dia,
        "OMRONVitalDataPulseKey" to pulse,
        "OMRONVitalDataMeasurementStartDateKey" to time,
        "OMRONVitalDataSequenceKey" to seq,
        "OMRONVitalDataUserIdKey" to user,
        "OMRONVitalDataIrregularFlagKey" to 0
    )

    @Test
    fun `blood pressure readings are parsed with their measured time`() {
        val data = mapOf("OMRONVitalDataBloodPressureKey" to listOf(bp(116, 79, 81, 1_728_581_693L, 24)))
        val r = OmronManager.parse(data, user = 1).single()
        assertEquals(116, r.systolic)
        assertEquals(79, r.diastolic)
        assertEquals(81, r.pulse)
        assertEquals(Instant.ofEpochSecond(1_728_581_693L), r.time)
        assertTrue(r.isBloodPressure)
    }

    @Test
    fun `millisecond timestamps are accepted too`() {
        val data = mapOf("OMRONVitalDataBloodPressureKey" to listOf(bp(120, 80, 70, 1_728_581_693_000L, 1)))
        assertEquals(Instant.ofEpochSecond(1_728_581_693L), OmronManager.parse(data, 1).single().time)
    }

    @Test
    fun `readings of another user on the monitor are skipped`() {
        val data = mapOf("OMRONVitalDataBloodPressureKey" to listOf(bp(120, 80, 70, 1_728_581_693L, 1, user = 2)))
        assertTrue(OmronManager.parse(data, user = 1).isEmpty())
    }

    @Test
    fun `invalid marker value is treated as missing`() {
        val data = mapOf("OMRONVitalDataBloodPressureKey" to listOf(bp(120, 80, Int.MIN_VALUE, 1_728_581_693L, 1)))
        assertNull(OmronManager.parse(data, 1).single().pulse)
    }

    @Test
    fun `unset device clock falls back to transfer time`() {
        val before = Instant.now()
        val data = mapOf("OMRONVitalDataBloodPressureKey" to listOf(bp(120, 80, 70, 1_420_070_460L, 1))) // Jan 2015
        assertTrue(!OmronManager.parse(data, 1).single().time.isBefore(before))
    }

    @Test
    fun `weight readings are parsed`() {
        val data = mapOf(
            "OMRONVitalDataWeightKey" to listOf(
                hashMapOf<String, Any>(
                    "OMRONWeightKey" to 72.4, "OMRONWeightBodyFatPercentageKey" to 21.5,
                    "OMRONWeightDataStartDateKey" to 1_728_581_693L, "OMRONWeightDataSequenceKey" to 3,
                    "OMRONWeightDataUserIdKey" to 1
                )
            )
        )
        val r = OmronManager.parse(data, 1).single()
        assertEquals(72.4, r.weightKg!!, 0.001)
        assertEquals(21.5, r.bodyFatPercent!!, 0.001)
        assertTrue(!r.isBloodPressure)
    }
}
