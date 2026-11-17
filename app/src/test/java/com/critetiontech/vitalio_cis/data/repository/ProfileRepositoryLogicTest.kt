package com.critetiontech.ctvitalio.data.repository

import com.critetiontech.ctvitalio.utils.Patient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class ProfileRepositoryLogicTest {

    /** What login saves: zip always "", countryId always 0. */
    private fun loggedIn(zip: String = "", countryId: Int = 0, cityName: String = "") = Patient(
        pid = 7, uhId = "UHID-TEST", firstName = "Test", lastName = "User",
        dob = "2000-01-01", genderId = 1, countryCallingCode = "",
        mobileNo = "9000000000", emailAddress = "", age = "26",
        guardianRelationId = 0, guardianName = "", countryId = countryId, stateId = 38, cityId = 4933,
        address = "", imageUrl = "", bloodGroupId = 1, zip = zip, isActive = true, clientId = 45,
        departmentName = null, cityName = cityName, stateName = "", countryName = "",
        genderName = "Male", guardianRelationName = null, bloodGroupName = "O+"
    )

    // Shape of GetPatientDetailsByMobileNo (values made up)
    private val server = """{"status":1,"responseValue":[{"zip":"110001","countryId":101,"stateId":38,"cityId":4933,
        "countryName":"India","stateName":"Uttar Pradesh","cityName":"Lucknow","countryCallingCode":"91"}]}"""

    @Test fun `fills zip and location that login left blank`() {
        val p = ProfileRepositoryImpl.mergeServerDetails(loggedIn(), server)
        assertEquals("110001", p.zip)
        assertEquals(101, p.countryId)
        assertEquals("India", p.countryName)
        assertEquals("Lucknow", p.cityName)
        assertEquals("91", p.countryCallingCode)
    }

    @Test fun `never overwrites values the user already has`() {
        val p = ProfileRepositoryImpl.mergeServerDetails(loggedIn(zip = "226010", countryId = 231, cityName = "Kanpur"), server)
        assertEquals("226010", p.zip)
        assertEquals(231, p.countryId)
        assertEquals("Kanpur", p.cityName)
    }

    @Test fun `bad or empty answers leave the patient unchanged`() {
        val local = loggedIn()
        assertSame(local, ProfileRepositoryImpl.mergeServerDetails(local, ""))
        assertSame(local, ProfileRepositoryImpl.mergeServerDetails(local, """{"status":0,"responseValue":"No record found"}"""))
        assertSame(local, ProfileRepositoryImpl.mergeServerDetails(local, """{"status":1,"responseValue":[]}"""))
    }

    @Test fun `reads the server's validation message`() {
        // Exact 400 body the live API sent on 17 Nov 2026 for a photo upload with empty Zip
        val body = """{"errors":{"Zip":["The Zip field is required."]},"type":"https://tools.ietf.org/html/rfc7231#section-6.5.1","title":"One or more validation errors occurred.","status":400}"""
        assertEquals("The Zip field is required.", ProfileRepositoryImpl.validationMessage(body))
        assertNull(ProfileRepositoryImpl.validationMessage("Server error 400"))
        assertNull(ProfileRepositoryImpl.validationMessage("""{"status":0,"message":"failure"}"""))
    }
}
