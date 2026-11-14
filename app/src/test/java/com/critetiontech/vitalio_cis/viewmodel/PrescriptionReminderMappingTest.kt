package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.reminder.SubscriptionRenewalReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PrescriptionReminderMappingTest {

    private fun slots(f: String?) = PrescriptionDetailViewModel.timeSlotsFor(f)

    @Test
    fun `once a day maps to one morning slot`() {
        assertEquals("""[{"timeSlot":"08:00"}]""", slots("Once a Day (24 Hourly)"))
    }

    @Test
    fun `twice a day maps to morning and night`() {
        assertEquals("""[{"timeSlot":"08:00"}, {"timeSlot":"21:00"}]""", slots("Twice a Day (12 Hourly)"))
    }

    @Test
    fun `thrice and four times a day`() {
        assertEquals(3, Regex("timeSlot").findAll(slots("Thrice a Day (8 Hourly)")).count())
        assertEquals(4, Regex("timeSlot").findAll(slots("Four times a Day (6 Hourly)")).count())
    }

    @Test
    fun `unknown or missing frequency falls back to one morning slot`() {
        assertEquals("""[{"timeSlot":"08:00"}]""", slots(null))
        assertEquals("""[{"timeSlot":"08:00"}]""", slots("As directed"))
    }

    @Test
    fun `duration is read in days with a one-week fallback`() {
        assertEquals(2L, PrescriptionDetailViewModel.durationDays("2"))
        assertEquals(10L, PrescriptionDetailViewModel.durationDays(" 10 "))
        assertEquals(7L, PrescriptionDetailViewModel.durationDays("0"))
        assertEquals(7L, PrescriptionDetailViewModel.durationDays(null))
        assertEquals(7L, PrescriptionDetailViewModel.durationDays("abc"))
    }

    @Test
    fun `renewal date in the API's format is parsed`() {
        assertEquals(LocalDate.of(2026, 12, 12), SubscriptionRenewalReminder.parseRenewalDate("12 Dec 2026"))
        assertEquals(LocalDate.of(2026, 11, 2), SubscriptionRenewalReminder.parseRenewalDate("2 Nov 2026"))
        assertNull(SubscriptionRenewalReminder.parseRenewalDate(""))
        assertNull(SubscriptionRenewalReminder.parseRenewalDate("2026-12-12"))
    }
}
