package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietMeal
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.model.dietDayId
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/** Builds a 7-day plan (or shows a saved one read-only) and saves it with InsertDietPlan. */
class DietPlanBuilderViewModel : ViewModel() {

    private val deps = AppDependencies
    private val repo = deps.dietRepository
    private val prefs = deps.prefs

    /** Set when an existing plan is opened from Diet Box: nothing can be changed. */
    private val _readOnly = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnly.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _day = MutableStateFlow(LocalDate.now().dayOfWeek.dietDayId())
    val day: StateFlow<Int> = _day.asStateFlow()

    /** dayId → meal slot → foods. */
    private val _foods = MutableStateFlow<Map<Int, Map<MealTime, List<DietPlanFood>>>>(emptyMap())
    val foods: StateFlow<Map<Int, Map<MealTime, List<DietPlanFood>>>> = _foods.asStateFlow()

    private val _expanded = MutableStateFlow<MealTime?>(MealTime.MORNING)
    val expanded: StateFlow<MealTime?> = _expanded.asStateFlow()

    private val _requiredKcal = MutableStateFlow(prefs.getPatient()?.pid?.let { repo.requiredKcal(it) } ?: 2000)
    val requiredKcal: StateFlow<Int> = _requiredKcal.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** The plan just saved, for the success sheet; its id is 0 until the server list shows it. */
    private val _saved = MutableStateFlow<DietPlan?>(null)
    val saved: StateFlow<DietPlan?> = _saved.asStateFlow()

    private var started = false

    /** [planId] null → a new plan; otherwise show that saved plan. */
    fun start(planId: Int?) {
        if (started) return
        started = true
        val patient = prefs.getPatient() ?: return
        val plans = repo.cachedDietPlans(patient.pid)
        if (planId == null) {
            _name.value = "New Diet Plan ${plans.size + 1}"
            return
        }
        _readOnly.value = true
        plans.firstOrNull { it.id == planId }?.let { plan ->
            _name.value = plan.name
            _foods.value = plan.days.mapValues { (_, meals) -> meals.associate { it.mealTime to it.foods } }
            _expanded.value = MealTime.entries.firstOrNull { t -> _foods.value[_day.value]?.get(t).orEmpty().isNotEmpty() } ?: MealTime.MORNING
        }
    }

    fun rename(value: String) {
        if (value.isNotBlank()) _name.value = value.trim()
    }

    fun selectDay(dayId: Int) { _day.value = dayId }

    fun toggleExpanded(meal: MealTime) {
        _expanded.value = if (_expanded.value == meal) null else meal
    }

    fun setRequiredKcal(kcal: Int) {
        val pid = prefs.getPatient()?.pid ?: return
        val v = kcal.coerceIn(800, 6000)
        repo.setRequiredKcal(pid, v)
        _requiredKcal.value = v
    }

    fun addFood(food: PickedFood) {
        if (_readOnly.value) return
        val dayId = _day.value
        val meal = MealTime.of(food.mealTimeId)
        val item = DietPlanFood(
            rowId = 0,
            foodId = food.foodId,
            foodName = food.foodName,
            quantity = food.quantity,
            unitName = food.unitName,
            calories = food.nutrition.kcal.roundToInt().toDouble(),
            imageUrl = food.imageUrl,
        )
        val day = _foods.value[dayId].orEmpty()
        _foods.value = _foods.value + (dayId to (day + (meal to (day[meal].orEmpty() + item))))
        _expanded.value = meal
    }

    fun removeFood(meal: MealTime, index: Int) {
        if (_readOnly.value) return
        val dayId = _day.value
        val day = _foods.value[dayId].orEmpty()
        val list = day[meal].orEmpty().toMutableList().apply { if (index in indices) removeAt(index) }
        _foods.value = _foods.value + (dayId to (day + (meal to list)))
    }

    val hasFood: Boolean get() = _foods.value.values.any { day -> day.values.any { it.isNotEmpty() } }

    private fun buildPlan(id: Int = 0) = DietPlan(
        id = id,
        name = _name.value,
        days = _foods.value.mapValues { (_, meals) ->
            MealTime.entries.mapNotNull { t ->
                meals[t]?.takeIf { it.isNotEmpty() }?.let { DietMeal(t, t.defaultStart, t.defaultEnd, it) }
            }
        }.filterValues { it.isNotEmpty() },
    )

    fun save() {
        val patient = prefs.getPatient() ?: return
        if (_saving.value || !hasFood) return
        viewModelScope.launch {
            _saving.value = true
            _error.value = null
            val plan = buildPlan()
            when (val r = deps.insertDietPlan()(patient.pid, patient.clientId, patient.pid, plan)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _saved.value = plan
                    // The insert doesn't return the new id; read it back from the list.
                    val known = repo.cachedDietPlans(patient.pid).map { it.id }.toSet()
                    when (val list = deps.fetchDietPlans()(patient.pid, patient.clientId)) {
                        is DomainResult.Success -> {
                            val created = list.data.filter { it.id !in known }.maxByOrNull { it.id }
                                ?: list.data.filter { it.name == plan.name }.maxByOrNull { it.id }
                            if (created != null) {
                                repo.markPlanCreated(patient.pid, created.id)
                                _saved.value = plan.copy(id = created.id)
                            }
                        }
                        is DomainResult.Error -> Log.e(TAG, list.exception.message.orEmpty())
                    }
                }
                is DomainResult.Error -> {
                    Haptics.error()
                    _error.value = r.exception.message
                }
            }
            _saving.value = false
        }
    }

    /** "Mark as Active" on the success sheet. */
    fun markSavedActive() {
        val pid = prefs.getPatient()?.pid ?: return
        _saved.value?.id?.takeIf { it > 0 }?.let { repo.setActivePlanId(pid, it) }
    }

    fun clearError() { _error.value = null }

    private companion object { const val TAG = "DietPlanBuilderVM" }
}
