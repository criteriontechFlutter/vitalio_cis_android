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
import com.critetiontech.ctvitalio.model.dietDayId
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.Patient
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime

/**
 * The active plan's diet by weekday, all from the server. Its tick has no date, so ticks are
 * shown and changed for today only.
 */
class DietChecklistViewModel : ViewModel() {

    private val deps = AppDependencies
    private val prefs = deps.prefs

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _plan = MutableStateFlow<DietPlan?>(null)
    val plan: StateFlow<DietPlan?> = _plan.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    /** This date's foods couldn't be loaded. */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    /** Food keys being saved right now, to show a spinner in place of the checkbox. */
    private val _saving = MutableStateFlow<Set<String>>(emptySet())
    val saving: StateFlow<Set<String>> = _saving.asStateFlow()

    private val _expanded = MutableStateFlow<MealTime?>(currentMealTime())
    val expanded: StateFlow<MealTime?> = _expanded.asStateFlow()

    private var dayJob: Job? = null
    /** Bumped on each tick, so a day response that started earlier can't undo that tick. */
    private var tickCount = 0

    private fun patient(): Patient? = prefs.getPatient()

    fun load() {
        val patient = patient() ?: return
        val dayId = _date.value.dayOfWeek.dietDayId()
        dayJob?.cancel()
        dayJob = viewModelScope.launch {
            _loading.value = _plan.value?.days?.containsKey(dayId) != true
            _loadFailed.value = false
            val ticksAtStart = tickCount
            when (val r = deps.fetchPlanDay()(patient.pid, dayId, patient.clientId)) {
                is DomainResult.Success -> if (tickCount == ticksAtStart) showDay(dayId, r.data)
                is DomainResult.Error -> {
                    Log.e(TAG, r.exception.message.orEmpty())
                    _loadFailed.value = _plan.value?.days?.containsKey(dayId) != true
                }
            }
            _loading.value = false
        }
    }

    private fun showDay(dayId: Int, day: DietPlan?) {
        val current = _plan.value
        _plan.value = when {
            day == null -> null
            current?.id == day.id -> current.copy(days = current.days - dayId + day.days)
            else -> day
        }
    }

    fun shiftDate(days: Long) {
        val next = _date.value.plusDays(days)
        if (next.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) return
        _date.value = next
        _expanded.value = if (next == LocalDate.now()) currentMealTime() else MealTime.MORNING
        load()
    }

    fun toggleExpanded(meal: MealTime) {
        _expanded.value = if (_expanded.value == meal) null else meal
    }

    /** Meals for the selected date's weekday, always all five slots in order. */
    fun mealsFor(plan: DietPlan?, date: LocalDate): List<DietMeal> {
        val planned = plan?.mealsFor(date.dayOfWeek.dietDayId()).orEmpty().associateBy { it.mealTime }
        return MealTime.entries.map { t -> planned[t] ?: DietMeal(t, t.defaultStart, t.defaultEnd, emptyList()) }
    }

    fun planKey(food: DietPlanFood) = "plan:${food.rowId}:${food.foodId}"

    fun isTicked(food: DietPlanFood, date: LocalDate): Boolean = date == LocalDate.now() && food.isTaken

    val canTick: Boolean get() = _date.value == LocalDate.now()

    fun togglePlanFood(food: DietPlanFood) {
        val patient = patient() ?: return
        if (!canTick) return
        val key = planKey(food)
        if (key in _saving.value) return
        val taken = !food.isTaken
        tickCount++
        viewModelScope.launch {
            _saving.value = _saving.value + key
            when (val r = deps.updateDietIntake()(food.rowId, patient.pid, patient.clientId, taken)) {
                is DomainResult.Success -> {
                    setTaken(food.rowId, taken)
                    Haptics.success()
                }
                is DomainResult.Error -> {
                    Log.e(TAG, "updateDietIntake: ${r.exception.message}")
                    Haptics.error()
                    Toast.makeText(MyApplication.appContext, R.string.diet_update_failed, Toast.LENGTH_SHORT).show()
                }
            }
            _saving.value = _saving.value - key
        }
    }

    private fun setTaken(rowId: Int, taken: Boolean) {
        val plan = _plan.value ?: return
        _plan.value = plan.copy(
            days = plan.days.mapValues { (_, meals) ->
                meals.map { meal -> meal.copy(foods = meal.foods.map { if (it.rowId == rowId) it.copy(isTaken = taken) else it }) }
            },
        )
    }

    private companion object {
        const val TAG = "DietChecklistVM"
        const val MAX_DAYS_AHEAD = 6L

        /** The meal slot the clock is in (or the next one), so it opens expanded. */
        fun currentMealTime(): MealTime {
            val now = LocalTime.now()
            return MealTime.entries.firstOrNull { now.isBefore(LocalTime.parse(it.defaultEnd)) } ?: MealTime.NIGHT
        }
    }
}
