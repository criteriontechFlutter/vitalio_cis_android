package com.critetiontech.ctvitalio.data.repository

import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LabResultMapperTest {

    // ── Range check ──

    @Test fun `simple ranges`() {
        assertEquals(true, LabResultMapper.isWithinRange("1.1", "0.1 - 1.2"))
        assertEquals(false, LabResultMapper.isWithinRange("4.8", "0.0 - 0.4"))
        assertEquals(true, LabResultMapper.isWithinRange("0.4", "0.0 - 0.4")) // bounds inclusive
        assertEquals(false, LabResultMapper.isWithinRange("5", "7 - 55"))
    }

    @Test fun `units, dashes, labels and flags around numbers`() {
        assertEquals(true, LabResultMapper.isWithinRange("14.2", "13.0 - 17.0 g/dL"))
        assertEquals(true, LabResultMapper.isWithinRange("14.2", "13.0–17.0"))
        assertEquals(true, LabResultMapper.isWithinRange("14.2", "13 to 17"))
        assertEquals(false, LabResultMapper.isWithinRange("4.8 H", "0.0 - 0.4"))
        assertEquals(true, LabResultMapper.isWithinRange("7,500", "4,000 - 11,000"))
        assertEquals(true, LabResultMapper.isWithinRange("1.0", "Adult: 0.1 - 1.2"))
    }

    @Test fun `one-sided limits`() {
        assertEquals(true, LabResultMapper.isWithinRange("180", "< 200"))
        assertEquals(false, LabResultMapper.isWithinRange("200", "< 200"))
        assertEquals(true, LabResultMapper.isWithinRange("40", "Up to 40"))
        assertEquals(true, LabResultMapper.isWithinRange("60", "> 40"))
        assertEquals(false, LabResultMapper.isWithinRange("30", ">= 40"))
    }

    @Test fun `word results`() {
        assertEquals(true, LabResultMapper.isWithinRange("Negative", "Negative"))
        assertEquals(false, LabResultMapper.isWithinRange("Positive", "Negative"))
    }

    @Test fun `can't tell`() {
        assertNull(LabResultMapper.isWithinRange("14", "Male: 13-17 Female: 12-15"))
        assertNull(LabResultMapper.isWithinRange("", "1 - 2"))
        assertNull(LabResultMapper.isWithinRange("5", ""))
        assertNull(LabResultMapper.isWithinRange("Trace", "0 - 5"))
    }

    // ── Payload ──

    /** The 12 values from the payload reported on 17 Nov 2026, as the AI returns them. */
    private val ai = """{"response":[{"patient_details":{"lab_name":"Flabs","collection_date":"29/10/2026"},
      "report":[
        {"test_name":"BILIRUBIN TOTAL","result":"1.1","unit":"mg/dL","normal_values":"0.1 - 1.2"},
        {"test_name":"BILIRUBIN DIRECT","result":"4.8","unit":"mg/dL","normal_values":"0.0 - 0.4"},
        {"test_name":"BILIRUBIN INDIRECT","result":"2.90","unit":"mg/dL","normal_values":"0.1 - 1.0"},
        {"test_name":"SGPT","result":"5","unit":"IU/L","normal_values":"7 - 55"},
        {"test_name":"SGOT","result":"45","unit":"IU/L","normal_values":"8 - 48"},
        {"test_name":"SGOT/SGPT RATIO","result":"5.63","unit":"RATIO","normal_values":"0 - 46"},
        {"test_name":"ALKALINE PHOSPHATASE","result":"42","unit":"U/L","normal_values":"35 - 140","id":"1638"},
        {"test_name":"GAMMA GLUTAMYL TRANSFERASE (GGT)","result":"7","unit":"U/L","normal_values":"0 - 55"},
        {"test_name":"TOTAL PROTEINS","result":"8.5","unit":"gm/dL","normal_values":"6.2 - 8.0"},
        {"test_name":"ALBUMIN","result":"5","unit":"gm/dL","normal_values":"3.5 - 5.5"},
        {"test_name":"GLOBULIN","result":"7.50","unit":"gm/dL","normal_values":"2.3 - 3.5"},
        {"test_name":"A : G RATIO","result":"9.9","unit":"RATIO","normal_values":"1.0 - 1.2"},
        {"test_name":"","result":"1"}
      ]}]}"""

    private fun payload(testName: String = "LFT", dateTime: String = "2026-10-29 10:41") =
        LabResultMapper.buildInsertPayload(ai, "UHID-TEST", 45, testName, dateTime)!!

    @Test fun `isNormal follows the ranges (the reported payload had 4 of these wrong)`() {
        val results = JsonParser.parseString(payload()["investigationResultJson"] as String).asJsonArray
        val flags = results.associate { it.asJsonObject["subTestName"].asString to it.asJsonObject["isNormal"].asString }
        assertEquals("1", flags["BILIRUBIN TOTAL"])
        assertEquals("0", flags["BILIRUBIN DIRECT"])   // sent as "1"
        assertEquals("0", flags["BILIRUBIN INDIRECT"]) // sent as "1"
        assertEquals("0", flags["SGPT"])               // sent as "1"
        assertEquals("1", flags["SGOT"])
        assertEquals("1", flags["SGOT/SGPT RATIO"])
        assertEquals("1", flags["ALKALINE PHOSPHATASE"])
        assertEquals("1", flags["GAMMA GLUTAMYL TRANSFERASE (GGT)"])
        assertEquals("0", flags["TOTAL PROTEINS"])
        assertEquals("1", flags["ALBUMIN"])
        assertEquals("0", flags["GLOBULIN"])            // sent as "1"
        assertEquals("0", flags["A : G RATIO"])
        assertEquals(12, results.size()) // the row with no test name is dropped
    }

    @Test fun `uses the patient, the typed test name and the picked date`() {
        val p = payload()
        assertEquals("UHID-TEST", p["uhid"])
        assertEquals(45, p["clientId"])
        assertEquals(0, p["userId"])
        val details = JsonParser.parseString(p["investigationDetailsJson"] as String).asJsonArray[0].asJsonObject
        assertEquals("LFT", details["itemName"].asString)
        assertEquals("Flabs", details["labName"].asString)
        assertEquals("2026-10-29 10:41", details["resultDateTime"].asString)
        val first = JsonParser.parseString(p["investigationResultJson"] as String).asJsonArray[0].asJsonObject
        assertEquals("2026-10-29 10:41", first["resultDateTime"].asString)
        assertEquals("mg/dL", first["unit"].asString)
        assertEquals("1638", JsonParser.parseString(p["investigationResultJson"] as String).asJsonArray[6].asJsonObject["subTestId"].asString)
    }

    @Test fun `falls back to the lab name when no test name was typed`() {
        val details = JsonParser.parseString(payload(testName = "")["investigationDetailsJson"] as String).asJsonArray[0].asJsonObject
        assertEquals("Flabs Report", details["itemName"].asString)
    }

    @Test fun `nothing readable means nothing to save`() {
        assertNull(LabResultMapper.buildInsertPayload("""{"response":[{"report":[]}]}""", "U", 1, "", ""))
        assertNull(LabResultMapper.buildInsertPayload("""{"response":["Invalid Image Type"]}""", "U", 1, "", ""))
        assertNull(LabResultMapper.buildInsertPayload("not json", "U", 1, "", ""))
    }

    @Test fun `json is not html-escaped`() {
        val raw = LabResultMapper.buildInsertPayload(
            """{"response":[{"report":[{"test_name":"LDL","result":"90","normal_values":"< 100"}]}]}""", "U", 1, "Lipid", "2026-10-29 10:41"
        )!!["investigationResultJson"] as String
        assertTrue(raw.contains("< 100"))
        assertFalse(raw.contains("\\u003c"))
    }
}
