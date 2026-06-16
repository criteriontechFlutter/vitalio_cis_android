package com.critetiontech.ctvitalio.utils

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
    val fetchDoctorsAvalability="api/DoctorOPDSchedule/FetchDoctorsAvalability"
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




      val uploadLabreportUrl = "http://182.156.200.178:8016/uploadLabreport/"
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
}