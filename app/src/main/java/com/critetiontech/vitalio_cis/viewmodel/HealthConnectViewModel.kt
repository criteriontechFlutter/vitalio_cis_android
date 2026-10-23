package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.healthconnect.WearableData
import com.critetiontech.ctvitalio.healthconnect.WearableReader
import com.critetiontech.ctvitalio.healthconnect.WearableSnapshot
import com.critetiontech.ctvitalio.healthconnect.WearableSync
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class SyncState { IDLE, SYNCING, SUCCESS, ERROR, NOT_INSTALLED, UPDATE_REQUIRED, PERMISSIONS_DENIED }

// ─── ViewModel ────────────────────────────────────────────────────────────────

class HealthConnectViewModel : ViewModel() {

    private val context = MyApplication.appContext

    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState

    private val _errorMessage = MutableStateFlow("")
    val errorMessage: StateFlow<String> = _errorMessage

    private val _availabilityMessage = MutableStateFlow("Checking Health Connect…")
    val availabilityMessage: StateFlow<String> = _availabilityMessage

    private val _snapshot = MutableStateFlow<WearableSnapshot?>(null)
    val snapshot: StateFlow<WearableSnapshot?> = _snapshot

    /** Vitals and workouts saved to the server in the last sync. */
    private val _savedCount = MutableStateFlow(0 to 0)
    val savedCount: StateFlow<Pair<Int, Int>> = _savedCount

    private val _missingPermissions = MutableStateFlow<Set<String>>(emptySet())
    val missingPermissions: StateFlow<Set<String>> = _missingPermissions

    private val _autoSync = MutableStateFlow(WearableSync.isAutoSyncEnabled(context))
    val autoSync: StateFlow<Boolean> = _autoSync

    private val _backgroundSync = MutableStateFlow(false)
    val backgroundSync: StateFlow<Boolean> = _backgroundSync

    fun checkAvailability(sdkStatus: Int) {
        Log.d("HealthConnect", "SDK status = $sdkStatus")
        when (sdkStatus) {
            HealthConnectClient.SDK_AVAILABLE -> _availabilityMessage.value = "Health Connect is available ✓"
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                _availabilityMessage.value = "Health Connect needs an update"
                _syncState.value = SyncState.UPDATE_REQUIRED
            }
            else -> {
                _availabilityMessage.value = "Health Connect is not installed"
                _syncState.value = SyncState.NOT_INSTALLED
            }
        }
    }

    /** Re-reads granted permissions and shows the current wearable data without saving it. */
    fun refresh(client: HealthConnectClient) {
        viewModelScope.launch {
            val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            _backgroundSync.value = WearableSync.canReadInBackground(client)
            if (granted.none { it in WearableData.READ_PERMISSIONS }) return@launch
            _snapshot.value = WearableReader(client).read(granted)
        }
    }

    fun onPermissionsResult(client: HealthConnectClient, granted: Set<String>) {
        if (granted.none { it in WearableData.READ_PERMISSIONS }) {
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            _syncState.value = SyncState.PERMISSIONS_DENIED
        } else {
            syncNow(client)
        }
    }

    fun syncNow(client: HealthConnectClient) {
        viewModelScope.launch {
            _syncState.value = SyncState.SYNCING
            val granted = runCatching { client.permissionController.getGrantedPermissions() }.getOrDefault(emptySet())
            _missingPermissions.value = WearableData.READ_PERMISSIONS - granted
            val result = WearableSync.sync(context, client)
            _snapshot.value = result.snapshot
            _savedCount.value = result.vitalsSaved to result.workoutsSaved
            val snapshot = result.snapshot
            val nothingRead = snapshot.vitals.isEmpty() && snapshot.workouts.isEmpty() &&
                    snapshot.daily == com.critetiontech.ctvitalio.healthconnect.DailySummary()
            _syncState.value = when {
                result.error != null -> { _errorMessage.value = result.error; SyncState.ERROR }
                nothingRead -> {
                    _errorMessage.value = snapshot.readErrors.firstOrNull()?.let { "Couldn't read from Health Connect: $it" }
                        ?: "No data found from your watch or health apps yet.\nMake sure your watch app (e.g. Samsung Health) is set to sync with Health Connect."
                    SyncState.ERROR
                }
                else -> SyncState.SUCCESS
            }
        }
    }

    fun setAutoSync(enabled: Boolean) {
        WearableSync.setAutoSync(context, enabled)
        _autoSync.value = enabled
    }
}
