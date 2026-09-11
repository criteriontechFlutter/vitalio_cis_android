package com.critetiontech.vitalio_cis.model

data class DrugInteractionRequest(
    val brandId: String,
    val medicineName: String = ""
)

data class DrugInteractionResponse(
    val status: Int,
    val message: String,
    val responseValue: List<DrugInteractionItem>?
)

data class DrugInteractionItem(
    val medicineId: Int = 0,
    val interactedId: Int = 0,
    val interaction: String = "",
    val interactionNature: String = "",
    val interactionEffect: String = "",
    val isLifeThreatning: String? = null,
    val suggestiveAction: String = "",
    val substitute: String = "",
    val reference: String? = null,
    val substituteList: String? = null,
    val adrTypeId: Int? = null,
    val adrType: String? = null
)
