package com.critetiontech.ctvitalio.domain.usecase.profile

import com.critetiontech.ctvitalio.domain.repository.ProfileRepository
import com.critetiontech.ctvitalio.model.LocationItem

class FetchCountriesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(): List<LocationItem> = repository.getCountries()
}

class FetchStatesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(countryId: Int): List<LocationItem> = repository.getStates(countryId)
}

class FetchCitiesUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(stateId: Int): List<LocationItem> = repository.getCities(stateId)
}
