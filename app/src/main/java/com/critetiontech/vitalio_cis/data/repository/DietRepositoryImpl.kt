package com.critetiontech.ctvitalio.data.repository

import android.content.Context
import com.critetiontech.ctvitalio.data.remote.network.ApiClients
import com.critetiontech.ctvitalio.data.remote.network.ApiHelper
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DietRepository
import com.critetiontech.ctvitalio.model.DietJson
import com.critetiontech.ctvitalio.model.DietJsonDay
import com.critetiontech.ctvitalio.model.DietJsonFood
import com.critetiontech.ctvitalio.model.DietJsonMeal
import com.critetiontech.ctvitalio.model.DietLogEntry
import com.critetiontech.ctvitalio.model.DietMeal
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.DietPlanFood
import com.critetiontech.ctvitalio.model.DietPlanResponse
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.MealTime
import com.critetiontech.ctvitalio.model.RecentFood
import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.utils.ApiEndPointCorporateModule
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import java.time.LocalDate
import java.util.concurrent.ConcurrentHashMap

class DietRepositoryImpl(
    private val context: Context,
    private val prefs: PrefsManager,
    private val nutri: NutriRepository = NutriRepository(),
) : DietRepository {
    private val endpoints = ApiEndPointCorporateModule()
    private val gson = Gson()

    // GetDietPlan takes ~20 s on the server; the default 20 s read timeout cuts it off.
    private val slowHeaders = mapOf(RetrofitInstance.READ_TIMEOUT_HEADER to "60")

    // NutriAnalyser answers don't change, so keep them for the life of the process.
    private val unitsCache = ConcurrentHashMap<Int, List<NutriUnit>>()
    private val nutritionCache = ConcurrentHashMap<String, FoodNutrition>()

    // Keys include pid so another patient on the same device never sees this one's diet.
    private fun plansKey(pid: Int) = "${endpoints.getDietPlan}_$pid"
    private fun activeKey(pid: Int) = "diet_active_plan_$pid"
    private fun requiredKey(pid: Int) = "diet_required_kcal_$pid"
    private fun createdKey(pid: Int) = "diet_plan_created_$pid"
    private fun logKey(pid: Int, date: LocalDate) = "diet_log_${pid}_$date"
    private fun recentKey(pid: Int) = "diet_recent_foods_$pid"

    // ── Diet plans ──────────────────────────────────────────────────────────────

    private fun parsePlans(json: String?): List<DietPlan> {
        if (json.isNullOrEmpty()) return emptyList()
        val parsed = gson.fromJson(json, DietPlanResponse::class.java)
        if (parsed.status != 1) return emptyList()
        return parsed.responseValue.orEmpty().map { dto ->
            DietPlan(
                id = dto.dietPlanId,
                name = dto.dietPlanName.orEmpty().trim(),
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
                                )
                            },
                        )
                    }
                },
            )
        }
    }

    override fun cachedDietPlans(pid: Int): List<DietPlan> =
        runCatching { parsePlans(prefs.get(plansKey(pid), String::class.java)) }.getOrDefault(emptyList())

    override suspend fun fetchDietPlans(pid: Int, clientId: Int): DomainResult<List<DietPlan>> = try {
        val params = mapOf("pid" to pid, "clientId" to clientId)
        val json = prefs.getData(key = plansKey(pid), shouldSave = true) {
            val response = ApiHelper().callApi(context, endpoints.getDietPlan, showNoConnectionToast = false) { url ->
                ApiClients.module4094.dynamicGet(url = url, headers = slowHeaders, params = params)
            }
            if (response.isSuccessful) response.body()?.string() else throw Exception("API Error: ${response.code()}")
        }
        DomainResult.Success(parsePlans(json))
    } catch (e: Exception) { DomainResult.Error(e) }

    override suspend fun insertDietPlan(pid: Int, clientId: Int, userId: Int, plan: DietPlan): DomainResult<Unit> = try {
        val dietJson = DietJson(
            days = plan.days.toSortedMap().mapNotNull { (dayId, meals) ->
                val filled = meals.filter { it.foods.isNotEmpty() }.map { meal ->
                    DietJsonMeal(
                        mealTimeId = meal.mealTime.id,
                        foods = meal.foods.map { DietJsonFood(it.foodId, it.quantity, it.unitName, it.calories) },
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

    private fun read(key: String): String? = prefs.getString(context, key)
    private fun write(key: String, value: String) = prefs.saveString(context, key, value)

    private inline fun <reified T> readList(key: String): List<T> = runCatching {
        gson.fromJson<List<T>>(read(key) ?: return emptyList(), object : TypeToken<List<T>>() {}.type)
    }.getOrNull().orEmpty()

    override fun activePlanId(pid: Int): Int? = read(activeKey(pid))?.toIntOrNull()

    override fun setActivePlanId(pid: Int, planId: Int?) {
        if (planId == null) prefs.clearKey(activeKey(pid)) else write(activeKey(pid), planId.toString())
    }

    override fun requiredKcal(pid: Int): Int = read(requiredKey(pid))?.toIntOrNull() ?: DEFAULT_REQUIRED_KCAL

    override fun setRequiredKcal(pid: Int, kcal: Int) = write(requiredKey(pid), kcal.toString())

    private fun createdMap(pid: Int): Map<String, Long> = runCatching {
        gson.fromJson<Map<String, Long>>(read(createdKey(pid)) ?: return emptyMap(), object : TypeToken<Map<String, Long>>() {}.type)
    }.getOrNull().orEmpty()

    override fun planCreatedAt(pid: Int, planId: Int): Long? = createdMap(pid)[planId.toString()]

    override fun markPlanCreated(pid: Int, planId: Int, at: Long) =
        write(createdKey(pid), gson.toJson(createdMap(pid) + (planId.toString() to at)))

    override fun logsFor(pid: Int, date: LocalDate): List<DietLogEntry> = readList(logKey(pid, date))

    override fun saveLog(pid: Int, date: LocalDate, entry: DietLogEntry) =
        write(logKey(pid, date), gson.toJson(logsFor(pid, date).filter { it.key != entry.key } + entry))

    override fun removeLog(pid: Int, date: LocalDate, key: String) =
        write(logKey(pid, date), gson.toJson(logsFor(pid, date).filter { it.key != key }))

    override fun recentFoods(pid: Int): List<RecentFood> = readList(recentKey(pid))

    override fun addRecentFood(pid: Int, food: RecentFood) =
        write(recentKey(pid), gson.toJson((listOf(food) + recentFoods(pid).filter { it.foodId != food.foodId }).take(MAX_RECENT)))

    companion object {
        const val DEFAULT_REQUIRED_KCAL = 2000
        private const val MAX_RECENT = 10
    }
}
