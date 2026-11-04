package com.critetiontech.ctvitalio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.FoodNutrition
import com.critetiontech.ctvitalio.model.PickedFood
import com.critetiontech.ctvitalio.model.RecentFood
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.Locale

/** Add Food (search) → Nutrition Facts (serving + nutrients) → a [PickedFood] for the caller. */
class DietFoodViewModel : ViewModel() {

    enum class Filter { ALL, HIGH_PROTEIN, LOW_CALORIE }
    enum class Sort { POPULARITY, CALORIES, PROTEIN }

    /** A food row: the food and, once loaded, its default serving and nutrition. */
    data class Preview(val unit: NutriUnit, val quantity: Double, val nutrition: FoodNutrition)

    private val deps = AppDependencies
    private val repo = deps.dietRepository
    private val prefs = deps.prefs

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<NutriFood>>(emptyList())
    val results: StateFlow<List<NutriFood>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _previews = MutableStateFlow<Map<Int, Preview>>(emptyMap())
    val previews: StateFlow<Map<Int, Preview>> = _previews.asStateFlow()

    private val _filter = MutableStateFlow(Filter.ALL)
    val filter: StateFlow<Filter> = _filter.asStateFlow()

    private val _sort = MutableStateFlow(Sort.POPULARITY)
    val sort: StateFlow<Sort> = _sort.asStateFlow()

    // No recent-foods API yet, so the section stays hidden.
    private val _recent = MutableStateFlow(emptyList<RecentFood>())
    val recent: StateFlow<List<RecentFood>> = _recent.asStateFlow()

    // ── Nutrition Facts ──
    private val _selected = MutableStateFlow<NutriFood?>(null)
    val selected: StateFlow<NutriFood?> = _selected.asStateFlow()

    private val _units = MutableStateFlow<List<NutriUnit>>(emptyList())
    val units: StateFlow<List<NutriUnit>> = _units.asStateFlow()

    private val _unit = MutableStateFlow<NutriUnit?>(null)
    val unit: StateFlow<NutriUnit?> = _unit.asStateFlow()

    private val _quantity = MutableStateFlow(1.0)
    val quantity: StateFlow<Double> = _quantity.asStateFlow()

