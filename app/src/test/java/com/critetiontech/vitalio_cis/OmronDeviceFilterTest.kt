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

    // Scales: classified id 0001 in front of the internal id
    private val scales = listOf(
        OmronModels.byCode("HN-300T2")!! to mapOf(
            "modelName" to "HN-300T2", "identifier" to "HN-300T2_AP", "deviceGroupIncludedGroupIDKey" to "1042", "id" to "1042",
        ),
        // Older entry: "id" is 068, not the internal id; group id may be missing
        OmronModels.byCode("HBF-222T")!! to mapOf(
            "modelName" to "HBF-222T", "identifier" to "HBF-222T-AP", "id" to "068",
        ),
    )

    @Test
    fun `a scale is identified from the internal id in its Bluetooth name`() {
        // 0x0412 = 1042; reading all 8 digits (0x00010412) gave 66578 and matched nothing
        assertEquals("HN-300T2", OmronManager.identify(null, null, "BLEsmart_00010412005FBFCD0001", scales)?.first?.code)
    }

    @Test
    fun `HBF-222T seen on a Pixel is identified by the library's name prefix`() {
        // The exact advertisement that failed with "isn't a model we support yet"
        assertEquals("HBF-222T", OmronManager.identify(null, null, "BLEsmart_00010408005FBFCD9709", scales)?.first?.code)
    }

    @Test
    fun `the matching variant config is chosen when a model has several`() {
        val aap = mapOf("modelName" to "HEM-7156T", "identifier" to "HEM-7156T_AAP", "deviceGroupIncludedGroupIDKey" to "1146", "id" to "25003")
        val both = candidates + (OmronModels.byCode("HEM-7156T")!! to aap)
        assertEquals(aap, OmronManager.identify(null, null, "BLEsmart_0000047A005FBF000001", both)?.second)
        assertEquals("1402", OmronManager.identify(null, null, "BLEsmart_0000057A005FBF000001", both)?.second?.get("id"))
    }

    @Test
    fun `a BP name is not taken for a scale with the same internal id`() {
        // Pairing a scale only offers scale candidates, so a nearby BP monitor stays unmatched
        assertNull(OmronManager.identify(null, null, "BLEsmart_0000057A005FBF000001", scales))
    }

    @Test
    fun `an unknown device is not identified`() {
        assertNull(OmronManager.identify("999", "HEM-9999T", "BLEsmart_000003E7", candidates))
        assertNull(OmronManager.identify(null, null, "YK-BPA1", candidates))
    }
}
