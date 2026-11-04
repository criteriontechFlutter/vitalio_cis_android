package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietMeal
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.model.dietDayId
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Builds a plan for the days picked (or shows an active one read-only) and saves it with
 * InsertDietPlan. A new plan lives only in this screen until it is saved.
 */
class DietPlanBuilderViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    /** Set when an existing plan is opened from Diet Box: nothing can be changed. */
    private val _readOnly = MutableStateFlow(false)
    val readOnly: StateFlow<Boolean> = _readOnly.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()
    private var renamed = false

    /** The day shown for a saved plan. */
    private val _day = MutableStateFlow(LocalDate.now().dayOfWeek.dietDayId())
    val day: StateFlow<Int> = _day.asStateFlow()

    /** New plan: the days a food is added to. */
    private val _selectedDays = MutableStateFlow(setOf(_day.value))
    val selectedDays: StateFlow<Set<Int>> = _selectedDays.asStateFlow()

    /** dayId → meal slot → foods. */
    private val _foods = MutableStateFlow<Map<Int, Map<MealTime, List<DietPlanFood>>>>(emptyMap())
    val foods: StateFlow<Map<Int, Map<MealTime, List<DietPlanFood>>>> = _foods.asStateFlow()

    private val _expanded = MutableStateFlow<MealTime?>(MealTime.MORNING)
    val expanded: StateFlow<MealTime?> = _expanded.asStateFlow()

    /** Null for a saved plan until the server gives it. */
    private val _requiredKcal = MutableStateFlow<Int?>(null)
    val requiredKcal: StateFlow<Int?> = _requiredKcal.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    /** The plan just saved, for the success sheet; its id is 0 until the server list shows it. */
    private val _saved = MutableStateFlow<DietPlan?>(null)
    val saved: StateFlow<DietPlan?> = _saved.asStateFlow()

    /** Days whose foods are being fetched for a saved plan. */
    private val _loadingDays = MutableStateFlow<Set<Int>>(emptySet())
    val loadingDays: StateFlow<Set<Int>> = _loadingDays.asStateFlow()

    /** Days that failed to load; picking the day again retries. */
    private val _failedDays = MutableStateFlow<Set<Int>>(emptySet())
    val failedDays: StateFlow<Set<Int>> = _failedDays.asStateFlow()

    /** GetDietPlanByDay only serves active plans, so another plan's foods can't be shown. */
    private val _foodsUnavailable = MutableStateFlow(false)
    val foodsUnavailable: StateFlow<Boolean> = _foodsUnavailable.asStateFlow()

    private var viewedPlanId: Int? = null
    private val requestedDays = mutableSetOf<Int>()

    private var started = false

    /** [planId] null → a new plan; otherwise show that saved plan. */
    fun start(planId: Int?) {
        if (started) return
        started = true
        val patient = prefs.getPatient() ?: return
        if (planId == null) startNew(patient.pid, patient.clientId) else startSaved(planId, patient.pid, patient.clientId)
    }

    private fun startNew(pid: Int, clientId: Int) {
        _requiredKcal.value = DEFAULT_REQUIRED_KCAL
        _name.value = "New Diet Plan"
        viewModelScope.launch {
            val list = deps.fetchDietPlans()(pid, clientId)
            if (list is DomainResult.Success && !renamed) _name.value = "New Diet Plan ${list.data.size + 1}"
        }
    }

    private fun startSaved(planId: Int, pid: Int, clientId: Int) {
        _readOnly.value = true
        viewedPlanId = planId
        val dayId = _day.value
        viewModelScope.launch {
            _loadingDays.value = _loadingDays.value + dayId
            when (val list = deps.fetchDietPlans()(pid, clientId)) {
                is DomainResult.Success -> {
                    val plan = list.data.firstOrNull { it.id == planId }
                    _name.value = plan?.name.orEmpty()
                    _foodsUnavailable.value = plan?.isActive != true
                    _loadingDays.value = _loadingDays.value - dayId
                    if (!_foodsUnavailable.value) {
                        loadDay(dayId)
                        val overview = deps.fetchDailyDietOverview()(pid, clientId)
                        if (overview is DomainResult.Success) {
                            _requiredKcal.value = overview.data.firstOrNull { it.dietPlanId == planId }?.totalEnergyIntake?.roundToInt()
                        }
                    }
                }
                is DomainResult.Error -> {
                    Log.e(TAG, list.exception.message.orEmpty())
                    _loadingDays.value = _loadingDays.value - dayId
                    _failedDays.value = _failedDays.value + dayId
                }
            }
        }
    }

    private fun loadDay(dayId: Int) {
        val patient = prefs.getPatient() ?: return
        if (!requestedDays.add(dayId)) return
        viewModelScope.launch {
            _loadingDays.value = _loadingDays.value + dayId
            _failedDays.value = _failedDays.value - dayId
            when (val r = deps.fetchPlanDay()(patient.pid, dayId, patient.clientId, viewedPlanId)) {
                is DomainResult.Success -> r.data?.let { showDay(dayId, it) }
                is DomainResult.Error -> {
                    Log.e(TAG, r.exception.message.orEmpty())
                    requestedDays.remove(dayId)
                    _failedDays.value = _failedDays.value + dayId
                }
            }
            _loadingDays.value = _loadingDays.value - dayId
        }
    }

    private fun showDay(dayId: Int, plan: DietPlan) {
        _foods.value = _foods.value + (dayId to plan.mealsFor(dayId).associate { it.mealTime to it.foods })
        if (dayId == _day.value) {
            _expanded.value = MealTime.entries.firstOrNull { t -> _foods.value[dayId]?.get(t).orEmpty().isNotEmpty() } ?: MealTime.MORNING
        }
    }

    fun rename(value: String) {
        if (value.isNotBlank()) {
            renamed = true
            _name.value = value.trim()
        }
    }

    /** Saved plan: shows that day. New plan: adds or drops the day; at least one stays selected. */
    fun selectDay(dayId: Int) {
        if (_readOnly.value) {
            _day.value = dayId
            if (!_foodsUnavailable.value) loadDay(dayId)
            return
        }
        val days = _selectedDays.value
        if (dayId !in days) _selectedDays.value = days + dayId
        else if (days.size > 1) _selectedDays.value = days - dayId
    }

    /** The foods to show for the selected days: each food once, however many of the days have it. */
    fun selectedFoods(foods: Map<Int, Map<MealTime, List<DietPlanFood>>>, days: Set<Int>): Map<MealTime, List<DietPlanFood>> =
        MealTime.entries.associateWith { meal ->
            val shown = mutableListOf<DietPlanFood>()
            days.sorted().forEach { dayId ->
                val unmatched = shown.toMutableList()
                foods[dayId]?.get(meal).orEmpty().forEach { food -> if (!unmatched.remove(food)) shown += food }
            }
            shown
        }

    fun toggleExpanded(meal: MealTime) {
        _expanded.value = if (_expanded.value == meal) null else meal
    }

    fun setRequiredKcal(kcal: Int) {
        _requiredKcal.value = kcal.coerceIn(800, 6000)
    }

    fun addFood(food: PickedFood) {
        if (_readOnly.value) return
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
        _selectedDays.value.forEach { dayId ->
            val day = _foods.value[dayId].orEmpty()
            _foods.value = _foods.value + (dayId to (day + (meal to (day[meal].orEmpty() + item))))
        }
        _expanded.value = meal
    }

    /** Removes the food from every selected day. */
    fun removeFood(meal: MealTime, food: DietPlanFood) {
        if (_readOnly.value) return
        _selectedDays.value.forEach { dayId ->
            val day = _foods.value[dayId].orEmpty()
            val list = day[meal].orEmpty().toMutableList().apply { remove(food) }
            _foods.value = _foods.value + (dayId to (day + (meal to list)))
        }
    }

    val hasFood: Boolean get() = _foods.value.values.any { day -> day.values.any { it.isNotEmpty() } }

    private fun buildPlan() = DietPlan(
        id = 0,
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
            val required = _requiredKcal.value ?: DEFAULT_REQUIRED_KCAL
            when (val r = deps.insertDietPlan()(patient.pid, patient.clientId, patient.pid, plan, required)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    _saved.value = plan
                    // The insert doesn't return the new id; read it back from the list.
                    when (val list = deps.fetchDietPlans()(patient.pid, patient.clientId)) {
                        is DomainResult.Success -> {
                            val created = list.data.filter { it.name == plan.name }.maxByOrNull { it.id }
                            if (created != null) _saved.value = plan.copy(id = created.id)
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
    fun markSavedActive(onDone: () -> Unit) = updateSaved(onDone) { id, pid, clientId ->
        deps.activateDietPlan()(id, pid, clientId)
    }

    /** "Later": the server makes every new plan active, so turn this one back off. */
    fun leaveSavedInactive(onDone: () -> Unit) = updateSaved(onDone) { id, pid, clientId ->
        deps.updateDietPlanStatus()(id, pid, clientId, false)
    }

    private fun updateSaved(onDone: () -> Unit, update: suspend (id: Int, pid: Int, clientId: Int) -> DomainResult<Unit>) {
        val patient = prefs.getPatient() ?: return onDone()
        val id = _saved.value?.id?.takeIf { it > 0 } ?: return onDone()
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            val r = update(id, patient.pid, patient.clientId)
            if (r is DomainResult.Error) {
                Log.e(TAG, r.exception.message.orEmpty())
                Haptics.error()
                Toast.makeText(MyApplication.appContext, R.string.diet_update_failed, Toast.LENGTH_SHORT).show()
            }
            _saving.value = false
            onDone()
        }
    }

    /** Removes the saved plan being viewed; [onRemoved] runs only when the server accepted it. */
    fun removePlan(onRemoved: () -> Unit) {
        val patient = prefs.getPatient() ?: return
        val id = viewedPlanId ?: return
        if (_saving.value) return
        viewModelScope.launch {
            _saving.value = true
            when (val r = deps.removeDietPlan()(id, patient.pid, patient.clientId)) {
                is DomainResult.Success -> {
                    Haptics.success()
                    onRemoved()
                }
                is DomainResult.Error -> {
                    Log.e(TAG, r.exception.message.orEmpty())
                    Haptics.error()
                    Toast.makeText(MyApplication.appContext, R.string.diet_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
            _saving.value = false
        }
    }

    fun clearError() { _error.value = null }

    private companion object {
        const val TAG = "DietPlanBuilderVM"
        /** Starting value for a new plan; the user can change it before saving. */
        const val DEFAULT_REQUIRED_KCAL = 2000
    }
}
