package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.Disease
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.model.FamilyHealthCondition
import com.critetiontech.ctvitalio.domain.model.FamilyRelation
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FamilyHealthHistoryUiState(
    val loading: Boolean = true,
    /** Set when the list couldn't be loaded (as opposed to the patient having none). */
    val error: String? = null,
    val conditions: List<FamilyHealthCondition> = emptyList(),
    /** True while an add or remove is with the server. */
    val saving: Boolean = false,
)

class FamilyHealthHistoryViewModel : ViewModel() {

    private val deps = AppDependencies

    private val _state = MutableStateFlow(FamilyHealthHistoryUiState())
    val state: StateFlow<FamilyHealthHistoryUiState> = _state.asStateFlow()

    private val _message = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val message: SharedFlow<String> = _message

    /** Fired once an add or remove went through, so the screen can close its sheet or dialog. */
    private val _done = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val done: SharedFlow<Unit> = _done

    private val _diseases = MutableStateFlow<List<Disease>>(emptyList())
    val diseases: StateFlow<List<Disease>> = _diseases.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val patient = deps.prefs.getPatient() ?: run {
                _state.update { it.copy(loading = false, error = MyApplication.appContext.getString(R.string.profile_session_expired_long)) }
                return@launch
            }
            when (val r = deps.getFamilyHealthHistory()(patient.pid, patient.clientId)) {
                is DomainResult.Success -> _state.update { it.copy(loading = false, conditions = r.data) }
                is DomainResult.Error -> {
                    Log.e("FamilyHealthVM", r.exception.message.orEmpty())
                    _state.update { it.copy(loading = false, error = MyApplication.appContext.getString(R.string.profile_check_connection)) }
                }
            }
        }
    }

    /** Loads the disease list for the add sheet's search (once; the repository keeps it). */
    fun loadDiseases() {
        if (_diseases.value.isNotEmpty()) return
        viewModelScope.launch {
            when (val r = deps.familyHealthHistoryRepository.fetchDiseases()) {
                is DomainResult.Success -> _diseases.value = r.data
                is DomainResult.Error -> {
                    Log.e("FamilyHealthVM", r.exception.message.orEmpty())
                    _message.emit(MyApplication.appContext.getString(R.string.profile_disease_list_load_failed))
                }
            }
        }
    }

    /** Records [relations] for every disease in [diseases]; the API takes one disease per call. */
    fun add(diseases: List<Disease>, relations: Set<FamilyRelation>) {
        if (_state.value.saving || diseases.isEmpty()) return
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.emit(MyApplication.appContext.getString(R.string.profile_session_expired_long))
                return@launch
            }
            _state.update { it.copy(saving = true) }
            val conditions = _state.value.conditions
            val failed = diseases.filter { disease ->
                val existing = conditions.firstOrNull { it.diseaseId == disease.id }
                val r = deps.addFamilyHealthHistory()(patient.pid, patient.clientId, disease.id, relations, existing)
                if (r is DomainResult.Error) Log.e("FamilyHealthVM", "${disease.name}: ${r.exception.message}")
                r is DomainResult.Error
            }
            _state.update { it.copy(saving = false) }
            when {
                failed.isEmpty() -> {
                    Haptics.success()
                    _message.emit(MyApplication.appContext.getString(R.string.profile_family_history_added))
                    _done.emit(Unit)
                }
                failed.size == diseases.size -> {
                    Haptics.error()
                    _message.emit(MyApplication.appContext.getString(R.string.profile_save_failed))
                }
                // Some went through: close the sheet and say which didn't.
                else -> {
                    Haptics.error()
                    _message.emit(MyApplication.appContext.getString(R.string.profile_save_some_failed, failed.joinToString { it.name }))
                    _done.emit(Unit)
                }
            }
            if (failed.size < diseases.size) load()
        }
    }

    fun remove(condition: FamilyHealthCondition) {
        if (_state.value.saving) return
        viewModelScope.launch {
            val patient = deps.prefs.getPatient() ?: run {
                _message.emit(MyApplication.appContext.getString(R.string.profile_session_expired_long))
                return@launch
            }
            _state.update { it.copy(saving = true) }
            when (val r = deps.removeFamilyHealthHistory()(condition, patient.pid, patient.clientId)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _state.update { s -> s.copy(conditions = s.conditions.filterNot { it.diseaseId == condition.diseaseId }) }
                    _message.emit(MyApplication.appContext.getString(R.string.profile_removed))
                    _done.emit(Unit)
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    Log.e("FamilyHealthVM", r.exception.message.orEmpty())
                    _message.emit(
                        if (condition.recordId == null) MyApplication.appContext.getString(R.string.profile_entry_cannot_remove)
                        else MyApplication.appContext.getString(R.string.profile_remove_failed)
                    )
                }
            }
            _state.update { it.copy(saving = false) }
        }
    }
}
