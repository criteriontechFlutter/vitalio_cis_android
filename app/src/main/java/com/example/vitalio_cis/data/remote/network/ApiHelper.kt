package com.critetiontech.ctvitalio.data.remote.network

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import com.critetiontech.ctvitalio.networking.ApiService
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.example.vitalio_cis.utils.PrefsManager

class ApiHelper {

    suspend fun callApi(
        context: Context,
        endpoint: String,
        cacheResponse: Boolean = false,
        showNoConnectionDialog: Boolean = true,
        apiCall: suspend (String) -> Response<ResponseBody>
    ): Response<ResponseBody> = withContext(Dispatchers.IO) {

        val localKey = endpoint

        // ✅ OFFLINE CASE
        if (!NetworkUtils.isConnected(context)) {

            val cached = PrefsManager(context).getString(context, localKey)

            // 🔥 1. Agar cache hai → DIRECT return (NO dialog)
            if (cached != null) {
                Log.d("ApiHelper", "Offline → Returning cached data")

                return@withContext Response.success(
                    ResponseBody.create(null, cached)
                )
            }

            // 🔥 2. Cache nahi hai → tab decision lo
            if (showNoConnectionDialog) {
                withContext(Dispatchers.Main) {
                    AlertDialog.Builder(context)
                        .setTitle("No Internet Connection")
                        .setMessage("Please check your network and try again.")
                        .setPositiveButton("OK", null)
                        .show()
                }
            }

            // ❌ No cache → error
            return@withContext Response.error(
                504,
                ResponseBody.create(null, "No Connection & No Cache")
            )
        }

        // ✅ ONLINE CASE
        try {
            val response = apiCall(endpoint)

            val bodyString = response.body()?.string()

            Log.d("ApiHelper", "API Response: $bodyString")

            // ✅ SAVE CACHE
            if (response.isSuccessful && cacheResponse && !bodyString.isNullOrEmpty()) {
                PrefsManager(context).saveString(context, localKey, bodyString)
                Log.d("ApiHelper", "Cached successfully")
            }

            return@withContext Response.success(
                ResponseBody.create(null, bodyString ?: "")
            )

        } catch (e: Exception) {

            Log.e("ApiHelper", "API Failed: ${e.message}")

            val cached = PrefsManager(context).getString(context, localKey)

            // 🔥 Exception me bhi fallback
            return@withContext if (cached != null) {
                Log.d("ApiHelper", "Exception → Returning cached data")

                Response.success(ResponseBody.create(null, cached))
            } else {
                Response.error(
                    500,
                    ResponseBody.create(null, "Exception: ${e.localizedMessage}")
                )
            }
        }
    }

    fun getCachedData(context: Context, localKey: String): String? {
        return PrefsManager(context).getString(context, localKey)
    }
}