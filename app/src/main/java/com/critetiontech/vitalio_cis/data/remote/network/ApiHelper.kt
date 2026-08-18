package com.critetiontech.ctvitalio.data.remote.network

import android.content.Context
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Response
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.critetiontech.vitalio_cis.utils.PrefsManager
import com.critetiontech.vitalio_cis.BuildConfig
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import okhttp3.ResponseBody.Companion.toResponseBody

class ApiHelper {

    suspend fun callApi(
        context: Context,
        endpoint: String,
        cacheResponse: Boolean = false,
        showNoConnectionToast: Boolean = true,
        apiCall: suspend (String) -> Response<ResponseBody>
    ): Response<ResponseBody> = withContext(IO) {

        if (!NetworkUtils.isConnected(context)) {

            val cached = PrefsManager(context).getString(context, endpoint)
            if (cached != null) {
                Log.d("ApiHelper", "Offline → Returning cached data")
                return@withContext Response.success(
                    ResponseBody.create(null, cached)
                )
            }
            if (showNoConnectionToast) {
                withContext(Main) {
                    Toast.makeText(context, "No internet connection", Toast.LENGTH_SHORT).show()
                }
            }

            return@withContext Response.error(
                504,
                "No Connection & No Cache".toResponseBody(null)
            )
        }

        try {
            val response = apiCall(endpoint)
            val bodyString = response.body()?.string()
            if (BuildConfig.DEBUG) Log.d("ApiHelper", "API Response [${response.code()}]: $bodyString")

            if (!response.isSuccessful) {
                val errorBodyString = response.errorBody()?.string()
                    ?: bodyString
                    ?: "Server error ${response.code()}"
                Log.e("ApiHelper", "Server error ${response.code()}: $errorBodyString")
                return@withContext Response.error(
                    response.code(),
                    errorBodyString.toResponseBody(null)
                )
            }

            if (cacheResponse && !bodyString.isNullOrEmpty()) {
                PrefsManager(context).saveString(context, endpoint, bodyString)
                Log.d("ApiHelper", "Cached successfully")
            }

            return@withContext Response.success(
                (bodyString ?: "").toResponseBody(null)
            )

        } catch (e: Exception) {

            Log.e("ApiHelper", "API Failed: ${e.message}")
            val cached = PrefsManager(context).getString(context, endpoint)

            return@withContext if (cached != null) {
                Log.d("ApiHelper", "Exception → Returning cached data")
                Response.success(ResponseBody.create(null, cached))
            } else {
                Response.error(
                    500,
                    "Exception: ${e.localizedMessage}".toResponseBody(null)
                )
            }
        }
    }

}