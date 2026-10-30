package com.critetiontech.ctvitalio.networking

import com.critetiontech.ctvitalio.config.ApiConfig

import com.critetiontech.ctvitalio.network.RetryInterceptor
import com.critetiontech.ctvitalio.utils.MyApplication
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.critetiontech.ctvitalio.BuildConfig
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // Server addresses live in ApiConfig
    private val DEFAULT_BASE_URL = ApiConfig.MODULE_4082

    // 50 MB disk cache — GET responses served instantly on repeat calls
//    private val httpCache by lazy {
//        Cache(
//            directory = File(MyApplication.appContext.cacheDir, "http_cache"),
//            maxSize = 50L * 1024 * 1024
//        )
//    }

    // Force Cache-Control on responses so OkHttp caches them for 60 s when online
    private val onlineCacheInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=60")
            .removeHeader("Pragma")
            .build()
    }

    // Per-request read timeout for slow endpoints: send this header (seconds) and it overrides
    // the 20 s default for that call only. The header is stripped before it reaches the server.
    const val READ_TIMEOUT_HEADER = "X-Read-Timeout-Sec"

    private val perRequestTimeoutInterceptor = Interceptor { chain ->
        val request = chain.request()
        val seconds = request.header(READ_TIMEOUT_HEADER)?.toIntOrNull()
            ?: return@Interceptor chain.proceed(request)
        chain.withReadTimeout(seconds, TimeUnit.SECONDS)
            .proceed(request.newBuilder().removeHeader(READ_TIMEOUT_HEADER).build())
    }

    // When offline, serve stale cache up to 7 days
//    private val offlineCacheInterceptor = Interceptor { chain ->
//        var request = chain.request()
//        if (!NetworkUtils.isConnected(MyApplication.appContext)) {
//            request = request.newBuilder()
//                .header("Cache-Control", "public, only-if-cached, max-age=${7 * 24 * 60 * 60}")
//                .build()
//        }
//        chain.proceed(request)
//    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    // Shared base client — all per-URL clients derive from this
    private val baseClient by lazy {
        OkHttpClient.Builder()
//            .cache(httpCache)
//            .addInterceptor(offlineCacheInterceptor)   // offline: serve cache
            .addInterceptor(perRequestTimeoutInterceptor)   // before Retry so every attempt gets it
            .addInterceptor(RetryInterceptor(maxRetries = 3))
            .addInterceptor(loggingInterceptor)
            .addNetworkInterceptor(onlineCacheInterceptor) // online: write cache headers
            .connectTimeout(10, TimeUnit.SECONDS)   // was 60 s — fail fast on no route
            .readTimeout(20, TimeUnit.SECONDS)       // was 60 s
            .writeTimeout(20, TimeUnit.SECONDS)      // was 60 s
            .retryOnConnectionFailure(false)         // RetryInterceptor handles this now
            .build()
    }

    private fun authInterceptor(headers: Map<String, String>) = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        chain.proceed(builder.build())
    }

    fun createApiService(
        baseUrl: String = DEFAULT_BASE_URL,
        includeAuthHeader: Boolean = false,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiService {
        val headers = mutableMapOf<String, String>()
        if (includeAuthHeader) {
            headers["Content-Type"] = "application/json"
        }
        headers.putAll(additionalHeaders)

        val client = baseClient.newBuilder()
            .addInterceptor(authInterceptor(headers))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}