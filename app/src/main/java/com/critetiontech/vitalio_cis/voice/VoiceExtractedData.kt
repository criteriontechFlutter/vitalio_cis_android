package com.critetiontech.ctvitalio.voice

import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════════
//  VoiceExtractedData
//  Android equivalent of the iOS VoiceExtractedData struct.
//  Holds vitals, symptoms, fluid intakes, fluid output, and medicines
//  extracted from spoken text.
// ═══════════════════════════════════════════════════════════════════════════════

data class VoiceSymptomItem(
    val detailID: String,
    val detailsDate: String,
    val details: String,
    val pmId: Int = 2
)

data class VoiceFluidIntakeItem(
    val id: String = UUID.randomUUID().toString(),
    val foodId: Int,
    val name: String,
    val quantity: Int // in ml
)

data class VoiceFluidOutputItem(
    val quantity: Int, // in ml
    val colourHex: String,
    val colourName: String
)

data class VoiceMedicineItem(
    val medicineId: Int,
    val medicineName: String,
    val scheduledDateTime: String,
    val instructions: String,
    val frequency: String
) {
    val id: String get() = "${medicineId}_${scheduledDateTime}"
}

data class VoiceExtractedData(
    val vitals: Map<String, String>? = null,
    val symptoms: List<VoiceSymptomItem>? = null,
    val fluidIntakes: List<VoiceFluidIntakeItem>? = null,
    val fluidOutput: VoiceFluidOutputItem? = null,
    val medicines: List<VoiceMedicineItem>? = null
) {
    val isEmpty: Boolean
        get() = (vitals?.isEmpty() != false)
                && (symptoms?.isEmpty() != false)
                && (fluidIntakes?.isEmpty() != false)
                && fluidOutput == null
                && (medicines?.isEmpty() != false)

    /** Merges newly extracted data into a copy of this object (mirrors iOS merge logic). */
    fun merge(with: VoiceExtractedData): VoiceExtractedData {
        // 1. Merge vitals – new values overwrite existing keys
        val mergedVitals: Map<String, String>? = when {
            vitals == null && with.vitals == null -> null
            vitals == null -> with.vitals
            with.vitals == null -> vitals
            else -> vitals + with.vitals
        }

        // 2. Merge symptoms – deduplicate by name (case-insensitive)
        val mergedSymptoms: List<VoiceSymptomItem>? = when {
            symptoms == null && with.symptoms == null -> null
            symptoms == null -> with.symptoms
            with.symptoms == null -> symptoms
            else -> {
                val existingNames = symptoms.map { it.details.lowercase().trim() }.toSet()
                val newOnes = with.symptoms.filter { it.details.lowercase().trim() !in existingNames }
                (symptoms + newOnes).takeIf { it.isNotEmpty() }
            }
        }

        // 3. Merge fluid intakes – update existing foodId or append new
        val mergedIntakes: List<VoiceFluidIntakeItem>? = when {
            fluidIntakes == null && with.fluidIntakes == null -> null
            fluidIntakes == null -> with.fluidIntakes
            with.fluidIntakes == null -> fluidIntakes
            else -> {
                val current = fluidIntakes.toMutableList()
                for (intake in with.fluidIntakes) {
                    val idx = current.indexOfFirst { it.foodId == intake.foodId }
                    if (idx >= 0) current[idx] = intake else current.add(intake)
                }
                current.takeIf { it.isNotEmpty() }
            }
        }

        // 4. Fluid output – overwrite if new output provided
        val mergedOutput = with.fluidOutput ?: fluidOutput

        // 5. Merge medicines – deduplicate by id
        val mergedMedicines: List<VoiceMedicineItem>? = when {
            medicines == null && with.medicines == null -> null
            medicines == null -> with.medicines
            with.medicines == null -> medicines
            else -> {
                val existingIds = medicines.map { it.id }.toSet()
                val newOnes = with.medicines.filter { it.id !in existingIds }
                (medicines + newOnes).takeIf { it.isNotEmpty() }
            }
        }

        return copy(
            vitals       = mergedVitals,
            symptoms     = mergedSymptoms,
            fluidIntakes = mergedIntakes,
            fluidOutput  = mergedOutput,
            medicines    = mergedMedicines
        )
    }
}
