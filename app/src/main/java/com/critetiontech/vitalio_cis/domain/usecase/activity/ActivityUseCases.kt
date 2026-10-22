package com.critetiontech.ctvitalio.domain.usecase.activity

import com.critetiontech.ctvitalio.domain.model.DomainResult
import com.critetiontech.ctvitalio.domain.repository.ActivityRepository
import com.critetiontech.ctvitalio.model.ActivityModel
import com.critetiontech.ctvitalio.model.ActivitySaveResult
import com.critetiontech.ctvitalio.model.AddPatientActivityRequest

class FetchActivityMasterUseCase(private val repository: ActivityRepository) {
    suspend operator fun invoke(): DomainResult<List<ActivityModel>> = repository.fetchActivityMaster()
}

class AddPatientActivityUseCase(private val repository: ActivityRepository) {
    suspend operator fun invoke(request: AddPatientActivityRequest): DomainResult<ActivitySaveResult> =
        repository.addPatientActivity(request)
}
