package com.example.vitalio_cis.model

data class OutputResponse(
    val status: Int,
    val message: String,
    val responseValue: List<OutputItem>
)

data class OutputItem(
    val id: Int = 0,
    val pmID: Int = 0,
    val outputTypeId: Int = 0,
    val quantity: Int = 0,
    val unitID: Int = 0,
    val outputDate: String = "",
    val userID: Int = 0,
    val outputDateFormat: String = "",
    val outputTimeFormat: String = "",
    val clientID: Int = 0,
    val colour: String = "",
    val isFromPatient: Boolean = false,
    val isFromMachine: Boolean = false,
    val userName: String = "",
    val unitName: String? = null,
    val outputType: String? = null
)

data class OutputSummaryResponse(
    val status: Int,
    val message: String,
    val responseValue: List<OutputSummaryItem>
)

data class OutputSummaryItem(
    val outputDate: String = "",
    val quantity: Int = 0,
    val repetition: Int = 0
)
