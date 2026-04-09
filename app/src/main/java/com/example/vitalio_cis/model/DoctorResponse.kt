package com.example.vitalio_cis.model

data class DoctorResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Doctor>
)

data class Doctor(
    val assignedUserId: Int,
    val totalDays: Int,
    val scheduleDays: String,
    val doctorName: String,
    val qualification: String?,
    val departmentName: String?,
    val profileUrl: String
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

