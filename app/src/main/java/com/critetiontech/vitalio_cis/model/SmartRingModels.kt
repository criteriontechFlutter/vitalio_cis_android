package com.critetiontech.ctvitalio.model

import com.google.gson.annotations.SerializedName

// Response of api/UltrahumanVitals/GetUltrahumanVitalsByPid
data class UltrahumanVitalsApiResponse(
    val status: Int? = null,
    val message: String? = null,
    val responseValue: UltrahumanResponseValue? = null
)

data class UltrahumanResponseValue(
    val lastVital: List<UltrahumanLastVitalItem>? = null,
    val sleepmetrics: List<UltrahumanSleepMetricItem>? = null,
    val vitalInsights: List<UltrahumanVitalInsightItem>? = null
)

// ── 1. Last vital ─────────────────────────────────────────────────────────────
// Gson reads numeric JSON into String fields and quoted numbers into Double,
// so vitalPosition / userID (Int or String) and vitalValue (Int or Double) are safe.
data class UltrahumanLastVitalItem(
    val pmId: Int? = null,
    val vitalId: Int? = null,
    val vitalName: String? = null,
    val vitalPosition: String? = null,
    val vitalValue: Double? = null,
    val unit: String? = null,
    val vmValueText: String? = null,
    val vitalDateTime: String? = null,
    val userID: String? = null,
    val vitalColor: String? = null,
    val severityLevel: String? = null
) {
    val key: String get() = "${vitalId ?: 0}_${vitalName.orEmpty()}_${vitalDateTime.orEmpty()}"

    val formattedValue: String
        get() {
            val v = vitalValue ?: return "--"
            return if (v % 1.0 == 0.0) v.toLong().toString() else String.format("%.1f", v)
        }
}

// ── 2. Sleep metrics (vitalValue is an embedded JSON string) ─────────────────
data class UltrahumanSleepMetricItem(
    val pmId: Int? = null,
    val vitalID: Int? = null,
    val vitalName: String? = null,
    val vitalValue: String? = null
)

data class UltrahumanSleepDetail(
    @SerializedName("QuickMetrics") val quickMetrics: List<SleepQuickMetric>? = null,
    @SerializedName("SleepStages") val sleepStages: List<SleepStageItem>? = null,
    @SerializedName("TossTurn") val tossTurn: SleepTossTurn? = null,
    @SerializedName("Spo2") val spo2: SleepSpo2? = null,
    @SerializedName("SleepCycles") val sleepCycles: SleepCyclesData? = null
)

data class SleepQuickMetric(
    @SerializedName("Title") val title: String? = null,
    @SerializedName("DisplayText") val displayText: String? = null,
    @SerializedName("Unit") val unit: String? = null,
    @SerializedName("Type") val type: String? = null
)

data class SleepStageItem(
    @SerializedName("Title") val title: String? = null,
    @SerializedName("Type") val type: String? = null,
    @SerializedName("Percentage") val percentage: Double? = null,
    @SerializedName("StageTimeText") val stageTimeText: String? = null,
    @SerializedName("StageTime") val stageTime: Double? = null
) {
    val colorHex: Long
        get() = when (type?.lowercase()) {
            "deep_sleep" -> 0xFF312E81
            "rem_sleep" -> 0xFF6366F1
            "light_sleep" -> 0xFF818CF8
            "awake" -> 0xFFCBD5E1
            else -> 0xFF2F6FE4
        }

    val meaning: String
        get() = when (type?.lowercase()) {
            "deep_sleep" -> "Physical Recovery & Muscle Repair"
            "rem_sleep" -> "Mental Reset & Memory Processing"
            "light_sleep" -> "Core Sleep & Metabolic Rest"
            "awake" -> "Restlessness & Brief Awakenings"
            else -> "Sleep Phase"
        }

    val detailedDescription: String
        get() = when (type?.lowercase()) {
            "deep_sleep" -> "Deepest restorative stage: rebuilds muscles, tissues, and strengthens immunity with slow brainwaves."
            "rem_sleep" -> "Critical for brain health: consolidates memories, restores mood, and supports cognitive learning."
            "light_sleep" -> "Forms the base foundation of sleep: lowers heart rate, regulates temperature, and relaxes muscles."
            "awake" -> "Normal brief periods of wakefulness and movement during nocturnal sleep transitions."
            else -> "Physiological sleep stage recorded by ring sensors."
        }

    val targetRange: String
        get() = when (type?.lowercase()) {
            "deep_sleep" -> "15–20% ideal"
            "rem_sleep" -> "20–25% ideal"
            "light_sleep" -> "45–55% ideal"
            "awake" -> "< 10% ideal"
            else -> "--"
        }
}

