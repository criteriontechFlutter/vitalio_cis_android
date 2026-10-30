package com.critetiontech.ctvitalio.data.offline

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AtomicFile
import android.util.Log
import android.widget.Toast
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.MyApplication
import com.google.gson.GsonBuilder
import com.google.gson.ToNumberPolicy
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

/** What a save attempt came to. */
sealed interface SendResult {
    data object Sent : SendResult
    /** No network, timeout or gateway error: worth sending again later. */
    data object Offline : SendResult
    /** The server answered and refused; sending the same body again won't help. */
    data class Rejected(val reason: String) : SendResult

    companion object {
        /**
         * Reads [ApiHelper]'s conventions: offline is a 504 "No Connection…", a dropped
         * connection or timeout a 500 "Exception: …".
         */
        fun isNetworkFailure(response: Response<ResponseBody>, raw: String?): Boolean {
            val text = raw.orEmpty()
            return (response.code() == 504 && text.startsWith("No Connection")) ||
                (response.code() == 500 && text.startsWith("Exception:")) ||
                response.code() in 502..504
        }
    }
}

/**
 * Patient logs (vitals, fluid in/out, symptoms) that couldn't reach the server. They're kept
 * on the phone and uploaded in order once there's a network, so logging never needs internet.
 */
object OfflineOutbox {

    private const val TAG = "OfflineOutbox"
    private const val WORK_NAME = "offline-outbox"

    enum class Kind { VITAL, INTAKE, OUTPUT, SYMPTOMS }

    data class Entry(
        val id: String,
        val kind: Kind,
        /** JSON of the request body, exactly as the screen built it (timestamps included). */
        val bodyJson: String,
        val createdAt: Long,
        /** Set once the server refused it; the entry then waits for the user to retry or discard. */
        val error: String? = null,
    ) {
        val failed: Boolean get() = error != null
        fun body(): Map<String, Any> = gson.fromJson(bodyJson, mapType)
    }

    // Numbers come back as Long where they were whole, so 1107 isn't re-sent as 1107.0.
    private val gson = GsonBuilder().setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE).create()
    private val mapType = object : TypeToken<Map<String, Any>>() {}.type
    private val listType = object : TypeToken<List<Entry>>() {}.type

    private val lock = Mutex()
    private val file by lazy { AtomicFile(File(MyApplication.appContext.filesDir, "offline_outbox.json")) }

    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<Entry>> = _entries

    private val _syncing = MutableStateFlow(false)
    val syncing: StateFlow<Boolean> = _syncing

    /** When the queue last emptied after an upload, for a brief "All synced" note. */
    private val _lastSyncedAt = MutableStateFlow(0L)
    val lastSyncedAt: StateFlow<Long> = _lastSyncedAt

    /**
     * Sends [body] now; if there's no network it's queued instead and counts as saved.
     * Only a refusal from the server comes back as an error.
     */
    suspend fun submit(kind: Kind, body: Map<String, Any>, send: suspend (Map<String, Any>) -> SendResult): DomainResult<Unit> =
        when (val result = send(body)) {
            SendResult.Sent -> DomainResult.Success(Unit)
            is SendResult.Rejected -> DomainResult.Error(Exception(result.reason))
            SendResult.Offline -> {
                enqueue(kind, body)
                toast(R.string.offline_saved_toast)
                DomainResult.Success(Unit)
            }
        }

    /** Queued entries of [kind] still waiting to upload (not the refused ones). */
    fun pending(kind: Kind): List<Entry> = _entries.value.filter { it.kind == kind && !it.failed }

    /** Starts an upload as soon as there's a network; safe to call any time. */
    fun requestSync(context: Context) {
        if (_entries.value.none { !it.failed }) return
        val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, request)
    }

    /** Puts refused entries back in the queue and tries again. */
    suspend fun retryFailed(context: Context) {
        update { list -> list.map { it.copy(error = null) } }
        requestSync(context)
    }

    suspend fun discardFailed() = update { list -> list.filterNot { it.failed } }

    private suspend fun enqueue(kind: Kind, body: Map<String, Any>) {
        val entry = Entry(UUID.randomUUID().toString(), kind, gson.toJson(body), System.currentTimeMillis())
        update { it + entry }
        Log.d(TAG, "queued $kind (${_entries.value.size} waiting)")
        requestSync(MyApplication.appContext)
    }

    /** Uploads queued entries oldest first. Returns false if the network dropped midway. */
    internal suspend fun drain(): Boolean {
        _syncing.value = true
        try {
            while (true) {
                val next = _entries.value.firstOrNull { !it.failed } ?: break
                when (val result = sendQueued(next)) {
                    SendResult.Sent -> update { list -> list.filterNot { it.id == next.id } }
                    SendResult.Offline -> return false
                    is SendResult.Rejected -> {
                        Log.w(TAG, "server refused ${next.kind}: ${result.reason}")
                        update { list -> list.map { if (it.id == next.id) it.copy(error = result.reason) else it } }
                    }
                }
            }
            if (_entries.value.isEmpty()) _lastSyncedAt.value = System.currentTimeMillis()
            return true
        } finally {
            _syncing.value = false
        }
    }

    private suspend fun sendQueued(entry: Entry): SendResult {
        val body = try { entry.body() } catch (e: Exception) { return SendResult.Rejected("Unreadable entry") }
        return when (entry.kind) {
            Kind.VITAL -> AppDependencies.vitalRepository.sendVital(body)
            Kind.INTAKE -> AppDependencies.fluidRepository.sendIntake(body)
            Kind.OUTPUT -> AppDependencies.fluidRepository.sendOutput(body)
            Kind.SYMPTOMS -> AppDependencies.symptomRepository.sendSymptoms(body)
        }
    }

    private suspend fun update(transform: (List<Entry>) -> List<Entry>) = lock.withLock {
        val next = transform(_entries.value)
        save(next)
        _entries.value = next
    }

    private fun load(): List<Entry> = try {
        if (!file.baseFile.exists()) emptyList()
        else gson.fromJson<List<Entry>>(String(file.readFully()), listType).orEmpty()
    } catch (e: Exception) {
        Log.e(TAG, "could not read queue", e)
        emptyList()
    }

    private fun save(list: List<Entry>) {
        val out = file.startWrite()
        try {
            out.write(gson.toJson(list).toByteArray())
            file.finishWrite(out)
        } catch (e: Exception) {
            file.failWrite(out)
            Log.e(TAG, "could not write queue", e)
        }
    }

    private fun toast(res: Int) {
        val context = MyApplication.appContext
        Handler(Looper.getMainLooper()).post { Toast.makeText(context, context.getString(res), Toast.LENGTH_LONG).show() }
    }
}

class OutboxSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result =
        if (OfflineOutbox.drain()) Result.success() else Result.retry()
}
