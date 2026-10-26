package com.critetiontech.ctvitalio.model

/**
 * One row of api/PatientFamilyHealthHistory/GetPatientFamilyHealthHistory: the server sends one row per
 * disease + relation. [id] and [diseaseName] aren't sent today; they're read in case the backend adds them.
 */
data class FamilyHealthHistoryRow(
    val id: Int? = null,
    val pid: Int = 0,
    val diseaseId: Int = 0,
    val diseaseName: String? = null,
    val relationId: Int = 0,
    val relationName: String? = null,
)

/** One entry of api/KnowMedApis/getAllDiseaseByAlphabet. */
data class DiseaseItem(
    val id: Int = 0,
    val problemName: String? = null,
)
