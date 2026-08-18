package com.critetiontech.vitalio_cis.domain.usecase

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.domain.repository.AllergyRepository
import com.critetiontech.vitalio_cis.domain.repository.AuthRepository
import com.critetiontech.vitalio_cis.domain.repository.VitalRepository
import com.critetiontech.vitalio_cis.domain.usecase.auth.SendOtpUseCase
import com.critetiontech.vitalio_cis.domain.usecase.auth.VerifyOtpUseCase
import com.critetiontech.vitalio_cis.model.AllergyItem
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import com.critetiontech.vitalio_cis.utils.Patient
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Use Cases using manual Fake repositories (no Mockito needed).
 */
class UseCaseTest {

    // ─────────────────────────────────────────────────────────────────
    // Fake Repositories
    // ─────────────────────────────────────────────────────────────────

    private class FakeAuthRepository(
        private val sendOtpResult: DomainResult<Boolean> = DomainResult.Success(true),
        private val verifyOtpResult: DomainResult<Patient> = DomainResult.Success(fakePatient())
    ) : AuthRepository {
        override suspend fun sendOtp(mobile: String): DomainResult<Boolean> = sendOtpResult
        override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> = verifyOtpResult
    }

    private class FakeAllergyRepository(
        private val fetchResult: DomainResult<List<AllergyItem>> = DomainResult.Success(emptyList()),
        private val addResult: DomainResult<Unit> = DomainResult.Success(Unit)
    ) : AllergyRepository {
        override suspend fun fetchAllergies(uhid: String, clientId: Int, typeAllergy: String): DomainResult<List<AllergyItem>> = fetchResult
        override suspend fun addAllergy(body: Map<String, Any>): DomainResult<Unit> = addResult
    }

    private class FakeVitalRepository(
        private val fetchResult: DomainResult<List<Vital>> = DomainResult.Success(emptyList()),
        private val addResult: DomainResult<Unit> = DomainResult.Success(Unit),
        private val analyticsResult: DomainResult<List<VitalGraphEntry>> = DomainResult.Success(emptyList())
    ) : VitalRepository {
        override suspend fun fetchLastVital(uhid: String, clientId: String, userId: String): DomainResult<List<Vital>> = fetchResult
        override suspend fun addVital(body: Map<String, Any>): DomainResult<Unit> = addResult
        override suspend fun fetchVitalAnalytics(params: Map<String, Any>): DomainResult<List<VitalGraphEntry>> = analyticsResult
    }

