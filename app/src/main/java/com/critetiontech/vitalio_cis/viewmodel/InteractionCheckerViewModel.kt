package com.critetiontech.ctvitalio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.DrugInteractionItem
import com.critetiontech.ctvitalio.model.SearchMedicineItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class Drug(
    val id: String = java.util.UUID.randomUUID().toString(),
    val medicineId: Int = 0,
    val name: String,
    val activeIngredients: String
)

enum class InteractionSeverity {
    Caution, Harmful;
    val title: String get() = if (this == Caution) "Caution" else "Harmful"
}

data class InteractionInstructionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val severity: InteractionSeverity,
    val drugPair: String,
    val substituteMedicine: String,
    val interactionEffect: String,
    val suggestiveAction: String
)

class InteractionCheckerViewModel : ViewModel() {
    private val repository = AppDependencies.medicineRepository

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SearchMedicineItem>>(emptyList())
    val searchResults = _searchResults.asStateFlow()

    private val _selectedDrugs = MutableStateFlow<List<Drug>>(emptyList())
    val selectedDrugs = _selectedDrugs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isCheckingInteraction = MutableStateFlow(false)
    val isCheckingInteraction = _isCheckingInteraction.asStateFlow()

    private val _displayInstructions = MutableStateFlow<List<InteractionInstructionItem>>(emptyList())
    val displayInstructions = _displayInstructions.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()

    private var searchJob: Job? = null
    private val searchCache = mutableMapOf<String, List<SearchMedicineItem>>()
    private val interactionCache = mutableMapOf<String, List<InteractionInstructionItem>>()

    fun updateSearchText(text: String) {
        _searchText.value = text
        if (text.trim().length < 2) {
            _searchResults.value = emptyList()
            _isLoading.value = false
            searchJob?.cancel()
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300)
            val trimmed = text.trim()
            val cacheKey = trimmed.lowercase()
            if (searchCache.containsKey(cacheKey)) {
                _searchResults.value = searchCache[cacheKey] ?: emptyList()
                return@launch
            }
            _isLoading.value = true
            when (val result = repository.searchMedicine(trimmed)) {
                is DomainResult.Success -> {
                    searchCache[cacheKey] = result.data
                    _searchResults.value = result.data
                }
                is DomainResult.Error -> {
                    _searchResults.value = emptyList()
                }
            }
            _isLoading.value = false
        }
    }

    fun addDrug(med: SearchMedicineItem) {
        val name = if (med.brandName.isEmpty()) med.medicineName else med.brandName
        val ingredients = if (med.medicineName.isEmpty()) med.dosageFormName else "${med.medicineName} • ${med.dosageFormName}"
        
        val currentList = _selectedDrugs.value.toMutableList()
        if (currentList.none { it.medicineId == med.medicineID }) {
            currentList.add(Drug(medicineId = med.medicineID, name = name, activeIngredients = ingredients))
            _selectedDrugs.value = currentList
        }
        _searchText.value = ""
        _searchResults.value = emptyList()
    }

    fun removeDrug(drug: Drug) {
        _selectedDrugs.value = _selectedDrugs.value.filter { it.id != drug.id }
    }

    fun clearSelectedDrugs() {
        _selectedDrugs.value = emptyList()
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun fetchDrugInteractions(onComplete: (Boolean) -> Unit) {
        val drugs = _selectedDrugs.value
        if (drugs.isEmpty()) {
            onComplete(false)
            return
        }

        val brandIdString = drugs.map { it.medicineId.toString() }.filter { it.isNotEmpty() && it != "0" }.joinToString(",,,")
        
        if (interactionCache.containsKey(brandIdString)) {
            _displayInstructions.value = interactionCache[brandIdString] ?: emptyList()
            onComplete(true)
            return
        }

        viewModelScope.launch {
            _isCheckingInteraction.value = true
            _errorMessage.value = null

            when (val result = repository.getDrugInteraction(brandIdString)) {
                is DomainResult.Success -> {
                    val items = result.data
                    val instructions = items.map { item ->
                        val isCaution = item.interactionNature.contains("Caution", ignoreCase = true)
                        InteractionInstructionItem(
                            severity = if (isCaution) InteractionSeverity.Caution else InteractionSeverity.Harmful,
                            drugPair = item.interaction.trim(),
                            substituteMedicine = item.substitute.trim(),
                            interactionEffect = item.interactionEffect.trim(),
                            suggestiveAction = item.suggestiveAction.trim()
                        )
                    }
                    interactionCache[brandIdString] = instructions
                    _displayInstructions.value = instructions
                    _isCheckingInteraction.value = false
                    onComplete(true)
                }
                is DomainResult.Error -> {
                    _errorMessage.value = result.exception.message ?: "Failed to check interactions"
                    _displayInstructions.value = emptyList()
                    _isCheckingInteraction.value = false
                    onComplete(false)
                }
            }
        }
    }
}
