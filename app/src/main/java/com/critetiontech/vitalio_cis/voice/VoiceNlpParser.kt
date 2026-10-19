package com.critetiontech.ctvitalio.voice

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceNlpParser
//  Rule-based extraction that runs on-device in milliseconds. Used two ways:
//    • on its own, to show the confirmation card instantly while Echo AI is still thinking
//    • on top of the Echo AI result, to fill whatever the AI missed
//  Pure Kotlin (no Android APIs) so every rule is covered by VoiceNlpParserTest.
//
//  Every keyword is matched as a whole word/phrase: "ulti" must not fire on "multivitamin",
//  "tension" not on "hypertension", "ras" not on "rash", "tea" not on "instead".
// ═══════════════════════════════════════════════════════════════════════════════

object VoiceNlpParser {

    // ── Plausible ranges — anything outside is a mis-hearing, never saved ──────

    private val VITAL_RANGES: Map<String, List<ClosedFloatingPointRange<Double>>> = mapOf(
        "vmValueBPSys"           to listOf(50.0..300.0),
        "vmValueBPDias"          to listOf(20.0..200.0),
        "vmValuePulse"           to listOf(20.0..250.0),
        "vmValueHeartRate"       to listOf(20.0..250.0),
        "vmValueSPO2"            to listOf(50.0..100.0),
        "vmValueTemperature"     to listOf(30.0..45.0, 86.0..113.0),  // °C or °F
        "vmValueRespiratoryRate" to listOf(4.0..60.0),
        "vmValueRbs"             to listOf(1.0..700.0),                // mmol/L or mg/dL
        "weight"                 to listOf(1.0..700.0),                // kg or lbs
        "vmValueWeight"          to listOf(1.0..700.0)
    )

    private const val DEFAULT_GLASS_ML = 250

    // ── Word-boundary helpers ─────────────────────────────────────────────────

    private const val WB_START = "(?<![\\p{L}\\p{N}])"
    private const val WB_END   = "(?![\\p{L}\\p{N}])"

    private val phraseCache = HashMap<String, Regex>()

    private fun phraseRegex(phrase: String): Regex = synchronized(phraseCache) {
        phraseCache.getOrPut(phrase) { Regex(WB_START + Regex.escape(phrase) + WB_END) }
    }

    /** True when [phrase] appears in [text] as a whole word / whole phrase. */
    fun containsPhrase(text: String, phrase: String): Boolean =
        phrase.isNotBlank() && phraseRegex(phrase).containsMatchIn(text)

    private fun normalize(text: String) = text.lowercase().replace(Regex("\\s+"), " ").trim()

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fills gaps in [existing] (usually the Echo AI result) from [text], then drops any
     * implausible vital. [pendingMedicines] are today's not-yet-taken doses for name matching.
     */
    fun extract(
        text: String,
        existing: VoiceExtractedData = VoiceExtractedData(),
        pendingMedicines: List<ScheduledMedicineRef> = emptyList()
    ): VoiceExtractedData {
        val lower = normalize(text)
        var result = sanitize(existing).first

        result = fillVitals(lower, result)
        result = fillFluidIntakes(lower, result)
        result = fillFluidOutput(lower, result)
        result = fillMedicines(lower, result, pendingMedicines)
        result = fillSymptoms(lower, result)

        return sanitize(result).first
    }

    /** Removes vitals that are non-numeric or outside a plausible range. Returns the dropped labels. */
    fun sanitize(data: VoiceExtractedData): Pair<VoiceExtractedData, List<String>> {
        val vitals = data.vitals ?: return data to emptyList()
        val rejected = mutableListOf<String>()
        val clean = vitals.filter { (key, value) ->
            val ranges = VITAL_RANGES[key] ?: return@filter true
            val num = value.trim().toDoubleOrNull()
            val ok = num != null && ranges.any { num in it }
            if (!ok) rejected += "$key=$value"
            ok
        }.toMutableMap()

        val sys  = clean["vmValueBPSys"]?.toDoubleOrNull()
        val dias = clean["vmValueBPDias"]?.toDoubleOrNull()
        if (sys != null && dias != null && sys <= dias) {
            rejected += "BP $sys/$dias"
            clean.remove("vmValueBPSys")
            clean.remove("vmValueBPDias")
        }
        return data.copy(vitals = clean.takeIf { it.isNotEmpty() }) to rejected
    }

    // ── Vitals ────────────────────────────────────────────────────────────────

