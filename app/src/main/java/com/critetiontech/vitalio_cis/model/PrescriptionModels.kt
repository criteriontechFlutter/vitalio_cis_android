package com.critetiontech.ctvitalio.model

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import java.util.UUID

// Response of http://food.shopright.ai:3480/uploadPrescription/
// { "response": [ { patient, prescription } ] }  or  { "response": [ "error message" ] }

data class PrescriptionResponseItem(
    val patient: PrescriptionPatient? = null,
    val prescription: PrescriptionDetails? = null
)

data class PrescriptionPatient(
    val name: String? = null,
    // Gson reads numeric JSON values into String fields, so age can be Int or String
    val age: String? = null,
    val gender: String? = null
)

data class PrescriptionDetails(
    val date: String? = null,
    val medicines: List<PrescriptionMedicine>? = null,
    val advice: List<PrescriptionAdvice>? = null
)

data class PrescriptionTimeSlot(val timeSlot: String? = null)

data class PrescriptionMedicine(
    @SerializedName("medicineID") val medicineID: Int? = null,
    val name: String? = null,
    val medicineName: String? = null,
    val brandName: String? = null,
    val strength: String? = null,
    val doseStrength: Double? = null,
    val unitName: String? = null,
    val form: String? = null,
    val dosageFormName: String? = null,
    val dosage: String? = null,
    val frequency: String? = null,
    val timing: String? = null,
    val duration: String? = null,
    val timeSlotsJson: String? = null,
    // Local-only identity used for selection; Gson leaves it null so it is assigned after parsing
    @Transient val localId: String? = null
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: medicineName ?: "Medicine"
    val isInsertable: Boolean get() = (medicineID ?: 0) > 0
    val parsedTimeSlots: List<String> get() = parseTimeSlots(timeSlotsJson)
}

data class PrescriptionAdvice(
    val product: String? = null,
    val type: String? = null,
    val instruction: String? = null,
    val frequency: String? = null,
    val duration: String? = null,
    val timeSlotsJson: String? = null
) {
    val parsedTimeSlots: List<String> get() = parseTimeSlots(timeSlotsJson)
}

private fun parseTimeSlots(json: String?): List<String> = try {
    if (json.isNullOrBlank()) emptyList()
    else Gson().fromJson<List<PrescriptionTimeSlot>>(json, object : TypeToken<List<PrescriptionTimeSlot>>() {}.type)
        ?.mapNotNull { it.timeSlot } ?: emptyList()
} catch (e: Exception) { emptyList() }

fun PrescriptionMedicine.withLocalId(): PrescriptionMedicine = copy(localId = UUID.randomUUID().toString())