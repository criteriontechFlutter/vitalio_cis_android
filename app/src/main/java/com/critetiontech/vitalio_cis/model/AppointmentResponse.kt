package com.critetiontech.vitalio_cis.model

data class UpcomingAppointmentItem(
    val appoientmentId: Int = 0,
    val pid: Int = 0,
    val departmentId: Int = 0,
    val doctorId: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val visitNo: String = "",
    val visitId: Int = 0,
    val visitDate: String = "",
    val slotTime: String = "",
    val doctorName: String = "",
    val patientType: String = "",
    val paymentStatus: String = "",
    val tokenNumber: Int = 0,
    val address: String = "",
    val age: String = "",
    val imageUrl: String = ""
)

data class UpcomingAppointmentsResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<UpcomingAppointmentItem> = emptyList()
)

data class AppointmentHistoryItem(
    val appoientmentId: Int = 0,
    val pid: Int = 0,
    val departmentId: Int = 0,
    val doctorId: Int = 0,
    val visitNo: String = "",
    val visitId: Int = 0,
    val visitDate: String = "",
    val slotTime: String = "",
    val tokenNumber: Int = 0,
    val doctorName: String = "",
    val qualification: String? = null,
    val designation: String = "",
    val profileUrl: String = "",
    val address: String = ""
)

data class AppointmentHistoryResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<AppointmentHistoryItem> = emptyList()
)