package com.critetiontech.ctvitalio.utils

import android.util.Log
import com.critetiontech.ctvitalio.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import java.io.IOException

/** A failed backend call, recorded in Crashlytics as a non-fatal so failures real patients hit show up. */
class ApiFailure(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Sends API failures to Crashlytics as non-fatals. Only the endpoint path and HTTP code go out: no
 * query string, body, uhid or pid, since these are medical records. Skipped:
 * - network problems on the patient's side (IOException: no signal, timeouts), which are noise;
 * - 4xx answers, which this backend uses for normal outcomes like "No record found";
 * - debug builds, so testing doesn't flood the dashboard.
 */
object ApiErrorReporter {

    private fun path(endpoint: String): String = endpoint.substringBefore('?').substringAfter("://").let {
        // Drop a host if one was passed, keep "api/Controller/Action"
        if (it.contains("/api/")) "api/" + it.substringAfter("/api/") else it
    }

    fun httpError(endpoint: String, code: Int) {
        if (code < 500) return
        record(ApiFailure("HTTP $code ${path(endpoint)}"))
    }

    fun exception(endpoint: String, e: Throwable) {
        if (e is IOException) return
        record(ApiFailure("${e.javaClass.simpleName} ${path(endpoint)}", e))
    }

    private fun record(failure: ApiFailure) {
        if (BuildConfig.DEBUG) {
            Log.d("ApiErrorReporter", "would report: ${failure.message}")
            return
        }
        runCatching { FirebaseCrashlytics.getInstance().recordException(failure) }
    }
}
