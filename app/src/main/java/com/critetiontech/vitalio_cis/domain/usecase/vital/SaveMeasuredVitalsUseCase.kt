package com.critetiontech.ctvitalio.domain.usecase.vital

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.VitalRepository
import com.critetiontech.ctvitalio.utils.Patient
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Saves vitals read from a device (watch, Omron monitor…) through AddVital, stamped with when they were measured. */
class SaveMeasuredVitalsUseCase(private val repository: VitalRepository) {

    /** [fields] are AddVital's own names, e.g. "vmValueBPSys" to "120". */
    suspend operator fun invoke(patient: Patient, measuredAt: Instant, fields: Map<String, String>): DomainResult<Unit> {
        val local = measuredAt.atZone(ZoneId.systemDefault())
        val body = EMPTY_FIELDS + fields + mapOf(
            "uhid" to patient.uhId, "userId" to patient.pid.toString(),
            "vitalDate" to local.format(DateTimeFormatter.ISO_LOCAL_DATE),
            "vitalTime" to local.format(DateTimeFormatter.ofPattern("HH:mm")),
            "clientId" to patient.clientId.toString(),
            "isFromPatient" to "true", "isFromMachine" to "0", "positionId" to "0"
        )
        return repository.addVital(body)
    }

    private companion object {
        /** Every AddVital field, so a request only carries the vitals it is saving. */
        val EMPTY_FIELDS = listOf(
            "vmValueBPSys", "vmValueBPDias", "vmValueSPO2", "vmValueRespiratoryRate", "vmValueHeartRate",
            "vmValuePulse", "vmValueRbs", "vmValueTemperature", "vmValueWeight", "vmValueHeight"
        ).associateWith { "" }
    }
}
