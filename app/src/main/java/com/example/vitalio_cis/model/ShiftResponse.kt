package com.example.vitalio_cis.model

data class ShiftResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<ShiftData> = emptyList()
)


data class ShiftData(
    val shiftId: Int = 0,
    val shift_Name: String = "",
    val shiftDate: String = "",
    val slot_Day: String = "",
    val slotJson: String = ""
)



data class SlotData(
    val slotTime: String = "",
    val slotStatus: String = "",
    val queuNo: Int = 0
)