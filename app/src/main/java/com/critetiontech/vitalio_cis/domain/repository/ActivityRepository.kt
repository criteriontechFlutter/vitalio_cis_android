package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest
import com.critetiontech.ctvitalio.model.PatientActivityItem

interface ActivityRepository {
    suspend fun fetchActivityMaster(): DomainResult<List<ActivityModel>>
    suspend fun addPatientActivity(request: AddPatientActivityRequest): DomainResult<ActivitySaveResult>
    /** Activities the patient logged on [date] (yyyy-MM-dd). */
    suspend fun fetchPatientActivity(uhid: String, date: String, clientId: Int): DomainResult<List<PatientActivityItem>>
    /** Deletes one logged activity (the row id from [fetchPatientActivity]). */
    suspend fun removePatientActivity(id: Int, clientId: Int, userId: Int): DomainResult<Unit>
}
