package com.critetiontech.vitalio_cis.model

data class MedicineIntakeByDateResponse(
    val status: Int,
    val message: String,
    val responseValue: MedicineIntakeByDate
)

data class MedicineIntakeByDate(
    val loggedMedicines: List<LoggedMedicine>,
    val allMedicines: List<AllMedicine>
)

data class LoggedMedicine(
    val medicineId: Int,
    val medicineName: String,
    val unit: String,
    val dosageType: String,
    val doseDate: String,
    val doseStatus: String,
    val isTaken: Boolean,
    val takenDateTime: String
)

data class AllMedicine(
    val medicineId: Int,
    val medicineName: String,
    val unit: String,
    val dosageType: String
)
