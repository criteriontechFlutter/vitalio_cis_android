package com.critetiontech.ctvitalio.model

import com.google.gson.annotations.SerializedName

data class ShiftTypeSettingResponse(
    val status: Int,
    val message: String,
    val responseValue: List<ShiftTypeSetting> = emptyList()
)

data class ShiftTypeSetting(
    val id: Int,
    @SerializedName("assigneduserId") val assignedUserId: Int,
    @SerializedName("shiftTypeid") val shiftTypeId: Int,
    val shiftTypeName: String,
    val clientId: Int
)

data class ScheduleResponse(
    val status: Int,
    val message: String,
    val responseValue: List<DoctorSchedule> = emptyList()
)

data class DoctorSchedule(
    val id: Int,
    val doctorId: Int,
    val shiftId: Int,
    val shiftName: String,
    val timeInterval: String,
    val dayId: Int,
    val dayName: String,
    val shiftTimeFrom: String,
    val shiftTimeTo: String,
    val userId: Int,
    val doctorName: String
)

data class LatestTokenResponse(
    val status: Int,
    val message: String,
    val responseValue: List<LatestToken> = emptyList()
)

data class LatestToken(
    val nextQueueNumber: Int
)

data class ConsentResponse(
    val status: Int,
    val message: String,
    val responseValue: List<ConsentItem> = emptyList()
)

data class ConsentItem(
    val id: Int,
    val name: String,
    val description: String,
    val status: Boolean,
    val createdDate: String,
    val userId: Int
)

data class AddConsentResponse(
    val status: Int,
    val message: String,
    val responseValue: String?
)
