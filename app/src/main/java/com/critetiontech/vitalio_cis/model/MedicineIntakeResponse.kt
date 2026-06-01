package com.critetiontech.vitalio_cis.model

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
)
