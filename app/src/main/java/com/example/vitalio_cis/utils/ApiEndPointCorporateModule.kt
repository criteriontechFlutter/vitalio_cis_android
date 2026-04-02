package com.critetiontech.ctvitalio.utils

class ApiEndPointCorporateModule {

    val corporateEmployeeLogin = "api/LogInForVitalioApp/SendLoginOtp"
    val verifyLogInOTPForSHFCApp = "api/LogInForSHFCApp/VerifyLogInOTPForSHFCApp"



    val getPatientDetailsByMobileNo = "api/PatientRegistration/GetPatientDetailsByMobileNo"



    val fetchLastVital = "api/PatientVital/FetchLastVital"

    val getSymptoms="api/PatientPrescription/FetchSymtomsOrDiagnosis"

    val insertSymtoms="api/PatientPrescription/AddSymtomsOrDiagnosis"
    val getProblemsWithIcon="Patient/getProblemsWithIcon"
    val getAllProblems="Patient/getAllProblems"
}