    // ─────────────────────────────────────────────────────────────────
    // SendOtpUseCase Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `SendOtpUseCase returns Success true when OTP sent`() = runTest {
        val repo = FakeAuthRepository(sendOtpResult = DomainResult.Success(true))
        val useCase = SendOtpUseCase(repo)
        val result = useCase("9876543210")
        assertTrue(result is DomainResult.Success)
        assertTrue((result as DomainResult.Success).data)
    }

    @Test
    fun `SendOtpUseCase returns Success false when number not registered`() = runTest {
        val repo = FakeAuthRepository(sendOtpResult = DomainResult.Success(false))
        val useCase = SendOtpUseCase(repo)
        val result = useCase("0000000000")
        assertTrue(result is DomainResult.Success)
        assertFalse((result as DomainResult.Success).data)
    }

    @Test
    fun `SendOtpUseCase returns Error on network failure`() = runTest {
        val repo = FakeAuthRepository(sendOtpResult = DomainResult.Error(Exception("Network error")))
        val useCase = SendOtpUseCase(repo)
        val result = useCase("9876543210")
        assertTrue(result is DomainResult.Error)
        assertEquals("Network error", (result as DomainResult.Error).exception.message)
    }

    @Test
    fun `SendOtpUseCase delegates to repository with correct mobile`() = runTest {
        var capturedMobile = ""
        val repo = object : AuthRepository {
            override suspend fun sendOtp(mobile: String): DomainResult<Boolean> {
                capturedMobile = mobile
                return DomainResult.Success(true)
            }
            override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> =
                DomainResult.Error(Exception())
        }
        val useCase = SendOtpUseCase(repo)
        useCase("1234567890")
        assertEquals("1234567890", capturedMobile)
    }

    // ─────────────────────────────────────────────────────────────────
    // VerifyOtpUseCase Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `VerifyOtpUseCase returns patient on success`() = runTest {
        val patient = fakePatient()
        val repo = FakeAuthRepository(verifyOtpResult = DomainResult.Success(patient))
        val useCase = VerifyOtpUseCase(repo)
        val result = useCase("UHID001", "123456", "token_abc")
        assertTrue(result is DomainResult.Success)
        assertEquals("UHID001", (result as DomainResult.Success).data.uhId)
    }

    @Test
    fun `VerifyOtpUseCase returns Error on wrong OTP`() = runTest {
        val repo = FakeAuthRepository(verifyOtpResult = DomainResult.Error(Exception("Invalid OTP")))
        val useCase = VerifyOtpUseCase(repo)
        val result = useCase("UHID001", "000000", "token_abc")
        assertTrue(result is DomainResult.Error)
        assertEquals("Invalid OTP", (result as DomainResult.Error).exception.message)
    }

    @Test
    fun `VerifyOtpUseCase passes all params to repository`() = runTest {
        var capturedUhid = ""; var capturedOtp = ""; var capturedToken = ""
        val repo = object : AuthRepository {
            override suspend fun sendOtp(mobile: String): DomainResult<Boolean> = DomainResult.Error(Exception())
            override suspend fun verifyOtp(uhid: String, otp: String, deviceToken: String): DomainResult<Patient> {
                capturedUhid = uhid; capturedOtp = otp; capturedToken = deviceToken
                return DomainResult.Success(fakePatient())
            }
        }
        val useCase = VerifyOtpUseCase(repo)
        useCase("UHID_TEST", "654321", "firebase_token")
        assertEquals("UHID_TEST", capturedUhid)
        assertEquals("654321", capturedOtp)
        assertEquals("firebase_token", capturedToken)
    }

    // ─────────────────────────────────────────────────────────────────
    // AllergyRepository Fake Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `AllergyRepository fetchAllergies returns list`() = runTest {
        val items = listOf(
            AllergyItem(detailID = 1, substanceName = "Peanuts", allergy = "High", reaction = "Anaphylaxis"),
            AllergyItem(detailID = 2, substanceName = "Shellfish", allergy = "Medium", reaction = "Hives")
        )
        val repo = FakeAllergyRepository(fetchResult = DomainResult.Success(items))
        val result = repo.fetchAllergies("UHID001", 1, "MedicineAllergy")
        assertTrue(result is DomainResult.Success)
        assertEquals(2, (result as DomainResult.Success).data.size)
        assertEquals("Peanuts", result.data[0].substanceName)
    }

    @Test
    fun `AllergyRepository addAllergy returns Success on valid body`() = runTest {
        val repo = FakeAllergyRepository(addResult = DomainResult.Success(Unit))
        val body = mapOf("uhId" to "UHID001", "substanceName" to "Mango")
        val result = repo.addAllergy(body)
        assertTrue(result is DomainResult.Success)
    }

    // ─────────────────────────────────────────────────────────────────
    // VitalRepository Fake Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `VitalRepository fetchLastVital returns list`() = runTest {
        val vitals = listOf(
            Vital(id = 1, vitalName = "Heart Rate", vitalValue = 75.0, unit = "bpm"),
            Vital(id = 2, vitalName = "SpO2", vitalValue = 98.0, unit = "%")
        )
        val repo = FakeVitalRepository(fetchResult = DomainResult.Success(vitals))
        val result = repo.fetchLastVital("UHID001", "1", "100")
        assertTrue(result is DomainResult.Success)
        assertEquals(2, (result as DomainResult.Success).data.size)
        assertEquals("Heart Rate", result.data[0].vitalName)
    }

    @Test
    fun `VitalRepository fetchLastVital returns empty list`() = runTest {
        val repo = FakeVitalRepository(fetchResult = DomainResult.Success(emptyList()))
        val result = repo.fetchLastVital("UHID001", "1", "100")
        assertTrue(result is DomainResult.Success)
        assertTrue((result as DomainResult.Success).data.isEmpty())
    }

    @Test
    fun `VitalRepository addVital returns Success`() = runTest {
        val repo = FakeVitalRepository(addResult = DomainResult.Success(Unit))
        val result = repo.addVital(mapOf("vmValueHeartRate" to "80"))
        assertTrue(result is DomainResult.Success)
    }

    @Test
    fun `VitalRepository fetchVitalAnalytics returns analytics data`() = runTest {
        val entries = listOf(VitalGraphEntry(dateTime = "2024-01-01T10:00", details = emptyList()))
        val repo = FakeVitalRepository(analyticsResult = DomainResult.Success(entries))
        val result = repo.fetchVitalAnalytics(mapOf("fromDate" to "2024-01-01"))
        assertTrue(result is DomainResult.Success)
        assertEquals(1, (result as DomainResult.Success).data.size)
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    companion object {
        fun fakePatient() = Patient(
            pid = 1, uhId = "UHID001", firstName = "John", lastName = "Doe",
            dob = "1990-01-01", genderId = 1, countryCallingCode = "+91",
            mobileNo = "9876543210", emailAddress = "john@test.com",
            age = "34", guardianRelationId = 0, guardianName = "",
            countryId = 1, stateId = 1, cityId = 1, address = "123 Street",
            imageUrl = "", bloodGroupId = 1, zip = "400001", isActive = true,
            clientId = 10, departmentName = null, cityName = "Mumbai",
            stateName = "Maharashtra", countryName = "India",
            genderName = "Male", guardianRelationName = null, bloodGroupName = "O+"
        )
    }
}
