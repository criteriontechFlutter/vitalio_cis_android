package com.critetiontech.ctvitalio.model

import java.time.DayOfWeek

// ── API: api/DietPlan/GetAllDietPlanByPid (no days) and GetDietPlanByDay (one day) ──

data class DietPlanResponse(
    val status: Int = 0,
    val message: String? = null,
    val responseValue: List<DietPlanDto>? = null,
)

data class DietPlanDto(
    val dietPlanId: Int = 0,
    val dietPlanName: String? = null,
    val isActive: Int = 0,
    val daysAgo: Int? = null,
    val days: List<DietDayDto>? = null,
)

data class DietDayDto(
    val dayId: Int = 0,
    val dayName: String? = null,
    val meals: List<DietMealDto>? = null,
)

data class DietMealDto(
    val mealTimeId: Int = 0,
    val mealTimeType: String? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val foods: List<DietPlanFoodDto>? = null,
)

data class DietPlanFoodDto(
    val id: Int = 0,
    val foodId: Int = 0,
    val istaken: Boolean = false,
    val foodName: String? = null,
    val quantity: Double = 0.0,
    val unitName: String? = null,
    val calories: Double = 0.0,
)

// ── API: api/DietPlan/GetDailyDietOverview ────────────────────────────────────

data class DailyDietOverviewResponse(
    val status: Int = 0,
    val message: String? = null,
    val responseValue: List<DailyDietOverview>? = null,
)

data class DailyDietOverview(
    val dietPlanId: Int = 0,
    val dietPlanName: String? = null,
    val totalEnergyIntake: Double = 0.0,
    val consumedCalories: Double = 0.0,
    val remainingCalories: Double = 0.0,
    val consumedPercentage: Double = 0.0,
)

// ── API: api/DietPlan/InsertDietPlan (the dietJson string) ────────────────────

data class DietJson(val days: List<DietJsonDay>)
data class DietJsonDay(val dayId: Int, val meals: List<DietJsonMeal>)
data class DietJsonMeal(val mealTimeId: Int, val foods: List<DietJsonFood>)
data class DietJsonFood(val foodId: Int, val quantity: Double, val unitName: String, val calories: Double)

// ── App models ────────────────────────────────────────────────────────────────

/** The five meal slots of a diet day. Ids match the server's mealTimeId. */
enum class MealTime(val id: Int, val defaultStart: String, val defaultEnd: String) {
    MORNING(1, "07:00:00", "09:00:00"),
    MID_MORNING(2, "10:30:00", "11:30:00"),
    AFTERNOON(3, "12:30:00", "14:00:00"),
    EVENING(4, "16:30:00", "17:30:00"),
    NIGHT(5, "19:30:00", "20:30:00");

    companion object {
        fun of(id: Int): MealTime = entries.firstOrNull { it.id == id } ?: MORNING
    }
}

/** Server dayId: 1 = Monday … 7 = Sunday, the same numbering as [DayOfWeek.getValue]. */
fun DayOfWeek.dietDayId(): Int = value

data class DietPlanFood(
    /** Row id on the server; 0 for a food added on this device and not saved yet. */
    val rowId: Int,
    val foodId: Int,
    val foodName: String,
    val quantity: Double,
    val unitName: String,
    val calories: Double,
    val imageUrl: String? = null,
    /** Server's tick for this row. It has no date, so it only speaks for today. */
    val isTaken: Boolean = false,
)

data class DietMeal(
    val mealTime: MealTime,
    val startTime: String,
    val endTime: String,
    val foods: List<DietPlanFood>,
)

data class DietPlan(
    val id: Int,
    val name: String,
    /** dayId (1..7) → meals that day; days with no food are absent. Empty in the plan list. */
    val days: Map<Int, List<DietMeal>>,
    val isActive: Boolean = false,
    val daysAgo: Int? = null,
) {
    fun mealsFor(dayId: Int): List<DietMeal> = days[dayId].orEmpty()
    fun caloriesFor(dayId: Int): Double = mealsFor(dayId).sumOf { m -> m.foods.sumOf { it.calories } }
    val plannedDays: Int get() = days.count { (_, meals) -> meals.any { it.foods.isNotEmpty() } }
    val mealCount: Int get() = days.values.sumOf { meals -> meals.count { it.foods.isNotEmpty() } }
    val avgCaloriesPerDay: Double get() = if (plannedDays == 0) 0.0 else days.keys.sumOf { caloriesFor(it) } / plannedDays
}

/** Nutrition for one serving, as NutriAnalyser reports it. Missing values are 0. */
data class FoodNutrition(
    val kcal: Double = 0.0,
    val carbs: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val fiber: Double = 0.0,
    val sugar: Double = 0.0,
    val sodium: Double = 0.0,
)

/**
 * A food the user picked on the Add Food → Nutrition Facts screens, handed back to the screen
 * that opened them (the plan builder, or the checklist for "Food Outside Plan").
 */
data class PickedFood(
    val mealTimeId: Int,
    val foodId: Int,
    val foodName: String,
    val imageUrl: String?,
    val unitId: Int,
    val unitName: String,
    val quantity: Double,
    val nutrition: FoodNutrition,
)

/** One food ticked as eaten, as Nutrition Analytics shows it. */
data class DietLogEntry(
    /** "plan:<rowId>" for a planned food, "extra:<uuid>" for food outside the plan. */
    val key: String,
    val mealTimeId: Int,
    val foodId: Int,
    val foodName: String,
    val quantity: Double,
    val unitName: String,
    val nutrition: FoodNutrition,
    val fromPlan: Boolean,
    val loggedAt: Long = System.currentTimeMillis(),
)

/** A food shown under "Recent Searched" on Add Food. */
data class RecentFood(
    val foodId: Int,
    val foodName: String,
    val imageUrl: String?,
    val unitId: Int,
    val unitName: String,
    val quantity: Double,
    val nutrition: FoodNutrition,
)
