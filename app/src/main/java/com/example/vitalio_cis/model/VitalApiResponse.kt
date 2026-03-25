package com.example.vitalio_cis.model

data class VitalApiResponse(
    val status: Int,
    val message: String,
    val responseValue: VitalResponseValue
)

data class VitalResponseValue(
    val lastVital: List<Vital>
)

data class Vital(
    val uhid: String,
    val id: Int,
    val pmId: Int,
    val vitalID: Int,
    val vitalName: String,
    val vitalValue: Double,
    val unit: String,
    val vitalDateTime: String,
    val userId: Int,
    val rowId: Int
)