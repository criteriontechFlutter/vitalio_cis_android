package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DailyDietOverview
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.FoodNutrition

/** Diet data comes only from the server: nothing here is saved on the device. */
interface DietRepository {
    // ── Diet plans ──
    suspend fun fetchDietPlans(pid: Int, clientId: Int): DomainResult<List<DietPlan>>
    /**
     * Plan [planId] with [dayId]'s meals, or the newest active plan when [planId] is null; null when
     * the server doesn't return it (it only returns active plans). Slow on the server.
     */
    suspend fun fetchPlanDay(pid: Int, dayId: Int, clientId: Int, planId: Int? = null): DomainResult<DietPlan?>
    suspend fun updateDietPlanStatus(id: Int, pid: Int, clientId: Int, isActive: Boolean): DomainResult<Unit>
    /** Makes [id] the only active plan: the server allows several, the app shows one. */
    suspend fun activatePlan(id: Int, pid: Int, clientId: Int): DomainResult<Unit>
    suspend fun removeDietPlan(id: Int, pid: Int, clientId: Int): DomainResult<Unit>
    /** [id] is the food's row id in the plan. */
    suspend fun updateDietIntake(id: Int, pid: Int, clientId: Int, isTaken: Boolean): DomainResult<Unit>
    suspend fun fetchDailyDietOverview(pid: Int, clientId: Int): DomainResult<List<DailyDietOverview>>
    suspend fun insertDietPlan(pid: Int, clientId: Int, userId: Int, plan: DietPlan, requiredCalories: Int): DomainResult<Unit>

    // ── Foods (NutriAnalyser) ──
    suspend fun searchFoods(prefix: String): DomainResult<List<NutriFood>>
    suspend fun foodUnits(foodId: Int): DomainResult<List<NutriUnit>>
    suspend fun nutrition(foodId: Int, quantity: Double, unitId: Int): DomainResult<FoodNutrition>
    /** The serving a food opens with, and its nutrition; used for the kcal line in search results. */
    suspend fun defaultServing(foodId: Int): DomainResult<Pair<NutriUnit, FoodNutrition>>
    /** Unit id for a plan food, which the server only gives as a name ("piece (50.00 Gm)"). */
    suspend fun unitIdFor(foodId: Int, unitName: String): Int?
}
