package com.critetiontech.ctvitalio.networking

import com.critetiontech.vitalio_cis.network.RetryInterceptor
import com.critetiontech.vitalio_cis.utils.MyApplication
import com.critetiontech.ctvitalio.utils.NetworkUtils
import com.critetiontech.vitalio_cis.BuildConfig
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // Base URLs
    const val BASE_5082 = "https://vitaliocis.vitalio.care:4082/"
    const val BASE_5084 = "https://vitaliocis.vitalio.care:4084/"
    const val BASE_4096 = "https://vitaliocis.vitalio.care:4096/"
    const val BASE_44374 = "https://vitaliocis.vitalio.care:44374/"
    const val BASE_4090 = "https://vitaliocis.vitalio.care:4090/"
    const val Digi_doctor_BaseURL = "http://52.172.134.222:205/api/v1.0/"

    private const val DEFAULT_BASE_URL = BASE_5082

    // 50 MB disk cache — GET responses served instantly on repeat calls
    private val httpCache by lazy {
        Cache(
            directory = File(MyApplication.appContext.cacheDir, "http_cache"),
            maxSize = 50L * 1024 * 1024
        )
    }

    // Force Cache-Control on responses so OkHttp caches them for 60 s when online
    private val onlineCacheInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        response.newBuilder()
            .header("Cache-Control", "public, max-age=60")
            .removeHeader("Pragma")
            .build()
    }

    // When offline, serve stale cache up to 7 days
    private val offlineCacheInterceptor = Interceptor { chain ->
        var request = chain.request()
        if (!NetworkUtils.isConnected(MyApplication.appContext)) {
            request = request.newBuilder()
                .header("Cache-Control", "public, only-if-cached, max-age=${7 * 24 * 60 * 60}")
                .build()
        }
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
    }

    // Shared base client — all per-URL clients derive from this
    private val baseClient by lazy {
        OkHttpClient.Builder()
            .cache(httpCache)
            .addInterceptor(offlineCacheInterceptor)   // offline: serve cache
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