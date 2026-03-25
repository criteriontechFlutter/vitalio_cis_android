package com.example.vitalio_cis.utils

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.internal.Contexts

object FCMHelper {

    private const val TAG = "FCMHelper"


    fun initializeFCM(context: Context,onTokenReceived: (token: String) -> Unit, onError: (Exception) -> Unit = {}) {


        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    onError(task.exception ?: Exception("Unknown error"))
                    return@addOnCompleteListener
                }

                // Get new FCM registration token
                val token = task.result
                Log.d(TAG, "FCM Token: $token")
//                PrefsManager(context).saveDeviceToken(token)

                // Send token to your server or use it as needed
                onTokenReceived(token)
            }
    }
}