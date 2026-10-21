package com.critetiontech.ctvitalio.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.critetiontech.ctvitalio.MainActivity
import com.critetiontech.ctvitalio.R
import com.critetiontech.ctvitalio.di.AppDependencies
import com.critetiontech.ctvitalio.domain.model.DomainResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

/** Tells open screens that a dose was logged somewhere else (alarm, notification, worker). */
object MedicineIntakeEvents {
    private val _changed = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    val changed: SharedFlow<Unit> = _changed.asSharedFlow()

    fun notifyChanged() { _changed.tryEmit(Unit) }

    /** Request codes of alarms answered from the notification, so an open alarm screen closes. */
    private val _handled = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    val handled: SharedFlow<Int> = _handled.asSharedFlow()

    fun alarmHandled(requestCode: Int) { _handled.tryEmit(requestCode) }
}

/**
 * Logs doses as taken from outside the dashboard.
 *
 * It calls the same API as the dashboard, then re-reads that day's schedule to confirm the
 * server really recorded it. Anything that doesn't go through (offline, server error, not
 * confirmed) is handed to [MarkTakenWorker], which retries with backoff once there's network.
 */
object IntakeRecorder {

    enum class Outcome { Recorded, Queued }

    private const val TAG = "IntakeRecorder"
    private const val CALL_TIMEOUT_MS = 15_000L

    suspend fun markTaken(context: Context, dose: ReminderDose): Outcome {
        if (dose.isTest) return Outcome.Recorded
        val app = context.applicationContext
        rememberTaken(app, dose)

        val confirmed = withTimeoutOrNull(CALL_TIMEOUT_MS) { sendAndVerify(dose) } == true
        return if (confirmed) {
            onRecorded(app, dose)
            Outcome.Recorded
        } else {
            Log.w(TAG, "markTaken: ${dose.key} not confirmed, queuing retry")
            enqueueRetry(app, dose)
            MedicineIntakeEvents.notifyChanged()
            Outcome.Queued
        }
    }

    /** Sends the intake log and checks the server now reports the dose as taken. */
    internal suspend fun sendAndVerify(dose: ReminderDose): Boolean {
        val sent = AppDependencies.markMedicineIntake()(dose.intakeId, dose.scheduledDateTime)
        if (sent !is DomainResult.Success) {
            Log.e(TAG, "markIntake failed for ${dose.key}: $sent")
            return false
        }
        val patient = AppDependencies.prefs.getPatient() ?: return true
        val date = dose.scheduledAt()?.toLocalDate()?.toString() ?: return true
        val check = AppDependencies.fetchMedicineIntake()(patient.pid, date, patient.clientId)
        if (check !is DomainResult.Success) return true // can't verify; the API accepted it
        val item = check.data.flatMap { it.medicineData.orEmpty() }.firstOrNull { it.id == dose.intakeId }
            ?: return true
        val taken = item.isActuallyTaken()
        if (!taken) Log.e(TAG, "server accepted ${dose.key} but still reports it as not taken")
        return taken
    }

    internal fun onRecorded(context: Context, dose: ReminderDose) {
        val settings = ReminderSettings(context)
        settings.takenLocally = settings.takenLocally - dose.key
        settings.doses = settings.doses.filterNot { it.key == dose.key }
        MedicineIntakeEvents.notifyChanged()
    }

    /** Keeps the dose from ringing again while the server catches up. */
    private fun rememberTaken(context: Context, dose: ReminderDose) {
        val settings = ReminderSettings(context)
        settings.takenLocally = settings.takenLocally + dose.key
        settings.doses = settings.doses.filterNot { it.key == dose.key }
    }

    private fun enqueueRetry(context: Context, dose: ReminderDose) {
        val request = OneTimeWorkRequestBuilder<MarkTakenWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .setInputData(workDataOf(
                MarkTakenWorker.KEY_ID to dose.intakeId,
                MarkTakenWorker.KEY_NAME to dose.medicineName,
                MarkTakenWorker.KEY_SCHEDULED to dose.scheduledDateTime,
            ))
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork("mark-taken-${dose.key}", ExistingWorkPolicy.KEEP, request)
    }

    internal fun notifyFailure(context: Context, medicineName: String) {
        MedicineAlarmNotifier.ensureChannels(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, MedicineAlarmNotifier.CHANNEL_STATUS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Couldn't log $medicineName")
            .setContentText("Open the app and mark it as taken from Today's Medications.")
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify("intake-failed|$medicineName".hashCode(), notification)
    }
}

/** Retries an intake log that couldn't be confirmed when the user tapped "taken". */
class MarkTakenWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val dose = ReminderDose(
            intakeId = inputData.getInt(KEY_ID, 0),
            medicineName = inputData.getString(KEY_NAME).orEmpty(),
            dose = "",
            instructions = "",
            scheduledDateTime = inputData.getString(KEY_SCHEDULED) ?: return Result.failure(),
        )
        if (IntakeRecorder.sendAndVerify(dose)) {
            IntakeRecorder.onRecorded(applicationContext, dose)
            return Result.success()
        }
        if (runAttemptCount + 1 < MAX_ATTEMPTS) return Result.retry()

        // Give up: let the dose show as pending again so the user can log it by hand.
        val settings = ReminderSettings(applicationContext)
        settings.takenLocally = settings.takenLocally - dose.key
        IntakeRecorder.notifyFailure(applicationContext, dose.medicineName)
        MedicineReminderScheduler.requestSync(applicationContext)
        MedicineIntakeEvents.notifyChanged()
        return Result.failure()
    }

    companion object {
        const val KEY_ID = "intake_id"
        const val KEY_NAME = "medicine_name"
        const val KEY_SCHEDULED = "scheduled"
        private const val MAX_ATTEMPTS = 6
    }
}
