package com.critetiontech.vitalio_cis.model

data class AllergyApiResponse(
    val status: Int,
    val message: String,
    val responseValue: List<AllergyItem>
)

data class AllergyItem(
    val detailID: Int = 0,
    val detailsDate: String = "",
    val details: String = "",
    val substanceName: String = "",
    val allergy: String = "",
    val reaction: String = ""
)