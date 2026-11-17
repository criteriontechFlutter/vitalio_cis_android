package com.critetiontech.ctvitalio.data.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser

/**
 * Turns the AI lab-report answer into the InvestigationByPatient/InsertResult payload, and decides
 * whether each value is inside its normal range. Pure Kotlin so it can be unit tested.
 */
object LabResultMapper {

    private val gson = GsonBuilder().disableHtmlEscaping().create()

    /**
     * true = inside the normal range, false = outside, null = can't tell (no number, no range,
     * or a range with several bands such as "Male: 13-17 Female: 12-15").
     */
    fun isWithinRange(result: String?, range: String?): Boolean? {
        val r = result?.trim().orEmpty()
        val n = range?.trim().orEmpty()
        if (r.isEmpty() || n.isEmpty()) return null

        val value = firstNumber(r)
        val text = n.lowercase()
            .replace('–', '-').replace('—', '-').replace('−', '-')
            .replace(Regex("""(\d),(\d{3})\b"""), "$1$2") // 1,500 -> 1500

        if (value == null) {
            // Word results: "Negative" against "Negative", "Absent" against "Absent".
            val word = r.lowercase()
            return if (text.any(Char::isDigit)) null else word == text
        }

        val bands = Regex("""(\d+(?:\.\d+)?)\s*(?:-|to)\s*(\d+(?:\.\d+)?)""").findAll(text).toList()
        if (bands.size == 1) {
            val (lo, hi) = bands[0].destructured
            return value in lo.toDouble()..hi.toDouble()
        }
        if (bands.size > 1) return null

        val bound = firstNumber(text) ?: return null
        return when {
            Regex("""^(<=|≤|up\s*to|upto|max)""").containsMatchIn(text) -> value <= bound
            Regex("""^(<|less than|below)""").containsMatchIn(text) -> value < bound
            Regex("""^(>=|≥|min)""").containsMatchIn(text) -> value >= bound
            Regex("""^(>|more than|greater than|above)""").containsMatchIn(text) -> value > bound
            else -> null
        }
    }

    /** "4.8 H" -> 4.8, "1,200" -> 1200.0, "<0.5" -> 0.5, "Negative" -> null */
    private fun firstNumber(s: String): Double? =
        Regex("""\d+(?:\.\d+)?""").find(s.replace(Regex("""(\d),(\d{3})\b"""), "$1$2"))?.value?.toDoubleOrNull()

    /**
     * Payload for InsertResult, or null when the AI found no results (nothing worth saving).
     *
     * @param testName what the user typed as the test (e.g. "KFT"); the iOS app sends this as itemName
     * @param dateTime the report date the user picked, "yyyy-MM-dd HH:mm"
     */
    fun buildInsertPayload(
        aiResponseJson: String,
        uhid: String,
        clientId: Int,
        testName: String,
        dateTime: String,
    ): Map<String, Any>? {
        val first = runCatching {
            JsonParser.parseString(aiResponseJson).asJsonObject.getAsJsonArray("response")
                ?.firstOrNull()?.takeIf { it.isJsonObject }?.asJsonObject
        }.getOrNull() ?: return null
        fun com.google.gson.JsonObject.str(k: String) =
            get(k)?.takeIf { it.isJsonPrimitive }?.asString?.trim().orEmpty()

        val details = first.getAsJsonObject("patient_details")
        val labName = details?.str("lab_name").orEmpty()
        val when_ = dateTime.ifBlank { details?.str("collection_date").orEmpty() }

        val results = first.getAsJsonArray("report")?.mapNotNull { el ->
            val o = el.takeIf { it.isJsonObject }?.asJsonObject ?: return@mapNotNull null
            val name = o.str("test_name")
            val result = o.str("result")
            if (name.isEmpty() || result.isEmpty()) return@mapNotNull null
            val range = o.str("normal_values")
            linkedMapOf(
                "result" to result,
                "resultDateTime" to when_,
                "subTestId" to o.str("id"),
                "unit" to o.str("unit"),
                "range" to range,
                // Backend takes "1"/"0"; a value we can't judge is sent as normal, as before.
                "isNormal" to if (isWithinRange(result, range) == false) "0" else "1",
                "subTestName" to name,
            )
        }.orEmpty()
        if (results.isEmpty()) return null

        val itemName = testName.trim().takeIf { it.isNotEmpty() && !it.equals("General", true) }
            ?: labName.takeIf { it.isNotEmpty() }?.let { "$it Report" }
            ?: "Lab Report"

        val investigationDetails = listOf(linkedMapOf(
            "receiptNo" to "",
            "itemName" to itemName,
            "labName" to labName,
            "itemId" to "",
            "resultDateTime" to when_,
        ))

        return linkedMapOf(
            "userId" to 0,
            "uhid" to uhid,
            "investigationDetailsJson" to gson.toJson(investigationDetails),
            "investigationResultJson" to gson.toJson(results),
            "clientId" to clientId,
        )
    }
}
