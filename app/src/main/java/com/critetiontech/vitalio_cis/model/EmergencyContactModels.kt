package com.critetiontech.ctvitalio.model

/** One row of api/EmergencyContact; the server may send null for the text fields. */
data class EmergencyContact(
    val id: Int = 0,
    val pid: Int = 0,
    val contactName: String? = null,
    val contactNumber: String? = null,
    val relationship: String? = null,
    val userId: Int = 0,
    val clientId: Int = 0,
)
