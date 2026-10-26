package com.critetiontech.ctvitalio.domain.model

import com.critetiontech.ctvitalio.model.SymptomItem
import java.time.LocalDate

/** Symptoms recorded on one day; [date] is null for rows whose date couldn't be read. */
data class SymptomHistoryDay(
    val date: LocalDate?,
    val symptoms: List<SymptomItem>,
)
