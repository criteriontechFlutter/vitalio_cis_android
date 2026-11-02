package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.DietMeal
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.model.dietDayId
import com.critetiontech.ctvitalio.ui.feedback.Haptics
import com.critetiontech.ctvitalio.utils.Patient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

/** Today's diet from the active plan; ticking a food logs it as eaten (and sends it as intake). */
class DietChecklistViewModel : ViewModel() {

    private val deps = AppDependencies
    private val repo = deps.dietRepository
    private val prefs = deps.prefs

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _plan = MutableStateFlow<DietPlan?>(null)
    val plan: StateFlow<DietPlan?> = _plan.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _logs = MutableStateFlow<List<DietLogEntry>>(emptyList())
    val logs: StateFlow<List<DietLogEntry>> = _logs.asStateFlow()

    /** Food keys being saved right now, to show a spinner in place of the checkbox. */
    private val _saving = MutableStateFlow<Set<String>>(emptySet())
    val saving: StateFlow<Set<String>> = _saving.asStateFlow()

    private val _expanded = MutableStateFlow<MealTime?>(currentMealTime())
    val expanded: StateFlow<MealTime?> = _expanded.asStateFlow()

    private fun patient(): Patient? = prefs.getPatient()

    fun load() {
        val patient = patient() ?: return
        refreshLocal()
        viewModelScope.launch {
            _loading.value = _plan.value == null
            when (val r = deps.fetchDietPlans()(patient.pid, patient.clientId)) {
                is DomainResult.Success -> _plan.value = pickActive(patient.pid, r.data)
                is DomainResult.Error -> Log.e(TAG, r.exception.message.orEmpty())
            }
            _loading.value = false
        }
    }

    /** Cached plan and this date's ticks, without the network. */
    private fun refreshLocal() {
        val patient = patient() ?: return
        if (_plan.value == null) _plan.value = pickActive(patient.pid, repo.cachedDietPlans(patient.pid))
        _logs.value = repo.logsFor(patient.pid, _date.value)
    }

    /** The plan marked active in Diet Box, else the newest one. */
    private fun pickActive(pid: Int, plans: List<DietPlan>): DietPlan? {
        val activeId = repo.activePlanId(pid)
        return plans.firstOrNull { it.id == activeId } ?: plans.maxByOrNull { it.id }
    }

    fun shiftDate(days: Long) {
        val next = _date.value.plusDays(days)
        if (next.isAfter(LocalDate.now().plusDays(MAX_DAYS_AHEAD))) return
        _date.value = next
        _expanded.value = if (next == LocalDate.now()) currentMealTime() else MealTime.MORNING
        refreshLocal()
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

    /** Ticking is for today and past days only. */
    val canTick: Boolean get() = !_date.value.isAfter(LocalDate.now())

    fun togglePlanFood(meal: DietMeal, food: DietPlanFood) {
        val patient = patient() ?: return
        if (!canTick) return
        val date = _date.value
        val key = planKey(food)
        if (_logs.value.any { it.key == key }) {
            repo.removeLog(patient.pid, date, key)
            refreshLocal()
            return
        }
        if (key in _saving.value) return
        viewModelScope.launch {
            _saving.value = _saving.value + key
            val unitId = repo.unitIdFor(food.foodId, food.unitName)
            // Plans only carry calories; the macros come from NutriAnalyser for the same serving.
            val nutrition = unitId
                ?.let { (deps.fetchFoodNutrition()(food.foodId, food.quantity, it) as? DomainResult.Success)?.data }
                ?.let { if (it.kcal <= 0.0) it.copy(kcal = food.calories) else it }
                ?: FoodNutrition(kcal = food.calories)
            repo.saveLog(
                patient.pid, date,
                DietLogEntry(key, meal.mealTime.id, food.foodId, food.foodName, food.quantity, food.unitName, nutrition, fromPlan = true),
            )
            refreshLocal()
            Haptics.success()
            if (unitId != null) sendIntake(patient, date, food.foodId, food.quantity, unitId)
            _saving.value = _saving.value - key
        }
    }

    /** "Food Outside Plan": logged as eaten straight away. */
    fun addOutsideFood(food: PickedFood) {
        val patient = patient() ?: return
        val date = _date.value
        val entry = DietLogEntry(
            key = "extra:${UUID.randomUUID()}",
            mealTimeId = food.mealTimeId,
            foodId = food.foodId,
            foodName = food.foodName,
            quantity = food.quantity,
            unitName = food.unitName,
            nutrition = food.nutrition,
            fromPlan = false,
        )
        repo.saveLog(patient.pid, date, entry)
        _expanded.value = MealTime.of(food.mealTimeId)
        refreshLocal()
        Haptics.success()
        viewModelScope.launch { sendIntake(patient, date, food.foodId, food.quantity, food.unitId) }
    }

    fun removeOutsideFood(key: String) {
        val patient = patient() ?: return
        repo.removeLog(patient.pid, _date.value, key)
        refreshLocal()
    }

    private suspend fun sendIntake(patient: Patient, date: LocalDate, foodId: Int, quantity: Double, unitId: Int) {
        // Local wall-clock time, no offset, the same as fluid intake.
        val at = if (date == LocalDate.now()) LocalDateTime.now() else date.atTime(LocalTime.now())
        val body = mapOf<String, Any>(
            "FoodDate" to at.withNano(0).toString(),
            "FoodQuantity" to quantity,
            "UserID" to patient.pid,
            "foodId" to foodId,
            "clientId" to patient.clientId,
            "FoodUnitId" to unitId,
            "uhid" to patient.uhId,
            "isGiven" to 1,
            "entryType" to "N",
            "isFromPatient" to 1,
        )
        when (val r = deps.addIntake()(body)) {
            is DomainResult.Success -> Unit
            is DomainResult.Error -> Log.e(TAG, "addIntake: ${r.exception.message}")
        }
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
