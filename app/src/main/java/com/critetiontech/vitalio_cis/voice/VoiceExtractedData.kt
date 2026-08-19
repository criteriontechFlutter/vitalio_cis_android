package com.critetiontech.vitalio_cis.voice

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceExtractedData
//  Android equivalent of the iOS VoiceExtractedData struct.
//  Holds vitals (key→value map) and symptoms extracted from spoken text.
// ═══════════════════════════════════════════════════════════════════════════════

data class VoiceSymptomItem(
    val detailID: String,
    val detailsDate: String,
    val details: String,
    val pmId: Int = 2
)

data class VoiceExtractedData(
    val vitals: Map<String, String>? = null,
    val symptoms: List<VoiceSymptomItem>? = null
) {
    val isEmpty: Boolean
        get() = (vitals?.isEmpty() != false) && (symptoms?.isEmpty() != false)

    /** Merges newly extracted data into a copy of this object. */
    fun merge(with: VoiceExtractedData): VoiceExtractedData {
        // Merge vitals – new values overwrite existing keys
        val mergedVitals: Map<String, String>? = when {
            vitals == null && with.vitals == null -> null
            vitals == null -> with.vitals
            with.vitals == null -> vitals
            else -> vitals + with.vitals          // Map + Map merges, right-side wins on dup keys
        }

        // Merge symptoms – deduplicate by name (case-insensitive)
        val mergedSymptoms: List<VoiceSymptomItem>? = when {
            symptoms == null && with.symptoms == null -> null
            symptoms == null -> with.symptoms
            with.symptoms == null -> symptoms
            else -> {
                val existingNames = symptoms.map { it.details.lowercase().trim() }.toSet()
                val newOnes = with.symptoms.filter { it.details.lowercase().trim() !in existingNames }
                symptoms + newOnes
            }
        }

        return copy(vitals = mergedVitals, symptoms = mergedSymptoms?.takeIf { it.isNotEmpty() })
    }
}
