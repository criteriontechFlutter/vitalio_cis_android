package com.critetiontech.ctvitalio.voice

import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult

/** Phone implementation of the shared voice pipeline's [VoiceBackend]. */
object AppVoiceBackend : VoiceBackend {

    override fun currentPatient(): VoicePatient? =
        AppDependencies.prefs.getPatient()?.let { VoicePatient(it.pid, it.clientId, it.uhId) }

    override suspend fun pendingMedicines(date: String): List<ScheduledMedicineRef> {
        val patient = AppDependencies.prefs.getPatient() ?: return emptyList()
        val res = AppDependencies.fetchMedicineIntake()(patient.pid, date, patient.clientId)
        if (res !is DomainResult.Success) return emptyList()
        return res.data.flatMap { it.medicineData }.filter { !it.isTaken }.map { med ->
            ScheduledMedicineRef(
                id                = med.id,
                name              = med.medicineName,
                scheduledDateTime = med.scheduledDateTime,
                dosageStrength    = med.dosageStrength.toString(),
                unit              = med.unit,
                isTaken           = med.isTaken
            )
        }
    }

    override suspend fun lastVitalReadings(): List<Pair<String, Double>> {
        val patient = AppDependencies.prefs.getPatient() ?: return emptyList()
        val res = AppDependencies.fetchLastVital()(
            patient.uhId,
            patient.clientId.toString(),
            patient.pid.toString()
        )
        return if (res is DomainResult.Success) res.data.map { it.vitalName to it.vitalValue } else emptyList()
    }

    override suspend fun addVital(body: Map<String, Any>) =
        AppDependencies.addVital()(body) is DomainResult.Success

    override suspend fun insertSymptoms(body: Map<String, Any>) =
        AppDependencies.insertSymptoms()(body) is DomainResult.Success

    override suspend fun addIntake(body: Map<String, Any>) =
        AppDependencies.addIntake()(body) is DomainResult.Success

    override suspend fun addOutput(body: Map<String, Any>) =
        AppDependencies.addOutput()(body) is DomainResult.Success

    override suspend fun markMedicineTaken(scheduleId: Int, scheduledDateTime: String) =
        AppDependencies.markMedicineIntake()(scheduleId, scheduledDateTime) is DomainResult.Success
}