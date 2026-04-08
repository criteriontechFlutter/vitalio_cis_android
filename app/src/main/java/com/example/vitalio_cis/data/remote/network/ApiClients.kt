package com.critetiontech.ctvitalio.data.remote.network

import com.critetiontech.ctvitalio.networking.RetrofitInstance
import com.critetiontech.ctvitalio.networking.RetrofitInstance.Digi_doctor_BaseURL

object ApiClients {
    val module4082 = RetrofitInstance.createApiService(includeAuthHeader = true)
    val Digidoctor_BaseURL = RetrofitInstance.createApiService(includeAuthHeader = true,

        baseUrl = Digi_doctor_BaseURL)
  }