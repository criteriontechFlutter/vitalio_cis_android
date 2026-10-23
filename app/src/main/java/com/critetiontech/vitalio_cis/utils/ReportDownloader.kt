package com.critetiontech.ctvitalio.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.critetiontech.ctvitalio.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.OutputStream
import java.util.concurrent.TimeUnit

/**
 * Saves patient reports to Downloads/Vitalio and opens them.
 *
 * Uses the app's own OkHttp client rather than the system DownloadManager: the report server's
 * TLS setup trips DownloadManager ("BAD_DECRYPT") while OkHttp, which the app already uses for
 * the same URLs, handles it. Downloads run in an app-wide scope, so leaving the screen doesn't
 * cancel them, and a notification opens the file when it's done.
 */
object ReportDownloader {

    sealed interface State {
        data class Running(val progress: Float?) : State
        data class Done(val uri: Uri, val mimeType: String) : State
        data object Failed : State
    }

    private const val TAG = "ReportDownloader"
    private const val CHANNEL = "report_downloads"
    private const val FOLDER = "Vitalio"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private val _states = MutableStateFlow<Map<String, State>>(emptyMap())
    /** Download state per report key; absent means never downloaded this session. */
    val states: StateFlow<Map<String, State>> = _states

    /** Server paths come back with Windows separators ("Upload\Image\x.png"). */
    fun normalizeUrl(raw: String?): String? =
        raw?.trim()?.replace('\\', '/')?.takeIf { it.startsWith("http", ignoreCase = true) }

    /**
     * Starts downloading [rawUrl] under [key]. Returns false if the URL isn't downloadable.
     * [onFinished] runs on a background thread with the final state.
     */
    fun start(
        context: Context,
        key: String,
        rawUrl: String?,
        fileName: String?,
        mimeType: String?,
        onFinished: (State) -> Unit = {},
    ): Boolean {
        val url = normalizeUrl(rawUrl) ?: return false
        if (_states.value[key] is State.Running) return true
        val app = context.applicationContext
        set(key, State.Running(null))
        scope.launch {
            val result = try {
                download(app, key, url, fileName, mimeType)
            } catch (e: Exception) {
                Log.e(TAG, "download failed: $url", e)
                State.Failed
            }
            set(key, result)
            if (result is State.Done) notifyDone(app, key, displayName(fileName, url), result)
            onFinished(result)
        }
        return true
    }

    /** Opens a downloaded report in whatever app handles its type. Returns false if none can. */
    fun open(context: Context, done: State.Done): Boolean =
        launch(context, viewIntent(done.uri, done.mimeType))

    /** Opens the report from the server in another app (browser, PDF viewer). */
    fun openRemote(context: Context, rawUrl: String?): Boolean {
        val url = normalizeUrl(rawUrl) ?: return false
        return launch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun isImage(mimeType: String?, fileName: String?): Boolean =
        mimeType?.startsWith("image/", ignoreCase = true) == true ||
            fileName?.substringAfterLast('.', "")?.lowercase() in setOf("png", "jpg", "jpeg", "webp", "gif", "bmp", "heic")

    // ── internals ───────────────────────────────────────────────────────────────

    private fun set(key: String, state: State) = _states.update { it + (key to state) }

    private fun download(context: Context, key: String, url: String, fileName: String?, mimeType: String?): State {
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) {
                Log.w(TAG, "HTTP ${response.code} for $url")
                return State.Failed
            }
            val body = response.body
            val name = uniqueName(displayName(fileName, url))
            val mime = resolveMime(mimeType ?: body.contentType()?.toString(), name)
            val total = body.contentLength()
            val target = createTarget(context, name, mime) ?: return State.Failed

            try {
                target.open().use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(16 * 1024)
                        var written = 0L
                        var lastReported = -1
                        while (true) {
                            val read = input.read(buffer)
                            if (read < 0) break
                            out.write(buffer, 0, read)
                            written += read
                            if (total > 0) {
                                val percent = (written * 100 / total).toInt()
                                if (percent != lastReported) {
                                    lastReported = percent
                                    set(key, State.Running(percent / 100f))
                                }
                            }
                        }
                    }
                }
                target.publish()
            } catch (e: Exception) {
                target.discard()
                throw e
            }
            return State.Done(target.uri, mime)
        }
    }

    /** Where a download is written, hiding the MediaStore (Android 10+) vs file (9 and older) split. */
    private class Target(
        val uri: Uri,
        val open: () -> OutputStream,
        val publish: () -> Unit,
        val discard: () -> Unit,
    )

    private fun createTarget(context: Context, name: String, mime: String): Target? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, mime)
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$FOLDER")
                // Hidden from other apps until fully written.
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return null
            return Target(
                uri = uri,
                open = { resolver.openOutputStream(uri) ?: error("no output stream") },
                publish = { resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) },
                discard = { resolver.delete(uri, null, null) },
            )
        }
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER)
        if (!dir.exists() && !dir.mkdirs()) return null
        val file = File(dir, name)
        return Target(
            uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file),
            open = { file.outputStream() },
            publish = { MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null) },
            discard = { file.delete() },
        )
    }

    private fun notifyDone(context: Context, key: String, title: String, done: State.Done) {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL, "Report downloads", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Tells you when a downloaded report is ready to open"
            }
        )
        val open = PendingIntent.getActivity(
            context, key.hashCode(), viewIntent(done.uri, done.mimeType),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText("Saved to Downloads/$FOLDER. Tap to open.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(key.hashCode(), notification)
        } catch (e: SecurityException) {
            // Notification permission revoked mid-download.
        }
    }

    private fun viewIntent(uri: Uri, mime: String) = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)

    private fun launch(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun resolveMime(mimeType: String?, name: String): String =
        mimeType?.substringBefore(';')?.trim()?.takeIf { it.contains('/') }
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase())
            ?: "application/octet-stream"

    private fun displayName(fileName: String?, url: String): String =
        (fileName?.takeIf { it.isNotBlank() } ?: URLUtil.guessFileName(url, null, null))
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")

    /** Timestamped so downloading "report.pdf" twice doesn't overwrite or fail. */
    private fun uniqueName(name: String): String {
        val dot = name.lastIndexOf('.')
        val stamp = System.currentTimeMillis() / 1000
        return if (dot > 0) "${name.substring(0, dot)}_$stamp${name.substring(dot)}" else "${name}_$stamp"
    }
}
