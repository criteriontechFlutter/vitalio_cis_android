package com.example.vitalio_cis.model

data class IntakeResponse(
    val status: Int,
    val message: String,
    val responseValue: List<IntakeItem>
)

data class IntakeItem(
    val id: Int = 0,
    val foodId: Int = 0,
    val foodName: String = "",
    val foodQuantity: Int = 0,
    val foodDate: String = "",
    val foodUnitId: Int = 0,
    val userID: Int = 0,
    val clientId: Int = 0,
    val uhid: String = "",
    val entryType: String = "",
    val isFromPatient: Int = 0,
    val isGiven: Int = 0,
    val unitName: String? = null,
    val intakeDateFormat: String = "",
    val intakeTimeFormat: String = ""
)

data class ManualFoodAssignResponse(
    val status: Int,
    val message: String,
    val responseValue: List<ManualFoodAssignItem>
)

data class ManualFoodAssignItem(
    val foodID: Int = 0,
    val foodName: String = "",
    val quantity: Int = 0,
    val givenFoodDate: String = ""
)

data class FluidSummaryResponse(
    val status: Int,
    val message: String,
    val responseValue: List<FluidSummaryItem>
)

data class FluidSummaryItem(
    val foodQuantity: Int = 0,
    val foodId: Int = 0,
    val givenFoodDate: String = "",
    val assignedLimit: Int = 0
)
