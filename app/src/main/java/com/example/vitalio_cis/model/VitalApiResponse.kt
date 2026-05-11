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
    val uhid: String = "",
    val id: Int = 0,
    val pmId: Int = 0,
    val vitalID: Int = 0,
    val vitalName: String = "",
    val vitalValue: Double = 0.0,
    val unit: String = "",
    val vitalDateTime: String = "",
    val userId: Int = 0,
    val rowId: Int = 0,
    val displayValue: String = ""   // 👈 add this
)