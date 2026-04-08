package com.example.vitalio_cis.model

data class SymptomApiResponse(
    val status: Int,
    val message: String,
    val responseValue: List<SymptomItem>
)

data class SymptomItem(
    val detailId: Int,
    val details: String
)