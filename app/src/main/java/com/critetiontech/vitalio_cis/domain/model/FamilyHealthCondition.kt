package com.critetiontech.ctvitalio.domain.model

/** Relations the backend knows, by its relation id. */
enum class FamilyRelation(val id: Int, val label: String) {
    MOTHER(2, "Mother"),
    FATHER(1, "Father"),
    BROTHER(4, "Brother"),
    SISTER(5, "Sister"),
    SPOUSE(3, "Spouse"),
    SON(6, "Son"),
    DAUGHTER(7, "Daughter"),
    OTHER(8, "Other");

    companion object {
        fun fromId(id: Int): FamilyRelation? = entries.firstOrNull { it.id == id }
    }
}

/** A disease someone in the patient's family has, with everyone who has it. */
data class FamilyHealthCondition(
    val diseaseId: Int,
    val diseaseName: String,
    val relations: List<FamilyRelation>,
    /**
     * Server record ids of every row behind this disease (one row per relation, plus repeats). All of them
     * must be deleted to remove the disease. Empty while the GET API doesn't return `id`.
     */
    val recordIds: List<Int>,
)

data class Disease(val id: Int, val name: String)
