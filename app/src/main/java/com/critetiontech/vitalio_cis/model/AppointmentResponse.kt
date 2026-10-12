package com.critetiontech.vitalio_cis.model

data class UpcomingAppointmentItem(
    val appoientmentId: Int = 0,
    val pid: Int = 0,
    val departmentId: Int = 0,
    val doctorId: Int = 0,
    val firstName: String = "",
    val lastName: String = "",
    val genderId: Int = 0,
    val visitNo: String = "",
    val visitId: Int = 0,
    val shiftId: Int = 0,
    val uhID: String = "",
    val visitDate: String = "",
    val mobileNo: String = "",
    val address: String = "",
    val cityId: Int = 0,
    val stateId: Int = 0,
    val dob: String = "",
    val age: String = "",
    val patientType: String = "",
    val tokenNumber: Int = 0,
    val slotTime: String = "",
    val genderName: String = "",
    val doctorName: String = "",
    val paymentStatus: String = "",
    val height: String = "",
    val weight: String = "",
    val lastVisitDate: String = "",
    val joinedOn: String = "",
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
    val shiftId: Int = 0,
    val appointmentDate: String = "",
    val visitDate: String = "",
    val slotTime: String = "",
    val tokenNumber: Int = 0,
    val isCancelAppointment: Boolean = false,
    val prescriptionStatus: String = "",
    val shiftName: String = "",
    val shiftTime: String = "",
    val dayName: String = "",
    val formattedDate: String = "",
    val doctorName: String = "",
    val qualification: String? = null,
    val departmentName: String? = null,
    val designation: String = "",
    val profileUrl: String = "",
    val address: String = ""
)

data class AppointmentHistoryResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: List<AppointmentHistoryItem> = emptyList()
)