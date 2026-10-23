package com.critetiontech.ctvitalio.config

import android.content.Context
import android.content.Intent
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.reminder.MedicineReminderScheduler
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.PrefsManager
import com.critetiontech.ctvitalio.voice.VoiceEndpoints
import kotlinx.coroutines.runBlocking

/**
 * Vitalio backend servers. Every environment runs the same services on the same ports; only the
 * host changes.
 */
enum class ApiEnvironment(val label: String, val host: String) {
    LOCAL("Local", "http://182.156.200.178"),
    STAGING("Staging", "https://vitaliocis.vitalio.care"),
    PRODUCTION("Production", "https://cis.vitalio.care"),
}

/** Every server address the app talks to. Nothing else in the app should hard-code a URL. */
object ApiConfig {

    private const val PREFS = "app_environment"
    private const val KEY_ENV = "api_env"
    private const val KEY_LOCAL_HOST = "local_host"

    /**
     * Whether testers can change the server from inside the app (tap the login picture 7 times).
     * Always on for debug builds; release builds only when built with ENV_SWITCHER=true, so a
     * Play Store build can never be pointed at a test server.
     */
    val canSwitch: Boolean = BuildConfig.DEBUG || BuildConfig.ENV_SWITCHER

    /** The build's default: API_ENV in local.properties (always PRODUCTION for release). */
    private val buildDefault: ApiEnvironment =
        runCatching { ApiEnvironment.valueOf(BuildConfig.API_ENV.uppercase()) }.getOrDefault(ApiEnvironment.PRODUCTION)

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Fixed for the life of the process; [switchTo] restarts the app to change it. */
    val environment: ApiEnvironment = run {
        if (!canSwitch) return@run buildDefault
        val saved = prefs(MyApplication.appContext).getString(KEY_ENV, null)
        saved?.let { runCatching { ApiEnvironment.valueOf(it) }.getOrNull() } ?: buildDefault
    }

    /** The Local server a tester typed in, or [ApiEnvironment.LOCAL]'s default. */
    val localHost: String =
        (if (canSwitch) prefs(MyApplication.appContext).getString(KEY_LOCAL_HOST, null) else null)
            ?: ApiEnvironment.LOCAL.host

    /** Host the app is using right now. */
    val host: String = if (environment == ApiEnvironment.LOCAL) localHost else environment.host

    /**
     * Turns what a tester typed ("192.168.1.10", "http://192.168.1.10/") into "http://192.168.1.10",
     * or null if it isn't a usable host. Ports are added per service, so none is allowed here.
     */
    fun normalizeHost(input: String): String? {
        val trimmed = input.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        val withScheme = if ("://" in trimmed) trimmed else "http://$trimmed"
        val uri = runCatching { java.net.URI(withScheme) }.getOrNull() ?: return null
        if (uri.scheme !in listOf("http", "https") || uri.host.isNullOrEmpty()) return null
        if (uri.port != -1 || !uri.path.isNullOrEmpty()) return null
        return "${uri.scheme}://${uri.host}"
    }

    /**
     * Saves [env], signs the user out (accounts and data differ per server) and restarts the app
     * so every API client is rebuilt against the new host.
     */
    fun switchTo(context: Context, env: ApiEnvironment, localHost: String = this.localHost) {
        if (!canSwitch) return
        if (env == environment && (env != ApiEnvironment.LOCAL || localHost == this.localHost)) return
        val app = context.applicationContext
        // commit(): the process is killed right after, so the writes must be on disk first
        prefs(app).edit().putString(KEY_ENV, env.name).putString(KEY_LOCAL_HOST, localHost).commit()
        PrefsManager(app).clearAll()
        // clearAll() uses apply(); flush it the same way before the process exits
        app.getSharedPreferences("patient_prefs", Context.MODE_PRIVATE).edit().commit()
        // No patient now, so this cancels every medicine alarm; quick, it doesn't hit the network
        runBlocking { MedicineReminderScheduler.sync(app) }

        val launch = app.packageManager.getLaunchIntentForPackage(app.packageName) ?: return
        app.startActivity(Intent.makeRestartActivityTask(launch.component))
        Runtime.getRuntime().exit(0)
    }

    private fun service(port: Int) = "$host:$port/"

    // ── Vitalio backend (switches with the environment) ─────────────────────────
    /** Patient, vitals, activity, media: the default for most calls. */
    val MODULE_4082 = service(4082)
    /** Doctor profiles. */
    val MODULE_4084 = service(4084)
    /** Lab investigations and results. */
    val MODULE_4090 = service(4090)
    /** Fluid intake. */
    val MODULE_4096 = service(4096)
    /** Masters: country, state, city. */
    val MODULE_4119 = service(4119)
    val MODULE_44374 = service(44374)

    // ── Other services (same for every environment) ─────────────────────────────
    const val DIGI_DOCTOR = "http://52.172.134.222:205/api/v1.0/"
    /** Lab report and prescription reading (OCR). */
    const val AI_UPLOAD = "http://food.shopright.ai:3480/"
    const val ECHO_AI = VoiceEndpoints.ECHO_AI
    const val DICTATION_WS = VoiceEndpoints.DICTATION_WS
    const val NUTRI_ANALYSER = "https://nutrianalyser.com:313/api"
    /** Smart ring token storage and vitals. */
    const val RING_TOKENS = "https://vitalioapi.medvantage.tech:5082/"
    const val RING_VITALS = "https://demo.vitalio.care:5082/"
    /**
     * Video consultation (LiveKit) APIs: api/consultation/join, decline, end, status.
     * Only on the local test server for now; change this one line when it goes live
     * (e.g. to MODULE_4082 once the main backend has them).
     */
    const val CONSULTATION = "http://172.16.61.31:4082/"
}
