package com.critetiontech.ctvitalio

import com.critetiontech.ctvitalio.omron.OmronKind
import com.critetiontech.ctvitalio.omron.OmronManager
import com.critetiontech.ctvitalio.omron.OmronModels
import com.critetiontech.ctvitalio.ui.screens.omronDevicesFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OmronDeviceFilterTest {

    @Test
    fun `BP and pulse vitals offer one BP machine entry that detects the model`() {
        listOf("Blood Pressure", "BP_Sys", "Pulse", "Heart Rate").forEach { vital ->
            val devices = omronDevicesFor(vital)
            assertEquals(vital, listOf("Omron BP Machine"), devices.map { it.name })
            assertEquals(vital, OmronModels.autoCode(OmronKind.BP), devices.single().code)
        }
    }

    @Test
    fun `weight offers one scale entry`() {
        assertEquals(listOf("Omron Weight Scale"), omronDevicesFor("Weight").map { it.name })
        assertEquals(OmronKind.SCALE, OmronModels.autoKind(omronDevicesFor("Weight").single().code!!))
    }

    @Test
    fun `vitals no Omron device measures show none`() {
        listOf("Temperature", "SpO2", "Blood Glucose", "RBS", "Respiratory Rate", "Height").forEach {
            assertEquals(it, emptyList<Any>(), omronDevicesFor(it))
        }
    }

    @Test
    fun `a model number is not an auto code`() {
        assertNull(OmronModels.autoKind("HEM-7156T"))
    }

    // Entries as the library's configuration lists them (recorded from a phone)
    private val candidates = listOf(
        OmronModels.byCode("HEM-7600T")!! to mapOf(
            "modelName" to "HEM-7600T", "identifier" to "HEM-7600T-AP3", "deviceGroupIncludedGroupIDKey" to "31", "id" to "25",
        ),
        OmronModels.byCode("HEM-7156T")!! to mapOf(
            "modelName" to "HEM-7156T-AP", "identifier" to "HEM-7156T-AP", "deviceGroupIncludedGroupIDKey" to "1402", "id" to "1402",
        ),
    )

    @Test
    fun `device is identified by its device group`() {
        assertEquals("HEM-7156T", OmronManager.identify("1402", null, null, candidates)?.first?.code)
        assertEquals("HEM-7600T", OmronManager.identify("31", null, null, candidates)?.first?.code)
    }

    @Test
    fun `device is identified by its model name`() {
        assertEquals("HEM-7156T", OmronManager.identify(null, "HEM-7156T-AP", null, candidates)?.first?.code)
        assertEquals("HEM-7600T", OmronManager.identify("", "HEM-7600T", null, candidates)?.first?.code)
    }

    @Test
    fun `device is identified by the model id in its Bluetooth name`() {
        // A monitor that was taken for a HEM-7600T but reports itself as HEM-7156T (0x57A = 1402)
        assertEquals("HEM-7156T", OmronManager.identify(null, null, "BLEsmart_0000057A005FBFBF7152", candidates)?.first?.code)
    }

    @Test
    fun `an unknown device is not identified`() {
        assertNull(OmronManager.identify("999", "HEM-9999T", "BLEsmart_000003E7", candidates))
        assertNull(OmronManager.identify(null, null, "YK-BPA1", candidates))
    }
}
