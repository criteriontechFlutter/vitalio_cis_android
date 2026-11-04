package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DietRepository
import com.critetiontech.ctvitalio.model.DailyDietOverview
import com.critetiontech.ctvitalio.model.DietJson
import com.critetiontech.ctvitalio.model.DietJsonDay
import com.critetiontech.ctvitalio.model.DietJsonFood
import com.critetiontech.ctvitalio.model.DietJsonMeal
import com.critetiontech.ctvitalio.model.DietMeal
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.DietPlanDto
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

class DietRepositoryImpl(
    private val context: Context,
    private val nutri: NutriRepository = NutriRepository(),
) : DietRepository {
    private val endpoints = ApiEndPointCorporateModule()
    private val gson = Gson()

    private val slowHeaders = mapOf(RetrofitInstance.READ_TIMEOUT_HEADER to "60")
    // GetDietPlanByDay took 20-130 s when measured; the default 20 s read timeout cuts it off.
    private val planDayHeaders = mapOf(RetrofitInstance.READ_TIMEOUT_HEADER to "180")

    // NutriAnalyser answers don't change, so keep them for the life of the process.
    private val unitsCache = ConcurrentHashMap<Int, List<NutriUnit>>()
    private val nutritionCache = ConcurrentHashMap<String, FoodNutrition>()

    // ── Diet plans ──────────────────────────────────────────────────────────────

    /** The list in a response. With no data the server sends responseValue "" instead of []. */
    private fun responseList(json: String?): JsonArray {
        val root = gson.fromJson(json, JsonObject::class.java) ?: throw Exception("Empty response.")
        if (root.get("status")?.asInt != 1) throw Exception(serverMessage(json) ?: "Request failed.")
        return root.get("responseValue")?.takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    }

    private fun parsePlans(json: String?): List<DietPlan> {
        return gson.fromJson(responseList(json), Array<DietPlanDto>::class.java).map { dto ->
            DietPlan(
                id = dto.dietPlanId,
                name = dto.dietPlanName.orEmpty().trim(),
                isActive = dto.isActive == 1,
                daysAgo = dto.daysAgo,
                days = dto.days.orEmpty().associate { day ->
                    day.dayId to day.meals.orEmpty().map { meal ->
                        val time = MealTime.of(meal.mealTimeId)
                        DietMeal(
                            mealTime = time,
                            startTime = meal.startTime ?: time.defaultStart,
                            endTime = meal.endTime ?: time.defaultEnd,
                            foods = meal.foods.orEmpty().map { f ->
                                DietPlanFood(
                                    rowId = f.id,
                                    foodId = f.foodId,
                                    foodName = f.foodName.orEmpty().trim(),
                                    quantity = f.quantity,
                                    unitName = f.unitName.orEmpty().trim(),
                                    calories = f.calories,
                                    isTaken = f.istaken,
                                )
                            },
                        )
                    }
                },
            )
        }
    }

    /** The body of a successful GET; anything else throws, so a failure is never read as "no data". */
    private suspend fun getBody(endpoint: String, params: Map<String, Any>, headers: Map<String, String> = emptyMap()): String? {
        val response = ApiHelper().callApi(context, endpoint, showNoConnectionToast = false) { url ->
            ApiClients.module4094.dynamicGet(url = url, headers = headers, params = params)
        }
        return if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
    }

    override suspend fun fetchDietPlans(pid: Int, clientId: Int): DomainResult<List<DietPlan>> = try {
        DomainResult.Success(parsePlans(getBody(endpoints.getAllDietPlanByPid, mapOf("pid" to pid, "clientId" to clientId))))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchDailyDietOverview(pid: Int, clientId: Int): DomainResult<List<DailyDietOverview>> = try {
        val json = getBody(endpoints.getDailyDietOverview, mapOf("pid" to pid, "clientId" to clientId))
        DomainResult.Success(gson.fromJson(responseList(json), Array<DailyDietOverview>::class.java).toList())
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun fetchPlanDay(pid: Int, dayId: Int, clientId: Int, planId: Int?): DomainResult<DietPlan?> = try {
        val json = getBody(endpoints.getDietPlanByDay, mapOf("pid" to pid, "dayId" to dayId, "clientId" to clientId), planDayHeaders)
        val plans = parsePlans(json)
        DomainResult.Success(if (planId != null) plans.firstOrNull { it.id == planId } else plans.maxByOrNull { it.id })
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun updateDietPlanStatus(id: Int, pid: Int, clientId: Int, isActive: Boolean): DomainResult<Unit> {
        val params = mapOf<String, Any>("id" to id, "pid" to pid, "clientId" to clientId, "isActive" to isActive)
        return dietWrite(endpoints.updateDietPlanStatus) { url -> ApiClients.module4094.dynamicPut(url = url, params = params) }
    }

    override suspend fun activatePlan(id: Int, pid: Int, clientId: Int): DomainResult<Unit> {
        val plans = when (val list = fetchDietPlans(pid, clientId)) {
            is DomainResult.Success -> list.data
            is DomainResult.Error -> return DomainResult.Error(list.exception)
        }
        val activated = updateDietPlanStatus(id, pid, clientId, true)
        if (activated is DomainResult.Error) return activated
        plans.filter { it.isActive && it.id != id }.forEach { other ->
            val off = updateDietPlanStatus(other.id, pid, clientId, false)
            if (off is DomainResult.Error) return off
        }
        return DomainResult.Success(Unit)
    }

    override suspend fun removeDietPlan(id: Int, pid: Int, clientId: Int): DomainResult<Unit> {
        val params = mapOf<String, Any>("id" to id, "pid" to pid, "clientId" to clientId)
        return dietWrite(endpoints.removeDietPlan) { url -> ApiClients.module4094.dynamicDelete(url = url, params = params) }
    }

    override suspend fun updateDietIntake(id: Int, pid: Int, clientId: Int, isTaken: Boolean): DomainResult<Unit> {
        val params = mapOf<String, Any>("id" to id, "pid" to pid, "clientId" to clientId, "isTaken" to isTaken)
        return dietWrite(endpoints.updateDietIntake) { url -> ApiClients.module4094.dynamicPut(url = url, params = params) }
    }

    /** A write that succeeds only on HTTP success with status 1 (when the body has a status). */
    private suspend fun dietWrite(
        endpoint: String,
        call: suspend (String) -> retrofit2.Response<okhttp3.ResponseBody>,
    ): DomainResult<Unit> = try {
        val response = ApiHelper().callApi(context, endpoint, showNoConnectionToast = false, apiCall = call)
        val text = response.body()?.string()
        val status = runCatching { gson.fromJson(text, JsonObject::class.java)?.get("status")?.asInt }.getOrNull()
        when {
            !response.isSuccessful -> DomainResult.Error(Exception("API Error: ${response.code()}"))
            status != null && status != 1 -> DomainResult.Error(Exception(serverMessage(text) ?: "Request failed."))
            else -> DomainResult.Success(Unit)
        }
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun insertDietPlan(pid: Int, clientId: Int, userId: Int, plan: DietPlan, requiredCalories: Int): DomainResult<Unit> = try {
        val dietJson = DietJson(
            days = plan.days.toSortedMap().mapNotNull { (dayId, meals) ->
                val filled = meals.filter { it.foods.isNotEmpty() }.map { meal ->
                    DietJsonMeal(
                        mealTimeId = meal.mealTime.id,
                        foods = meal.foods.map { DietJsonFood(it.foodId, it.quantity, it.unitName.trim(), it.calories) },
                    )
                }
                if (filled.isEmpty()) null else DietJsonDay(dayId, filled)
            }
        )
        val body = mapOf<String, Any>(
            "pid" to pid,
            "clientId" to clientId,
            "dietPlanName" to plan.name,
            "dietJson" to gson.toJson(dietJson),
            "requiredCalories" to requiredCalories,
            "userId" to userId,
        )
        val response = ApiHelper().callApi(context, endpoints.insertDietPlan, showNoConnectionToast = true) { url ->
            ApiClients.module4094.dynamicRawPost(url = url, headers = slowHeaders, body = body)
        }
        val text = response.body()?.string()
        val status = runCatching { gson.fromJson(text, JsonObject::class.java)?.get("status")?.asInt }.getOrNull()
        when {
            !response.isSuccessful -> DomainResult.Error(Exception("API Error: ${response.code()}"))
            status != null && status != 1 -> DomainResult.Error(Exception(serverMessage(text) ?: "Couldn't save the diet plan."))
            else -> DomainResult.Success(Unit)
        }
    } catch (e: Exception) { DomainResult.Error(e) }

    private fun serverMessage(text: String?): String? = runCatching {
        gson.fromJson(text, JsonObject::class.java)?.get("message")?.asString
    }.getOrNull()?.takeIf { it.isNotBlank() }

    // ── Foods ───────────────────────────────────────────────────────────────────

    override suspend fun searchFoods(prefix: String): DomainResult<List<NutriFood>> = try {
        // The API returns one row per matching synonym, so the same food can repeat.
        val list = nutri.searchFoods(prefix)
            .filter { it.foodId > 0 && it.displayName.isNotEmpty() }
            .distinctBy { it.foodId }
        DomainResult.Success(list)
    } catch (e: Exception) { DomainResult.Error(e) }

    private suspend fun units(foodId: Int): List<NutriUnit> =
        unitsCache[foodId] ?: nutri.foodUnits(foodId).also { unitsCache[foodId] = it }

    override suspend fun foodUnits(foodId: Int): DomainResult<List<NutriUnit>> = try {
        DomainResult.Success(units(foodId))
    } catch (e: Exception) { DomainResult.Error(e) }

    private suspend fun nutritionOf(foodId: Int, quantity: Double, unitId: Int): FoodNutrition {
        val key = "$foodId|$quantity|$unitId"
        nutritionCache[key]?.let { return it }
        val list = nutri.nutrients(foodId, quantity, unitId)
        fun find(vararg names: String): Double =
            (list.firstOrNull { n -> names.any { n.nutrientName.trim().equals(it, ignoreCase = true) } }
                ?: list.firstOrNull { n -> names.any { n.nutrientName.contains(it, ignoreCase = true) } })
                ?.amount ?: 0.0
        return FoodNutrition(
            kcal = find("Energy", "Calories", "Kcal"),
            carbs = find("Carbohydrate", "Carb"),
            protein = find("Protein"),
            fat = find("FAT", "Total Fat"),
            fiber = find("Dietary Fiber", "Fiber", "Fibre"),
            sugar = find("Sugar", "Sugars"),
            sodium = find("Sodium"),
        ).also { nutritionCache[key] = it }
    }

    override suspend fun nutrition(foodId: Int, quantity: Double, unitId: Int): DomainResult<FoodNutrition> = try {
        DomainResult.Success(nutritionOf(foodId, quantity, unitId))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun defaultServing(foodId: Int): DomainResult<Pair<NutriUnit, FoodNutrition>> = try {
        val list = units(foodId)
        // A household serving ("piece", "bowl") reads better than raw grams.
        val unit = list.firstOrNull { it.isDefault }
            ?: list.firstOrNull { !it.unitName.trim().equals("gm", true) }
            ?: list.firstOrNull()
            ?: throw Exception("No serving sizes for this food")
        val qty = if (unit.unitName.trim().equals("gm", true)) 100.0 else 1.0
        DomainResult.Success(unit to nutritionOf(foodId, qty, unit.id))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun unitIdFor(foodId: Int, unitName: String): Int? = runCatching {
        val list = units(foodId)
        val name = unitName.trim()
        (list.firstOrNull { it.unitName.trim().equals(name, true) }
            ?: list.firstOrNull { it.unitName.substringBefore('(').trim().equals(name.substringBefore('(').trim(), true) }
            ?: list.firstOrNull { it.isDefault }
            ?: list.firstOrNull())?.id
    }.getOrNull()

    // ── Local ───────────────────────────────────────────────────────────────────
}
