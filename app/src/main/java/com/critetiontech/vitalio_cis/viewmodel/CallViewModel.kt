package com.critetiontech.ctvitalio.viewmodel

import android.app.Application
import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.critetiontech.ctvitalio.consultation.CallNotifier
import com.critetiontech.ctvitalio.consultation.CallQuality
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.IncomingCall
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.LiveKit
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.ConnectionQuality
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.VideoTrackPublishDefaults
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.RemoteTrackPublication
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.VideoTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed interface CallPhase {
    data object Ringing : CallPhase
    data object Connecting : CallPhase
    data object InCall : CallPhase
    data class Ended(val message: String, val failed: Boolean = false) : CallPhase
}

/** One video consultation: ringing, joining the LiveKit room, the call itself and hanging up. */
class CallViewModel(app: Application) : AndroidViewModel(app) {

    private val joinCallUseCase = AppDependencies.joinCall()
    private val endCallUseCase = AppDependencies.endCall()
    private val prefs = AppDependencies.prefs

    var call: IncomingCall? = null
        private set

    var phase by mutableStateOf<CallPhase>(CallPhase.Ringing)
        private set
    var room by mutableStateOf<Room?>(null)
        private set
    var remoteVideo by mutableStateOf<VideoTrack?>(null)
        private set
    var localVideo by mutableStateOf<VideoTrack?>(null)
        private set
    /** Name the doctor joined with, once they're in the room. */
    var remoteName by mutableStateOf<String?>(null)
        private set
    var doctorJoined by mutableStateOf(false)
        private set
    var micOn by mutableStateOf(true)
        private set
    var cameraOn by mutableStateOf(true)
        private set
    /** Where call audio can play right now (speaker, phone earpiece, headsets) and where it plays. */
    var audioDevices by mutableStateOf<List<AudioDevice>>(emptyList())
        private set
    var audioDevice by mutableStateOf<AudioDevice?>(null)
        private set
    var frontCamera by mutableStateOf(true)
        private set
    var reconnecting by mutableStateOf(false)
        private set
    /** [SystemClock.elapsedRealtime] when the call connected, for the timer. */
    var connectedAt by mutableStateOf(0L)
        private set
    var quality by mutableStateOf(CallQuality.saved(app))
        private set
    /** This phone's link to the call server, as LiveKit rates it every few seconds. */
    var myConnection by mutableStateOf(ConnectionQuality.UNKNOWN)
        private set
    var doctorConnection by mutableStateOf(ConnectionQuality.UNKNOWN)
        private set
    /** Set by the screen; in Auto the doctor's video drops to a low layer while in PiP. */
    var inPip = false
        set(value) {
            field = value
            applyReceiveQuality()
        }

    private var timeoutJob: Job? = null

    fun start(incoming: IncomingCall, answerNow: Boolean) {
        if (call?.roomName == incoming.roomName) return
        call = incoming
        // A new call after an earlier one ended on this screen
        phase = CallPhase.Ringing
        doctorJoined = false
        remoteName = null
        if (answerNow) return // the screen asks for camera/mic first, then calls accept()
        timeoutJob = viewModelScope.launch {
            delay(CallNotifier.RING_TIMEOUT_MS)
            if (phase == CallPhase.Ringing) {
                CallNotifier.decline(getApplication(), incoming, reason = "no_answer")
                CallNotifier.showMissed(getApplication(), incoming)
                phase = CallPhase.Ended("Missed call")
            }
        }
        // The doctor may hang up before the patient answers
        viewModelScope.launch {
            CallNotifier.ended.collect { roomName ->
                if (roomName == incoming.roomName && phase == CallPhase.Ringing) phase = CallPhase.Ended("Call cancelled")
            }
        }
    }

    fun decline() {
        val c = call ?: return
        timeoutJob?.cancel()
        CallNotifier.decline(getApplication(), c, reason = "declined")
        phase = CallPhase.Ended("Call declined")
    }

