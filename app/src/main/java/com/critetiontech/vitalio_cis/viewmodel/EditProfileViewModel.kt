package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class EditProfileViewModel : ViewModel() {

    private val updateProfileUseCase = AppDependencies.updateProfile()
    private val prefs = AppDependencies.prefs

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _updateSuccess = MutableStateFlow(false)
    val updateSuccess: StateFlow<Boolean> = _updateSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun resetSuccess() { _updateSuccess.value = false }
    fun clearError() { _errorMessage.value = null }

    fun updateProfile(firstName: String, lastName: String, dob: String, mobileNo: String, emailId: String, address: String, zip: String, imageFile: File?) {
        viewModelScope.launch {
            _loading.value = true
            val patient = prefs.getPatient() ?: run { _errorMessage.value = "Session expired"; _loading.value = false; return@launch }
            when (val r = updateProfileUseCase(patient, firstName, lastName, dob, mobileNo, emailId, address, zip, imageFile)) {
                is DomainResult.Success -> _updateSuccess.value = true
                is DomainResult.Error -> { _errorMessage.value = r.exception.message ?: "Update failed"; Log.e("EditProfileVM", r.exception.message.orEmpty()) }
            }
            _loading.value = false
        }
    }
}
