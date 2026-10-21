package com.critetiontech.ctvitalio.reminder

import android.content.Context
import android.content.Intent
import androidx.core.content.edit
import com.critetiontech.ctvitalio.model.MedicineItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime
import java.time.ZoneId

/** The server sends this as takenDateTime for doses that were never taken. */
private const val NEVER_TAKEN = "1900-01-01T00:00:00"

/** Same rule the dashboard uses: `isTaken` alone isn't reliable. */
fun MedicineItem.isActuallyTaken(): Boolean =
    isTaken && !takenDateTime.isNullOrBlank() && takenDateTime != NEVER_TAKEN

/**
 * Shows doses logged from an alarm as taken while their upload is still pending,
 * so screens don't offer to log them a second time.
 */
fun List<MedicineItem>.withLocallyTaken(context: Context): List<MedicineItem> {
    val local = ReminderSettings(context).takenLocally
    if (local.isEmpty()) return this
    val now = LocalDateTime.now().withNano(0).toString()
    return map { item ->
        if (!item.isActuallyTaken() && "${item.id}|${item.scheduledDateTime}" in local)
            item.copy(isTaken = true, takenDateTime = now)
        else item
    }
}

/** One scheduled dose from the server's daily intake list, as the alarm system sees it. */
data class ReminderDose(
    val intakeId: Int,
    val medicineName: String,
    val dose: String,
    val instructions: String,
    val scheduledDateTime: String,
) {
    val key: String get() = "$intakeId|$scheduledDateTime"

    val isTest: Boolean get() = intakeId == TEST_INTAKE_ID

    fun scheduledAt(): LocalDateTime? =
        try { LocalDateTime.parse(scheduledDateTime) } catch (e: Exception) { null }

    fun scheduledMillis(): Long? =
        scheduledAt()?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    companion object {
        const val TEST_INTAKE_ID = -1

        fun from(item: MedicineItem): ReminderDose {
            val strength = item.dosageStrength.let {
                if (it % 1.0 == 0.0) it.toLong().toString() else it.toString()
            }
            return ReminderDose(
                intakeId = item.id,
                medicineName = item.medicineName.trim(),
                dose = listOf("$strength ${item.unit.orEmpty()}".trim(), item.dosageType.orEmpty())
                    .filter { it.isNotBlank() && it != "0" }
                    .joinToString(" · "),
                instructions = item.instructions.orEmpty(),
                scheduledDateTime = item.scheduledDateTime,
            )
        }
    }
}

/**
 * Every dose that rings at the same moment shares one alarm. Android throttles
 * separate alarms in Doze, and one popup listing all of them is easier on the patient.
 */
data class ReminderAlarm(
    val alertAt: Long,
    val doses: List<ReminderDose>,
) {
    /** Stable id for this alarm's PendingIntents and notification. */
    val requestCode: Int get() = "alarm|$alertAt".hashCode()

    val isTest: Boolean get() = doses.all { it.isTest }

    fun putInto(intent: Intent): Intent = intent.putExtra(EXTRA_ALARM, gson.toJson(this))

    companion object {
        private const val EXTRA_ALARM = "reminder_alarm"
        private val gson = Gson()

        fun from(intent: Intent?): ReminderAlarm? =
            intent?.getStringExtra(EXTRA_ALARM)?.let {
                try { gson.fromJson(it, ReminderAlarm::class.java) } catch (e: Exception) { null }
            }?.takeIf { it.doses.isNotEmpty() }
    }
}

/** User preferences for medicine alarms, plus the dose list the alarms were built from. */
class ReminderSettings(context: Context) {

    private val sp = context.applicationContext.getSharedPreferences("medicine_reminders", Context.MODE_PRIVATE)
    private val gson = Gson()

    var enabled: Boolean
        get() = sp.getBoolean(KEY_ENABLED, true)
        set(value) = sp.edit { putBoolean(KEY_ENABLED, value) }

    /** How many minutes before the dose time the alarm rings; 0 = on time. */
    var offsetMinutes: Int
        get() = sp.getInt(KEY_OFFSET, 0)
        set(value) = sp.edit { putInt(KEY_OFFSET, value) }

    var soundEnabled: Boolean
        get() = sp.getBoolean(KEY_SOUND, true)
        set(value) = sp.edit { putBoolean(KEY_SOUND, value) }

    var snoozeMinutes: Int
        get() = sp.getInt(KEY_SNOOZE, 10)
        set(value) = sp.edit { putInt(KEY_SNOOZE, value) }

    /** Whether the user already saw the one-time "set up alarms" prompt. */
    var setupPromptShown: Boolean
        get() = sp.getBoolean(KEY_SETUP_PROMPT, false)
        set(value) = sp.edit { putBoolean(KEY_SETUP_PROMPT, value) }

    /** Medicines (lower-cased names) the user switched alarms off for. */
    var mutedMedicines: Set<String>
        get() = sp.getStringSet(KEY_MUTED, emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet(KEY_MUTED, value) }

    fun isMuted(medicineName: String) = medicineName.trim().lowercase() in mutedMedicines

    fun setMuted(medicineName: String, muted: Boolean) {
        val name = medicineName.trim().lowercase()
        mutedMedicines = if (muted) mutedMedicines + name else mutedMedicines - name
    }

    /** Upcoming, not-yet-taken doses last fetched from the server. */
    var doses: List<ReminderDose>
        get() = sp.getString(KEY_DOSES, null)?.let {
            try { gson.fromJson<List<ReminderDose>>(it, object : TypeToken<List<ReminderDose>>() {}.type) }
            catch (e: Exception) { null }
        }.orEmpty()
        set(value) = sp.edit { putString(KEY_DOSES, gson.toJson(value)) }

    /** Keys of doses logged as taken from an alarm, kept until the server confirms them. */
    var takenLocally: Set<String>
        get() = sp.getStringSet(KEY_TAKEN_LOCALLY, emptySet()).orEmpty()
        set(value) = sp.edit { putStringSet(KEY_TAKEN_LOCALLY, value) }

    /**
     * Returns false if an alarm for this moment already rang recently (the inexact fallback
     * registers two copies of each alarm), otherwise records it and returns true.
     */
    fun claimFire(alertAt: Long): Boolean {
        val now = System.currentTimeMillis()
        val recent = sp.getStringSet(KEY_FIRED, emptySet()).orEmpty()
            .mapNotNull { it.split('@').takeIf { p -> p.size == 2 } }
            .filter { (_, firedAt) -> now - (firedAt.toLongOrNull() ?: 0L) < 2 * 60 * 60_000L }
        if (recent.any { (at, _) -> at == alertAt.toString() }) return false
        sp.edit { putStringSet(KEY_FIRED, (recent.map { "${it[0]}@${it[1]}" } + "$alertAt@$now").toSet()) }
        return true
    }

    /** Request codes of the alarms currently registered with AlarmManager. */
    var scheduledCodes: Set<Int>
        get() = sp.getStringSet(KEY_CODES, emptySet()).orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
        set(value) = sp.edit { putStringSet(KEY_CODES, value.map { it.toString() }.toSet()) }

    private companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_OFFSET = "offset_minutes"
        const val KEY_SOUND = "sound_enabled"
        const val KEY_SNOOZE = "snooze_minutes"
        const val KEY_SETUP_PROMPT = "setup_prompt_shown"
        const val KEY_MUTED = "muted_medicines"
        const val KEY_DOSES = "doses"
        const val KEY_TAKEN_LOCALLY = "taken_locally"
        const val KEY_CODES = "scheduled_codes"
        const val KEY_FIRED = "recently_fired"
    }
}