    fun accept(cameraAllowed: Boolean, micAllowed: Boolean) {
        val c = call ?: return
        if (phase != CallPhase.Ringing) return
        timeoutJob?.cancel()
        CallNotifier.stopRinging(getApplication(), c)
        phase = CallPhase.Connecting
        cameraOn = cameraAllowed
        micOn = micAllowed

        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val patient = prefs.getPatient()
                val name = listOfNotNull(patient?.firstName, patient?.lastName).joinToString(" ").trim().ifBlank { "Patient" }
                val join = when (val result = joinCallUseCase(c.roomName, CallNotifier.patientUserId(c), name, patient?.imageUrl)) {
                    is DomainResult.Success -> result.data
                    is DomainResult.Error -> throw result.exception
                }

                val r = LiveKit.create(app)
                // A video call: loudspeaker unless a headset is connected
                r.audioSwitchHandler?.preferredDeviceList = listOf(
                    AudioDevice.BluetoothHeadset::class.java,
                    AudioDevice.WiredHeadset::class.java,
                    AudioDevice.Speakerphone::class.java,
                    AudioDevice.Earpiece::class.java,
                )
                // Headsets plugged in or connected mid-call show up here (and switch over by themselves)
                r.audioSwitchHandler?.registerAudioDeviceChangeListener { devices, selected ->
                    audioDevices = devices
                    audioDevice = selected
                }
                // Quality is chosen by the patient (see changeQuality), not by the renderer's size;
                // dynacast stops sending layers nobody is watching
                r.adaptiveStream = false
                r.dynacast = true
                r.localParticipant.videoTrackCaptureDefaults = LocalVideoTrackOptions(captureParams = quality.sendPreset.capture)
                r.localParticipant.videoTrackPublishDefaults = VideoTrackPublishDefaults(videoEncoding = quality.sendPreset.encoding, simulcast = true)
                room = r
                launch { r.events.collect { onRoomEvent(it) } }
                r.connect(join.serverUrl, join.token)

                if (micAllowed) r.localParticipant.setMicrophoneEnabled(true)
                if (cameraAllowed) r.localParticipant.setCameraEnabled(true)
                refreshTracks()
                connectedAt = SystemClock.elapsedRealtime()
                phase = CallPhase.InCall
            } catch (e: Exception) {
                Log.w(TAG, "could not join ${c.roomName}", e)
                leaveRoom()
                // The repository already words API failures for the patient; this covers LiveKit's own
                val message = if (e is java.io.IOException) "Couldn't reach the call server. Check your internet connection."
                    else e.message ?: "Couldn't connect the call"
                phase = CallPhase.Ended(message, failed = true)
            }
        }
    }

    private fun onRoomEvent(event: RoomEvent) {
        when (event) {
            is RoomEvent.ParticipantConnected,
            is RoomEvent.TrackSubscribed,
            is RoomEvent.TrackUnsubscribed,
            is RoomEvent.TrackMuted,
            is RoomEvent.TrackUnmuted,
            is RoomEvent.TrackPublished,
            is RoomEvent.TrackUnpublished -> refreshTracks()
            is RoomEvent.ParticipantDisconnected -> {
                refreshTracks()
                // One-to-one consultation: the doctor leaving ends it
                if (doctorJoined && room?.remoteParticipants.isNullOrEmpty()) finish("The doctor ended the call")
            }
            is RoomEvent.ConnectionQualityChanged -> {
                if (event.participant is LocalParticipant) myConnection = event.quality else doctorConnection = event.quality
            }
            is RoomEvent.Reconnecting -> reconnecting = true
            is RoomEvent.Reconnected -> reconnecting = false
            is RoomEvent.Disconnected -> if (phase == CallPhase.InCall) finish("Call ended")
            else -> Unit
        }
    }

    private fun refreshTracks() {
        val r = room ?: return
        val doctor = r.remoteParticipants.values.firstOrNull()
        if (doctor != null) doctorJoined = true
        remoteName = doctor?.name?.takeIf { it.isNotBlank() }
        remoteVideo = doctor?.getTrackPublication(Track.Source.CAMERA)
            ?.takeIf { !it.muted }?.track as? VideoTrack
        localVideo = r.localParticipant.getTrackPublication(Track.Source.CAMERA)
            ?.takeIf { !it.muted }?.track as? VideoTrack
        doctor?.let { doctorConnection = it.connectionQuality }
        applyReceiveQuality()
    }

    /** Switches quality mid-call: restarts the camera at the new size and asks for the matching layer of the doctor's video. */
    fun changeQuality(value: CallQuality) {
        if (value == quality) return
        quality = value
        CallQuality.save(getApplication(), value)
        val r = room ?: return
        val capture = LocalVideoTrackOptions(
            position = if (frontCamera) CameraPosition.FRONT else CameraPosition.BACK,
            captureParams = value.sendPreset.capture,
        )
        r.localParticipant.videoTrackCaptureDefaults = capture
        // Change the running capturer's size in place. restartTrack() stops and reopens the camera,
        // which left it frozen on some phones (vivo, Android 16).
        (localVideo as? LocalVideoTrack)?.let { track ->
            val p = value.sendPreset.capture
            runCatching {
                track.capturer.changeCaptureFormat(p.width, p.height, p.maxFps)
                track.options = track.options.copy(captureParams = p)
            }.onFailure { Log.w(TAG, "could not change camera quality", it) }
        }
        applyReceiveQuality()
    }

    private fun applyReceiveQuality() {
        val publication = room?.remoteParticipants?.values?.firstOrNull()
            ?.getTrackPublication(Track.Source.CAMERA) as? RemoteTrackPublication ?: return
        publication.setVideoQuality(quality.receiveQuality(inPip))
    }

    fun toggleMic() {
        val r = room ?: return
        viewModelScope.launch {
            runCatching { r.localParticipant.setMicrophoneEnabled(!micOn) }.onSuccess { micOn = !micOn }
        }
    }

    fun toggleCamera() {
        val r = room ?: return
        viewModelScope.launch {
            runCatching { r.localParticipant.setCameraEnabled(!cameraOn) }.onSuccess {
                cameraOn = !cameraOn
                refreshTracks()
            }
        }
    }

    fun flipCamera() {
        val track = localVideo as? LocalVideoTrack ?: return
        frontCamera = !frontCamera
        track.switchCamera(position = if (frontCamera) CameraPosition.FRONT else CameraPosition.BACK)
    }

    fun selectAudioDevice(device: AudioDevice) {
        val handler = room?.audioSwitchHandler ?: return
        handler.selectDevice(device)
        audioDevice = handler.selectedAudioDevice ?: device
    }

    /** Patient hangs up. */
    fun hangUp() {
        val c = call ?: return
        if (phase is CallPhase.Ended) return
        if (phase == CallPhase.Ringing) return decline()
        val userId = CallNotifier.patientUserId(c)
        // Outlives this screen: the server must hear about it even as the activity closes
        CoroutineScope(Dispatchers.IO).launch {
            (endCallUseCase(c.roomName, userId, "patient_hung_up") as? DomainResult.Error)
                ?.let { Log.w(TAG, "end call not sent: ${it.exception.message}") }
        }
        finish("Call ended")
    }

    private fun finish(message: String) {
        if (phase is CallPhase.Ended) return
        phase = CallPhase.Ended(message)
        leaveRoom()
    }

    private fun leaveRoom() {
        val r = room ?: return
        room = null
        remoteVideo = null
        localVideo = null
        r.disconnect()
        r.release()
    }

    /** Seconds since the call connected. */
    fun elapsedSeconds(): Long =
        if (connectedAt == 0L) 0 else (SystemClock.elapsedRealtime() - connectedAt) / 1000

    override fun onCleared() = leaveRoom()

    private companion object { const val TAG = "Consultation" }
}
