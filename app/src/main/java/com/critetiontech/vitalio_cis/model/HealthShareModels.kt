package com.critetiontech.ctvitalio.model

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/** One row of api/PatientHealthShare/GetAllPatientHealthShareLinks. */
data class HealthShareLink(
    val id: Int = 0,
    val pId: Int = 0,
    val shareUuid: String? = null,
    val recipientEmail: String? = null,
    val isActive: Boolean = false,
    /** UTC, without an offset, e.g. "2026-10-30T11:05:41". */
    val createdDate: String? = null,
    val revokedAt: String? = null,
    /** "Active" or "Revoked". */
    val shareStatus: String? = null,
) {
    val active: Boolean get() = isActive && !shareStatus.equals("Revoked", ignoreCase = true)
}

/** What GeneratePatientHealthShareLink returns; the access code itself only goes out by email. */
data class HealthShareCreated(
    val shareUuid: String? = null,
    val shareUrl: String? = null,
    val recipientEmail: String? = null,
)

/** The share API sends UTC times without an offset; this turns one into the phone's local time. */
fun healthShareLocalTime(utc: String?): LocalDateTime? = utc?.takeIf { it.isNotBlank() }?.let {
    runCatching {
        LocalDateTime.parse(it.substringBefore('.').removeSuffix("Z"))
            .atOffset(ZoneOffset.UTC).atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }.getOrNull()
}
