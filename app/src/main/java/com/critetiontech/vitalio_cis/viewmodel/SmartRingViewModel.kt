package com.critetiontech.ctvitalio.viewmodel

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.model.UltrahumanDashboardData
import com.critetiontech.ctvitalio.ring.RingOAuthCallback
import com.critetiontech.ctvitalio.ring.SmartRingService
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Ultrahuman smart ring: OAuth connect (browser + redirect), persisted connection
 * state, and telemetry sync. Android port of the iOS OAuthViewModel.
 */
class SmartRingViewModel : ViewModel() {

    private val prefs = AppDependencies.prefs
    private val ctx: Context get() = MyApplication.appContext

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isAuthSuccess = MutableStateFlow(false)
    val isAuthSuccess: StateFlow<Boolean> = _isAuthSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _ringData = MutableStateFlow(UltrahumanDashboardData())
    val ringData: StateFlow<UltrahumanDashboardData> = _ringData

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing

    init {
        checkConnectionStatus()
        viewModelScope.launch {
            RingOAuthCallback.uri.filterNotNull().collect { uri ->
                RingOAuthCallback.consume()
                handleRedirect(uri)
            }
        }
    }

    // ── Status ────────────────────────────────────────────────────────────────
    fun checkConnectionStatus() {
        val connected = prefs.getString(ctx, KEY_CONNECTED) == "true"
        _isAuthSuccess.value = connected
        if (connected) viewModelScope.launch { loadTelemetry() }
    }

    /** Called when the screen resumes; clears the spinner if the user came back without finishing login. */
    fun onScreenResumed() {
        if (RingOAuthCallback.uri.value == null && !processingRedirect) _isLoading.value = false
    }

    fun disconnectRing() {
        listOf(KEY_ACCESS_TOKEN, KEY_REFRESH_TOKEN, KEY_TOKEN_TYPE).forEach { prefs.clearKey(it) }
        prefs.saveString(ctx, KEY_CONNECTED, "false")
        _isAuthSuccess.value = false
        _ringData.value = UltrahumanDashboardData()
    }

    // ── Telemetry ─────────────────────────────────────────────────────────────
    fun syncTelemetry() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            loadTelemetry()
            _isSyncing.value = false
        }
    }

    private suspend fun loadTelemetry() {
        val patient = prefs.getPatient() ?: return
        _ringData.value = SmartRingService.fetchUltrahumanData(patient.pid, patient.clientId)
    }

    // ── OAuth ─────────────────────────────────────────────────────────────────
    fun startOAuthFlow(context: Context) {
        _isLoading.value = true
        _errorMessage.value = null
        val authUri = Uri.parse(SmartRingService.AUTH_BASE_URL).buildUpon()
            .appendQueryParameter("client_id", SmartRingService.CLIENT_ID)
            .appendQueryParameter("redirect_uri", SmartRingService.REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SmartRingService.SCOPE)
            .build()
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, authUri))
        } catch (e: Exception) {
            Log.e(TAG, "Unable to open browser: ${e.message}")
            _isLoading.value = false
            _errorMessage.value = "Unable to start authentication session."
        }
    }

    private var processingRedirect = false

    private fun handleRedirect(uri: Uri) {
        processingRedirect = true
        _isLoading.value = true
        viewModelScope.launch {
            try { processRedirect(uri) } finally { processingRedirect = false }
        }
    }

    private suspend fun processRedirect(uri: Uri) {
        // Tokens may come directly in the redirect query, otherwise exchange the auth code
        val accessToken = uri.getQueryParameter("accessToken")
        val code = uri.getQueryParameter("code")
        val tokens = when {
            !accessToken.isNullOrEmpty() -> SmartRingService.Tokens(
                accessToken = accessToken,
                tokenType = uri.getQueryParameter("tokenType") ?: "Bearer",
                expiresIn = uri.getQueryParameter("expiresIn") ?: "86400",
                refreshToken = uri.getQueryParameter("refreshToken") ?: ""
            )
            !code.isNullOrEmpty() -> SmartRingService.exchangeCodeForToken(code)
            else -> null
        }
        if (tokens == null) {
            _isLoading.value = false
            _errorMessage.value = if (accessToken.isNullOrEmpty() && code.isNullOrEmpty())
                "Authorization credentials not found." else "Could not get access token from Ultrahuman."
            return
        }
        insertToken(tokens)
    }

    private suspend fun insertToken(tokens: SmartRingService.Tokens) {
        // 1. Persist locally and switch UI to the dashboard
        prefs.saveString(ctx, KEY_ACCESS_TOKEN, tokens.accessToken)
        prefs.saveString(ctx, KEY_REFRESH_TOKEN, tokens.refreshToken)
        prefs.saveString(ctx, KEY_TOKEN_TYPE, tokens.tokenType)
        prefs.saveString(ctx, KEY_CONNECTED, "true")
        _isAuthSuccess.value = true
        _isLoading.value = false
        _errorMessage.value = null

        // 2. Initial telemetry
        loadTelemetry()

        // 3. Background sync of tokens to the Vitalio backend
        val loginId = prefs.getPatient()?.emailAddress.orEmpty()
        viewModelScope.launch { SmartRingService.insertTokensOnBackend(loginId, tokens) }
    }

    companion object {
        private const val TAG = "SmartRingVM"
        private const val KEY_CONNECTED = "isSmartRingConnected"
        private const val KEY_ACCESS_TOKEN = "ultrahuman_access_token"
        private const val KEY_REFRESH_TOKEN = "ultrahuman_refresh_token"
        private const val KEY_TOKEN_TYPE = "ultrahuman_token_type"
    }
}