package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** My Diet Box: every saved plan, with one marked active. */
class DietBoxViewModel : ViewModel() {

    private val deps = AppDependencies
    private val repo = deps.dietRepository
    private val prefs = deps.prefs

    private val _plans = MutableStateFlow<List<DietPlan>>(emptyList())
    val plans: StateFlow<List<DietPlan>> = _plans.asStateFlow()

    private val _activeId = MutableStateFlow<Int?>(null)
    val activeId: StateFlow<Int?> = _activeId.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun load() {
        val patient = prefs.getPatient() ?: return
        setPlans(repo.cachedDietPlans(patient.pid))
        viewModelScope.launch {
            _loading.value = _plans.value.isEmpty()
            when (val r = deps.fetchDietPlans()(patient.pid, patient.clientId)) {
                is DomainResult.Success -> setPlans(r.data)
                is DomainResult.Error -> Log.e("DietBoxVM", r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    private fun setPlans(list: List<DietPlan>) {
        val pid = prefs.getPatient()?.pid ?: return
        // Newest first; with no choice made yet, the newest plan is the one the checklist uses.
        _plans.value = list.sortedByDescending { it.id }
        _activeId.value = repo.activePlanId(pid)?.takeIf { id -> list.any { it.id == id } } ?: _plans.value.firstOrNull()?.id
    }

    fun setActive(planId: Int, active: Boolean) {
        val pid = prefs.getPatient()?.pid ?: return
        val id = if (active) planId else null
        repo.setActivePlanId(pid, id)
        _activeId.value = id
    }

    fun createdAt(planId: Int): Long? = prefs.getPatient()?.pid?.let { repo.planCreatedAt(it, planId) }
}
