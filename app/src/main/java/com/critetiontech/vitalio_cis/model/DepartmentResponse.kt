package com.critetiontech.ctvitalio.model

data class DepartmentResponse(
    val status: Int,
    val message: String,
    val responseValue: List<Department>
)

data class Department(
    val id: Int,
    val departmentName: String,
    val code: String,
    val iconPath: String?,
    val status: Boolean,
    val userId: Int,
    val isCreatedByOwner: Boolean,
    val clientId: Int,
    val createdDate: String
)
