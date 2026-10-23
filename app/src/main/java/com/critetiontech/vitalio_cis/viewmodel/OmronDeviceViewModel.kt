package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.omron.OmronException
import com.critetiontech.ctvitalio.omron.OmronKind
import com.critetiontech.ctvitalio.omron.OmronManager
import com.critetiontech.ctvitalio.omron.OmronModel
import com.critetiontech.ctvitalio.omron.OmronModels
import com.critetiontech.ctvitalio.omron.OmronProfile
import com.critetiontech.ctvitalio.omron.OmronReading
import com.critetiontech.ctvitalio.omron.PairedOmronDevice
import com.critetiontech.ctvitalio.utils.MyApplication
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface OmronPhase {
    data object Idle : OmronPhase
    data object Pairing : OmronPhase
    data object Syncing : OmronPhase
    data object JustPaired : OmronPhase
    data class Synced(
        val latest: OmronReading?,
        val saved: Boolean,
        val saving: Boolean = false,
        val error: String? = null
    ) : OmronPhase
    data class Failed(val message: String) : OmronPhase
}

class OmronDeviceViewModel : ViewModel() {

    private val saveMeasuredVitals = AppDependencies.saveMeasuredVitals()
    private val prefs = AppDependencies.prefs
    private val context = MyApplication.appContext
    private var job: Job? = null

    /** Known once paired; null while pairing an unknown device of [kind]. */
    var model: OmronModel? = null
        private set

    var kind: OmronKind? = null
        private set

    private val _device = MutableStateFlow<PairedOmronDevice?>(null)
    val device: StateFlow<PairedOmronDevice?> = _device

    private val _phase = MutableStateFlow<OmronPhase>(OmronPhase.Idle)
    val phase: StateFlow<OmronPhase> = _phase

    private val _userCount = MutableStateFlow(1)
    val userCount: StateFlow<Int> = _userCount

    private val _selectedUser = MutableStateFlow(1)
    val selectedUser: StateFlow<Int> = _selectedUser

    /** [code] is a model number, or [OmronModels.autoCode] to pair whichever device of that kind is nearby. */
    fun load(code: String) {
        if (loaded == code) return
        loaded = code
        val auto = OmronModels.autoKind(code)
        model = if (auto == null) OmronModels.byCode(code) else null
        kind = auto ?: model?.kind
        _device.value = model?.let { OmronManager.pairedDevice(context, it.code) }
        // Before the model is known, offer as many users as the roomiest model of the kind has
        val models = model?.let(::listOf) ?: kind?.let(OmronModels::ofKind).orEmpty()
        _userCount.value = models.maxOfOrNull { OmronManager.userCount(OmronManager.deviceConfig(context, it)) } ?: 1
    }

    private var loaded: String? = null

    fun selectUser(user: Int) { _selectedUser.value = user }

    fun showError(message: String) { _phase.value = OmronPhase.Failed(message) }

    fun pair() = run(OmronPhase.Pairing) {
        val k = kind ?: return@run
        val paired = OmronManager.pair(context, k, _selectedUser.value, profile(k))
        model = OmronModels.byCode(paired.code)
        _device.value = paired
        _phase.value = OmronPhase.JustPaired
    }

    fun sync() = run(OmronPhase.Syncing) {
        val m = model ?: return@run
        val d = _device.value ?: return@run
        val result = OmronManager.sync(context, d, profile(m.kind))
        _device.value = OmronManager.pairedDevice(context, d.code)
        _phase.value = OmronPhase.Synced(result.latest, result.alreadySaved)
    }

    /** Saves the synced reading to the health record (AddVital). */
    fun save() {
        val synced = _phase.value as? OmronPhase.Synced ?: return
        val reading = synced.latest ?: return
        val d = _device.value ?: return
        if (synced.saving || synced.saved) return
        _phase.value = synced.copy(saving = true, error = null)
        viewModelScope.launch {
            val failure = saveReading(d, reading)
            val current = _phase.value as? OmronPhase.Synced ?: return@launch
            _phase.value = current.copy(saving = false, saved = failure == null, error = failure)
        }
    }

    /** Returns null when saved, otherwise the reason it wasn't. */
    private suspend fun saveReading(device: PairedOmronDevice, reading: OmronReading): String? {
        val patient = prefs.getPatient() ?: return "Please log in again."
        val fields = OmronManager.vitalFields(reading) ?: return "Only blood pressure readings can be saved."
        return when (val result = saveMeasuredVitals(patient, reading.time, fields)) {
            is DomainResult.Success -> { OmronManager.markSaved(context, device, reading); null }
            is DomainResult.Error -> result.exception.message ?: "Couldn't save the vitals. Please try again."
        }
    }

    fun cancel() {
        job?.cancel()
        _phase.value = OmronPhase.Idle
    }

    fun remove() {
        model?.let { OmronManager.removeDevice(context, it.code) }
        _device.value = null
        _phase.value = OmronPhase.Idle
    }


    private fun run(busy: OmronPhase, block: suspend () -> Unit) {
        job?.cancel()
        _phase.value = busy
        job = viewModelScope.launch {
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: OmronException) {
                Log.w("OmronDevice", "error ${e.code}: ${e.message}")
                _phase.value = OmronPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            } catch (e: Exception) {
                Log.e("OmronDevice", "unexpected", e)
                _phase.value = OmronPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            }
        }
    }

    /**
     * Scales need height, gender and date of birth; height comes from the last saved vitals.
     * Some BP monitors (HEM-7361T) also store date of birth and fail pairing without it.
     */
    private suspend fun profile(kind: OmronKind): OmronProfile? {
        val patient = AppDependencies.prefs.getPatient() ?: return null
        val height = if (kind != OmronKind.SCALE) null
            else (AppDependencies.fetchLastVital()(patient.uhId, patient.clientId.toString(), patient.pid.toString())
                as? DomainResult.Success)?.data
            ?.firstOrNull { it.vitalName.equals("Height", ignoreCase = true) }?.vitalValue?.takeIf { it > 50 }
        val dob = runCatching { LocalDate.parse(patient.dob.take(10)) }.getOrNull()
        return OmronProfile(height, dob, isMale = patient.genderName.startsWith("M", ignoreCase = true))
    }
}
