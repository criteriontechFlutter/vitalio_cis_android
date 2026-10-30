package com.critetiontech.ctvitalio.data.offline

import com.critetiontech.ctvitalio.model.IntakeItem
import com.critetiontech.ctvitalio.model.OutputItem
import com.critetiontech.ctvitalio.model.Vital
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Turns queued (not yet uploaded) logs into the same models the screens show, so a log made
 * offline appears right away — tagged as pending — instead of vanishing until it syncs.
 */
object PendingEntries {

    private val timeFormat = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    private fun Map<String, Any>.str(key: String): String = this[key]?.toString().orEmpty()
    private fun Map<String, Any>.int(key: String): Int = str(key).toDoubleOrNull()?.toInt() ?: 0

    /** Water/fluid logged for [date] ("yyyy-MM-dd"); [foodNames] maps foodID to its name. */
    fun intake(date: String, foodNames: Map<Int, String?>): List<IntakeItem> =
        OfflineOutbox.pending(OfflineOutbox.Kind.INTAKE).mapNotNull { entry ->
            val body = entry.body()
            // FoodDate is local time ("2026-10-30T11:05:07"); entries queued by older builds hold a UTC
            // instant ("…Z"). Fall back to when it was queued.
            val raw = body.str("FoodDate")
            val at = runCatching { LocalDateTime.parse(raw) }.getOrNull()
                ?: runCatching { Instant.parse(raw).atZone(ZoneId.systemDefault()).toLocalDateTime() }.getOrNull()
                ?: Instant.ofEpochMilli(entry.createdAt).atZone(ZoneId.systemDefault()).toLocalDateTime()
            if (at.toLocalDate().toString() != date) return@mapNotNull null
            val foodId = body.int("foodId")
            IntakeItem(
                foodId = foodId,
                foodName = foodNames[foodId],
                foodQuantity = body.int("FoodQuantity"),
                foodDate = at.toString(),
                intakeTimeFormat = at.format(timeFormat),
                isPending = true,
            )
        }

    /** Urine output logged for [date] ("yyyy-MM-dd"). */
    fun output(date: String): List<OutputItem> =
        OfflineOutbox.pending(OfflineOutbox.Kind.OUTPUT).mapNotNull { entry ->
            val body = entry.body()
            val at = runCatching {
                LocalDateTime.parse(body.str("outputDate"), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            }.getOrNull() ?: return@mapNotNull null
            if (at.toLocalDate().toString() != date) return@mapNotNull null
            OutputItem(
                quantity = body.int("quantity"),
                outputDate = at.toString(),
                outputDateFormat = at.toLocalDate().toString(),
                outputTimeFormat = at.format(timeFormat),
                colour = body.str("colour"),
                isPending = true,
            )
        }

    /** Request keys of the add-vital body → the vital names the server lists them under. */
    private val vitalKeys = linkedMapOf(
        "vmValueBPSys" to "BP_Sys", "vmValueBPDias" to "BP_Dias", "vmValueSPO2" to "SpO2",
        "vmValueRespiratoryRate" to "RespRate", "vmValueHeartRate" to "HeartRate", "vmValuePulse" to "Pulse",
        "vmValueRbs" to "RBS", "vmValueTemperature" to "Temperature", "vmValueWeight" to "Weight",
        "vmValueHeight" to "Height",
    )

    /** [latest] with any newer value logged offline laid over it (newest offline value wins). */
    fun overlayVitals(latest: List<Vital>): List<Vital> {
        val pending = OfflineOutbox.pending(OfflineOutbox.Kind.VITAL)
        if (pending.isEmpty()) return latest
        val result = latest.toMutableList()
        pending.sortedBy { it.createdAt }.forEach { entry ->
            val body = entry.body()
            val dateTime = "${body.str("vitalDate").ifBlank { LocalDate.now().toString() }}T${body.str("vitalTime").ifBlank { "00:00" }}:00"
            vitalKeys.forEach { (key, name) ->
                val value = body.str(key).toDoubleOrNull() ?: return@forEach
                val i = result.indexOfFirst { it.vitalName.equals(name, ignoreCase = true) }
                if (i >= 0) result[i] = result[i].copy(vitalValue = value, vitalDateTime = dateTime, displayValue = "", isPending = true)
                else result += Vital(vitalName = name, vitalValue = value, vitalDateTime = dateTime, isPending = true)
            }
        }
        return result
    }
}
