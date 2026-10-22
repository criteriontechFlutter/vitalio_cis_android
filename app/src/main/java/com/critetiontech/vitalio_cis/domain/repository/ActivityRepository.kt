package com.critetiontech.ctvitalio.domain.repository

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest

interface ActivityRepository {
    suspend fun fetchActivityMaster(): DomainResult<List<ActivityModel>>
    suspend fun addPatientActivity(request: AddPatientActivityRequest): DomainResult<ActivitySaveResult>
}
