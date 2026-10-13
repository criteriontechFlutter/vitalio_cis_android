package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ── location data models ──────────────────────────────────────────────────
data class LocationItem(val id: Int, val name: String)

data class LocationState(
    val countries: List<LocationItem> = emptyList(),
    val states: List<LocationItem> = emptyList(),
    val cities: List<LocationItem> = emptyList(),
    val selectedCountryId: Int? = null,
    val selectedCountryName: String = "",
    val selectedStateId: Int? = null,
    val selectedStateName: String = "",
    val selectedCityId: Int? = null,
    val selectedCityName: String = ""
)

class EditProfileViewModel : ViewModel() {

    private val updateProfileUseCase = AppDependencies.updateProfile()
    private val prefs = AppDependencies.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // ── location state ────────────────────────────────────────────────────
    private val _locationState = MutableStateFlow(LocationState())
    val locationState: StateFlow<LocationState> = _locationState.asStateFlow()

    fun resetSuccess() {
        _updateSuccess.value = false
    }

    fun resetError() {
        _errorMessage.value = null
    }

    // ── load country list from assets + pre-select saved values ──────────
    fun loadLocationData(countryId: Int, stateId: Int, cityId: Int) {
        viewModelScope.launch {
            try {
                val countryList = AppDependencies.profileRepository.getCountries()
                val selectedCountry = countryList.firstOrNull { it.id == countryId }
                    ?: countryList.firstOrNull { it.id == 101 }
                    ?: countryList.firstOrNull()

                _locationState.value = _locationState.value.copy(
                    countries = countryList,
                    selectedCountryId = selectedCountry?.id,
                    selectedCountryName = selectedCountry?.name ?: ""
                )

                if (selectedCountry != null) {
                    val stateList = AppDependencies.profileRepository.getStates(selectedCountry.id)
                    val selectedState = stateList.firstOrNull { it.id == stateId }

                    _locationState.value = _locationState.value.copy(
                        states = stateList,
                        selectedStateId = selectedState?.id ?: if (stateId > 0) stateId else null,
                        selectedStateName = selectedState?.name ?: ""
                    )

                    if (selectedState != null || stateId > 0) {
                        val targetStateId = selectedState?.id ?: stateId
                        val cityList = AppDependencies.profileRepository.getCities(targetStateId)
                        val selectedCity = cityList.firstOrNull { it.id == cityId }

                        _locationState.value = _locationState.value.copy(
                            cities = cityList,
                            selectedCityId = selectedCity?.id ?: if (cityId > 0) cityId else null,
                            selectedCityName = selectedCity?.name ?: ""
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("EditProfileVM", "loadLocationData failed: ${e.message}")
            }
        }
    }

    fun onCountrySelected(countryId: Int, countryName: String) {
        _locationState.value = _locationState.value.copy(
            selectedCountryId = countryId,
            selectedCountryName = countryName,
            states = emptyList(),
            selectedStateId = null,
            selectedStateName = "",
            cities = emptyList(),
            selectedCityId = null,
            selectedCityName = ""
        )
        viewModelScope.launch {
            try {
                val stateList = AppDependencies.profileRepository.getStates(countryId)
                _locationState.value = _locationState.value.copy(states = stateList)
            } catch (e: Exception) {
                Log.e("EditProfileVM", "getStates failed: ${e.message}")
            }
        }
    }

    fun onStateSelected(stateId: Int, stateName: String) {
        _locationState.value = _locationState.value.copy(
            selectedStateId = stateId,
            selectedStateName = stateName,
            cities = emptyList(),
            selectedCityId = null,
            selectedCityName = ""
        )
        viewModelScope.launch {
            try {
                val cityList = AppDependencies.profileRepository.getCities(stateId)
                _locationState.value = _locationState.value.copy(cities = cityList)
            } catch (e: Exception) {
                Log.e("EditProfileVM", "getCities failed: ${e.message}")
            }
        }
    }

    fun onCitySelected(cityId: Int, cityName: String) {
        _locationState.value = _locationState.value.copy(
            selectedCityId = cityId,
            selectedCityName = cityName
        )
    }

    // ── update profile ────────────────────────────────────────────────────
    fun updateProfile(
        firstName: String,
        lastName: String,
        dob: String,
        mobileNo: String,
        emailId: String,
        address: String,
        zip: String,
        imageFile: File?,
        gender: String = "",
        bloodGroupId: Int = 0,
        weight: String = "",
        countryId: Int = 101,
        stateId: Int = 0,
        cityId: Int = 0
    ) {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run {
                _errorMessage.value = "Session expired"
                _loading.value = false
                return@launch
            }

            // Map gender string to id
            val genderId = when (gender) {
                "Male" -> 1
                "Female" -> 2
                else -> if (gender.isNotBlank()) 3 else patient.genderId
            }
            val genderNameStr = when (genderId) {
                1 -> "Male"; 2 -> "Female"; else -> "Other"
            }
            val resolvedBloodGroupId = if (bloodGroupId > 0) bloodGroupId else patient.bloodGroupId
            val bloodGroupNameStr = when (resolvedBloodGroupId) {
                1 -> "A+"; 2 -> "A-"; 3 -> "B+"; 4 -> "B-"
                5 -> "O+"; 6 -> "O-"; 7 -> "AB+"; 8 -> "AB-"
                else -> patient.bloodGroupName ?: ""
            }

            val updatedPatient = patient.copy(
                genderId = genderId,
                genderName = genderNameStr,
                bloodGroupId = resolvedBloodGroupId,
                bloodGroupName = bloodGroupNameStr,
                countryId = countryId,
                countryName = _locationState.value.selectedCountryName.ifBlank { patient.countryName },
                stateId = stateId,
                stateName = _locationState.value.selectedStateName.ifBlank { patient.stateName },
                cityId = cityId,
                cityName = _locationState.value.selectedCityName.ifBlank { patient.cityName }
            )

            when (val r = updateProfileUseCase(
                updatedPatient,
                firstName,
                lastName,
                dob,
                mobileNo,
                emailId,
                address,
                zip,
                imageFile
            )) {
                is DomainResult.Success -> {
                    // Also persist weight locally if provided
                    if (weight.isNotBlank()) {
                        prefs.savePatient(
                            prefs.getPatient()?.copy() ?: updatedPatient
                        )
                    }
                    _updateSuccess.value = true
                }
                is DomainResult.Error -> {
                    _errorMessage.value = r.exception.message ?: "Update failed"
                    Log.e("EditProfileVM", r.exception.message.orEmpty())
                }
            }
            _loading.value = false
        }
    }
}
