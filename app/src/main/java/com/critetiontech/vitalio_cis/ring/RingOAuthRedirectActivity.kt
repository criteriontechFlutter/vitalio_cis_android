package com.critetiontech.ctvitalio.ring

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.critetiontech.ctvitalio.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Holds the latest Ultrahuman OAuth redirect until the Ring screen consumes it. */
object RingOAuthCallback {
    private val _uri = MutableStateFlow<Uri?>(null)
    val uri: StateFlow<Uri?> = _uri

    fun post(uri: Uri) { _uri.value = uri }
    fun consume() { _uri.value = null }
}

/**
 * Receives the `com.critetiontech.ctvitalio://...` redirect after Ultrahuman login
 * (same callback scheme as iOS), hands it to [RingOAuthCallback] and brings the
 * existing MainActivity back to the front.
 */
class RingOAuthRedirectActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.data?.let { RingOAuthCallback.post(it) }
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        )
        finish()
    }
}