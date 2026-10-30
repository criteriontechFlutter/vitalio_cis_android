package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.utils.MyApplication
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val sendOtpUseCase = AppDependencies.sendOtp()

    var mobile by mutableStateOf("")
        private set

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    fun onMobileChange(value: String) { mobile = value }

    fun sendOTP() {
        viewModelScope.launch {
            _loading.value = true
            _errorMessage.value = null
            when (val result = sendOtpUseCase(mobile)) {
                is DomainResult.Success -> {
                    if (result.data) _navigationEvent.emit("otp/$mobile")
                    else _errorMessage.value = MyApplication.appContext.getString(R.string.auth_login_error_not_registered)
                }
                is DomainResult.Error -> {
                    _errorMessage.value = result.exception.message ?: MyApplication.appContext.getString(R.string.auth_common_error_unknown)
                    Log.e("LoginViewModel", "sendOTP error", result.exception)
                }
            }
            _loading.value = false
        }
    }

}
