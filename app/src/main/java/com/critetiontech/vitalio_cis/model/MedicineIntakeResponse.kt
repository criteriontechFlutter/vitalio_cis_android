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
