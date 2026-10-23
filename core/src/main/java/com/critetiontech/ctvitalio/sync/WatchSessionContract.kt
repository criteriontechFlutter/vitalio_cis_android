package com.critetiontech.ctvitalio.sync

/**
 * Wearable Data Layer contract for syncing the logged-in patient from the phone to the watch.
 * The phone writes a DataItem at [PATH]; the watch listens for it.
 */
object WatchSessionContract {
    const val PATH = "/vitalio/session"

    const val KEY_LOGGED_IN = "loggedIn"
    const val KEY_PID = "pid"
    const val KEY_CLIENT_ID = "clientId"
    const val KEY_UHID = "uhId"
    const val KEY_NAME = "name"
}