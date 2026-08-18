package com.critetiontech.vitalio_cis.di

import com.critetiontech.vitalio_cis.data.repository.AllergyRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.AuthRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.DoctorRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.FluidRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.MedicineRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.ProfileRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.ReportRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.SymptomRepositoryImpl
import com.critetiontech.vitalio_cis.data.repository.VitalRepositoryImpl
import com.critetiontech.vitalio_cis.domain.usecase.allergy.AddAllergyUseCase
import com.critetiontech.vitalio_cis.domain.usecase.allergy.FetchAllergiesUseCase
import com.critetiontech.vitalio_cis.domain.usecase.auth.SendOtpUseCase
import com.critetiontech.vitalio_cis.domain.usecase.auth.VerifyOtpUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.BookAppointmentUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.FetchAppointmentHistoryUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.FetchAvailableSlotsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.FetchDoctorProfileUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.FetchDoctorsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.doctor.FetchUpcomingAppointmentsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.AddIntakeUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.AddOutputUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.FetchFluidSummaryUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.FetchIntakeUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.FetchOutputSummaryUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.FetchOutputUseCase
import com.critetiontech.vitalio_cis.domain.usecase.fluid.GetManualFoodListUseCase
import com.critetiontech.vitalio_cis.domain.usecase.medicine.FetchMedicineByDateUseCase
import com.critetiontech.vitalio_cis.domain.usecase.medicine.FetchMedicineIntakeUseCase
import com.critetiontech.vitalio_cis.domain.usecase.medicine.MarkMedicineIntakeUseCase
import com.critetiontech.vitalio_cis.domain.usecase.profile.UpdateProfileUseCase
import com.critetiontech.vitalio_cis.domain.usecase.report.AiReportUseCase
import com.critetiontech.vitalio_cis.domain.usecase.report.FetchMediaUseCase
import com.critetiontech.vitalio_cis.domain.usecase.report.InsertInvestigationResultUseCase
import com.critetiontech.vitalio_cis.domain.usecase.report.UploadMediaUseCase
import com.critetiontech.vitalio_cis.domain.usecase.symptom.FetchSymptomsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.symptom.GetAllProblemsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.symptom.GetProblemsWithIconUseCase
import com.critetiontech.vitalio_cis.domain.usecase.symptom.InsertSymptomsUseCase
import com.critetiontech.vitalio_cis.domain.usecase.vital.AddVitalUseCase
import com.critetiontech.vitalio_cis.domain.usecase.vital.FetchLastVitalUseCase
import com.critetiontech.vitalio_cis.domain.usecase.vital.FetchVitalAnalyticsUseCase
import com.critetiontech.vitalio_cis.network.NetworkMonitor
import com.critetiontech.vitalio_cis.utils.MyApplication
import com.critetiontech.vitalio_cis.utils.PrefsManager

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
    fun fetchDoctors() = FetchDoctorsUseCase(doctorRepository)
    fun fetchDoctorProfile() = FetchDoctorProfileUseCase(doctorRepository)
    fun fetchAvailableSlots() = FetchAvailableSlotsUseCase(doctorRepository)
    fun bookAppointment() = BookAppointmentUseCase(doctorRepository)
    fun fetchUpcomingAppointments() = FetchUpcomingAppointmentsUseCase(doctorRepository)
    fun fetchAppointmentHistory() = FetchAppointmentHistoryUseCase(doctorRepository)
    fun fetchMedia() = FetchMediaUseCase(reportRepository)
    fun uploadMedia() = UploadMediaUseCase(reportRepository)
    fun aiReport() = AiReportUseCase(reportRepository)
    fun insertInvestigationResult() = InsertInvestigationResultUseCase(reportRepository)
    fun fetchSymptoms() = FetchSymptomsUseCase(symptomRepository)
    fun insertSymptoms() = InsertSymptomsUseCase(symptomRepository)
    fun getProblemsWithIcon() = GetProblemsWithIconUseCase(symptomRepository)
    fun getAllProblems() = GetAllProblemsUseCase(symptomRepository)
    fun updateProfile() = UpdateProfileUseCase(profileRepository)
}