    // "bp 120 80", "blood pressure is 130 by 85", "bp mera 140/90"
    private val BP_WITH_KEYWORD = Regex(
        """(?:\bbp\b|\bblood\s*pressure\b)\D{0,20}?(\d{2,3})\s*(?:/|\bby\b|\bover\b|\band\b|,|\s)\s*(\d{2,3})(?![\d.])"""
    )
    // A bare pair needs an explicit separator, so "pulse 98 spo2 97" never becomes BP 98/97
    private val BP_WITH_SEPARATOR = Regex(
        """(?<![\d.])(\d{2,3})\s*(?:/|\bby\b|\bover\b)\s*(\d{2,3})(?![\d.])"""
    )

    private val RBS_PATTERNS = listOf(
        Regex("""(?:\brbs\b|\brandom\s*blood\s*sugar\b|\bblood\s*sugar\b|\bsugar\b|\bglucose\b)\s*(?:level|value|reading|is|was|=|:|\s)*(\d{2,3})(?![\d.])"""),
        Regex("""(?<![\d.])(\d{2,3})\s*mg\s*/?\s*dl\b""")
    )

    // "hr" only counts when it isn't a duration ("3 hr 30 min")
    private val PULSE_PATTERNS = listOf(
        Regex("""(?:\bpulse(?:\s*rate)?\b|\bheart(?:\s*(?:rate|beat))?\b|(?<!\d\s)(?<!\d)\bhr\b|\bnabz\b|\bnaad\b|\bdil\s*ki\s*dhadkan\b)\s*(?:is|was|=|:)?\s*(\d{2,3})(?![\d.])"""),
        Regex("""(?<![\d.])(\d{2,3})\s*(?:bpm|beats\s*per\s*minute|beats)\b""")
    )

    private val SPO2_PATTERNS = listOf(
        Regex("""(?:\bspo2\b|\bsp\s*o2\b|\boxygen\s*saturation\b|\boxygen\b|\bo2\b|\bsaturation\b|\bspo\b)\s*(?:level|reading|is|was|=|:)?\s*(\d{2,3})(?![\d.])"""),
        Regex("""(?<![\d.])(\d{2,3})\s*%?\s*(?:\bspo2\b|\boxygen\b|\bsaturation\b)""")
    )

    private val TEMP_PATTERNS = listOf(
        Regex("""(?:\btemperature\b|\btemp\b|\bbukhar\b|\bbadan\s*ka\s*taap\b)\s*(?:is|was|=|:)?\s*(\d{2,3}(?:\.\d{1,2})?)(?![\d])"""),
        Regex("""(?<![\d.])(\d{2,3}(?:\.\d{1,2})?)\s*(?:°\s*[fc]\b|\bdegrees?\b|\bcelsius\b|\bfahrenheit\b)""")
    )

    private fun fillVitals(lower: String, data: VoiceExtractedData): VoiceExtractedData {
        val vitals = data.vitals?.toMutableMap() ?: mutableMapOf()

        if ("vmValueBPSys" !in vitals || "vmValueBPDias" !in vitals) {
            val m = BP_WITH_KEYWORD.find(lower) ?: BP_WITH_SEPARATOR.find(lower)
            val sys  = m?.groupValues?.get(1)?.toIntOrNull()
            val dias = m?.groupValues?.get(2)?.toIntOrNull()
            if (sys != null && dias != null && sys > dias && sys in 60..300 && dias in 30..200) {
                vitals.putIfAbsent("vmValueBPSys", sys.toString())
                vitals.putIfAbsent("vmValueBPDias", dias.toString())
            }
        }

        fun fill(key: String, patterns: List<Regex>, valid: (Double) -> Boolean) {
            if (key in vitals) return
            for (p in patterns) {
                val value = p.find(lower)?.groupValues?.get(1)?.trim() ?: continue
                val num = value.toDoubleOrNull() ?: continue
                if (valid(num)) { vitals[key] = value; return }
            }
        }
        fill("vmValueRbs", RBS_PATTERNS) { it in 30.0..600.0 }
        fill("vmValuePulse", PULSE_PATTERNS) { it in 30.0..250.0 }
        fill("vmValueSPO2", SPO2_PATTERNS) { it in 70.0..100.0 }
        fill("vmValueTemperature", TEMP_PATTERNS) { it in 90.0..110.0 || it in 32.0..44.0 }

        return data.copy(vitals = vitals.takeIf { it.isNotEmpty() })
    }

    // ── Quantities ────────────────────────────────────────────────────────────

    private const val QTY  = """(\d+(?:\.\d+)?)"""
    private const val UNIT = """(millilit(?:er|re)s?|ml|lit(?:er|re)s?|ltr|l|glass(?:es)?|gilas|gilaas|cups?)"""

