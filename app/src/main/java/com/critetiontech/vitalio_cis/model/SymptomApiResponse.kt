package com.critetiontech.vitalio_cis.model

data class SymptomApiResponse(
    val status: Int,
    val message: String,
    val responseValue: List<SymptomItem>
)

data class SymptomItem(
    val detailId: Int,
    val details: String,
    val detailsDate: String = "",
    val pmId: Int = 0,
    val clientId: Int = 0,
    val pdmId: Int = 2,
    val isProvisionalDiagnosis: Boolean = false,
    var isStillFeeling: Boolean = true
)