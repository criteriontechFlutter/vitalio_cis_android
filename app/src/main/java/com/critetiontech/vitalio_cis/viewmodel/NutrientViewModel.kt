package com.critetiontech.ctvitalio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.data.repository.NutriFood
import com.critetiontech.ctvitalio.data.repository.NutriNutrient
import com.critetiontech.ctvitalio.data.repository.NutriRepository
import com.critetiontech.ctvitalio.data.repository.NutriUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Search a food, pick its serving, see the nutrients (NutriAnalyser). */
class NutrientViewModel(private val repo: NutriRepository = NutriRepository()) : ViewModel() {

    val isConfigured: Boolean get() = repo.isConfigured

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<NutriFood>>(emptyList())
    val results: StateFlow<List<NutriFood>> = _results.asStateFlow()

    private val _searching = MutableStateFlow(false)
    val searching: StateFlow<Boolean> = _searching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    private val _selected = MutableStateFlow<NutriFood?>(null)
    val selected: StateFlow<NutriFood?> = _selected.asStateFlow()

    private val _units = MutableStateFlow<List<NutriUnit>>(emptyList())
    val units: StateFlow<List<NutriUnit>> = _units.asStateFlow()

    private val _unit = MutableStateFlow<NutriUnit?>(null)
    val unit: StateFlow<NutriUnit?> = _unit.asStateFlow()

    private val _quantity = MutableStateFlow(1.0)
    val quantity: StateFlow<Double> = _quantity.asStateFlow()

    private val _nutrients = MutableStateFlow<List<NutriNutrient>>(emptyList())
    val nutrients: StateFlow<List<NutriNutrient>> = _nutrients.asStateFlow()

    private val _detailLoading = MutableStateFlow(false)
    val detailLoading: StateFlow<Boolean> = _detailLoading.asStateFlow()

    private val _detailError = MutableStateFlow<String?>(null)
    val detailError: StateFlow<String?> = _detailError.asStateFlow()

    private var searchJob: Job? = null
    private var detailJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
        searchJob?.cancel()
        val q = text.trim()
        if (q.length < MIN_CHARS) {
            _results.value = emptyList(); _searching.value = false; _searchError.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // wait for the user to pause typing
            _searching.value = true
            _searchError.value = null
            runCatching { repo.searchFoods(q) }
                .onSuccess { list ->
                    // The API returns one row per matching synonym, so the same food can repeat.
                    _results.value = list
                        .filter { f -> f.foodId > 0 && f.displayName.isNotEmpty() }
                        .distinctBy { f -> f.foodId to f.displayName.lowercase() }
                }
                .onFailure { _results.value = emptyList(); _searchError.value = it.message }
            _searching.value = false
        }
    }

    fun select(food: NutriFood) {
        _selected.value = food
        _units.value = emptyList(); _unit.value = null; _nutrients.value = emptyList()
        _quantity.value = 1.0
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            _detailLoading.value = true
            _detailError.value = null
            runCatching { repo.foodUnits(food.foodId) }
                .onSuccess { list ->
                    _units.value = list
                    // A household serving ("piece", "bowl") reads better than raw grams.
                    val pick = list.firstOrNull { it.isDefault }
                        ?: list.firstOrNull { !it.unitName.trim().equals("gm", true) }
                        ?: list.firstOrNull()
                    _unit.value = pick
                    if (pick == null) {
                        _detailError.value = "No serving sizes are available for this food."
                        _detailLoading.value = false
                    } else loadNutrients()
                }
                .onFailure { _detailError.value = it.message; _detailLoading.value = false }
        }
    }

    fun clearSelection() {
        detailJob?.cancel()
        _selected.value = null
        _nutrients.value = emptyList(); _units.value = emptyList(); _unit.value = null
        _detailError.value = null; _detailLoading.value = false
    }

    fun setUnit(unit: NutriUnit) {
        if (unit == _unit.value) return
        _unit.value = unit
        // 1 gm is meaningless; start grams at 100.
        if (unit.unitName.trim().equals("gm", true) && _quantity.value < 10) _quantity.value = 100.0
        else if (!unit.unitName.trim().equals("gm", true) && _quantity.value >= 10) _quantity.value = 1.0
        loadNutrients()
    }

    fun setQuantity(q: Double) {
        val v = q.coerceIn(MIN_QTY, MAX_QTY)
        if (v == _quantity.value) return
        _quantity.value = v
        loadNutrients(debounce = true)
    }

    fun retryDetail() {
        val food = _selected.value ?: return
        if (_unit.value == null) select(food) else loadNutrients()
    }

    private fun loadNutrients(debounce: Boolean = false) {
        val food = _selected.value ?: return
        val unit = _unit.value ?: return
        detailJob?.cancel()
        detailJob = viewModelScope.launch {
            if (debounce) delay(300)
            _detailLoading.value = true
            _detailError.value = null
            runCatching { repo.nutrients(food.foodId, _quantity.value, unit.id) }
                .onSuccess {
                    _nutrients.value = it.filter { n -> n.nutrientName.isNotBlank() }
                    if (_nutrients.value.isEmpty()) _detailError.value = "No nutrient data for this serving."
                }
                .onFailure { _nutrients.value = emptyList(); _detailError.value = it.message }
            _detailLoading.value = false
        }
    }

    companion object {
        const val MIN_CHARS = 2
        const val MIN_QTY = 0.5
        const val MAX_QTY = 2000.0
    }
}