    /**
     * Converts a spoken amount to ml, using only the unit spoken next to *this* number
     * ("1 liter water and 200 ml milk" → 1000 and 200, not 1000 and 200000).
     */
    fun toMl(amount: Double, unit: String?): Int? {
        val u = unit?.lowercase()
        val ml = when {
            u == null || u.isEmpty()                         -> if (amount <= 10) amount * DEFAULT_GLASS_ML else amount
            u.startsWith("milli") || u == "ml"               -> amount
            u.startsWith("lit") || u == "ltr" || u == "l"    -> amount * 1000
            u.startsWith("glass") || u.startsWith("gila")    -> amount * DEFAULT_GLASS_ML
            u.startsWith("cup")                              -> amount * 150
            else                                             -> amount
        }.toInt()
        return ml.takeIf { it in 10..5000 }
    }

    /** Quantity spoken right before or right after [keyword], in ml. */
    private fun quantityNear(lower: String, keyword: String): Int? {
        val kw = WB_START + Regex.escape(keyword) + WB_END
        val before = Regex("""(?<![\d.])$QTY\s*$UNIT?\s*(?:of\s+)?$kw""")
        val after  = Regex("""$kw\s*(?:of\s+)?$QTY\s*$UNIT?(?![\p{L}])""")
        before.find(lower)?.let { m ->
            return toMl(m.groupValues[1].toDouble(), m.groupValues[2].ifEmpty { null })
        }
        after.find(lower)?.let { m ->
            return toMl(m.groupValues[1].toDouble(), m.groupValues[2].ifEmpty { null })
        }
        return null
    }

    // ── Fluid intake ──────────────────────────────────────────────────────────

    private data class FluidSpec(val foodId: Int, val name: String, val keywords: List<String>)

    private val FLUID_SPECS = listOf(
        FluidSpec(97694,  "Water",  listOf("water", "pani", "paani")),
        FluidSpec(66,     "Juice",  listOf("juice", "ras", "sharbat")),
        FluidSpec(76,     "Milk",   listOf("milk", "doodh", "dudh")),
        FluidSpec(114973, "Tea",    listOf("tea", "chai")),
        FluidSpec(168,    "Coffee", listOf("coffee", "kafi", "cappuccino", "latte", "espresso"))
    )

    private fun fillFluidIntakes(lower: String, data: VoiceExtractedData): VoiceExtractedData {
        val known = data.fluidIntakes?.map { it.foodId }?.toSet() ?: emptySet()
        val intakes = data.fluidIntakes?.toMutableList() ?: mutableListOf()

        for (spec in FLUID_SPECS) {
            if (spec.foodId in known) continue
            val kw = spec.keywords.firstOrNull { containsPhrase(lower, it) } ?: continue
            val qty = quantityNear(lower, kw) ?: DEFAULT_GLASS_ML
            intakes += VoiceFluidIntakeItem(foodId = spec.foodId, name = spec.name, quantity = qty)
        }
        return data.copy(fluidIntakes = intakes.takeIf { it.isNotEmpty() })
    }

    // ── Fluid output ──────────────────────────────────────────────────────────

    // Not "pee": in Hinglish "pani pee liya" means *drank*
    private val URINE_KEYWORDS = listOf(
        "urine output", "urine", "urination", "output", "su su", "susu", "peshab", "pishab", "mutra", "peed"
    )

    private fun fillFluidOutput(lower: String, data: VoiceExtractedData): VoiceExtractedData {
        if (data.fluidOutput != null) return data
        val kw = URINE_KEYWORDS.firstOrNull { containsPhrase(lower, it) } ?: return data
        val qty = quantityNear(lower, kw) ?: DEFAULT_GLASS_ML
        return data.copy(fluidOutput = buildUrineOutput(qty, lower))
    }

    fun buildUrineOutput(qty: Int, text: String): VoiceFluidOutputItem {
        val lower = normalize(text)
        val (hex, name) = when {
            containsPhrase(lower, "dark yellow")  -> "#FFA500" to "Dark Yellow"
            containsPhrase(lower, "light yellow") -> "#FFF9CC" to "Light Yellow"
            containsPhrase(lower, "yellow")       -> "#FFFF00" to "Yellow"
            containsPhrase(lower, "amber")        -> "#FFB347" to "Amber"
            containsPhrase(lower, "brown")        -> "#A52A2A" to "Brown"
            containsPhrase(lower, "red")          -> "#FF0000" to "Red"
            else                                  -> "#FFF9CC" to "Light Yellow"
        }
        return VoiceFluidOutputItem(quantity = qty, colourHex = hex, colourName = name)
    }

    // ── Medicines ─────────────────────────────────────────────────────────────

