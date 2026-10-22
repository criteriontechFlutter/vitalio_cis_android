package com.critetiontech.ctvitalio.ring

import com.critetiontech.ctvitalio.config.ApiConfig

import android.util.Log
import com.critetiontech.ctvitalio.BuildConfig
import com.critetiontech.ctvitalio.model.UltrahumanDashboardData
import com.critetiontech.ctvitalio.model.UltrahumanSleepDetail
import com.critetiontech.ctvitalio.model.UltrahumanVitalsApiResponse
import com.critetiontech.ctvitalio.model.VitalInsightDetail
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private const val TAG = "SmartRingService"

/** Ultrahuman ring network calls: telemetry fetch, OAuth code exchange and backend token storage. */
object SmartRingService {

    const val CLIENT_ID = "W3hWLU2juogFGfgJBdpj3uuaI1n876CwvalFCIFEBKo"
    const val REDIRECT_URI = "${ApiConfig.RING_TOKENS}callback"
    const val AUTH_BASE_URL = "https://auth.ultrahuman.com/authorise"
    const val SCOPE = "profile ring_data cgm_data ring_extended_data"
    private const val TOKEN_EXCHANGE_URL = "https://partner.ultrahuman.com/oauth/token"
    private const val INSERT_TOKEN_URL = "${ApiConfig.RING_TOKENS}api/APITokens/InsertAPITokens"
    private const val VITALS_URL = "${ApiConfig.RING_VITALS}api/UltrahumanVitals/GetUltrahumanVitalsByPid"

    private val gson = Gson()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
    private val jsonType = "application/json".toMediaType()

    /** Fetches lastVital, sleepmetrics and vitalInsights; returns empty data on any failure. */
    suspend fun fetchUltrahumanData(pid: Int, clientId: Int): UltrahumanDashboardData = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$VITALS_URL?pid=$pid&clientId=$clientId")
                .header("Accept", "application/json")
                .get()
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Non-200 HTTP status: ${response.code}")
                    return@withContext UltrahumanDashboardData()
                }
                response.body?.string()
            } ?: return@withContext UltrahumanDashboardData()

            val value = gson.fromJson(body, UltrahumanVitalsApiResponse::class.java)?.responseValue
                ?: return@withContext UltrahumanDashboardData()

            val sleepDetail = value.sleepmetrics?.firstOrNull()?.vitalValue?.let {
                try { gson.fromJson(it, UltrahumanSleepDetail::class.java) }
                catch (e: Exception) { Log.w(TAG, "Failed to decode sleepmetrics vitalValue", e); null }
            }
            val insightDetail = value.vitalInsights?.firstOrNull()?.insightJson?.let {
                try { gson.fromJson(it, VitalInsightDetail::class.java) }
                catch (e: Exception) { Log.w(TAG, "Failed to decode vitalInsights insightJson", e); null }
            }

            UltrahumanDashboardData(
                lastVitals = value.lastVital.orEmpty(),
                sleepDetail = sleepDetail,
                insightDetail = insightDetail
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching ring data: ${e.message}")
            UltrahumanDashboardData()
        }
    }

    data class Tokens(val accessToken: String, val tokenType: String, val expiresIn: String, val refreshToken: String)

    /** Exchanges an authorization code for tokens (used only when the redirect carries a code). */
    suspend fun exchangeCodeForToken(code: String): Tokens? = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("grant_type", "authorization_code")
                .put("code", code)
                .put("redirect_uri", REDIRECT_URI)
                .put("client_id", CLIENT_ID)
            val request = Request.Builder()
                .url(TOKEN_EXCHANGE_URL)
                .post(payload.toString().toRequestBody(jsonType))
                .build()
            val body = client.newCall(request).execute().use { it.body?.string() } ?: return@withContext null
            if (BuildConfig.DEBUG) Log.d(TAG, "Token response: $body")
            val json = JSONObject(body)
            val accessToken = json.optString("access_token").takeIf { it.isNotEmpty() } ?: return@withContext null
            Tokens(
                accessToken = accessToken,
                tokenType = json.optString("token_type", "Bearer"),
                expiresIn = json.opt("expires_in")?.toString() ?: "86400",
                refreshToken = json.optString("refresh_token", "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Token exchange failed: ${e.message}")
            null
        }
    }

    /** Stores tokens on the Vitalio backend. Failures are logged only, as on iOS. */
    suspend fun insertTokensOnBackend(loginId: String, tokens: Tokens) = withContext(Dispatchers.IO) {
        try {
            val payload = JSONObject()
                .put("loginId", loginId)
                .put("accessToken", tokens.accessToken)
                .put("tokenType", tokens.tokenType)
                .put("expiresIn", tokens.expiresIn)
                .put("refreshToken", tokens.refreshToken)
                .put("scope", "profile ring_data cgm_data")
            val request = Request.Builder()
                .url(INSERT_TOKEN_URL)
                .post(payload.toString().toRequestBody(jsonType))
                .build()
            client.newCall(request).execute().use { Log.d(TAG, "InsertAPITokens ← [${it.code}]") }
        } catch (e: Exception) {
            Log.w(TAG, "InsertAPITokens failed: ${e.message}")
        }
    }
}