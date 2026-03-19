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
import com.critetiontech.ctvitalio.utils.LocalStorage
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.example.vitalio_cis.utils.PrefsManager

//class ApiHelper {
//
//    /**
//     * Generic function for any API call with automatic caching.
//     *
//     * @param localKey Key to store cached data
//     * @param apiCall Lambda that performs the Retrofit call
//     */
//    suspend fun callApi(
//        context: Context,
//        endpoint: String,
//        cacheResponse: Boolean = false,           // Save locally only for GET
//        showNoConnectionDialog: Boolean = true,  // Show dialog if offline
//        apiCall: suspend (String) -> Response<ResponseBody>
//    ): Response<ResponseBody> = withContext(Dispatchers.IO) {
//
//        val localKey = endpoint
//
//        // Offline: show dialog and return cached data if available
//        if (!NetworkUtils.isConnected(context)) {
//            if (showNoConnectionDialog) {
//                withContext(Dispatchers.Main) {
//                    AlertDialog.Builder(context)
//                        .setTitle("No Internet Connection")
//                        .setMessage("Please check your network and try again.")
//                        .setPositiveButton("OK", null)
//                        .show()
//                }
//            }
//
//            val cached = LocalStorage.getString(context, localKey)
//            return@withContext if (cached != null) {
//                Response.success(ResponseBody.create(null, cached))
//            } else {
//                Response.error(504, ResponseBody.create(null, "No Connection"))
//            }
//        }
//
//        try {
//            val response = apiCall(endpoint)
//
//            // Save body locally if GET (cacheResponse = true)
//            if (response.isSuccessful && cacheResponse) {
//                response.body()?.string()?.let { bodyString ->
//                    LocalStorage.saveString(context, localKey, bodyString)
//                }
//            }
//
//            // Return the full Response object
//            response
//        } catch (e: Exception) {
//            // Exception: fallback to cached data for GET
//            val cached = LocalStorage.getString(context, localKey)
//            cached?.let {
//                Response.success(ResponseBody.create(null, it))
//            } ?: Response.error(500, ResponseBody.create(null, "Exception: ${e.localizedMessage}"))
//        }
//    }
//
//    // Helper to retrieve cached data directly
//    fun getCachedData(  context: Context,localKey: String): String? {
//        return LocalStorage.getString(context, localKey)
//    }
//}


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

            val cached = LocalStorage.getString(context, localKey)

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
                LocalStorage.saveString(context, localKey, bodyString)
                Log.d("ApiHelper", "Cached successfully")
            }

            return@withContext Response.success(
                ResponseBody.create(null, bodyString ?: "")
            )

        } catch (e: Exception) {

            Log.e("ApiHelper", "API Failed: ${e.message}")

            val cached = LocalStorage.getString(context, localKey)

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
        return LocalStorage.getString(context, localKey)
    }
}