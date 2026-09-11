package com.critetiontech.vitalio_cis.model

data class DoctorResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Doctor>
)
data class SingleDoctorResponse(
    val status: Int,
    val message: String,
    val responseValue: Doctor?
)

data class Doctor(
    val doctorId: Int = 0,
    val doctorName: String? = null,
    val bookingType: String? = null,
    val appointmentSlotSize: Int = 0,
    val maxAppointmentsPerShift: Int = 0,
    val allowOverbooking: Boolean = false,
    val isDefaultBooking: Boolean = false,
    val isDoctorShiftActive: Boolean = false,
    val isShiftActive: Boolean = false,
    val workingDays: List<WorkingDay> = emptyList(),
    // Old fields mapped for compatibility
    val assignedUserId: Int = 0,
    val totalDays: Int = 0,
    val scheduleDays: String = "",
    val qualification: String? = null,
    val departmentName: String? = null,
    val profileUrl: String = ""
)

data class WorkingDay(
    val dayId: Int,
    val dayName: String,
    val shifts: List<DoctorShift>
)

data class DoctorShift(
    val shiftId: Int,
    val shiftName: String,
    val shiftTimeFrom: String,
    val shiftTimeTo: String
)

data class DoctorResponsedata(
    val status: Int,
    val message: String,
    val responseValue: List<DoctorDetails>
)

data class DoctorDetails(
    val id: Int = 0,
    val clientID: Int = 0,
    val name: String = "",
    val email: String = "",
    val mobileNo: String = "",
    val userName: String = "",
    val genderId: Int = 0,
    val genderName: String = "",
    val roleId: Int = 0,
    val roleName: String = "",
    val biography: String = "",
    val consultedPatientCount: String = "",
    val experience: String = "",
    val profilePath: String = "",
    val isOtpAuthentication: Boolean = false,
    val createdDate: String = "",
    val departmentId: Int = 0,
    val departmentName: String = "",
    val highestQualificationId: Int = 0,
    val highestQualificationName: String = "",
    val consultationFee: String = "",
    val additionalInfos: List<AdditionalInfo> = emptyList(),
    val userDocuments: List<UserDocument> = emptyList(),
    val isBlocked: Boolean = false,
    val actionAt: String = "",
    val actionBy: Int = 0,
    val actionType: String = ""
)

data class AdditionalInfo(
    val id: Int,
    val qualificationId: Int,
    val qualificationName: String,
    val collegeName: String,
    val year: String,
    val attachmentPath: String?
)

data class UserDocument(
    val id: Int,
    val documentId: Int,
    val documentName: String,
    val documentNumber: String,
    val documentPath: String?
)

