package com.critetiontech.ctvitalio.model

/**
 * One row of api/PatientFamilyHealthHistory/GetPatientFamilyHealthHistory: the server sends one row per
 * disease + relation. [id] is the row's record id that DeletePatientFamilyHealthHistory takes; the
 * backend is adding it to the GET response, and delete works as soon as it's there (until then the
 * screen says the entry can't be removed). [diseaseName] isn't sent today.
 */
data class FamilyHealthHistoryRow(
    @com.google.gson.annotations.SerializedName(
        value = "id",
        alternate = ["familyHealthHistoryId", "patientFamilyHealthHistoryId", "familyHistoryId", "historyId"]
    )
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
