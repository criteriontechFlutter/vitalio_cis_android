package com.critetiontech.ctvitalio.model

data class MedicineIntakeResponse(
    val status: Int,
    val message: String,
    val responseValue: List<MedicinePeriod>
)

data class MedicinePeriod(
    val dayPeriod: String,
    val medicineData: List<MedicineItem>
)

data class MedicineItem(
    val id: Int,
    val medicineIntakeId: Int,
    val medicineName: String,
    val brandName: String,
    val unit: String,
    val dosageType: String,
    val dosageStrength: Double,
    val instructions: String,
    val scheduledDateTime: String,
    val isTaken: Boolean,
    val takenDateTime: String,
    val isSkipped: Boolean
) {
    /**
     * Gson ignores Kotlin nullability, and the server sends null text for some medicines
     * (e.g. {"medicineName":null,"brandName":null,"unit":null}), which crashed the reminder card
     * on .trim(). Replaces nulls with "" and falls back to the brand, then "Medicine", for the name.
     */
    fun sanitized(): MedicineItem {
        fun String?.clean() = this?.trim().orEmpty()
        val brand = (brandName as String?).clean()
        return copy(
            medicineName = (medicineName as String?).clean().ifEmpty { brand }.ifEmpty { "Medicine" },
            brandName = brand,
            unit = (unit as String?).clean(),
            dosageType = (dosageType as String?).clean(),
            instructions = (instructions as String?).clean(),
            scheduledDateTime = (scheduledDateTime as String?).clean(),
            takenDateTime = (takenDateTime as String?).clean(),
        )
    }
}

/** Same null clean-up for a whole period; a null medicine list becomes empty. */
@Suppress("USELESS_ELVIS", "UNNECESSARY_SAFE_CALL")
fun MedicinePeriod.sanitized() = MedicinePeriod(
    dayPeriod = (dayPeriod as String?).orEmpty(),
    medicineData = (medicineData as List<MedicineItem?>?).orEmpty().filterNotNull().map { it.sanitized() }
)

data class SearchMedicineResponse(
    val status: Int,
    val message: String,
    val responseValue: List<SearchMedicineItem>
)

data class SearchMedicineItem(
    val id: Int,
    val medicineID: Int,
    val medicineName: String,
    val name: String,
    val brandName: String,
    val dosageFormName: String,
    val doseStrength: Double,
    val unitName: String
)

data class InsertMedicineRequest(
    val pid: String,
    val medicineId: Int,
    val dosageType: String,
    val dosageStrength: Int,
    val frequency: String,
    val frequencyValue: String,
    val weekDays: String,
    val monthDates: String,
    val instructions: String,
    val timeSlotsJson: String,
    val startdate: String,
    val enddate: String,
    val clientId: String
)

data class InsertMedicineResponse(
    val status: Int,
    val message: String,
    val responseValue: String?
)
