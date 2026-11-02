package com.critetiontech.ctvitalio.domain.usecase.diet

import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.DietRepository
import com.critetiontech.ctvitalio.model.DietPlan
import com.critetiontech.ctvitalio.model.FoodNutrition

class FetchDietPlansUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(pid: Int, clientId: Int): DomainResult<List<DietPlan>> =
        repository.fetchDietPlans(pid, clientId)
}

class InsertDietPlanUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(pid: Int, clientId: Int, userId: Int, plan: DietPlan): DomainResult<Unit> =
        repository.insertDietPlan(pid, clientId, userId, plan)
}

class SearchFoodsUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(prefix: String): DomainResult<List<NutriFood>> = repository.searchFoods(prefix)
}

class FetchFoodUnitsUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(foodId: Int): DomainResult<List<NutriUnit>> = repository.foodUnits(foodId)
}

class FetchFoodNutritionUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(foodId: Int, quantity: Double, unitId: Int): DomainResult<FoodNutrition> =
        repository.nutrition(foodId, quantity, unitId)
}

class FetchDefaultServingUseCase(private val repository: DietRepository) {
    suspend operator fun invoke(foodId: Int): DomainResult<Pair<NutriUnit, FoodNutrition>> =
        repository.defaultServing(foodId)
}
