package com.critetiontech.ctvitalio.viewmodel

import com.critetiontech.ctvitalio.utils.AppGson

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.reminder.SubscriptionRenewalReminder
import com.critetiontech.ctvitalio.utils.MyApplication
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** One row of api/PatientRegistration/GetSubscriptionDetailsByPid. */
data class SubscriptionDetails(
    @SerializedName("pId") val pid: Int = 0,
    val clientId: Int = 0,
    val planTier: String? = null,
    val planName: String? = null,
    val pricePerMonth: Double? = null,
    val status: String? = null,
    val renewsInDays: Int? = null,
    val nextRenewalDate: String? = null,
    val planStartDate: String? = null,
    val validityMonths: Int? = null,
    val validityText: String? = null,
    /** JSON array sent as a string, e.g. [{"FeatureName":"Video Consultation","accessType":"Limited","usageLimit":4}]. */
    val features: String? = null,
) {
    val isActive: Boolean get() = status.equals("Active", ignoreCase = true)

    val parsedFeatures: List<SubscriptionFeature>
        get() = try {
            if (features.isNullOrBlank()) emptyList()
            else AppGson.fromJson<List<SubscriptionFeature>>(features, object : TypeToken<List<SubscriptionFeature>>() {}.type)
                ?.filter { !it.featureName.isNullOrBlank() }.orEmpty()
        } catch (e: Exception) {
            emptyList()
        }
}

data class SubscriptionFeature(
    @SerializedName("FeatureName", alternate = ["featureName"]) val featureName: String? = null,
    val accessType: String? = null,
    val usageLimit: Int? = null,
    val availability: String? = null,
) {
    val isFull: Boolean get() = accessType.equals("Full", ignoreCase = true)
}

sealed interface SubscriptionUiState {
    data object Loading : SubscriptionUiState
    data object None : SubscriptionUiState
    data object Error : SubscriptionUiState
    data class Loaded(val plan: SubscriptionDetails) : SubscriptionUiState
}

class SubscriptionViewModel : ViewModel() {

    private val _state = MutableStateFlow<SubscriptionUiState>(SubscriptionUiState.Loading)
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    /** True during a pull-to-refresh, while the current plan stays on screen. */
    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = SubscriptionUiState.Loading
            _state.value = fetch()
        }
    }

    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            val result = fetch()
            if (result !is SubscriptionUiState.Error || _state.value !is SubscriptionUiState.Loaded) _state.value = result
            _refreshing.value = false
        }
    }

    private suspend fun fetch(): SubscriptionUiState {
        val patient = AppDependencies.prefs.getPatient()
        if (patient == null || patient.pid <= 0 || patient.clientId <= 0) return SubscriptionUiState.Error
        return try {
            // Saved copy is shown when offline; "No record found" is cached as an empty list
            val body = fetchCached(
                cacheKey = "subscription_${patient.pid}",
                path = "api/PatientRegistration/GetSubscriptionDetailsByPid",
                params = mapOf("pid" to patient.pid, "ClientId" to patient.clientId),
                emptyBody = """{"status":1,"responseValue":[]}""",
            ) ?: return SubscriptionUiState.Error
            val value = JsonParser.parseString(body).asJsonObject.get("responseValue")
            if (value == null || !value.isJsonArray) return SubscriptionUiState.Error
            val plans = AppGson.fromJson(value, Array<SubscriptionDetails>::class.java).toList()
            // Prefer the active plan if the server ever sends history alongside it.
            val plan = plans.firstOrNull { it.isActive } ?: plans.firstOrNull()
            SubscriptionRenewalReminder.schedule(
                MyApplication.appContext, plan?.planName, plan?.nextRenewalDate, active = plan?.isActive == true
            )
            if (plan == null) SubscriptionUiState.None else SubscriptionUiState.Loaded(plan)
        } catch (e: Exception) {
            Log.e("SubscriptionVM", "Load failed", e)
            SubscriptionUiState.Error
        }
    }
}
