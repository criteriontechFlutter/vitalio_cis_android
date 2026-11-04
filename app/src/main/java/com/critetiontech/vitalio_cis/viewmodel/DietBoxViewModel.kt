package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** My Diet Box: every saved plan, with one marked active. */
class DietBoxViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    private val _plans = MutableStateFlow<List<DietPlan>>(emptyList())
    val plans: StateFlow<List<DietPlan>> = _plans.asStateFlow()

    private val _activeId = MutableStateFlow<Int?>(null)
    val activeId: StateFlow<Int?> = _activeId.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    fun load() {
        val patient = prefs.getPatient() ?: return
        viewModelScope.launch {
            _loading.value = _plans.value.isEmpty()
            _loadFailed.value = false
            when (val r = deps.fetchDietPlans()(patient.pid, patient.clientId)) {
                is DomainResult.Success -> setPlans(r.data)
                is DomainResult.Error -> {
                    Log.e("DietBoxVM", r.exception.message.orEmpty())
                    _loadFailed.value = _plans.value.isEmpty()
                }
            }
            _loading.value = false
        }
    }

    private fun setPlans(list: List<DietPlan>) {
        _plans.value = list.sortedByDescending { it.id }
        _activeId.value = list.filter { it.isActive }.maxByOrNull { it.id }?.id
    }

    fun setActive(planId: Int, active: Boolean) {
        val patient = prefs.getPatient() ?: return
        val previous = _activeId.value
        _activeId.value = if (active) planId else null
        viewModelScope.launch {
            val result = if (active) deps.activateDietPlan()(planId, patient.pid, patient.clientId)
            else deps.updateDietPlanStatus()(planId, patient.pid, patient.clientId, false)
            when (val r = result) {
                is DomainResult.Success -> {
                    Haptics.success()
                    // The server decides what else changed, so show its list.
                    (deps.fetchDietPlans()(patient.pid, patient.clientId) as? DomainResult.Success)?.let { setPlans(it.data) }
                }
                is DomainResult.Error -> {
                    Log.e("DietBoxVM", r.exception.message.orEmpty())
                    _activeId.value = previous
                    Haptics.error()
                    Toast.makeText(MyApplication.appContext, R.string.diet_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
