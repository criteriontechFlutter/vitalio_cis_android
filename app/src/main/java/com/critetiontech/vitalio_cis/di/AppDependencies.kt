package com.critetiontech.ctvitalio.di

import com.critetiontech.ctvitalio.data.repository.AllergyRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.AuthRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.DoctorRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.FluidRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.MedicineRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.ProfileRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.ReportRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.SymptomRepositoryImpl
import com.critetiontech.ctvitalio.data.repository.VitalRepositoryImpl
import com.critetiontech.ctvitalio.domain.usecase.allergy.AddAllergyUseCase
import com.critetiontech.ctvitalio.domain.usecase.allergy.FetchAllergiesUseCase
import com.critetiontech.ctvitalio.domain.usecase.auth.SendOtpUseCase
import com.critetiontech.ctvitalio.domain.usecase.auth.VerifyOtpUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.BookAppointmentUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.FetchAppointmentHistoryUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.FetchAvailableSlotsUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.FetchDoctorProfileUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.FetchDoctorsUseCase
import com.critetiontech.ctvitalio.domain.usecase.doctor.FetchUpcomingAppointmentsUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.AddIntakeUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.AddOutputUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.FetchFluidSummaryUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.FetchIntakeUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.FetchOutputSummaryUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.FetchOutputUseCase
import com.critetiontech.ctvitalio.domain.usecase.fluid.GetManualFoodListUseCase
import com.critetiontech.ctvitalio.domain.usecase.medicine.FetchMedicineByDateUseCase
import com.critetiontech.ctvitalio.domain.usecase.medicine.FetchMedicineIntakeUseCase
import com.critetiontech.ctvitalio.domain.usecase.medicine.InsertMedicineUseCase
import com.critetiontech.ctvitalio.domain.usecase.medicine.MarkMedicineIntakeUseCase
import com.critetiontech.ctvitalio.domain.usecase.medicine.SearchMedicineUseCase
import com.critetiontech.ctvitalio.domain.usecase.profile.UpdateProfileUseCase
import com.critetiontech.ctvitalio.domain.usecase.report.AiReportUseCase
import com.critetiontech.ctvitalio.domain.usecase.report.FetchMediaUseCase
import com.critetiontech.ctvitalio.domain.usecase.report.InsertInvestigationResultUseCase
import com.critetiontech.ctvitalio.domain.usecase.report.UploadMediaUseCase
import com.critetiontech.ctvitalio.domain.usecase.report.UploadPrescriptionUseCase
import com.critetiontech.ctvitalio.domain.usecase.symptom.FetchSymptomsUseCase
import com.critetiontech.ctvitalio.domain.usecase.symptom.GetAllProblemsUseCase
import com.critetiontech.ctvitalio.domain.usecase.symptom.GetProblemsWithIconUseCase
import com.critetiontech.ctvitalio.domain.usecase.symptom.InsertSymptomsUseCase
import com.critetiontech.ctvitalio.domain.usecase.vital.AddVitalUseCase
import com.critetiontech.ctvitalio.domain.usecase.vital.FetchLastVitalUseCase
import com.critetiontech.ctvitalio.domain.usecase.vital.FetchVitalAnalyticsUseCase
import com.critetiontech.ctvitalio.network.NetworkMonitor
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager

// Manual service locator used until Hilt annotation processing is unblocked.
// All objects are lazily created and the PrefsManager + repositories are singletons.
object AppDependencies {

    val prefs: PrefsManager by lazy { PrefsManager(MyApplication.appContext) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(MyApplication.appContext) }

    // Repositories
    val authRepository by lazy { AuthRepositoryImpl(MyApplication.appContext, prefs) }
    val vitalRepository by lazy { VitalRepositoryImpl(MyApplication.appContext, prefs) }
    val allergyRepository by lazy { AllergyRepositoryImpl(MyApplication.appContext, prefs) }
    val fluidRepository by lazy { FluidRepositoryImpl(MyApplication.appContext, prefs) }
    val medicineRepository by lazy { MedicineRepositoryImpl(MyApplication.appContext, prefs) }
    val doctorRepository by lazy { DoctorRepositoryImpl(MyApplication.appContext, prefs) }
    val reportRepository by lazy { ReportRepositoryImpl(MyApplication.appContext, prefs) }
    val symptomRepository by lazy { SymptomRepositoryImpl(MyApplication.appContext, prefs) }
    val profileRepository by lazy { ProfileRepositoryImpl(MyApplication.appContext, prefs) }

    // Use cases — created fresh each time (they're stateless wrappers)
    fun sendOtp() = SendOtpUseCase(authRepository)
    fun verifyOtp() = VerifyOtpUseCase(authRepository)
    fun fetchLastVital() = FetchLastVitalUseCase(vitalRepository)
    fun addVital() = AddVitalUseCase(vitalRepository)
    fun fetchVitalAnalytics() = FetchVitalAnalyticsUseCase(vitalRepository)
    fun fetchAllergies() = FetchAllergiesUseCase(allergyRepository)
    fun addAllergy() = AddAllergyUseCase(allergyRepository)
    fun getManualFoodList() = GetManualFoodListUseCase(fluidRepository)
    fun fetchIntake() = FetchIntakeUseCase(fluidRepository)
    fun addIntake() = AddIntakeUseCase(fluidRepository)
    fun fetchOutput() = FetchOutputUseCase(fluidRepository)
    fun addOutput() = AddOutputUseCase(fluidRepository)
    fun fetchFluidSummary() = FetchFluidSummaryUseCase(fluidRepository)
    fun fetchOutputSummary() = FetchOutputSummaryUseCase(fluidRepository)
    fun fetchMedicineIntake() = FetchMedicineIntakeUseCase(medicineRepository)
    fun markMedicineIntake() = MarkMedicineIntakeUseCase(medicineRepository)
    fun fetchMedicineByDate() = FetchMedicineByDateUseCase(medicineRepository)
    fun searchMedicine() = SearchMedicineUseCase(medicineRepository)
    fun insertMedicine() = InsertMedicineUseCase(medicineRepository)
    fun fetchDoctors() = FetchDoctorsUseCase(doctorRepository)
    fun fetchDepartments() = com.critetiontech.ctvitalio.domain.usecase.doctor.FetchDepartmentsUseCase(doctorRepository)
    fun fetchDoctorProfile() = FetchDoctorProfileUseCase(doctorRepository)
    fun fetchAvailableSlots() = FetchAvailableSlotsUseCase(doctorRepository)
    fun bookAppointment() = BookAppointmentUseCase(doctorRepository)
    fun fetchUpcomingAppointments() = FetchUpcomingAppointmentsUseCase(doctorRepository)
    fun fetchAppointmentHistory() = FetchAppointmentHistoryUseCase(doctorRepository)
    fun fetchMedia() = FetchMediaUseCase(reportRepository)
    fun uploadMedia() = UploadMediaUseCase(reportRepository)
    fun aiReport() = AiReportUseCase(reportRepository)
    fun insertInvestigationResult() = InsertInvestigationResultUseCase(reportRepository)
    fun uploadPrescription() = UploadPrescriptionUseCase(reportRepository)
    fun fetchSymptoms() = FetchSymptomsUseCase(symptomRepository)
    fun insertSymptoms() = InsertSymptomsUseCase(symptomRepository)
    fun getProblemsWithIcon() = GetProblemsWithIconUseCase(symptomRepository)
    fun getAllProblems() = GetAllProblemsUseCase(symptomRepository)
    fun updateProfile() = UpdateProfileUseCase(profileRepository)
}
