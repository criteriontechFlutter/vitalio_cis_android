package com.critetiontech.ctvitalio.data.remote.network

import com.critetiontech.ctvitalio.config.ApiConfig
import com.critetiontech.ctvitalio.networking.RetrofitInstance

object ApiClients {
    val module4082 = RetrofitInstance.createApiService(includeAuthHeader = true)
    val module4084 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.MODULE_4084)
    val module4094 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.MODULE_4096)
    val module4090 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.MODULE_4090)
    val module44374 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.MODULE_44374)
    val digidoctor_BaseURL = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.DIGI_DOCTOR)
    val module4119 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.MODULE_4119)
    val consultation = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl = ApiConfig.CONSULTATION)
}
