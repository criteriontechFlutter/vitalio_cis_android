package com.critetiontech.ctvitalio.consultation

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.model.IncomingCall
import com.critetiontech.ctvitalio.ui.screens.CallScreen
import com.critetiontech.ctvitalio.viewmodel.CallPhase
import com.critetiontech.ctvitalio.viewmodel.CallViewModel
import com.critetiontech.ctvitalio.ui.theme.MyAppTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Full-screen video consultation: rings over the lock screen, then hosts the LiveKit call.
 * Leaving the screen mid-call shrinks it to picture-in-picture; [CallService] keeps camera and
 * mic running meanwhile.
 */
class CallActivity : ComponentActivity() {

    private val vm: CallViewModel by viewModels()
    private var inPip by mutableStateOf(false)

    private val permissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        vm.accept(
            cameraAllowed = result[Manifest.permission.CAMERA] ?: granted(Manifest.permission.CAMERA),
            micAllowed = result[Manifest.permission.RECORD_AUDIO] ?: granted(Manifest.permission.RECORD_AUDIO),
        )
    }

    /** Mute / hang-up buttons on the PiP window. */
    private val pipActions = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_PIP_MIC -> vm.toggleMic()
                ACTION_PIP_HANG_UP -> vm.hangUp()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showOverLockScreen()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )

        if (savedInstanceState == null || vm.call == null) {
            val incoming = intent.incomingCall() ?: run { finish(); return }
            open(incoming, intent.getBooleanExtra(EXTRA_ANSWER, false))
        }

        // Back keeps the call going in a small window
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (vm.phase) {
                    is CallPhase.Ended -> finish()
                    CallPhase.InCall -> if (!enterPip()) moveTaskToBack(true)
                    else -> moveTaskToBack(true)
                }
            }
        })

        ContextCompat.registerReceiver(
            this, pipActions,
            IntentFilter().apply { addAction(ACTION_PIP_MIC); addAction(ACTION_PIP_HANG_UP) },
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        setContent {
            MyAppTheme {
                CallScreen(
                    vm = vm,
                    onAccept = ::answer,
                    onDecline = vm::decline,
                    onClose = ::finish,
                    onMinimize = onBackPressedDispatcher::onBackPressed,
                    pip = inPip,
                )
            }
        }

        lifecycleScope.launch {
            snapshotFlow { vm.phase }.collect { phase ->
                when (phase) {
                    CallPhase.InCall -> vm.call?.let { CallService.start(this@CallActivity, it, vm.connectedAt) }
                    is CallPhase.Ended -> {
                        CallService.stop(this@CallActivity)
                        // Close by itself shortly after a normal end; a failure stays up so it can be read
                        if (!phase.failed || inPip) {
                            delay(if (inPip) 600 else 1_800)
                            finish()
                        }
                    }
                    else -> Unit
                }
            }
        }
        // PiP is only offered during the call; its mic button follows the mic state
        lifecycleScope.launch {
            snapshotFlow { vm.phase to vm.micOn }.collect { updatePipParams() }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val incoming = intent.incomingCall() ?: return
        val current = vm.call
        when {
            // "Hang up" on the ongoing-call notification
            intent.getBooleanExtra(EXTRA_HANG_UP, false) -> if (current?.roomName == incoming.roomName) vm.hangUp()
            // "Join" tapped on the notification while this screen was ringing
            current?.roomName == incoming.roomName -> if (intent.getBooleanExtra(EXTRA_ANSWER, false)) answer()
            // Already busy with another consultation
            vm.phase == CallPhase.InCall || vm.phase == CallPhase.Connecting ->
                CallNotifier.decline(this, incoming, reason = "busy")
            else -> {
                setIntent(intent)
                recreate()
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Home pressed mid-call; Android 12+ does this itself through setAutoEnterEnabled
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && vm.phase == CallPhase.InCall) enterPip()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        inPip = isInPictureInPictureMode
        vm.inPip = isInPictureInPictureMode
        // Leaving PiP without coming back to the front means the window was swiped away: end the call
        if (!isInPictureInPictureMode && lifecycle.currentState == Lifecycle.State.CREATED) vm.hangUp()
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipActions) }
        if (isFinishing) {
            vm.hangUp()
            CallService.stop(this)
        }
        super.onDestroy()
    }

    private fun enterPip(): Boolean = runCatching { enterPictureInPictureMode(pipParams()) }.getOrDefault(false)

    private fun updatePipParams() {
        runCatching { setPictureInPictureParams(pipParams()) }
    }

    private fun pipParams(): PictureInPictureParams {
        val inCall = vm.phase == CallPhase.InCall
        val micLabel = if (vm.micOn) "Mute" else "Unmute"
        val mic = RemoteAction(
            Icon.createWithResource(this, if (vm.micOn) R.drawable.ic_call_mic else R.drawable.ic_call_mic_off),
            micLabel, micLabel, pipBroadcast(ACTION_PIP_MIC, 0),
        )
        val hangUp = RemoteAction(
            Icon.createWithResource(this, R.drawable.ic_call_decline), "Hang up", "Hang up",
            pipBroadcast(ACTION_PIP_HANG_UP, 1),
        )
        return PictureInPictureParams.Builder()
            .setAspectRatio(Rational(9, 16))
            .setActions(if (inCall) listOf(mic, hangUp) else emptyList())
            .apply { if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(inCall).setSeamlessResizeEnabled(true) }
            .build()
    }

    private fun pipBroadcast(action: String, code: Int): PendingIntent = PendingIntent.getBroadcast(
        this, code, Intent(action).setPackage(packageName),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun open(incoming: IncomingCall, answerNow: Boolean) {
        vm.start(incoming, answerNow)
        if (answerNow) answer()
    }

    private fun answer() {
        if (vm.phase != CallPhase.Ringing) return
        val wanted = buildList {
            add(Manifest.permission.CAMERA)
            add(Manifest.permission.RECORD_AUDIO)
            // Lets Bluetooth headsets show up as an audio choice; the call works without it
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        val missing = wanted.filterNot(::granted)
        if (missing.isEmpty()) vm.accept(cameraAllowed = true, micAllowed = true)
        else permissions.launch(missing.toTypedArray())
    }

    private fun granted(permission: String) =
        ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        private const val EXTRA_ANSWER = "call_answer"
        const val EXTRA_HANG_UP = "call_hang_up"
        private const val ACTION_PIP_MIC = "com.critetiontech.ctvitalio.call.PIP_MIC"
        private const val ACTION_PIP_HANG_UP = "com.critetiontech.ctvitalio.call.PIP_HANG_UP"

        fun intent(context: Context, call: IncomingCall, answer: Boolean = false): Intent =
            call.putInto(Intent(context, CallActivity::class.java))
                .putExtra(EXTRA_ANSWER, answer)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
