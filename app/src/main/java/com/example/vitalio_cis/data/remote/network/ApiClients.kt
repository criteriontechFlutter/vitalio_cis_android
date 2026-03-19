package com.critetiontech.ctvitalio.data.remote.network

import com.critetiontech.ctvitalio.networking.RetrofitInstance

object ApiClients {
    val module4082 = RetrofitInstance.createApiService(includeAuthHeader = true)
  }