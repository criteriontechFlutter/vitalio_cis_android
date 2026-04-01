package com.critetiontech.ctvitalio.networking

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


object RetrofitInstance {

    // Base URLs
    const val BASE_5082 = "http://182.156.200.178:4082/"
    const val Digi_doctor_BaseURL = "http://52.172.134.222:205/api/v1.0/"
//    const val BASE_5096 = "http://182.156.200.177:5096/"

    // Default
    private const val DEFAULT_BASE_URL = BASE_5082

    // Shared OkHttpClient
    private val baseClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor())
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    // Logging
    private fun loggingInterceptor() = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Authorization / Header Interceptor
    private fun authInterceptor(headers: Map<String, String>) = Interceptor { chain ->
        val builder = chain.request().newBuilder()
        headers.forEach { (key, value) -> builder.addHeader(key, value) }
        chain.proceed(builder.build())
    }

    // 🔹 Main unified function to create ApiService
    fun createApiService(
        baseUrl: String = DEFAULT_BASE_URL,
        includeAuthHeader: Boolean = false,
        additionalHeaders: Map<String, String> = emptyMap()
    ): ApiService {
        val headers = mutableMapOf<String, String>()

        if (includeAuthHeader) {
            headers["Content-Type"] = "application/json"
            // You can also add auth token dynamically here:
            // headers["Authorization"] = "Bearer ${getToken()}"
        }
        headers.putAll(additionalHeaders)

        val clientWithHeaders = baseClient.newBuilder()
            .addInterceptor(authInterceptor(headers))
            .build()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(clientWithHeaders)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}