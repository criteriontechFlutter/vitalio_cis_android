package com.critetiontech.ctvitalio.model

import com.google.gson.annotations.SerializedName

// GET all activities master list
data class ActivityMasterResponse(
    val status: Int,
    val message: String,
    val responseValue: List<ActivityModel> = emptyList()
)

data class ActivityModel(
    val id: Int,
    val activityName: String,
    val category: String = ""
)

// Fetch patient activity (recent / history)
data class PatientActivityResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<PatientActivityItem> = emptyList()
)

data class PatientActivityItem(
    @SerializedName("id") val serverId: Int = 0,
    val pid: Int = 0,
    val activityId: Int = 0,
    val activityName: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val timeFrom: String = "",
    val timeTo: String = "",
    val clientId: Int = 0,
    @SerializedName("clientID") val clientID: Int = 0,
    val userId: Int = 0,
    val uhid: String? = null,
    val patientName: String? = null,
    val totalTimeInMinutes: String? = null,
    val remark: String? = null,
    val createdBy: String? = null
) {
    /** Resolved start time — prefers startTime, falls back to timeFrom */
    val resolvedStartTime: String get() = startTime.ifEmpty { timeFrom }
    /** Resolved end time — prefers endTime, falls back to timeTo */
    val resolvedEndTime: String get() = endTime.ifEmpty { timeTo }
    /** Resolved client id */
    val resolvedClientId: Int get() = if (clientId != 0) clientId else clientID
}

// Insert activity request body
data class AddPatientActivityRequest(
    val uhid: String,
    val activityId: Int,
    val timeFrom: String,
    val totalTimeInMinutes: String,
    val remark: String = "NA",
    val userId: Int = 0,
    val clientID: Int = 45
)

/** How the server took an AddPatientActivity request. */
enum class ActivitySaveResult { SAVED, ALREADY_EXISTS }
