package com.critetiontech.ctvitalio

import com.critetiontech.ctvitalio.ui.screens.omronDevicesFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OmronDeviceFilterTest {

    @Test
    fun `BP and pulse vitals show only BP machines`() {
        listOf("Blood Pressure", "BP_Sys", "Pulse", "Heart Rate").forEach { vital ->
            val devices = omronDevicesFor(vital)
            assertEquals(vital, 7, devices.size)
            assertTrue(vital, devices.all { "BP" in it.type })
        }
    }

    @Test
    fun `weight shows only scales`() {
        val devices = omronDevicesFor("Weight")
        assertEquals(listOf("Omron HBF-222T", "Omron HBF-255T", "Omron HBF-702T", "Omron HN-300T2"), devices.map { it.name })
    }

    @Test
    fun `vitals no Omron device measures show none`() {
        listOf("Temperature", "SpO2", "Blood Glucose", "RBS", "Respiratory Rate", "Height").forEach {
            assertEquals(it, emptyList<Any>(), omronDevicesFor(it))
        }
    }
}
