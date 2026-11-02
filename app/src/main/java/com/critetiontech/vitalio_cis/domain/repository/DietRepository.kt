package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.RecentFood
import java.time.LocalDate

interface DietRepository {
    // ── Diet plans (server) ──
    suspend fun fetchDietPlans(pid: Int, clientId: Int): DomainResult<List<DietPlan>>
    /** Last saved plans, read from local cache (no network): GetDietPlan takes ~20 s. */
    fun cachedDietPlans(pid: Int): List<DietPlan>
    suspend fun insertDietPlan(pid: Int, clientId: Int, userId: Int, plan: DietPlan): DomainResult<Unit>

    // ── Foods (NutriAnalyser) ──
    suspend fun searchFoods(prefix: String): DomainResult<List<NutriFood>>
    suspend fun foodUnits(foodId: Int): DomainResult<List<NutriUnit>>
    suspend fun nutrition(foodId: Int, quantity: Double, unitId: Int): DomainResult<FoodNutrition>
    /** The serving a food opens with, and its nutrition; used for the kcal line in search results. */
    suspend fun defaultServing(foodId: Int): DomainResult<Pair<NutriUnit, FoodNutrition>>
    /** Unit id for a plan food, which the server only gives as a name ("piece (50.00 Gm)"). */
    suspend fun unitIdFor(foodId: Int, unitName: String): Int?

    // ── On this device only (no API for these yet) ──
    fun activePlanId(pid: Int): Int?
    fun setActivePlanId(pid: Int, planId: Int?)
    fun requiredKcal(pid: Int): Int
    fun setRequiredKcal(pid: Int, kcal: Int)
    fun planCreatedAt(pid: Int, planId: Int): Long?
    fun markPlanCreated(pid: Int, planId: Int, at: Long = System.currentTimeMillis())
    fun logsFor(pid: Int, date: LocalDate): List<DietLogEntry>
    fun saveLog(pid: Int, date: LocalDate, entry: DietLogEntry)
    fun removeLog(pid: Int, date: LocalDate, key: String)
    fun recentFoods(pid: Int): List<RecentFood>
    fun addRecentFood(pid: Int, food: RecentFood)
}
