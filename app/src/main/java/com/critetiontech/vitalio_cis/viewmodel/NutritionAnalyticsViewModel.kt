package com.critetiontech.ctvitalio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DailyDietOverview
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.IntakeItem
import com.critetiontech.ctvitalio.model.dietDayId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Nutrition Analytics, from the server. Only today has data: GetDailyDietOverview for the calories
 * and goal, the foods ticked in GetDietPlanByDay for meals (macros from NutriAnalyser), and fluid
 * intake for water. Other days show only their planned calories until a date-wise API exists.
 */
class NutritionAnalyticsViewModel : ViewModel() {

    data class DayTotal(val date: LocalDate, val nutrition: FoodNutrition, val logged: Boolean)

    data class Summary(
        val date: LocalDate,
        val goalKcal: Int,
        val plannedKcal: Int,
        val totals: FoodNutrition,
        val week: List<DayTotal>,
        /** mealTimeId → foods eaten in that slot. */
        val meals: Map<Int, List<DietLogEntry>>,
        val waterMl: Double,
    ) {
        val consumedKcal: Int get() = totals.kcal.toInt()
        val leftKcal: Int get() = (goalKcal - consumedKcal).coerceAtLeast(0)
        val percent: Int get() = if (goalKcal <= 0) 0 else (consumedKcal * 100 / goalKcal)
        private val loggedDays get() = week.filter { it.logged }
        val weekAvgKcal: Int get() = loggedDays.takeIf { it.isNotEmpty() }?.let { d -> d.sumOf { it.nutrition.kcal }.toInt() / d.size } ?: 0
        /** Logged days that landed within ±10 % of the goal. */
        val adherence: Int get() = loggedDays.takeIf { it.isNotEmpty() }
            ?.let { d -> d.count { kotlin.math.abs(it.nutrition.kcal - goalKcal) <= goalKcal * 0.1 } * 100 / d.size } ?: 0
        val proteinGoalDays: Int get() = week.count { it.logged && it.nutrition.protein >= goalProtein }
        val goalCarbs: Double get() = goalKcal * 0.45 / 4
        val goalProtein: Double get() = goalKcal * 0.26 / 4
        val goalFat: Double get() = goalKcal * 0.27 / 9
    }

    private val deps = AppDependencies
    private val repo = deps.dietRepository
    private val prefs = deps.prefs

    private val _date = MutableStateFlow(LocalDate.now())
    val date: StateFlow<LocalDate> = _date.asStateFlow()

    private val _summary = MutableStateFlow<Summary?>(null)
    val summary: StateFlow<Summary?> = _summary.asStateFlow()

    private var overview: DailyDietOverview? = null
    /** dayId → the calories planned that weekday. */
    private var plannedKcal: Map<Int, Int> = emptyMap()
    /** Foods ticked today. */
    private var eatenToday: List<DietLogEntry> = emptyList()
    private var water: Map<LocalDate, Double> = emptyMap()

    fun load() {
        recompute()
        val patient = prefs.getPatient() ?: return
        val date = _date.value
        val dayId = date.dayOfWeek.dietDayId()
        viewModelScope.launch {
            val r = deps.fetchDailyDietOverview()(patient.pid, patient.clientId)
            if (r is DomainResult.Success) overview = r.data.maxByOrNull { it.dietPlanId }
            recompute()
        }
        viewModelScope.launch {
            val r = deps.fetchPlanDay()(patient.pid, dayId, patient.clientId)
            if (r is DomainResult.Success) {
                plannedKcal = plannedKcal + (dayId to (r.data?.caloriesFor(dayId)?.toInt() ?: 0))
                recompute()
                if (date == LocalDate.now()) {
                    eatenToday = r.data?.mealsFor(dayId).orEmpty().flatMap { meal ->
                        meal.foods.filter { it.isTaken }.map { eatenEntry(meal.mealTime.id, it) }
                    }
                    recompute()
                }
            }
        }
        viewModelScope.launch {
            val r = deps.fetchIntake()(patient.uhId, patient.clientId.toString(), date.toString())
            if (r is DomainResult.Success) water = water + (date to waterMl(r.data))
            recompute()
        }
    }

    private suspend fun eatenEntry(mealTimeId: Int, food: DietPlanFood): DietLogEntry {
        val unitId = repo.unitIdFor(food.foodId, food.unitName)
        val nutrition = unitId?.let { (deps.fetchFoodNutrition()(food.foodId, food.quantity, it) as? DomainResult.Success)?.data }
        return DietLogEntry(
            "plan:${food.rowId}", mealTimeId, food.foodId, food.foodName, food.quantity, food.unitName,
            (nutrition ?: FoodNutrition()).copy(kcal = food.calories), fromPlan = true,
        )
    }

    fun selectDate(d: LocalDate) {
        if (d.isAfter(LocalDate.now())) return
        _date.value = d
        load()
    }

    fun shiftWeek(weeks: Long) {
        val d = _date.value.plusWeeks(weeks)
        selectDate(if (d.isAfter(LocalDate.now())) LocalDate.now() else d)
    }

    private fun waterMl(items: List<IntakeItem>): Double =
        items.filter { it.unitName.orEmpty().contains("ml", true) }.sumOf { it.foodQuantity.toDouble() }

    private fun recompute() {
        val date = _date.value
        val today = LocalDate.now()
        val monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val todayTotals = eatenToday.sum().copy(kcal = overview?.consumedCalories ?: 0.0)
        val week = (0L..6L).map { i ->
            val d = monday.plusDays(i)
            if (d == today) DayTotal(d, todayTotals, todayTotals.kcal > 0) else DayTotal(d, FoodNutrition(), false)
        }
        val isToday = date == today
        _summary.value = Summary(
            date = date,
            goalKcal = overview?.totalEnergyIntake?.toInt() ?: 0,
            plannedKcal = plannedKcal[date.dayOfWeek.dietDayId()] ?: 0,
            totals = if (isToday) todayTotals else FoodNutrition(),
            week = week,
            meals = if (isToday) eatenToday.groupBy { it.mealTimeId } else emptyMap(),
            waterMl = water[date] ?: 0.0,
        )
    }

    private fun List<DietLogEntry>.sum() = FoodNutrition(
        kcal = sumOf { it.nutrition.kcal },
        carbs = sumOf { it.nutrition.carbs },
        protein = sumOf { it.nutrition.protein },
        fat = sumOf { it.nutrition.fat },
        fiber = sumOf { it.nutrition.fiber },
        sugar = sumOf { it.nutrition.sugar },
        sodium = sumOf { it.nutrition.sodium },
    )

    companion object {
        const val FIBER_GOAL_G = 30.0
        const val WATER_GOAL_ML = 3000.0
        const val SUGAR_LIMIT_G = 35.0
        const val SODIUM_LIMIT_MG = 2300.0
    }
}
