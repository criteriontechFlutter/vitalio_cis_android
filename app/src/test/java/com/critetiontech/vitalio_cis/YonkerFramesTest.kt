package com.critetiontech.ctvitalio

import com.critetiontech.ctvitalio.ui.screens.yonkerDevicesFor
import com.critetiontech.ctvitalio.yonker.YonkerEvent
import com.critetiontech.ctvitalio.yonker.YonkerFrames
import com.critetiontech.ctvitalio.yonker.YonkerKind
import com.critetiontech.ctvitalio.yonker.YonkerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class YonkerFramesTest {

    private val now = Instant.parse("2026-10-23T10:00:00Z")
    private fun frame(vararg b: Int) = ByteArray(b.size) { b[it].toByte() }
    private fun reading(kind: YonkerKind, vararg b: Int) = (YonkerFrames.parse(kind, frame(*b), now) as YonkerEvent.Reading).reading

    private val padding = IntArray(11)

    @Test
    fun `BP measuring frame gives cuff pressure`() {
        // Recorded from a YK-BPA1
        assertEquals(YonkerEvent.Pressure(0xA1), YonkerFrames.parse(YonkerKind.BP, frame(0x80, 0x00, 0xA1, 0, *padding), now))
        assertEquals(YonkerEvent.Pressure(260), YonkerFrames.parse(YonkerKind.BP, frame(0x80, 0x01, 0x04), now))
    }

    @Test
    fun `YK-BPA1 result frame`() {
        val r = reading(YonkerKind.BP, 0x81, 0x77, 0x4F, 0x4C, *padding)
        assertEquals(listOf(119, 79, 76), listOf(r.systolic, r.diastolic, r.pulse))
    }

    @Test
    fun `YK-BPA1 hello frame on connect is ignored`() {
        assertNull(YonkerFrames.parse(YonkerKind.BP, frame(0x01, 0x00), now))
    }

    @Test
    fun `BP result frame gives sys dia pulse, read as unsigned bytes`() {
        val r = reading(YonkerKind.BP, 0x81, 200, 130, 72)
        assertEquals(200, r.systolic)
        assertEquals(130, r.diastolic)
        assertEquals(72, r.pulse)
        assertEquals(now, r.time)
    }

    @Test
    fun `BP frames that aren't a plausible reading are ignored`() {
        listOf(
            frame(0x81, 0, 0, 0),        // error report
            frame(0x81, 80, 120, 70),    // dia above sys
            frame(0x81, 97, 78, 0, 0),   // an oximeter frame: no pulse byte
            frame(0x81, 120),            // too short
            frame(0x82, 120, 80, 70),    // unknown type
            frame(),
        ).forEach { assertNull(YonkerFrames.parse(YonkerKind.BP, it, now)) }
    }

    @Test
    fun `oximeter frame gives SpO2 and pulse`() {
        val r = reading(YonkerKind.OXIMETER, 0x81, 97, 78)
        assertEquals(97, r.spo2)
        assertEquals(78, r.pulse)
    }

    @Test
    fun `oximeter without a finger reports no signal`() {
        assertEquals(YonkerEvent.NoSignal, YonkerFrames.parse(YonkerKind.OXIMETER, frame(0x81, 127, 255), now))
    }

    @Test
    fun `vital fields carry only what the device measured`() {
        assertEquals(
            mapOf("vmValueBPSys" to "120", "vmValueBPDias" to "80", "vmValuePulse" to "70"),
            YonkerManager.vitalFields(reading(YonkerKind.BP, 0x81, 120, 80, 70))
        )
        assertEquals(
            mapOf("vmValueSPO2" to "98", "vmValuePulse" to "66"),
            YonkerManager.vitalFields(reading(YonkerKind.OXIMETER, 0x81, 98, 66))
        )
    }

    @Test
    fun `devices are recognised by module name or model number`() {
        listOf("BleModuleB", "YK-BPA1", "yk-bpa1 ").forEach { assertEquals(it, true, YonkerKind.BP.matches(it)) }
        listOf("BleModuleA", "YK-81C").forEach { assertEquals(it, true, YonkerKind.OXIMETER.matches(it)) }
        listOf(null, "BleModuleA", "YK-81C", "BLESmart_0000").forEach { assertEquals("$it", false, YonkerKind.BP.matches(it)) }
        assertEquals(false, YonkerKind.OXIMETER.matches("YK-BPA1"))
    }

    @Test
    fun `an unknown name advertising Yonker's service is accepted unless it's the other kind`() {
        assertEquals(true, YonkerKind.BP.matchesUnknown("YK-NEW9", advertisesService = true))
        assertEquals(true, YonkerKind.BP.matchesUnknown(null, advertisesService = true))
        assertEquals(false, YonkerKind.BP.matchesUnknown("YK-81C", advertisesService = true))
        assertEquals(false, YonkerKind.OXIMETER.matchesUnknown("YK-BPA1", advertisesService = true))
        assertEquals(false, YonkerKind.BP.matchesUnknown("YK-NEW9", advertisesService = false))
    }

    @Test
    fun `connection list offers the Yonker device for the vital`() {
        assertEquals(listOf("Yonker BP Machine"), yonkerDevicesFor("Blood Pressure").map { it.name })
        assertEquals(listOf("Yonker Pulse Oximeter"), yonkerDevicesFor("SpO2").map { it.name })
        assertEquals(listOf("Yonker BP Machine", "Yonker Pulse Oximeter"), yonkerDevicesFor("Pulse").map { it.name })
        assertEquals(emptyList<Any>(), yonkerDevicesFor("Temperature"))
    }
}
