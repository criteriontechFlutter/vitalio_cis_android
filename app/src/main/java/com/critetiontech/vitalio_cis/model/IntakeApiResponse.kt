package com.critetiontech.ctvitalio.model

import com.google.gson.annotations.SerializedName

data class IntakeResponse(
    val status: Int = 0,
    val message: String = "",
    @SerializedName("foodIntakeList")
    val responseValue: List<IntakeItem>? = null
)

// Standard adult daily fluid intake (~8 glasses), shown when the server sends no fluidRecommended
const val STANDARD_FLUID_INTAKE_ML = 2000

data class IntakeItem(
    val dietId: Int = 0,
    val foodId: Int = 0,
    val foodName: String? = null,
    @SerializedName("foodQty")
    val foodQuantity: Int = 0,
    @SerializedName("foodEntryDate")
    val foodDate: String? = null,
    @SerializedName("foodUnitID")
    val foodUnitId: Int = 0,
    val entryUserId: Int = 0,
    val unitName: String? = null,
    @SerializedName("foodEntryTime")
    val intakeTimeFormat: String? = null,
    val intakeDateFormat: String? = null,
    val isGiven: Int = 0,
    val translation: String? = null,
    // Server sends this as a decimal string, e.g. "3000.000000"
    val fluidRecommended: String? = null,
    /** Logged offline and still waiting to upload; never comes from the server. */
    @Transient val isPending: Boolean = false,
) {
    val fluidRecommendedMl: Int?
        get() = fluidRecommended?.toDoubleOrNull()?.toInt()?.takeIf { it > 0 }
}

data class ManualFoodAssignResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<ManualFoodAssignItem>? = null
)

data class ManualFoodAssignItem(
    val foodID: Int = 0,
    val foodName: String? = null,
    val quantity: String? = null,
    val givenFoodDate: String? = null
)

data class FluidSummaryResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<FluidSummaryItem>? = null
)

data class FluidSummaryItem(
    val foodQuantity: Int = 0,
    val foodId: Int = 0,
    val givenFoodDate: String = "",
    val assignedLimit: Int = 0
)
