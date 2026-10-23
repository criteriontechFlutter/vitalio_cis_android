package com.critetiontech.ctvitalio.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.critetiontech.ctvitalio.yonker.YonkerEvent
import com.critetiontech.ctvitalio.yonker.YonkerException
import com.critetiontech.ctvitalio.yonker.YonkerKind
import com.critetiontech.ctvitalio.yonker.YonkerManager
import com.critetiontech.ctvitalio.yonker.YonkerReading
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed interface YonkerPhase {
    data object Idle : YonkerPhase
    data object Searching : YonkerPhase
    data object Connecting : YonkerPhase
    /**
     * Connected and waiting for a measurement; [pressure] is the BP cuff while measuring and
     * [inflating] whether it's still rising (the machine measures as it lets air out).
     */
    data class Listening(val pressure: Int? = null, val inflating: Boolean = true, val noSignal: Boolean = false) : YonkerPhase
    /**
     * A reading to save. BP results replace it when a new measurement starts; oximeter values
     * update live until the user saves, then stay put so the saved value is what's shown.
     */
    data class Reading(
        val reading: YonkerReading,
        /** Oximeter: the last few values agree, so it's a good moment to save. BP results are always steady. */
        val steady: Boolean = true,
        val saving: Boolean = false,
        val saved: Boolean = false,
        val error: String? = null,
    ) : YonkerPhase {
        val locked get() = saving || saved || error != null
    }
    /** The device ended the connection on its own, usually switching off when left idle. Not an error. */
    data object Asleep : YonkerPhase
    data class Failed(val message: String) : YonkerPhase
}

/** Shown instead of a save error when there's no internet; the reading stays on screen to save later. */
const val OFFLINE = "You're offline. Connect to Wi-Fi or mobile data, then tap Save again."

private const val STEADY_COUNT = 4

class YonkerDeviceViewModel : ViewModel() {

    private val saveMeasuredVitals = AppDependencies.saveMeasuredVitals()
    private val prefs = AppDependencies.prefs
    private val context = MyApplication.appContext
    private var job: Job? = null
    /** Recent oximeter values, to tell when they've settled. */
    private val recent = ArrayDeque<YonkerReading>()

    var kind: YonkerKind? = null
        private set

    private val _phase = MutableStateFlow<YonkerPhase>(YonkerPhase.Idle)
    val phase: StateFlow<YonkerPhase> = _phase

    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected

    fun load(code: String) {
        if (kind?.code == code) return
        kind = YonkerKind.byCode(code)
    }

    fun showError(message: String) { _phase.value = YonkerPhase.Failed(message) }

    fun connect() {
        val k = kind ?: return
        job?.cancel()
        _phase.value = YonkerPhase.Searching
        job = viewModelScope.launch {
            try {
                YonkerManager.connect(context, k).collect(::onEvent)
                // The device switched off or walked away; keep an unsaved reading on screen
                // YK-BPA1 drops the link after about a minute without a measurement to save battery
                if (_phase.value !is YonkerPhase.Reading) _phase.value = YonkerPhase.Asleep
            } catch (e: CancellationException) {
                throw e
            } catch (e: YonkerException) {
                Log.w("YonkerDevice", "error: ${e.message}")
                _phase.value = YonkerPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            } catch (e: Exception) {
                Log.e("YonkerDevice", "unexpected", e)
                _phase.value = YonkerPhase.Failed(e.message ?: "Something went wrong. Please try again.")
            } finally {
                _connected.value = false
            }
        }
    }

    private fun onEvent(event: YonkerEvent) {
        val current = _phase.value
        val locked = (current as? YonkerPhase.Reading)?.locked == true
        when (event) {
            YonkerEvent.Connecting -> _phase.value = YonkerPhase.Connecting
            YonkerEvent.Connected -> { _connected.value = true; _phase.value = YonkerPhase.Listening() }
            // A new BP measurement started: the previous result is done with unless it's being saved
            is YonkerEvent.Pressure -> if (current !is YonkerPhase.Reading || (!current.saving && current.error == null)) {
                val before = (current as? YonkerPhase.Listening)?.pressure
                val inflating = when {
                    before == null -> true
                    event.mmHg > before -> true
                    event.mmHg < before -> false
                    else -> (current as YonkerPhase.Listening).inflating
                }
                _phase.value = YonkerPhase.Listening(pressure = event.mmHg, inflating = inflating)
            }
            YonkerEvent.NoSignal -> if (current !is YonkerPhase.Reading || !locked) {
                recent.clear()
                _phase.value = YonkerPhase.Listening(noSignal = true)
            }
            is YonkerEvent.Reading -> if (!locked || (event.reading.isBloodPressure && current is YonkerPhase.Reading && current.saved)) {
                _phase.value = YonkerPhase.Reading(event.reading, steady = event.reading.isBloodPressure || settled(event.reading))
            }
        }
    }

    /** True once the last [STEADY_COUNT] oximeter values are within a point of SpO2 and a few beats of pulse. */
    private fun settled(reading: YonkerReading): Boolean {
        recent.addLast(reading)
        while (recent.size > STEADY_COUNT) recent.removeFirst()
        if (recent.size < STEADY_COUNT) return false
        val spo2 = recent.mapNotNull { it.spo2 }
        val pulse = recent.mapNotNull { it.pulse }
        return spo2.size == STEADY_COUNT && spo2.max() - spo2.min() <= 1 && pulse.max() - pulse.min() <= 4
    }

    /** Saves the shown reading to the health record (AddVital). */
    fun save() {
        val shown = _phase.value as? YonkerPhase.Reading ?: return
        if (shown.saving || shown.saved) return
        if (!NetworkUtils.isConnected(context)) {
            _phase.value = shown.copy(error = OFFLINE)
            return
        }
        _phase.value = shown.copy(saving = true, error = null)
        viewModelScope.launch {
            val failure = saveReading(shown.reading)
            val current = _phase.value as? YonkerPhase.Reading ?: return@launch
            _phase.value = current.copy(saving = false, saved = failure == null, error = failure)
        }
    }

    private suspend fun saveReading(reading: YonkerReading): String? {
        val patient = prefs.getPatient() ?: return "Please log in again."
        return when (val result = saveMeasuredVitals(patient, reading.time, YonkerManager.vitalFields(reading))) {
            is DomainResult.Success -> null
            is DomainResult.Error -> result.exception.message ?: "Couldn't save the vitals. Please try again."
        }
    }

    /** Goes back to live values after a reading was saved (or its save is abandoned). */
    fun measureAgain() {
        recent.clear()
        _phase.value = if (_connected.value) YonkerPhase.Listening() else YonkerPhase.Idle
    }

    fun disconnect() {
        job?.cancel()
        _connected.value = false
        _phase.value = YonkerPhase.Idle
    }
}
