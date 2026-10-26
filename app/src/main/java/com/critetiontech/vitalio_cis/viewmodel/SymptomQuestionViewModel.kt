package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.SymptomToReview
import com.critetiontech.ctvitalio.model.SymptomItem
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SymptomQuestionUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val questions: List<SymptomToReview> = emptyList(),
    val currentIndex: Int = 0,
    /** True while the answers are being saved after the last question. */
    val saving: Boolean = false,
) {
    val current: SymptomToReview? get() = questions.getOrNull(currentIndex)
}

/** "Do you still have …?" walkthrough over the patient's recorded symptoms. */
class SymptomQuestionViewModel : ViewModel() {

    private val deps = AppDependencies

    private val _state = MutableStateFlow(SymptomQuestionUiState())
    val state: StateFlow<SymptomQuestionUiState> = _state.asStateFlow()

    /** Fired once every answer is saved; the screen moves on to the tracker. */
    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message

    /** Per symptom: every visit row with the patient's answer, ready to save. */
    private val answers = linkedMapOf<Int, List<SymptomItem>>()

    init { load() }

    fun load() {
        viewModelScope.launch {
            answers.clear()
            _state.value = SymptomQuestionUiState(loading = true)
            val patient = deps.prefs.getPatient() ?: run {
                _state.value = SymptomQuestionUiState(loading = false, error = "Your session has expired. Please log in again.")
                return@launch
            }
            _state.value = when (val r = deps.getSymptomsToReview()(patient.uhId, patient.clientId.toString())) {
                is DomainResult.Success -> SymptomQuestionUiState(loading = false, questions = r.data)
                is DomainResult.Error -> {
                    Log.e("SymptomQuestionVM", r.exception.message.orEmpty())
                    SymptomQuestionUiState(loading = false, error = "Check your connection and try again.")
                }
            }
        }
    }

    /** Records the answer (No → isStillFeeling false) for the current symptom; after the last one, saves them all. */
    fun answer(stillFeeling: Boolean) {
        val s = _state.value
        val question = s.current ?: return
        if (s.saving) return
        answers[question.detailId] = question.records.map { it.copy(isStillFeeling = stillFeeling) }

        if (s.currentIndex < s.questions.lastIndex) {
            _state.update { it.copy(currentIndex = it.currentIndex + 1) }
            return
        }

        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.emit("Your session has expired. Please log in again.")
                return@launch
            }
            _state.update { it.copy(saving = true) }
            when (val r = deps.saveSymptoms()(patient.uhId, patient.clientId, answers.values.flatten())) {
                is DomainResult.Success -> { Haptics.success(); _completed.emit(Unit) }
                is DomainResult.Error -> {
                    Haptics.error()
                    _message.emit("Couldn't update your symptoms. Please try again.")
                    Log.e("SymptomQuestionVM", r.exception.message.orEmpty())
                }
            }
            _state.update { it.copy(saving = false) }
        }
    }
}
