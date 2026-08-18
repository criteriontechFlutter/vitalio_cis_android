package com.critetiontech.vitalio_cis.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for all data model classes.
 * Tests equality, default values, copy, and data integrity.
 */
class ModelTest {

    // ─────────────────────────────────────────────────────────────────
    // AllergyItem tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `AllergyItem default values are correct`() {
        val item = AllergyItem()
        assertEquals(0, item.detailID)
        assertEquals("", item.detailsDate)
        assertEquals("", item.details)
        assertEquals("", item.substanceName)
        assertEquals("", item.allergy)
        assertEquals("", item.reaction)
    }

    @Test
    fun `AllergyItem equality works for same data`() {
        val a = AllergyItem(detailID = 1, substanceName = "Penicillin", allergy = "High", reaction = "Rash")
        val b = AllergyItem(detailID = 1, substanceName = "Penicillin", allergy = "High", reaction = "Rash")
        assertEquals(a, b)
    }

    @Test
    fun `AllergyItem copy creates independent instance`() {
        val original = AllergyItem(detailID = 10, substanceName = "Dust")
        val copy = original.copy(substanceName = "Pollen")
        assertEquals(10, copy.detailID)
        assertEquals("Pollen", copy.substanceName)
        assertEquals("Dust", original.substanceName)
    }

    @Test
    fun `AllergyApiResponse holds correct list`() {
        val items = listOf(
            AllergyItem(detailID = 1, substanceName = "Shellfish"),
            AllergyItem(detailID = 2, substanceName = "Nuts")
        )
        val response = AllergyApiResponse(status = 1, message = "OK", responseValue = items)
        assertEquals(1, response.status)
        assertEquals(2, response.responseValue.size)
        assertEquals("Shellfish", response.responseValue[0].substanceName)
    }

    // ─────────────────────────────────────────────────────────────────
    // Vital model tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `Vital default values are correct`() {
        val vital = Vital()
        assertEquals("", vital.uhid)
        assertEquals(0, vital.id)
        assertEquals("", vital.vitalName)
        assertEquals(0.0, vital.vitalValue, 0.001)
        assertEquals("", vital.unit)
    }

    @Test
    fun `Vital equality and copy work correctly`() {
        val v1 = Vital(id = 5, vitalName = "Blood Pressure", vitalValue = 120.0, unit = "mmHg")
        val v2 = v1.copy()
        assertEquals(v1, v2)
        assertNotSame(v1, v2)
    }

    @Test
    fun `VitalApiResponse holds correct structure`() {
        val vital = Vital(id = 1, vitalName = "SpO2", vitalValue = 98.0)
        val response = VitalApiResponse(
            status = 1,
            message = "Success",
            responseValue = VitalResponseValue(lastVital = listOf(vital))
        )
        assertEquals(1, response.responseValue.lastVital.size)
        assertEquals("SpO2", response.responseValue.lastVital[0].vitalName)
    }

    @Test
    fun `VitalGraphEntry holds date and details`() {
        val detail = VitalGraphDetail(vitalId = 3, vitalName = "Pulse", vitalValue = 72.0, vitalDate = "2024-01-15")
        val entry = VitalGraphEntry(dateTime = "2024-01-15T10:00:00", details = listOf(detail))
        assertEquals("2024-01-15T10:00:00", entry.dateTime)
        assertEquals(1, entry.details.size)
        assertEquals(72.0, entry.details[0].vitalValue, 0.001)
    }

    // ─────────────────────────────────────────────────────────────────
    // SymptomItem tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `SymptomItem has correct default detailsDate`() {
        val item = SymptomItem(detailId = 1, details = "Headache")
        assertEquals("", item.detailsDate)
        assertEquals(1, item.detailId)
        assertEquals("Headache", item.details)
    }

    @Test
    fun `SymptomApiResponse holds correct list`() {
        val symptoms = listOf(
            SymptomItem(detailId = 1, details = "Fever"),
            SymptomItem(detailId = 2, details = "Cough")
        )
        val response = SymptomApiResponse(status = 1, message = "OK", responseValue = symptoms)
        assertEquals(2, response.responseValue.size)
        assertEquals("Fever", response.responseValue[0].details)
    }

    // ─────────────────────────────────────────────────────────────────
    // Problem model tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `Problem data class stores fields correctly`() {
        val problem = Problem(problemId = 7, problemName = "Nausea", isVisible = 1, displayIcon = "nausea.png", translation = null)
        assertEquals(7, problem.problemId)
        assertEquals("Nausea", problem.problemName)
        assertEquals(1, problem.isVisible)
        assertNull(problem.translation)
    }

    @Test
    fun `ProblemResponse holds list of problems`() {
        val problems = listOf(
            Problem(problemId = 1, problemName = "Headache", isVisible = 1, displayIcon = "", translation = "सिरदर्द"),
            Problem(problemId = 2, problemName = "Fever", isVisible = 1, displayIcon = "", translation = null)
        )
        val response = ProblemResponse(responseCode = 200, responseMessage = "OK", responseValue = problems)
        assertEquals(200, response.responseCode)
        assertEquals(2, response.responseValue.size)
        assertEquals("सिरदर्द", response.responseValue[0].translation)
    }

    // ─────────────────────────────────────────────────────────────────
    // Dashboard models tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `DashboardResponse has correct defaults`() {
        val response = DashboardResponse()
        assertEquals(0, response.status)
        assertEquals("", response.message)
        assertTrue(response.responseValue.pendingMedicines.isEmpty())
        assertTrue(response.responseValue.latestVitals.isEmpty())
        assertTrue(response.responseValue.upcomingAppointments.isEmpty())
    }

    @Test
    fun `UpcomingAppointment stores doctor info correctly`() {
        val appt = UpcomingAppointment(
            appointmentId = 100,
            doctorName = "Dr. Smith",
            departmentName = "Cardiology",
            appointmentDate = "2024-02-20",
            slotTime = "10:30"
        )
        assertEquals(100, appt.appointmentId)
        assertEquals("Dr. Smith", appt.doctorName)
        assertEquals("Cardiology", appt.departmentName)
    }

    @Test
    fun `PendingMedicine stores medication info correctly`() {
        val medicine = PendingMedicine(
            pid = 1,
            isTaken = false,
            medicineId = 42,
            medicineName = "Metformin",
            frequency = "Twice daily",
            instructions = "After meals"
        )
        assertFalse(medicine.isTaken)
        assertEquals("Metformin", medicine.medicineName)
        assertEquals("After meals", medicine.instructions)
    }

    @Test
    fun `DashboardData with populated lists`() {
        val data = DashboardData(
            latestVitals = listOf(Vital(id = 1, vitalName = "Temperature", vitalValue = 98.6)),
            upcomingAppointments = listOf(UpcomingAppointment(appointmentId = 5)),
            pendingMedicines = listOf(PendingMedicine(pid = 1, medicineName = "Aspirin"))
        )
        assertEquals(1, data.latestVitals.size)
        assertEquals(1, data.upcomingAppointments.size)
        assertEquals(1, data.pendingMedicines.size)
        assertEquals(98.6, data.latestVitals[0].vitalValue, 0.001)
    }
}
