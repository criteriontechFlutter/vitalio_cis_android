package com.critetiontech.ctvitalio.networking

import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HeaderMap
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.QueryMap
import retrofit2.http.Url


interface ApiService {
    @GET
    suspend fun dynamicGet(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @QueryMap(encoded = true) params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST
    suspend fun dynamicRawPost(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Body body: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>

    @POST
    suspend fun queryDynamicRawPost(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @QueryMap(encoded = true) params: Map<String, @JvmSuppressWildcards Any>
    ): Response<ResponseBody>
    // Add similar annotations for PUT, DELETE, etc.


    @Multipart
    @POST
    suspend fun dynamicMultipartPost(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Part parts: List<MultipartBody.Part>
    ): Response<ResponseBody>


    @Multipart
    @PUT
    suspend fun dynamicMultipartPut(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @Part parts: List<MultipartBody.Part>
    ): Response<ResponseBody>


    @DELETE
    suspend fun dynamicDelete(
        @Url url: String,
        @HeaderMap headers: Map<String, String> = emptyMap(),
        @QueryMap(encoded = true) params: Map<String, @JvmSuppressWildcards Any> = emptyMap()
    ): Response<ResponseBody>

}