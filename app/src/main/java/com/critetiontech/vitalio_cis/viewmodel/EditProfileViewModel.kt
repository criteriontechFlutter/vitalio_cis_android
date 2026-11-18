package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.ui.feedback.Haptics

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.LocationItem
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

// ── location data models ──────────────────────────────────────────────────
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
    private val fetchCountries = AppDependencies.fetchCountries()
    private val fetchStates = AppDependencies.fetchStates()
    private val fetchCities = AppDependencies.fetchCities()
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
                val countryList = fetchCountries()
                val selectedCountry = countryList.firstOrNull { it.id == countryId }
                    ?: countryList.firstOrNull { it.id == 101 }
                    ?: countryList.firstOrNull()

                _locationState.value = _locationState.value.copy(
                    countries = countryList,
                    selectedCountryId = selectedCountry?.id,
                    selectedCountryName = selectedCountry?.name ?: ""
                )

                if (selectedCountry != null) {
                    val stateList = fetchStates(selectedCountry.id)
                    val selectedState = stateList.firstOrNull { it.id == stateId }

                    _locationState.value = _locationState.value.copy(
                        states = stateList,
                        selectedStateId = selectedState?.id ?: if (stateId > 0) stateId else null,
                        selectedStateName = selectedState?.name ?: ""
                    )

                    if (selectedState != null || stateId > 0) {
                        val targetStateId = selectedState?.id ?: stateId
                        val cityList = fetchCities(targetStateId)
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
                val stateList = fetchStates(countryId)
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
                val cityList = fetchCities(stateId)
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

    private val profileRepository = AppDependencies.profileRepository

    /**
     * The saved patient, with zip/country/city filled from the server when login left them blank
     * (it always does for zip). The filled copy is saved so this happens once per login.
     */
    private suspend fun completePatient(): com.critetiontech.ctvitalio.utils.Patient? {
        val saved = prefs.getPatient() ?: return null
        if (saved.zip.isNotBlank() && saved.countryId != 0) return saved
        val filled = profileRepository.fillMissingDetails(saved)
        if (filled != saved) prefs.savePatient(filled)
        return filled
    }

    /** Called when Edit Profile opens so the form shows the server's zip instead of an empty box. */
    fun prefillMissingDetails() {
        viewModelScope.launch { completePatient() }
    }

    // ── photo only ────────────────────────────────────────────────────────
    /** Outcome of [updatePhoto]; the UI shows it once and calls [clearPhotoResult]. */
    sealed interface PhotoResult {
        data object Updated : PhotoResult
        data class Failed(val reason: String?) : PhotoResult
    }

    private val _photoUploading = MutableStateFlow(false)
    val photoUploading: StateFlow<Boolean> = _photoUploading.asStateFlow()

    private val _photoResult = MutableStateFlow<PhotoResult?>(null)
    val photoResult: StateFlow<PhotoResult?> = _photoResult.asStateFlow()

    fun clearPhotoResult() { _photoResult.value = null }

    /**
     * Uploads just a new photo, the moment it's picked. The API only updates the whole profile, so
     * every other field is sent exactly as saved — never the half-edited values on the form.
     */
    fun updatePhoto(imageFile: File) {
        if (_photoUploading.value) return
        viewModelScope.launch {
            _photoUploading.value = true
            val patient = completePatient() ?: run {
                _photoResult.value = PhotoResult.Failed(MyApplication.appContext.getString(R.string.profile_session_expired_short))
                _photoUploading.value = false
                return@launch
            }
            if (patient.zip.isBlank()) {
                // The server rejects a profile update without Zip (400 "The Zip field is required.")
                _photoResult.value = PhotoResult.Failed(MyApplication.appContext.getString(R.string.profile_zip_required_for_photo))
                Haptics.error()
                _photoUploading.value = false
                return@launch
            }
            val r = updateProfileUseCase(
                patient,
                patient.firstName,
                patient.lastName.orEmpty(),
                patient.dob.substringBefore('T'),
                patient.mobileNo,
                patient.emailAddress.orEmpty(),
                patient.address.orEmpty(),
                patient.zip.orEmpty(),
                imageFile
            )
            _photoResult.value = when (r) {
                is DomainResult.Success -> { Haptics.success(); PhotoResult.Updated }
                is DomainResult.Error -> {
                    Log.e("EditProfileVM", "updatePhoto: ${r.exception.message}")
                    Haptics.error()
                    PhotoResult.Failed(r.exception.message)
                }
            }
            _photoUploading.value = false
        }
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
            val patient = completePatient() ?: run {
                _errorMessage.value = MyApplication.appContext.getString(R.string.profile_session_expired_short)
                _loading.value = false
                return@launch
            }

            // Map gender string to id
            val genderId = when (gender) {
                "Male" -> 1
                "Female" -> 2
                else -> if (gender.isNotBlank()) 3 else patient.genderId
            }
            val genderNameStr = com.critetiontech.ctvitalio.utils.genderNameFor(genderId)
            val resolvedBloodGroupId = if (bloodGroupId > 0) bloodGroupId else patient.bloodGroupId
            val bloodGroupNameStr = com.critetiontech.ctvitalio.utils.bloodGroupNameFor(resolvedBloodGroupId)
                ?: patient.bloodGroupName ?: ""

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
                zip.ifBlank { patient.zip }, // server requires Zip; an empty box keeps the saved one
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
                    Haptics.success()
                }
                is DomainResult.Error -> {
                    _errorMessage.value = r.exception.message ?: MyApplication.appContext.getString(R.string.profile_update_failed)
                    Haptics.error()
                    Log.e("EditProfileVM", r.exception.message.orEmpty())
                }
            }
            _loading.value = false
        }
    }
}
