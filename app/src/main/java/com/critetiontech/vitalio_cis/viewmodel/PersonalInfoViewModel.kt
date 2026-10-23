package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.utils.Patient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Personal Info: what login (VerifyOTP) and Update Profile saved for the patient. */
class PersonalInfoViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs
    private val fetchStates = AppDependencies.fetchStates()
    private val fetchCities = AppDependencies.fetchCities()

    private val _patient = MutableStateFlow(prefs.getPatient())
    val patient: StateFlow<Patient?> = _patient.asStateFlow()

    /** Re-reads the saved patient (it changes after Edit Profile) and fills in place names. */
    fun load() {
        _patient.value = prefs.getPatient()
        resolvePlaceNames()
    }

    /** Login only gives city/state ids: look their names up once and keep them. */
    private fun resolvePlaceNames() {
        val p = _patient.value ?: return
        val needsState = p.stateName.isBlank() && p.stateId > 0
        val needsCity = p.cityName.isBlank() && p.cityId > 0
        if (!needsState && !needsCity) return
        viewModelScope.launch {
            try {
                val countryId = p.countryId.takeIf { it > 0 } ?: INDIA
                val state = if (needsState) fetchStates(countryId).firstOrNull { it.id == p.stateId } else null
                val city = if (needsCity && p.stateId > 0) fetchCities(p.stateId).firstOrNull { it.id == p.cityId } else null
                if (state == null && city == null) return@launch
                val updated = p.copy(
                    stateName = p.stateName.ifBlank { state?.name.orEmpty() },
                    cityName = p.cityName.ifBlank { city?.name.orEmpty() },
                    countryId = countryId,
                    countryName = p.countryName.ifBlank { if (countryId == INDIA) "India" else "" }
                )
                prefs.savePatient(updated)
                _patient.value = updated
            } catch (e: Exception) {
                Log.w("PersonalInfoVM", "place names not resolved: ${e.message}")
            }
        }
    }

    private companion object { const val INDIA = 101 }
}
