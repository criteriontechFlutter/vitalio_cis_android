package com.critetiontech.ctvitalio.data.remote.network

import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.networking.RetrofitInstance.BASE_4096
import com.critetiontech.ctvitalio.networking.RetrofitInstance.BASE_5084
import com.critetiontech.ctvitalio.networking.RetrofitInstance.Digi_doctor_BaseURL

object ApiClients {
    val module4082 = RetrofitInstance.createApiService(includeAuthHeader = true)
    val module4084 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl =BASE_5084 )
    val module4094 = RetrofitInstance.createApiService(includeAuthHeader = true, baseUrl =BASE_4096 )
    val Digidoctor_BaseURL = RetrofitInstance.createApiService(includeAuthHeader = true,

        baseUrl = Digi_doctor_BaseURL)
  }