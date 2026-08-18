package com.critetiontech.vitalio_cis.model

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for appointment, medicine, and doctor-related models.
 */
class AppointmentAndMedicineModelTest {

    // ─────────────────────────────────────────────────────────────────
    // AppointmentResponse model tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `UpcomingAppointment default values are correct`() {
        val appt = UpcomingAppointment()
        assertEquals(0, appt.appointmentId)
        assertEquals(0, appt.doctorId)
        assertEquals("", appt.doctorName)
        assertEquals("", appt.qualification)
        assertEquals("", appt.departmentName)
        assertEquals("", appt.profileImage)
        assertEquals("", appt.clinicName)
        assertEquals("", appt.appointmentDate)
        assertEquals("", appt.slotTime)
    }

    @Test
    fun `UpcomingAppointment stores complete appointment data`() {
        val appt = UpcomingAppointment(
            appointmentId = 101,
            doctorId = 20,
            doctorName = "Dr. Kumar",
            qualification = "MBBS, MD",
            departmentName = "Neurology",
            profileImage = "https://example.com/image.jpg",
            clinicName = "City Hospital",
            appointmentDate = "2024-02-25",
            slotTime = "09:00"
        )
        assertEquals(101, appt.appointmentId)
        assertEquals("Dr. Kumar", appt.doctorName)
        assertEquals("Neurology", appt.departmentName)
        assertEquals("City Hospital", appt.clinicName)
        assertEquals("09:00", appt.slotTime)
    }

    @Test
    fun `UpcomingAppointment equality holds for same data`() {
        val a1 = UpcomingAppointment(appointmentId = 5, doctorName = "Dr. Smith")
        val a2 = UpcomingAppointment(appointmentId = 5, doctorName = "Dr. Smith")
        assertEquals(a1, a2)
    }

    @Test
    fun `UpcomingAppointment copy creates independent instance`() {
        val original = UpcomingAppointment(appointmentId = 10, doctorName = "Dr. A")
        val copy = original.copy(doctorName = "Dr. B")
        assertEquals("Dr. A", original.doctorName)
        assertEquals("Dr. B", copy.doctorName)
        assertEquals(10, copy.appointmentId)
    }

    // ─────────────────────────────────────────────────────────────────
    // PendingMedicine model tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `PendingMedicine default values are correct`() {
        val med = PendingMedicine()
        assertEquals(0, med.pid)
        assertFalse(med.isTaken)
        assertEquals(0, med.medicineId)
        assertEquals("", med.medicineName)
        assertEquals("", med.frequency)
        assertEquals("", med.instructions)
    }

    @Test
    fun `PendingMedicine taken flag can be set true`() {
        val med = PendingMedicine(pid = 1, isTaken = true, medicineName = "Metformin")
        assertTrue(med.isTaken)
    }

    @Test
    fun `PendingMedicine copy works correctly`() {
        val med = PendingMedicine(pid = 3, medicineName = "Aspirin", isTaken = false)
        val taken = med.copy(isTaken = true)
        assertFalse(med.isTaken)
        assertTrue(taken.isTaken)
    }

    // ─────────────────────────────────────────────────────────────────
    // MedicineIntakeResponse model (from model/MedicineIntakeResponse.kt)
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `DashboardData with all populated lists`() {
        val vital = Vital(id = 1, vitalName = "Pulse", vitalValue = 75.0, unit = "bpm")
        val appt = UpcomingAppointment(appointmentId = 1, doctorName = "Dr. Lee")
        val med = PendingMedicine(pid = 1, medicineName = "Paracetamol")
        val data = DashboardData(
            latestVitals = listOf(vital),
            upcomingAppointments = listOf(appt),
            pendingMedicines = listOf(med)
        )
        assertEquals(1, data.latestVitals.size)
        assertEquals(1, data.upcomingAppointments.size)
        assertEquals(1, data.pendingMedicines.size)
        assertEquals("Pulse", data.latestVitals[0].vitalName)
        assertEquals("Dr. Lee", data.upcomingAppointments[0].doctorName)
        assertEquals("Paracetamol", data.pendingMedicines[0].medicineName)
    }

    @Test
    fun `DashboardResponse equality for same data`() {
        val r1 = DashboardResponse(status = 1, message = "OK", responseValue = DashboardData())
        val r2 = DashboardResponse(status = 1, message = "OK", responseValue = DashboardData())
        assertEquals(r1, r2)
    }
}
