package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.R
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.Routes
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager
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
    val errorMessage: StateFlow<String?> = _errorMessage

    fun clearError() { _errorMessage.value = null }

    private val _navigationEvent = MutableSharedFlow<String>()
    val navigationEvent: SharedFlow<String> = _navigationEvent

    fun verifyOtp(uhid: String, otp: String) {
        // Auto-submit on the 6th digit and the Verify button can both fire; run only one at a time
        if (_loading.value) return
        // Show the loader right away — the FCM token fetch below can take a moment before the API call starts
        _loading.value = true
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            val prefs = PrefsManager(MyApplication.appContext)
            val fcmToken = if (task.isSuccessful) task.result else {
                Log.w("OTPViewModel", "FCM token fetch failed", task.exception)
                null
            }
            if (!fcmToken.isNullOrBlank()) prefs.saveDeviceToken(fcmToken)
            // Server rejects an empty deviceToken, so fall back to the cached token, then ANDROID_ID
            val deviceToken = fcmToken?.takeIf { it.isNotBlank() }
                ?: prefs.getDeviceToken()?.takeIf { it.isNotBlank() }
                ?: Settings.Secure.getString(MyApplication.appContext.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "unknown"
            viewModelScope.launch {
                _errorMessage.value = null
                when (val result = verifyOtpUseCase(uhid, otp, deviceToken)) {
                    is DomainResult.Success -> _navigationEvent.emit(Routes.DASHBOARD)
                    is DomainResult.Error -> {
                        _errorMessage.value = result.exception.message ?: MyApplication.appContext.getString(R.string.auth_otp_error_failed)
                        Log.e("OTPViewModel", "verifyOtp error", result.exception)
                    }
                }
                _loading.value = false
            }
        }
    }
}