    private fun fillMedicines(
        lower: String,
        data: VoiceExtractedData,
        pending: List<ScheduledMedicineRef>
    ): VoiceExtractedData {
        if (pending.isEmpty()) return data
        val meds = data.medicines?.toMutableList() ?: mutableListOf()
        val existing = meds.map { it.medicineId }.toMutableSet()

        for (med in pending) {
            if (med.isTaken || med.id in existing) continue
            if (!containsPhrase(lower, normalize(med.name))) continue
            // Schedule id is what markIntake expects
            meds += VoiceMedicineItem(
                medicineId        = med.id,
                medicineName      = med.name,
                scheduledDateTime = med.scheduledDateTime,
                instructions      = med.dosageStrength,
                frequency         = med.unit
            )
            existing += med.id
        }
        return data.copy(medicines = meds.takeIf { it.isNotEmpty() })
    }

    // ── Symptoms (only when the AI found none) ────────────────────────────────

    private data class SymptomSpec(
        val keywords: List<String>,
        val displayName: String,
        val detailId: Int,
        /** Phrases that contain a keyword but don't mean the symptom ("cold water") */
        val notWhen: Regex? = null
    )

    private val SYMPTOM_SPECS = listOf(
        SymptomSpec(listOf("fever", "feverish", "febrile", "bukhar", "bukhaar", "tez bukhar", "temperature high"), "Fever", 14),
        SymptomSpec(listOf("headache", "sir dard", "sar dard", "sir mein dard", "migraine", "head pain"), "Headache", 15),
        SymptomSpec(listOf("cough", "khansi", "khaansi", "dry cough", "wet cough"), "Cough", 12),
        SymptomSpec(listOf("cold", "sardi", "runny nose", "nazla", "nasal congestion"), "Cold", 17,
            notWhen = Regex("""\bcold\s+(?:water|drink|drinks|coffee|tea|milk|juice|pani|coke|weather)\b""")),
        SymptomSpec(listOf("vomiting", "ulti", "vomit", "nausea", "nauseous", "nauseated", "ji machalna"), "Vomiting", 18),
        SymptomSpec(listOf("diarrhea", "diarrhoea", "loose motion", "loose motions", "dast", "latrin"), "Diarrhea", 19),
        SymptomSpec(listOf("chest pain", "seene mein dard", "sine mein dard", "heart pain", "angina"), "Chest Pain", 20),
        SymptomSpec(listOf("shortness of breath", "saans lene mein takleef", "breathlessness", "saans phoolna", "dyspnea"), "Shortness of Breath", 21),
        SymptomSpec(listOf("fatigue", "weakness", "kamzori", "thakan", "tired", "thaka hua"), "Fatigue", 22),
        SymptomSpec(listOf("back pain", "kamar dard", "kamar mein dard", "peeth mein dard", "pith dard"), "Back Pain", 23),
        SymptomSpec(listOf("joint pain", "jodo mein dard", "ghutne mein dard", "hath paon mein dard"), "Joint Pain", 24),
        SymptomSpec(listOf("stomach pain", "pet dard", "pet mein dard", "stomach ache", "abdominal pain", "tummy ache"), "Stomach Pain", 25),
        SymptomSpec(listOf("dizziness", "dizzy", "chakkar", "chakkar aana", "vertigo", "giddiness", "head is spinning",
            "head spinning", "lightheaded", "light headed", "sar ghoom", "sir ghoom"), "Dizziness", 26),
        SymptomSpec(listOf("swelling", "sujan", "edema", "puffiness"), "Swelling", 27),
        SymptomSpec(listOf("skin rash", "rash", "khujli", "itching", "urticaria", "hives"), "Skin Rash", 28),
        SymptomSpec(listOf("insomnia", "sleep problem", "neend nahi", "nind nahi", "sleeping issue"), "Insomnia", 29),
        SymptomSpec(listOf("anxiety", "tension", "stress", "chinta", "ghabrahat"), "Anxiety", 30),
        SymptomSpec(listOf("body pain", "badan mein dard", "badan dard", "whole body pain", "sar se pair tak dard"), "Body Pain", 31)
    )

    private fun fillSymptoms(lower: String, data: VoiceExtractedData): VoiceExtractedData {
        if (!data.symptoms.isNullOrEmpty()) return data
        val found = SYMPTOM_SPECS.filter { spec ->
            // Blank out the look-alike phrases first, so "cold water" doesn't count but "I have a cold" still does
            val scoped = spec.notWhen?.replace(lower, " ") ?: lower
            spec.keywords.any { containsPhrase(scoped, it) }
        }.map { spec ->
            VoiceSymptomItem(detailID = spec.detailId.toString(), detailsDate = "", details = spec.displayName, pmId = 2)
        }
        return if (found.isEmpty()) data else data.copy(symptoms = found)
    }
}
