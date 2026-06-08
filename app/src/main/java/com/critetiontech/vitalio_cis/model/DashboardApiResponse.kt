package com.critetiontech.vitalio_cis.model

data class DashboardResponse(
    val status: Int = 0,
    val message: String = "",
    val responseValue: DashboardData = DashboardData()
)

data class DashboardData(
    val pendingMedicines: List<PendingMedicine> = emptyList(),
    val latestVitals: List<Vital> = emptyList(),
    val upcomingAppointments: List<UpcomingAppointment> = emptyList()
)

data class UpcomingAppointment(
    val appointmentId: Int = 0,
    val doctorId: Int = 0,
    val doctorName: String = "",
    val qualification: String = "",
    val departmentName: String = "",
    val profileImage: String = "",
    val clinicName: String = "",
    val appointmentDate: String = "",
    val slotTime: String = ""
)

data class PendingMedicine(
    val pid: Int = 0,
    val isTaken: Boolean = false,
    val medicineId: Int = 0,
    val medicineName: String = "",
    val frequency: String = "",
    val instructions: String = ""
)