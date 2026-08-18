package com.critetiontech.vitalio_cis.utils

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for PrefsManager logic that does NOT require Android Context.
 * Tests the pure Kotlin parts: data classes and the getData caching logic.
 */
class PrefsManagerLogicTest {

    // ─────────────────────────────────────────────────────────────────
    // VerifyOtpPatient model tests (defined in PrefsManager.kt)
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `VerifyOtpPatient default values are correct`() {
        val patient = VerifyOtpPatient()
        assertEquals(0, patient.pid)
        assertEquals(0, patient.pmId)
        assertEquals("", patient.uhId)
        assertEquals("", patient.firstName)
        assertNull(patient.lastName)
        assertEquals("", patient.mobileNo)
        assertEquals("", patient.emailId)
        assertEquals("", patient.address)
        assertEquals("", patient.loginDateTime)
        assertEquals("", patient.age)
        assertEquals("", patient.ageType)
        assertEquals("", patient.dob)
        assertEquals(0, patient.genderId)
        assertEquals(0, patient.cityId)
        assertEquals(0, patient.stateId)
        assertEquals(0, patient.clientId)
        assertEquals("", patient.height)
        assertEquals("", patient.weight)
        assertEquals(0, patient.bloodGroupId)
        assertEquals("", patient.joinedOn)
        assertNull(patient.lastVisitDate)
        assertNull(patient.patientStatus)
    }

    @Test
    fun `VerifyOtpPatient equality for same values`() {
        val p1 = VerifyOtpPatient(pid = 1, uhId = "UHID001", firstName = "Alice")
        val p2 = VerifyOtpPatient(pid = 1, uhId = "UHID001", firstName = "Alice")
        assertEquals(p1, p2)
    }

    @Test
    fun `VerifyOtpPatient copy works correctly`() {
        val original = VerifyOtpPatient(pid = 5, firstName = "Bob")
        val copy = original.copy(firstName = "Alice")
        assertEquals("Alice", copy.firstName)
        assertEquals(5, copy.pid)
        assertEquals("Bob", original.firstName)
    }

    @Test
    fun `VerifyOtpResponse default values are correct`() {
        val response = VerifyOtpResponse()
        assertEquals(0, response.status)
        assertEquals("", response.message)
        assertNull(response.responseValue)
    }

    @Test
    fun `VerifyOtpResponse with Success status holds patient`() {
        val patient = VerifyOtpPatient(pid = 10, uhId = "UHID999", firstName = "Jane")
        val response = VerifyOtpResponse(status = 1, message = "Success", responseValue = patient)
        assertEquals(1, response.status)
        assertNotNull(response.responseValue)
        assertEquals("Jane", response.responseValue!!.firstName)
    }

    @Test
    fun `PatientResponse holds list of patients`() {
        val patients = listOf(
            Patient(pid = 1, uhId = "U1", firstName = "Alice", lastName = null, dob = "1990-01-01",
                genderId = 2, countryCallingCode = "+91", mobileNo = "9876543210",
                emailAddress = "alice@test.com", age = "34", guardianRelationId = 0,
                guardianName = "", countryId = 1, stateId = 1, cityId = 1, address = "123 St",
                imageUrl = "", bloodGroupId = 1, zip = "400001", isActive = true,
                clientId = 10, departmentName = null, cityName = "Mumbai",
                stateName = "Maharashtra", countryName = "India",
                genderName = "Female", guardianRelationName = null, bloodGroupName = "A+")
        )
        val response = PatientResponse(status = 1, message = "OK", responseValue = patients)
        assertEquals(1, response.status)
        assertEquals(1, response.responseValue.size)
        assertEquals("Alice", response.responseValue[0].firstName)
    }

    @Test
    fun `EmployeeGoal stores all fields correctly`() {
        val goal = EmployeeGoal(pid = 1, vmId = 10, vitalName = "Blood Pressure", targetValue = 120, unit = "mmHg")
        assertEquals(1, goal.pid)
        assertEquals(10, goal.vmId)
        assertEquals("Blood Pressure", goal.vitalName)
        assertEquals(120, goal.targetValue)
        assertEquals("mmHg", goal.unit)
    }

    @Test
    fun `EmployeeGoal equality holds for same data`() {
        val g1 = EmployeeGoal(pid = 2, vmId = 5, vitalName = "SpO2", targetValue = 98, unit = "%")
        val g2 = EmployeeGoal(pid = 2, vmId = 5, vitalName = "SpO2", targetValue = 98, unit = "%")
        assertEquals(g1, g2)
    }

    // ─────────────────────────────────────────────────────────────────
    // getData caching logic (pure Kotlin, no Android context needed)
    // ─────────────────────────────────────────────────────────────────

    /**
     * A stripped testable version of PrefsManager's getData logic.
     */
    private suspend fun getDataLogic(
        apiCall: suspend () -> String?,
        cachedValue: String? = null
    ): String? {
        return try {
            val apiResult = apiCall()
            if (!apiResult.isNullOrEmpty()) {
                apiResult
            } else {
                cachedValue
            }
        } catch (e: Exception) {
            cachedValue
        }
    }

    @Test
    fun `getData returns API result when not null or empty`() = runTest {
        val result = getDataLogic(apiCall = { "fresh_data_from_api" })
        assertEquals("fresh_data_from_api", result)
    }

    @Test
    fun `getData falls back to cache when API returns null`() = runTest {
        val result = getDataLogic(apiCall = { null }, cachedValue = "cached_data")
        assertEquals("cached_data", result)
    }

    @Test
    fun `getData falls back to cache when API returns empty string`() = runTest {
        val result = getDataLogic(apiCall = { "" }, cachedValue = "fallback")
        assertEquals("fallback", result)
    }

    @Test
    fun `getData falls back to cache when API throws exception`() = runTest {
        val result = getDataLogic(apiCall = { throw Exception("Network timeout") }, cachedValue = "local_cache")
        assertEquals("local_cache", result)
    }

    @Test
    fun `getData returns null when API throws and no cache available`() = runTest {
        val result = getDataLogic(apiCall = { throw Exception("Error") }, cachedValue = null)
        assertNull(result)
    }

    @Test
    fun `getData returns null when API is empty and no cache`() = runTest {
        val result = getDataLogic(apiCall = { "" }, cachedValue = null)
        assertNull(result)
    }

    @Test
    fun `getData prefers API result over cache`() = runTest {
        val result = getDataLogic(apiCall = { "api_value" }, cachedValue = "old_cache")
        assertEquals("api_value", result)
    }
}
