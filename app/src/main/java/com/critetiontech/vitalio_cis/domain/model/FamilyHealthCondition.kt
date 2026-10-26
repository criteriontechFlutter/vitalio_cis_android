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
    /** Server record id, needed to remove the entry; null while the GET API doesn't return it. */
    val recordId: Int?,
)

data class Disease(val id: Int, val name: String)
