package com.critetiontech.ctvitalio.utils

import com.critetiontech.ctvitalio.config.ApiConfig

class ApiEndPointCorporateModule {
    val corporateEmployeeLogin = "api/LogInForVitalioApp/SendLoginOtp"
    val verifyLogInOTPForSHFCApp = "api/LogInForVitalioApp/VerifyOTP"
    val getPatientDetailsByMobileNo = "api/PatientRegistration/GetPatientDetailsByMobileNo"
    val fetchLastVital = "api/PatientVital/FetchLastVital"
    val addVital = "api/PatientVital/AddVital"
    val getSymptoms="api/PatientPrescription/FetchSymtomsOrDiagnosis"
    val insertSymtoms="api/PatientPrescription/AddSymtomsOrDiagnosis"
    val getProblemsWithIcon="Patient/getProblemsWithIcon"
    val getAllProblems="Patient/getAllProblems"
    val getAllDepartmentMaster="api/DepartmentMaster/GetAllDepartmentMaster"
    val fetchDoctorsAvalability="api/DoctorShiftConfiguration/GetDoctorShiftConfiguration"
    val fetchDoctorShiftConfig="api/DoctorShiftConfiguration/GetDoctorShiftConfigurationByDoctorId"
    val getDoctorProfile="api/Users/GetDoctorProfile"
    val fetchAvailableSlots="api/DoctorOPDSchedule/FetchAvailableSlots"
    val bookAppointment="api/PatientRegistration/BookAppointment"
    val addOutput="api/output/AddOutput"
    val fetchOutput="api/output/FetchOutput"
    val outputSummaryByDateRange="api/output/OutputSummaryByDateRange"
    val getManualFoodAssignList="api/ManualFoodAssign/GetManualFoodAssignList"
    val addIntake="api/FoodIntake/AddIntake"
    val fetchIntake="api/FoodIntake/FetchIntake"
    val fluidSummaryByDateRange="api/ManualFoodAssign/FluidSummaryByDateRange"




      val uploadLabreportUrl = "${ApiConfig.AI_UPLOAD}uploadLabreport/"
    val fetchMedia = "api/PatientMediaData/FetchMedia"

    val fetchVitalByDate="api/PatientVital/FetchVitalByDate"
    val fetchVitalAnalytics="api/PatientVital/FetchVitalAnalytics"
    val fetchPatientMedicineIntake = "api/PatientMedicineIntake/FetchPatientMedicineIntake"
    val fetchPatientMedicineIntakeByDate = "api/PatientMedicineIntake/FetchPatientMedicineIntakeByDate"
    val insertMedicineIntake = "api/PatientMedicineIntake/InsertEmployeeMedicineIntake"
    val updatePatientProfile = "api/PatientRegistration/UpdatePatientProfile"
    val addAllergies = "api/PatientPrescription/AddAllergies"
    val fetchAllergies = "api/PatientPrescription/FetchAllergies"
    val getPatientDashboardData = "api/PatientDashboard/GetPatientDashboardData"
    val fetchUpcomingAppointmentList = "api/PatientRegistration/FetchUpcomingAppointmentList"
    val appointmentHistory = "api/PatientRegistration/AppointmentHistory"

    val fetchShiftTypeSetting = "api/ShiftMaster/FetchShiftTypeSetting"
    val fetchSchedule = "api/DoctorOPDSchedule/FetchSchedule"
    val getLatestToken = "api/PatientRegistration/GetLatestToken"
    val fetchConsent = "api/ConcentMaster/FetchConcent"
    val addConsent = "api/PatientPrescription/AddConcent"

    // Activity
    val getAllActivityMaster = "api/ActivityMaster/GetAllActivityMaster"
    val fetchPatientActivity = "api/PatientPhysicalActivityTracker/FetchPatientActivity"
    val addPatientActivity = "api/PatientPhysicalActivityTracker/AddPatientActivity"
    val deleteActivityMaster = "api/EmployeeActivity/DeleteActivityMasterById"

    // Video consultation (ApiClients.consultation)
    val consultationJoin = "api/consultation/join"
    val consultationDecline = "api/consultation/decline"
    val consultationEnd = "api/consultation/end"
}