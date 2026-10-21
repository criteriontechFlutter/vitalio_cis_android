package com.critetiontech.ctvitalio.data

import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.utils.Patient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Saves vitals read from a device (watch, Omron monitor…) through AddVital, stamped with when they were measured. */
object VitalUpload {

    /** Every AddVital field, so a request only carries the vitals it is saving. */
    private val EMPTY_FIELDS = listOf(
        "vmValueBPSys", "vmValueBPDias", "vmValueSPO2", "vmValueRespiratoryRate", "vmValueHeartRate",
        "vmValuePulse", "vmValueRbs", "vmValueTemperature", "vmValueWeight", "vmValueHeight"
    ).associateWith { "" }

    /** Returns null when saved, otherwise the reason it wasn't. */
    suspend fun post(patient: Patient, measuredAt: Instant, fields: Map<String, String>): String? {
        val local = measuredAt.atZone(ZoneId.systemDefault())
        val body = EMPTY_FIELDS + fields + mapOf(
            "uhid" to patient.uhId, "userId" to patient.pid.toString(),
            "vitalDate" to local.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "vitalTime" to local.format(DateTimeFormatter.ofPattern("HH:mm")),
            "clientId" to patient.clientId.toString(),
            "isFromPatient" to "true", "isFromMachine" to "0", "positionId" to "0"
        )
        return when (val r = AppDependencies.addVital()(body)) {
            is DomainResult.Success -> null
            is DomainResult.Error -> r.exception.message ?: "Couldn't save the vitals. Please try again."
        }
    }
}
