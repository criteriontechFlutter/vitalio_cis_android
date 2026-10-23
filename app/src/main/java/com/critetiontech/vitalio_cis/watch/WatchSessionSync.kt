package com.critetiontech.ctvitalio.watch

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.sync.WatchSessionContract
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Publishes the logged-in patient to the Vitalio Wear OS app over the Wearable Data Layer.
 * Runs at app start and again whenever the stored patient changes (login / logout), so a
 * watch paired later still receives the current session.
 */
object WatchSessionSync {

    private const val TAG = "WatchSessionSync"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Held strongly — SharedPreferences only keeps weak references to listeners
    private var prefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun start(context: Context) {
        val appContext = context.applicationContext
        publish(appContext)

        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            // key == null when the whole file is cleared (logout via clearAll)
            if (key == null || key == PrefsManager.KEY_PATIENT) publish(appContext)
        }
        prefsListener = listener
        appContext.getSharedPreferences(PrefsManager.PREFS_NAME, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(listener)
    }

    private fun publish(context: Context) = scope.launch {
        val patient = AppDependencies.prefs.getPatient()
        val request = PutDataMapRequest.create(WatchSessionContract.PATH).apply {
            dataMap.putBoolean(WatchSessionContract.KEY_LOGGED_IN, patient != null)
            if (patient != null) {
                dataMap.putInt(WatchSessionContract.KEY_PID, patient.pid)
                dataMap.putInt(WatchSessionContract.KEY_CLIENT_ID, patient.clientId)
                dataMap.putString(WatchSessionContract.KEY_UHID, patient.uhId)
                dataMap.putString(
                    WatchSessionContract.KEY_NAME,
                    listOfNotNull(patient.firstName, patient.lastName).joinToString(" ").trim()
                )
            }
        }.asPutDataRequest().setUrgent()

        try {
            Tasks.await(Wearable.getDataClient(context).putDataItem(request))
            Log.d(TAG, "Session published (loggedIn=${patient != null})")
        } catch (e: Exception) {
            // No Play services / Wear API on this phone — the watch simply stays signed out
            Log.w(TAG, "Could not publish session to watch: ${e.message}")
        }
    }
}