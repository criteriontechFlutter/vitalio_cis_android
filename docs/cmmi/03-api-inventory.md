# API Inventory

Generated on 2026-11-05 from `develop` at commit `0e9450c`.

Endpoint names and paths come from `utils/ApiEndPointCorporateModule.kt`. Server addresses come from `config/ApiConfig.kt`.

Request and response formats are not listed here. The backend team's API specification should be linked as the source for those.

## Environments

| Environment | Host |
|---|---|
| Local | `http://172.16.61.31` |
| Staging | `https://vitaliocis.vitalio.care` |
| Production | `https://cis.vitalio.care` |

Debug builds choose the environment with `API_ENV` in `local.properties`. Release builds use production.

## Other services

| Service | Address | HTTPS |
|---|---|---|
| Symptom and problem icons (`DIGI_DOCTOR`) | `http://52.172.134.222:205/api/v1.0/` | No |
| Lab report and prescription reading (`AI_UPLOAD`) | `http://food.shopright.ai:3480/` | No |
| Nutrition analysis (`NUTRI_ANALYSER`) | `https://nutrianalyser.com:313/api` | Yes |
| Smart ring tokens (`RING_TOKENS`) | `https://vitalioapi.medvantage.tech:5082/` | Yes |
| Smart ring vitals (`RING_VITALS`) | `https://demo.vitalio.care:5082/` | Yes |
| Video consultation (`CONSULTATION`) | `http://172.16.61.31:4082/` | No |

## Endpoints (65)

| Name | Path |
|---|---|
| `corporateEmployeeLogin` | `api/LogInForVitalioApp/SendLoginOtp` |
| `verifyLogInOTPForSHFCApp` | `api/LogInForVitalioApp/VerifyOTP` |
| `getPatientDetailsByMobileNo` | `api/PatientRegistration/GetPatientDetailsByMobileNo` |
| `fetchLastVital` | `api/PatientVital/FetchLastVital` |
| `addVital` | `api/PatientVital/AddVital` |
| `getSymptoms` | `api/PatientPrescription/FetchSymtomsOrDiagnosis` |
| `insertSymtoms` | `api/PatientPrescription/AddSymtomsOrDiagnosis` |
| `getProblemsWithIcon` | `Patient/getProblemsWithIcon` |
| `getAllProblems` | `Patient/getAllProblems` |
| `getAllDepartmentMaster` | `api/DepartmentMaster/GetAllDepartmentMaster` |
| `fetchDoctorsAvalability` | `api/DoctorShiftConfiguration/GetDoctorShiftConfiguration` |
| `fetchDoctorShiftConfig` | `api/DoctorShiftConfiguration/GetDoctorShiftConfigurationByDoctorId` |
| `getDoctorProfile` | `api/Users/GetDoctorProfile` |
| `fetchAvailableSlots` | `api/DoctorOPDSchedule/FetchAvailableSlots` |
| `bookAppointment` | `api/PatientRegistration/BookAppointment` |
| `addOutput` | `api/output/AddOutput` |
| `fetchOutput` | `api/output/FetchOutput` |
| `outputSummaryByDateRange` | `api/output/OutputSummaryByDateRange` |
| `getManualFoodAssignList` | `api/ManualFoodAssign/GetManualFoodAssignList` |
| `addIntake` | `api/FoodIntake/AddIntake` |
| `fetchIntake` | `api/FoodIntake/FetchIntake` |
| `fluidSummaryByDateRange` | `api/ManualFoodAssign/FluidSummaryByDateRange` |
| `getAllDietPlanByPid` | `api/DietPlan/GetAllDietPlanByPid` |
| `getDietPlanByDay` | `api/DietPlan/GetDietPlanByDay` |
| `updateDietPlanStatus` | `api/DietPlan/UpdateDietPlanStatus` |
| `removeDietPlan` | `api/DietPlan/RemoveDietPlan` |
| `updateDietIntake` | `api/DietPlan/UpdateDietIntake` |
| `insertDietPlan` | `api/DietPlan/InsertDietPlan` |
| `getDailyDietOverview` | `api/DietPlan/GetDailyDietOverview` |
| `uploadLabreportUrl` | `${ApiConfig.AI_UPLOAD}uploadLabreport/` |
| `fetchMedia` | `api/PatientMediaData/FetchMedia` |
| `fetchVitalByDate` | `api/PatientVital/FetchVitalByDate` |
| `fetchVitalAnalytics` | `api/PatientVital/FetchVitalAnalytics` |
| `fetchPatientMedicineIntake` | `api/PatientMedicineIntake/FetchPatientMedicineIntake` |
| `fetchPatientMedicineIntakeByDate` | `api/PatientMedicineIntake/FetchPatientMedicineIntakeByDate` |
| `insertMedicineIntake` | `api/PatientMedicineIntake/InsertEmployeeMedicineIntake` |
| `updatePatientProfile` | `api/PatientRegistration/UpdatePatientProfile` |
| `addAllergies` | `api/PatientPrescription/AddAllergies` |
| `fetchAllergies` | `api/PatientPrescription/FetchAllergies` |
| `getPatientDashboardData` | `api/PatientDashboard/GetPatientDashboardData` |
| `fetchUpcomingAppointmentList` | `api/PatientRegistration/FetchUpcomingAppointmentList` |
| `appointmentHistory` | `api/PatientRegistration/AppointmentHistory` |
| `fetchShiftTypeSetting` | `api/ShiftMaster/FetchShiftTypeSetting` |
| `fetchSchedule` | `api/DoctorOPDSchedule/FetchSchedule` |
| `getLatestToken` | `api/PatientRegistration/GetLatestToken` |
| `fetchConsent` | `api/ConcentMaster/FetchConcent` |
| `addConsent` | `api/PatientPrescription/AddConcent` |
| `getAllActivityMaster` | `api/ActivityMaster/GetAllActivityMaster` |
| `fetchPatientActivity` | `api/PatientPhysicalActivityTracker/FetchPatientActivity` |
| `addPatientActivity` | `api/PatientPhysicalActivityTracker/AddPatientActivity` |
| `deleteActivityMaster` | `api/EmployeeActivity/DeleteActivityMasterById` |
| `getEmergencyContacts` | `api/EmergencyContact/GetEmergencyContactByPid` |
| `insertEmergencyContact` | `api/EmergencyContact/InsertEmergencyContact` |
| `updateEmergencyContact` | `api/EmergencyContact/UpdateEmergencyContact` |
| `removeEmergencyContact` | `api/EmergencyContact/RemoveEmergencyContact` |
| `getAllHealthShareLinks` | `api/PatientHealthShare/GetAllPatientHealthShareLinks` |
| `generateHealthShareLink` | `api/PatientHealthShare/GeneratePatientHealthShareLink` |
| `revokeHealthShareLink` | `api/PatientHealthShare/RevokePatientHealthLink` |
| `getFamilyHealthHistory` | `api/PatientFamilyHealthHistory/GetPatientFamilyHealthHistory` |
| `insertFamilyHealthHistory` | `api/PatientFamilyHealthHistory/InsertPatientFamilyHealthHistory` |
| `deleteFamilyHealthHistory` | `api/PatientFamilyHealthHistory/DeletePatientFamilyHealthHistory` |
| `getAllDiseases` | `api/KnowMedApis/getAllDiseaseByAlphabet` |
| `consultationJoin` | `api/consultation/join` |
| `consultationDecline` | `api/consultation/decline` |
| `consultationEnd` | `api/consultation/end` |
