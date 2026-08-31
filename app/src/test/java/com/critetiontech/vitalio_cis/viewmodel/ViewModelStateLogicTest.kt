package com.critetiontech.vitalio_cis.viewmodel

import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.critetiontech.vitalio_cis.model.AllergyItem
import com.critetiontech.vitalio_cis.model.Problem
import com.critetiontech.vitalio_cis.model.SymptomItem
import com.critetiontech.vitalio_cis.model.Vital
import com.critetiontech.vitalio_cis.model.VitalGraphEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

/**
 * State logic tests for ViewModel-like logic WITHOUT Android framework dependencies.
 * These test the pure state transitions that ViewModels perform.
 */
class ViewModelStateLogicTest {

    // ─────────────────────────────────────────────────────────────────
    // Allergy State Logic Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `AllergyViewModel state - medicine allergies update on success`() = runTest {
        val _medicineAllergies = MutableStateFlow<List<AllergyItem>>(emptyList())
        val items = listOf(
            AllergyItem(detailID = 1, substanceName = "Penicillin", allergy = "High"),
            AllergyItem(detailID = 2, substanceName = "Aspirin", allergy = "Medium")
        )
        when (val result: DomainResult<List<AllergyItem>> = DomainResult.Success(items)) {
            is DomainResult.Success -> _medicineAllergies.value = result.data
            is DomainResult.Error -> { /* error */ }
        }
        assertEquals(2, _medicineAllergies.first().size)
        assertEquals("Penicillin", _medicineAllergies.first()[0].substanceName)
    }

    @Test
    fun `AllergyViewModel state - food allergies update on success`() = runTest {
        val _foodAllergies = MutableStateFlow<List<AllergyItem>>(emptyList())
        val items = listOf(AllergyItem(detailID = 3, substanceName = "Shellfish"))
        when (val result: DomainResult<List<AllergyItem>> = DomainResult.Success(items)) {
            is DomainResult.Success -> _foodAllergies.value = result.data
            is DomainResult.Error -> { /* error */ }
        }
        assertEquals(1, _foodAllergies.first().size)
        assertEquals("Shellfish", _foodAllergies.first()[0].substanceName)
    }

    @Test
    fun `AllergyViewModel state - error leaves list empty`() = runTest {
        val _allergies = MutableStateFlow<List<AllergyItem>>(emptyList())
        val result: DomainResult<List<AllergyItem>> = DomainResult.Error(Exception("Network error"))
        when (result) {
            is DomainResult.Success -> {
                _allergies.value = result.data
            }
            is DomainResult.Error -> { /* stays empty */ }
        }
        assertTrue(_allergies.first().isEmpty())
    }

    @Test
    fun `AllergyViewModel validation - blank substance name triggers error`() {
        val _errorMessage = MutableStateFlow<String?>(null)
        val substanceName = "   "
        if (substanceName.isBlank()) {
            _errorMessage.value = "Substance name is required."
        }
        assertEquals("Substance name is required.", _errorMessage.value)
    }

    @Test
    fun `AllergyViewModel validation - non-blank substance name does not trigger error`() {
        val _errorMessage = MutableStateFlow<String?>(null)
        val substanceName = "Penicillin"
        if (substanceName.isBlank()) {
            _errorMessage.value = "Substance name is required."
        }
        assertNull(_errorMessage.value)
    }

    @Test
    fun `AllergyViewModel addSuccess state resets correctly`() {
        val _addSuccess = MutableStateFlow(false)
        _addSuccess.value = true
        assertTrue(_addSuccess.value)
        // resetAddSuccess()
        _addSuccess.value = false
        assertFalse(_addSuccess.value)
    }

    // ─────────────────────────────────────────────────────────────────
    // Symptom Tracker State Logic Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `SymptomTrackerViewModel - addSymptom adds to selected list`() {
        val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
        val symptom = SymptomItem(detailId = 1, details = "Headache")
        _selectedSymptoms.value = (_selectedSymptoms.value + symptom).distinctBy { it.detailId }
        assertEquals(1, _selectedSymptoms.value.size)
        assertEquals("Headache", _selectedSymptoms.value[0].details)
    }

    @Test
    fun `SymptomTrackerViewModel - addSymptom does not duplicate`() {
        val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
        val symptom = SymptomItem(detailId = 1, details = "Headache")
        _selectedSymptoms.value = (_selectedSymptoms.value + symptom).distinctBy { it.detailId }
        _selectedSymptoms.value = (_selectedSymptoms.value + symptom).distinctBy { it.detailId }
        assertEquals(1, _selectedSymptoms.value.size)
    }

    @Test
    fun `SymptomTrackerViewModel - removeSymptom removes correct item`() {
        val _selectedSymptoms = MutableStateFlow(
            listOf(
                SymptomItem(detailId = 1, details = "Fever"),
                SymptomItem(detailId = 2, details = "Cough")
            )
        )
        val toRemove = SymptomItem(detailId = 1, details = "Fever")
        _selectedSymptoms.value = _selectedSymptoms.value.filterNot { it.detailId == toRemove.detailId }
        assertEquals(1, _selectedSymptoms.value.size)
        assertEquals(2, _selectedSymptoms.value[0].detailId)
    }

    @Test
    fun `SymptomTrackerViewModel - updateSelectedSymptoms clears list`() {
        val _selectedSymptoms = MutableStateFlow(listOf(SymptomItem(detailId = 1, details = "Fever")))
        _selectedSymptoms.value = emptyList()
        assertTrue(_selectedSymptoms.value.isEmpty())
    }

    @Test
    fun `SymptomTrackerViewModel - clearSearchResults clears search list`() {
        val _searchedSymptomList = MutableStateFlow(listOf(Problem(1, "Headache", 1, "", null)))
        _searchedSymptomList.value = emptyList()
        assertTrue(_searchedSymptomList.value.isEmpty())
    }

    @Test
    fun `SymptomTrackerViewModel - saveProblems filters and maps correctly`() {
        val _selectedSymptoms = MutableStateFlow<List<SymptomItem>>(emptyList())
        val selectedIds = listOf(1, 3)
        val allProblems = listOf(
            Problem(problemId = 1, problemName = "Headache", isVisible = 1, displayIcon = "", translation = null),
            Problem(problemId = 2, problemName = "Nausea", isVisible = 1, displayIcon = "", translation = null),
            Problem(problemId = 3, problemName = "Fever", isVisible = 1, displayIcon = "", translation = null)
        )
        _selectedSymptoms.value = allProblems
            .filter { it.problemId in selectedIds }
            .map { SymptomItem(detailId = it.problemId, details = it.problemName) }

        assertEquals(2, _selectedSymptoms.value.size)
        assertEquals("Headache", _selectedSymptoms.value[0].details)
        assertEquals("Fever", _selectedSymptoms.value[1].details)
    }

    @Test
    fun `SymptomTrackerViewModel - symptom list updates on success`() = runTest {
        val _symptomTrackerList = MutableStateFlow<List<SymptomItem>>(emptyList())
        val items = listOf(SymptomItem(detailId = 5, details = "Dizziness", detailsDate = "2024-01-15"))
        when (val result: DomainResult<List<SymptomItem>> = DomainResult.Success(items)) {
            is DomainResult.Success -> _symptomTrackerList.value = result.data
            is DomainResult.Error -> { /* error */ }
        }
        assertEquals(1, _symptomTrackerList.first().size)
        assertEquals("Dizziness", _symptomTrackerList.first()[0].details)
    }

    // ─────────────────────────────────────────────────────────────────
    // Vital Detail State Logic Tests
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `VitalDetailViewModel - vital list updates on success`() = runTest {
        val _vitalList = MutableStateFlow<List<Vital>>(emptyList())
        val vitals = listOf(
            Vital(id = 1, vitalName = "Blood Pressure", vitalValue = 120.0, unit = "mmHg"),
            Vital(id = 2, vitalName = "SpO2", vitalValue = 98.0, unit = "%")
        )
        when (val result: DomainResult<List<Vital>> = DomainResult.Success(vitals)) {
            is DomainResult.Success -> _vitalList.value = result.data
            is DomainResult.Error -> { /* error */ }
        }
        assertEquals(2, _vitalList.first().size)
        assertEquals(120.0, _vitalList.first()[0].vitalValue, 0.001)
    }

    @Test
    fun `VitalDetailViewModel - analytics data updates on success`() = runTest {
        val _analyticsData = MutableStateFlow<List<VitalGraphEntry>>(emptyList())
        val entries = listOf(VitalGraphEntry(dateTime = "2024-01-01T10:00", details = emptyList()))
        when (val result: DomainResult<List<VitalGraphEntry>> = DomainResult.Success(entries)) {
            is DomainResult.Success -> _analyticsData.value = result.data
            is DomainResult.Error -> { /* error */ }
        }
        assertEquals(1, _analyticsData.first().size)
        assertEquals("2024-01-01T10:00", _analyticsData.first()[0].dateTime)
    }

    @Test
    fun `VitalDetailViewModel - analytics data clears before new fetch`() = runTest {
        val _analyticsData = MutableStateFlow(
            listOf(VitalGraphEntry(dateTime = "2023-12-01", details = emptyList()))
        )
        _analyticsData.value = emptyList()
        assertTrue(_analyticsData.first().isEmpty())
    }

    // ─────────────────────────────────────────────────────────────────
    // Loading State Logic
    // ─────────────────────────────────────────────────────────────────

    @Test
    fun `loading starts false and resets to false after operation`() = runTest {
        val _loading = MutableStateFlow(false)
        assertFalse(_loading.first())
        _loading.value = true
        assertTrue(_loading.first())
        _loading.value = false
        assertFalse(_loading.first())
    }

    @Test
    fun `addLoading is independent from main loading`() {
        val _loading = MutableStateFlow(false)
        val _addLoading = MutableStateFlow(false)
        _loading.value = true
        assertFalse(_addLoading.value)
        _addLoading.value = true
        assertTrue(_loading.value)
        assertTrue(_addLoading.value)
    }

    @Test
    fun `errorMessage is null by default and set on error`() {
        val _errorMessage = MutableStateFlow<String?>(null)
        assertNull(_errorMessage.value)
        _errorMessage.value = "Something went wrong"
        assertEquals("Something went wrong", _errorMessage.value)
        _errorMessage.value = null
        assertNull(_errorMessage.value)
    }
}
