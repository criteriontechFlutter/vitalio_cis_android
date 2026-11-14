package com.critetiontech.ctvitalio.reminder

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.critetiontech.ctvitalio.R
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Local notification a few days before the subscription renews ("Your plan renews on 12 Dec 2026"),
 * so a renewal charge never surprises the patient. Rescheduled every time My Subscription loads,
 * replacing the previous one, so a changed renewal date moves it.
 */
object SubscriptionRenewalReminder {

    private const val TAG = "SubscriptionRenewal"
    private const val WORK = "subscription-renewal"
    private const val DAYS_BEFORE = 3L
    private val NOTIFY_AT: LocalTime = LocalTime.of(10, 0)
    private const val NOTIFICATION_ID = 0x5B5C

    const val KEY_PLAN = "plan"
    const val KEY_DATE = "date"

    /** The API sends "12 Dec 2026". */
    private val API_DATE = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun parseRenewalDate(raw: String?): LocalDate? =
        raw?.trim()?.takeIf { it.isNotEmpty() }?.let { runCatching { LocalDate.parse(it, API_DATE) }.getOrNull() }

    /** Schedules (or replaces) the reminder; cancels it when the plan isn't active or the date is unknown/past. */
    fun schedule(context: Context, planName: String?, renewalDate: String?, active: Boolean) {
        val work = WorkManager.getInstance(context)
        val date = parseRenewalDate(renewalDate)
        if (!active || date == null) {
            work.cancelUniqueWork(WORK)
            return
        }
        var at = date.minusDays(DAYS_BEFORE).atTime(NOTIFY_AT)
        val now = LocalDateTime.now()
        if (at.isBefore(now)) {
            // Inside the 3-day window already: remind once later today, unless the renewal day has passed
            if (date.isBefore(now.toLocalDate())) { work.cancelUniqueWork(WORK); return }
            at = now.plusHours(1)
        }
        val request = OneTimeWorkRequestBuilder<SubscriptionRenewalWorker>()
            .setInitialDelay(Duration.between(now, at).toMillis().coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(KEY_PLAN to planName.orEmpty(), KEY_DATE to renewalDate.orEmpty()))
            .build()
        work.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
        Log.d(TAG, "renewal reminder at $at")
    }

    fun notify(context: Context, plan: String, date: String) {
        val title = context.getString(R.string.sub_renewal_notif_title)
        val text = if (plan.isNotBlank()) context.getString(R.string.sub_renewal_notif_text_plan, plan, date)
        else context.getString(R.string.sub_renewal_notif_text, date)
        HealthNudges.notify(context, NOTIFICATION_ID, title, text)
    }
}

class SubscriptionRenewalWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Logged out since it was scheduled: the plan isn't this phone's business any more
        if (com.critetiontech.ctvitalio.utils.PrefsManager(applicationContext).getPatient() == null) return Result.success()
        val ctx =com.critetiontech.ctvitalio.utils.LocaleHelper.wrap(applicationContext)
        SubscriptionRenewalReminder.notify(
            ctx,
            inputData.getString(SubscriptionRenewalReminder.KEY_PLAN).orEmpty(),
            inputData.getString(SubscriptionRenewalReminder.KEY_DATE).orEmpty(),
        )
        return Result.success()
    }
}
