package com.example.vitalio_cis.model

data class ProblemResponse(
    val responseCode: Int,
    val responseMessage: String,
    val responseValue: List<Problem>
)
data class Problem(
    val problemId: Int,
    val problemName: String,
    val isVisible: Int,
    val displayIcon: String,
    val translation: String?
)