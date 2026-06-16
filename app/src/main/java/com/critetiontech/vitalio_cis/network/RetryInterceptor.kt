package com.critetiontech.vitalio_cis.network

import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Application-level OkHttp interceptor that retries failed requests with exponential backoff.
 *
 * Retries on:
 *  - IOException (network drop, connection reset, timeout on connect)
 *  - HTTP 500, 502, 503, 504 (transient server errors)
 *
 * Does NOT retry:
 *  - 4xx client errors (bad request, unauthorized, not found — retrying won't help)
 *  - 2xx/3xx (already successful)
 *
 * Backoff: 1 s → 2 s → 4 s (capped at 8 s)
 */
class RetryInterceptor(private val maxRetries: Int = 3) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var attempt = 0
        var lastException: IOException? = null
        var lastResponse: Response? = null

        while (attempt <= maxRetries) {
            if (attempt > 0) {
                lastResponse?.close()
                val delayMs = minOf(1000L shl (attempt - 1), 8_000L) // 1s, 2s, 4s, capped 8s
                Log.d("RetryInterceptor", "Retry $attempt/${maxRetries} in ${delayMs}ms — ${request.url}")
                Thread.sleep(delayMs)
            }

            try {
                val response = chain.proceed(request)

                // Success or client error — don't retry
                if (response.isSuccessful || response.code in 400..499) return response

                // Server error — will retry if attempts remain
                lastResponse = response
            } catch (e: IOException) {
                lastException = e
                if (attempt == maxRetries) throw e
            }

            attempt++
        }

        return lastResponse
            ?: throw lastException
            ?: IOException("RetryInterceptor: max retries ($maxRetries) exhausted for ${request.url}")
    }
}