data class SleepTossTurn(
    @SerializedName("Title") val title: String? = null,
    @SerializedName("Subtitle") val subtitle: String? = null,
    @SerializedName("Value") val value: Double? = null
)

data class SleepSpo2(
    @SerializedName("Title") val title: String? = null,
    @SerializedName("Value") val value: Double? = null
)

data class SleepCyclesData(@SerializedName("Title") val title: String? = null)

// ── 3. Vital insights (insightJson is an embedded JSON string) ───────────────
data class UltrahumanVitalInsightItem(
    val pid: Int? = null,
    val insightDate: String? = null,
    val insightJson: String? = null
)

data class VitalInsightDetail(
    val date: String? = null,
    val wellnessScore: Double? = null,
    val colorCode: String? = null,
    val wellnessMessage: String? = null,
    val wellnessStatus: String? = null,
    val scores: InsightScores? = null,
    val insights: InsightCategories? = null
)

data class InsightScores(
    val sleepScore: Double? = null,
    val recoveryScore: Double? = null,
    val movementScore: Double? = null,
    val stressScore: Double? = null
)

data class InsightCategories(
    val sleep: InsightCategoryItem? = null,
    val recovery: InsightCategoryItem? = null,
    val movement: InsightCategoryItem? = null,
    val stress: InsightCategoryItem? = null
)

data class InsightCategoryItem(
    val quality: String? = null,
    val status: String? = null,
    val progress: String? = null,
    val level: String? = null,
    val message: String? = null,
    val colorCode: String? = null
) {
    val displayTag: String? get() = quality ?: status ?: progress ?: level
}

// ── Combined dashboard model ──────────────────────────────────────────────────
data class UltrahumanDashboardData(
    val lastVitals: List<UltrahumanLastVitalItem> = emptyList(),
    val sleepDetail: UltrahumanSleepDetail? = null,
    val insightDetail: VitalInsightDetail? = null
) {
    /** Ring biomarkers only — excludes height, weight, pulse, BP, temperature, SpO2 and heart rate. */
    val clinicalVitals: List<UltrahumanLastVitalItem>
        get() = lastVitals.filter { vital ->
            val name = vital.vitalName?.trim()?.lowercase() ?: return@filter false
            !(name.contains("height") || name.contains("weight") || name.contains("pulse") ||
                name.contains("temperature") || name.contains("spo2") || name.contains("heartrate") ||
                name.contains("heart rate") || name.startsWith("bp_") || name == "bp" ||
                name.contains("blood pressure"))
        }

    private fun quickMetric(type: String) =
        sleepDetail?.quickMetrics?.firstOrNull { it.type?.lowercase() == type }?.displayText

    val totalSleepText: String get() = quickMetric("total_sleep") ?: "--"
    val sleepEfficiencyText: String get() = quickMetric("sleep_efic") ?: "--"
    val timeInBedText: String get() = quickMetric("time_in_bed") ?: "--"
    val nightAvgHrText: String get() = quickMetric("avg_hr") ?: "--"
    val nightAvgHrvText: String get() = quickMetric("avg_hrv") ?: "--"
    val tossAndTurnsText: String get() = sleepDetail?.tossTurn?.value?.toInt()?.toString() ?: "--"
    val sleepStages: List<SleepStageItem> get() = sleepDetail?.sleepStages.orEmpty()

    val wellnessScore: Int? get() = insightDetail?.wellnessScore?.toInt()
    val wellnessStatus: String? get() = insightDetail?.wellnessStatus
    val wellnessMessage: String get() = insightDetail?.wellnessMessage ?: "Sync your ring to see today's wellness insights."
    val wellnessColorHex: String get() = insightDetail?.colorCode ?: "#0DC289"

    val sleepScore: Int? get() = insightDetail?.scores?.sleepScore?.toInt()
    val recoveryScore: Int? get() = insightDetail?.scores?.recoveryScore?.toInt()
    val movementScore: Int? get() = insightDetail?.scores?.movementScore?.toInt()
    val stressScore: Int? get() = insightDetail?.scores?.stressScore?.toInt()
}