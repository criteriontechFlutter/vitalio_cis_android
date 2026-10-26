package com.critetiontech.ctvitalio.domain.model

import com.critetiontech.ctvitalio.model.SymptomItem

/**
 * One "Do you still have …?" question. The server keeps a row per visit ([records]), and the answer
 * has to be saved against every one of them or the older visits keep the symptom open.
 */
data class SymptomToReview(
    val detailId: Int,
    val details: String,
    val records: List<SymptomItem>,
)
