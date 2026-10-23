package com.critetiontech.ctvitalio.wear.session

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import com.critetiontech.ctvitalio.sync.WatchSessionContract
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Patient signed in on the paired phone. */
data class WatchSession(
    val pid: Int,
    val clientId: Int,
    val uhId: String,
    val name: String
)

/**
 * Holds the session received from the phone, persisted so the watch works offline
 * from the phone after the first sync.
 */
object WatchSessionStore {

    private const val TAG = "WatchSessionStore"
    private lateinit var prefs: SharedPreferences

    private val _session = MutableStateFlow<WatchSession?>(null)
    val session: StateFlow<WatchSession?> = _session.asStateFlow()

    fun init(context: Context) {
        prefs = context.getSharedPreferences("watch_session", Context.MODE_PRIVATE)
        _session.value = readPersisted()
    }

    /** Applies a session DataMap published by the phone. */
    fun apply(dataMap: DataMap) {
        val session = if (dataMap.getBoolean(WatchSessionContract.KEY_LOGGED_IN)) {
            WatchSession(
                pid = dataMap.getInt(WatchSessionContract.KEY_PID),
                clientId = dataMap.getInt(WatchSessionContract.KEY_CLIENT_ID),
                uhId = dataMap.getString(WatchSessionContract.KEY_UHID).orEmpty(),
                name = dataMap.getString(WatchSessionContract.KEY_NAME).orEmpty()
            )
        } else null
        save(session)
    }

    fun clear() = save(null)

    /** Pulls the latest session DataItem directly — covers updates missed while the app was not running. */
    fun refreshFromDataLayer(context: Context) {
        try {
            val uri = Uri.parse("wear://*${WatchSessionContract.PATH}")
            val items = Tasks.await(Wearable.getDataClient(context).getDataItems(uri))
            try {
                items.firstOrNull()?.let { apply(DataMapItem.fromDataItem(it).dataMap) }
            } finally {
                items.release()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not read session from Data Layer: ${e.message}")
        }
    }

    private fun save(session: WatchSession?) {
        prefs.edit {
            if (session == null) clear()
            else {
                putInt("pid", session.pid)
                putInt("clientId", session.clientId)
                putString("uhId", session.uhId)
                putString("name", session.name)
            }
        }
        _session.value = session
        Log.d(TAG, "Session updated (signedIn=${session != null})")
    }

    private fun readPersisted(): WatchSession? {
        val pid = prefs.getInt("pid", 0)
        if (pid == 0) return null
        return WatchSession(
            pid = pid,
            clientId = prefs.getInt("clientId", 0),
            uhId = prefs.getString("uhId", "").orEmpty(),
            name = prefs.getString("name", "").orEmpty()
        )
    }
}