package com.critetiontech.vitalio_cis.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.vitalio_cis.Routes
import com.critetiontech.vitalio_cis.di.AppDependencies
import com.critetiontech.vitalio_cis.domain.model.DomainResult
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class OTPViewModel : ViewModel() {

    private val verifyOtpUseCase = AppDependencies.verifyOtp()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    fun verifyOtp(uhid: String, otp: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val deviceToken = if (task.isSuccessful) task.result else ""
            viewModelScope.launch {
                _loading.value = true
                _errorMessage.value = null
                when (val result = verifyOtpUseCase(uhid, otp, deviceToken)) {
                    is DomainResult.Success -> _navigationEvent.emit(Routes.DASHBOARD)
                    is DomainResult.Error -> {
                        _errorMessage.value = result.exception.message ?: "OTP verification failed"
                        Log.e("OTPViewModel", "verifyOtp error", result.exception)
                    }
                }
                _loading.value = false
            }
        }
    }
}
