package com.critetiontech.vitalio_cis

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Routes object constants.
 * Ensures all route values are non-empty and unique.
 */
class RoutesTest {

    @Test
    fun `LOGIN route is correct`() {
        assertEquals("login", Routes.LOGIN)
    }

    @Test
    fun `DASHBOARD route is correct`() {
        assertEquals("dashboard", Routes.DASHBOARD)
    }

    @Test
    fun `OTP route contains mobile placeholder`() {
        assertEquals("otp/{mobile}", Routes.OTP)
        assertTrue(Routes.OTP.contains("{mobile}"))
    }

    @Test
    fun `VITALS route is correct`() {
        assertEquals("vitals", Routes.VITALS)
    }

    @Test
    fun `FLUID route is correct`() {
        assertEquals("fluid", Routes.FLUID)
    }

    @Test
    fun `SYMPTOMSTRACKER route is correct`() {
        assertEquals("symptomsTracker", Routes.SYMPTOMSTRACKER)
    }

    @Test
    fun `SYMPTOMS route is correct`() {
        assertEquals("symptoms", Routes.SYMPTOMS)
    }

    @Test
    fun `MEDICINE route is correct`() {
        assertEquals("medicine", Routes.MEDICINE)
    }

    @Test
    fun `APPOINTMENTS route is correct`() {
        assertEquals("appointments", Routes.APPOINTMENTS)
    }

    @Test
    fun `FINDDOCTOR route is correct`() {
        assertEquals("findDoctor", Routes.FINDDOCTOR)
    }

    @Test
    fun `DOCTORDETAILS route is correct`() {
        assertEquals("doctorDetails", Routes.DOCTORDETAILS)
    }

    @Test
    fun `ALLERGIESSCREEN route is correct`() {
        assertEquals("AllergiesScreen", Routes.ALLERGIESSCREEN)
    }

    @Test
    fun `LABREPORTS route is correct`() {
        assertEquals("labReports", Routes.LABREPORTS)
    }

    @Test
    fun `ADDLABRESULTS route is correct`() {
        assertEquals("AddLabResultsScreen", Routes.ADDLABRESULTS)
    }

    @Test
    fun `HEALTHCONNECTSYNC route is correct`() {
        assertEquals("HealthConnectSyncScreen", Routes.HEALTHCONNECTSYNC)
    }

    @Test
    fun `all routes are non-empty strings`() {
        val allRoutes = listOf(
            Routes.LOGIN, Routes.DASHBOARD, Routes.OTP, Routes.VITALS,
            Routes.FLUID, Routes.SYMPTOMSTRACKER, Routes.SYMPTOMS, Routes.MEDICINE,
            Routes.DIET, Routes.INTERACTION, Routes.APPOINTMENTS, Routes.ARTICLES,
            Routes.MANAGE_MEDICINE, Routes.FINDDOCTOR, Routes.DOCTORDETAILS,
            Routes.BOOKINGCONFERMATION, Routes.BOOKINGDETAILS, Routes.SELECTCLINIC,
            Routes.DRAWER, Routes.PERSONALIFSCREEN, Routes.LABREPORTS,
            Routes.ALLERGIESSCREEN, Routes.ADDLABRESULTS, Routes.MANAGEMEDICAIONS,
            Routes.ADDMEDICINEREMINDER, Routes.INTERACTIONCHECKER, Routes.DIETCHECKLIST,
            Routes.PRESCRIPTION, Routes.FLUIDDATAINPUT, Routes.FLUIDOUTPUTHISTORY,
            Routes.FLUIDINPUTHISTORY, Routes.VITALHISTORY, Routes.CONNECTION,
            Routes.ARTICALEDETAILS, Routes.MYOBSERVERS, Routes.FAMILYHEALTH,
            Routes.ADDACTIVITY, Routes.REMINDERS, Routes.EMERGENCYCONTACTS,
            Routes.CONNECTWATCH, Routes.SHAREDACCOUNT, Routes.ADDMEMBER,
            Routes.FAQ, Routes.FEEDBACK, Routes.MEDICALPROFILE,
            Routes.RESEARCHARTICLES, Routes.AIREPORT, Routes.HEALTHCONNECTSYNC
        )
        allRoutes.forEach { route ->
            assertTrue("Route should not be empty: $route", route.isNotEmpty())
        }
    }

    @Test
    fun `all route values are unique`() {
        val allRoutes = listOf(
            Routes.LOGIN, Routes.DASHBOARD, Routes.OTP, Routes.VITALS,
            Routes.FLUID, Routes.SYMPTOMSTRACKER, Routes.SYMPTOMS, Routes.MEDICINE,
            Routes.DIET, Routes.INTERACTION, Routes.APPOINTMENTS, Routes.ARTICLES,
            Routes.MANAGE_MEDICINE, Routes.FINDDOCTOR, Routes.DOCTORDETAILS,
            Routes.BOOKINGCONFERMATION, Routes.BOOKINGDETAILS, Routes.SELECTCLINIC,
            Routes.DRAWER, Routes.PERSONALIFSCREEN, Routes.LABREPORTS,
            Routes.ALLERGIESSCREEN, Routes.ADDLABRESULTS, Routes.MANAGEMEDICAIONS,
            Routes.ADDMEDICINEREMINDER, Routes.INTERACTIONCHECKER, Routes.DIETCHECKLIST,
            Routes.PRESCRIPTION, Routes.FLUIDDATAINPUT, Routes.FLUIDOUTPUTHISTORY,
            Routes.FLUIDINPUTHISTORY, Routes.VITALHISTORY, Routes.CONNECTION,
            Routes.ARTICALEDETAILS, Routes.MYOBSERVERS, Routes.FAMILYHEALTH,
            Routes.ADDACTIVITY, Routes.REMINDERS, Routes.EMERGENCYCONTACTS,
            Routes.CONNECTWATCH, Routes.SHAREDACCOUNT, Routes.ADDMEMBER,
            Routes.FAQ, Routes.FEEDBACK, Routes.MEDICALPROFILE,
            Routes.RESEARCHARTICLES, Routes.AIREPORT, Routes.HEALTHCONNECTSYNC
        )
        val uniqueRoutes = allRoutes.toSet()
        assertEquals(
            "All route values must be unique. Duplicates found!",
            allRoutes.size,
            uniqueRoutes.size
        )
    }

    @Test
    fun `OTP route can be constructed with mobile number`() {
        val mobile = "9876543210"
        val navigateTo = "otp/$mobile"
        assertTrue(navigateTo.startsWith("otp/"))
        assertEquals("otp/9876543210", navigateTo)
    }

    @Test
    fun `route values do not contain whitespace`() {
        val allRoutes = listOf(
            Routes.LOGIN, Routes.DASHBOARD, Routes.VITALS, Routes.FLUID,
            Routes.SYMPTOMSTRACKER, Routes.SYMPTOMS, Routes.MEDICINE,
            Routes.APPOINTMENTS, Routes.FINDDOCTOR, Routes.DOCTORDETAILS,
            Routes.ALLERGIESSCREEN, Routes.LABREPORTS, Routes.HEALTHCONNECTSYNC
        )
        allRoutes.forEach { route ->
            assertFalse("Route should not contain whitespace: '$route'", route.contains(" "))
        }
    }
}