    private val _nutrition = MutableStateFlow<FoodNutrition?>(null)
    val nutrition: StateFlow<FoodNutrition?> = _nutrition.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null
    private val previewJobs = mutableSetOf<Int>()
    // Each preview is two NutriAnalyser calls; don't fire a whole page of them at once.
    private val previewLimit = Semaphore(4)

    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()
        val q = text.trim()
        if (q.length < MIN_CHARS) {
            _results.value = emptyList(); _searching.value = false; _searchError.value = null
            _filter.value = Filter.ALL
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // wait for the user to pause typing
            _searching.value = true
            _searchError.value = null
            when (val r = deps.searchFoods()(q)) {
                is DomainResult.Success -> _results.value = r.data
                is DomainResult.Error -> { _results.value = emptyList(); _searchError.value = r.exception.message }
            }
            _searching.value = false
        }
    }

    fun setFilter(f: Filter) { _filter.value = f }
    fun setSort(s: Sort) { _sort.value = s }

    /** Results after the chip filter and sort. Foods whose numbers aren't in yet stay in place. */
    fun visible(results: List<NutriFood>, previews: Map<Int, Preview>, filter: Filter, sort: Sort): List<NutriFood> {
        val filtered = when (filter) {
            Filter.ALL -> results
            Filter.HIGH_PROTEIN -> results.filter { (previews[it.foodId]?.nutrition?.protein ?: 0.0) >= HIGH_PROTEIN_G }
            Filter.LOW_CALORIE -> results.filter { previews[it.foodId]?.nutrition?.kcal?.let { k -> k in 0.1..LOW_CALORIE_KCAL } == true }
        }
        return when (sort) {
            Sort.POPULARITY -> filtered
            Sort.CALORIES -> filtered.sortedBy { previews[it.foodId]?.nutrition?.kcal ?: Double.MAX_VALUE }
            Sort.PROTEIN -> filtered.sortedByDescending { previews[it.foodId]?.nutrition?.protein ?: -1.0 }
        }
    }

    /** Loads the kcal/protein line for a row as it scrolls into view. */
    fun requestPreview(food: NutriFood) {
        if (food.foodId in _previews.value || !previewJobs.add(food.foodId)) return
        viewModelScope.launch {
            previewLimit.withPermit {
                when (val r = deps.fetchDefaultServing()(food.foodId)) {
                    is DomainResult.Success -> {
                        val (unit, n) = r.data
                        val qty = if (unit.isGrams) 100.0 else 1.0
                        _previews.value = _previews.value + (food.foodId to Preview(unit, qty, n))
                    }
                    is DomainResult.Error -> previewJobs.remove(food.foodId)
                }
            }
        }
    }

    fun select(food: NutriFood) {
        _selected.value = food
        _units.value = emptyList(); _unit.value = null; _nutrition.value = null
        _quantity.value = 1.0
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null
            when (val r = deps.fetchFoodUnits()(food.foodId)) {
                is DomainResult.Success -> {
                    _units.value = r.data
                    val pick = _previews.value[food.foodId]?.unit?.takeIf { it in r.data }
                        ?: r.data.firstOrNull { it.isDefault }
                        ?: r.data.firstOrNull { !it.isGrams }
                        ?: r.data.firstOrNull()
                    _unit.value = pick
                    if (pick == null) {
                        _detailError.value = "No serving sizes for this food."
                        _detailLoading.value = false
                    } else {
                        if (pick.isGrams) _quantity.value = 100.0
                        loadNutrition()
                    }
                }
                is DomainResult.Error -> { _detailError.value = r.exception.message; _detailLoading.value = false }
            }
        }
    }

    /** Opens Nutrition Facts for a "Recent Searched" row. */
    fun selectRecent(food: RecentFood) =
        select(NutriFood(foodId = food.foodId, foodName = food.foodName, foodImagePath = food.imageUrl))

    fun clearSelection() {
        detailJob?.cancel()
        _selected.value = null
        _nutrition.value = null; _units.value = emptyList(); _unit.value = null
        _detailError.value = null; _detailLoading.value = false
    }

    fun setUnit(unit: NutriUnit) {
        if (unit == _unit.value) return
        _unit.value = unit
        // 1 gm is meaningless; start grams at 100.
        if (unit.isGrams && _quantity.value < 10) _quantity.value = 100.0
        else if (!unit.isGrams && _quantity.value >= 10) _quantity.value = 1.0
        loadNutrition()
    }

    /** − / + on the quantity stepper: halves for servings, 10 g steps for grams. */
    fun stepQuantity(up: Boolean) {
        val step = if (_unit.value?.isGrams == true) 10.0 else 0.5
        val v = (_quantity.value + if (up) step else -step).coerceIn(if (_unit.value?.isGrams == true) 10.0 else MIN_QTY, MAX_QTY)
        if (v == _quantity.value) return
        _quantity.value = v
        loadNutrition(debounce = true)
    }

    fun retryDetail() {
        val food = _selected.value ?: return
        if (_unit.value == null) select(food) else loadNutrition()
    }

    private fun loadNutrition(debounce: Boolean = false) {
        val food = _selected.value ?: return
        val unit = _unit.value ?: return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            if (debounce) delay(300)
            _detailLoading.value = true
            _detailError.value = null
            when (val r = deps.fetchFoodNutrition()(food.foodId, _quantity.value, unit.id)) {
                is DomainResult.Success -> _nutrition.value = r.data
                is DomainResult.Error -> { _nutrition.value = null; _detailError.value = r.exception.message }
            }
            _detailLoading.value = false
        }
    }

    /** The food on Nutrition Facts, as chosen. Null until its nutrition has loaded. */
    fun pickSelected(mealTimeId: Int): PickedFood? {
        val food = _selected.value ?: return null
        val unit = _unit.value ?: return null
        val n = _nutrition.value ?: return null
        return picked(mealTimeId, food, unit, _quantity.value, n)
    }

    /** The "+" on a search row: that food's default serving, without opening Nutrition Facts. */
    fun quickPick(mealTimeId: Int, food: NutriFood): PickedFood? {
        val p = _previews.value[food.foodId] ?: return null
        return picked(mealTimeId, food, p.unit, p.quantity, p.nutrition)
    }

    /** The "+" on a recent row: the serving it was last added with. */
    fun quickPickRecent(mealTimeId: Int, food: RecentFood): PickedFood =
        picked(
            mealTimeId,
            NutriFood(foodId = food.foodId, foodName = food.foodName, foodImagePath = food.imageUrl),
            NutriUnit(id = food.unitId, unitName = food.unitName),
            food.quantity,
            food.nutrition,
        )

    private fun picked(mealTimeId: Int, food: NutriFood, unit: NutriUnit, qty: Double, n: FoodNutrition): PickedFood {
        return PickedFood(mealTimeId, food.foodId, food.displayName, food.foodImagePath, unit.id, unit.unitName, qty, n)
    }

    companion object {
        const val MIN_CHARS = 2
        const val MIN_QTY = 0.5
        const val MAX_QTY = 2000.0
        private const val HIGH_PROTEIN_G = 10.0
        private const val LOW_CALORIE_KCAL = 150.0

        val NutriUnit.isGrams: Boolean get() = unitName.trim().equals("gm", true)

        fun formatQty(q: Double): String =
            if (q % 1.0 == 0.0) q.toLong().toString() else String.format(Locale.US, "%.1f", q)

        fun servingText(qty: Double, unit: NutriUnit): String = "${formatQty(qty)} ${unit.unitName.trim()}"
    }
}
