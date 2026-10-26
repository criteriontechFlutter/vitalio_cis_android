package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.Problem
import com.critetiontech.ctvitalio.model.SymptomItem
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SymptomSelectionUiState(
    val iconsLoading: Boolean = true,
    val iconsError: Boolean = false,
    val icons: List<Problem> = emptyList(),
    val query: String = "",
    val searching: Boolean = false,
    val suggestions: List<Problem> = emptyList(),
    /** In the order they were picked, like the chips row shows them. */
    val selected: List<SymptomItem> = emptyList(),
    val saving: Boolean = false,
) {
    fun isSelected(problemId: Int) = selected.any { it.detailId == problemId }
}

/** "Highlight any other symptoms": icon grid, search and the selected-symptoms chips. */
class SymptomSelectionViewModel : ViewModel() {

    private val deps = AppDependencies

    private val _state = MutableStateFlow(SymptomSelectionUiState())
    val state: StateFlow<SymptomSelectionUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message

    private var searchJob: Job? = null

    init { loadIcons() }

    fun loadIcons() {
        viewModelScope.launch {
            _state.update { it.copy(iconsLoading = true, iconsError = false) }
            when (val r = deps.getProblemsWithIcon()()) {
                is DomainResult.Success -> _state.update { it.copy(iconsLoading = false, icons = r.data) }
                is DomainResult.Error -> {
                    Log.e("SymptomSelectionVM", r.exception.message.orEmpty())
                    _state.update { it.copy(iconsLoading = false, iconsError = it.icons.isEmpty()) }
                }
            }
        }
    }

    fun onQueryChange(query: String) {
        _state.update { it.copy(query = query) }
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.update { it.copy(suggestions = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300) // wait for the patient to stop typing
            _state.update { it.copy(searching = true) }
            val suggestions = when (val r = deps.getAllProblems()(query.trim())) {
                is DomainResult.Success -> r.data
                is DomainResult.Error -> { Log.e("SymptomSelectionVM", r.exception.message.orEmpty()); emptyList() }
            }
            _state.update { it.copy(searching = false, suggestions = suggestions) }
        }
    }

    /** Grid tap: picks or un-picks [problem]. */
    fun toggle(problem: Problem) {
        _state.update { s ->
            if (s.isSelected(problem.problemId)) s.copy(selected = s.selected.filterNot { it.detailId == problem.problemId })
            else s.copy(selected = s.selected + problem.toSymptom())
        }
    }

    /** Search result tap: picks [problem] and clears the search. */
    fun pickSuggestion(problem: Problem) {
        searchJob?.cancel()
        _state.update { s ->
            s.copy(
                query = "",
                suggestions = emptyList(),
                searching = false,
                selected = if (s.isSelected(problem.problemId)) s.selected else s.selected + problem.toSymptom(),
            )
        }
    }

    fun remove(symptom: SymptomItem) {
        _state.update { s -> s.copy(selected = s.selected.filterNot { it.detailId == symptom.detailId }) }
    }

    fun save() {
        val s = _state.value
        if (s.saving || s.selected.isEmpty()) return
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.emit("Your session has expired. Please log in again.")
                return@launch
            }
            _state.update { it.copy(saving = true) }
            when (val r = deps.saveSymptoms()(patient.uhId, patient.clientId, s.selected)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _state.update { it.copy(selected = emptyList()) }
                    _message.emit("Symptoms Added Successfully!")
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.emit("Couldn't save your symptoms. Please try again.")
                    Log.e("SymptomSelectionVM", r.exception.message.orEmpty())
                }
            }
            _state.update { it.copy(saving = false) }
        }
    }

    // Dated when saved, so the backend records it as now.
    private fun Problem.toSymptom() = SymptomItem(detailId = problemId, details = problemName, isStillFeeling = true)
}
