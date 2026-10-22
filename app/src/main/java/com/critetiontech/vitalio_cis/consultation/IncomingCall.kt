package com.critetiontech.ctvitalio.consultation

import android.content.Intent
import android.os.Bundle
import com.critetiontech.ctvitalio.model.CallPush
import com.critetiontech.ctvitalio.model.IncomingCall

// Android glue for [IncomingCall]: carrying it in intents and reading it from a tapped push.

private const val NOTIFICATION_BASE = 0x7C000
private const val EXTRA_ROOM = "call_room"
private const val EXTRA_DOCTOR = "call_doctor"
private const val EXTRA_DOCTOR_ID = "call_doctor_id"
private const val EXTRA_AVATAR = "call_avatar"
private const val EXTRA_PATIENT_ID = "call_patient_id"
private const val EXTRA_TITLE = "call_title"
private const val EXTRA_MESSAGE = "call_message"

/** Stable per room, so the ringing notification can be replaced or cancelled. */
val IncomingCall.notificationId: Int get() = notificationIdFor(roomName)

fun notificationIdFor(roomName: String): Int = NOTIFICATION_BASE + (roomName.hashCode() and 0xFFFF)

fun IncomingCall.putInto(intent: Intent): Intent = intent
    .putExtra(EXTRA_ROOM, roomName)
    .putExtra(EXTRA_DOCTOR, doctorName)
    .putExtra(EXTRA_DOCTOR_ID, doctorUserId)
    .putExtra(EXTRA_AVATAR, doctorAvatarUrl)
    .putExtra(EXTRA_PATIENT_ID, patientUserId)
    .putExtra(EXTRA_TITLE, title)
    .putExtra(EXTRA_MESSAGE, message)

fun Intent?.incomingCall(): IncomingCall? {
    val room = this?.getStringExtra(EXTRA_ROOM)?.takeIf { it.isNotBlank() } ?: return null
    return IncomingCall(
        roomName = room,
        doctorName = getStringExtra(EXTRA_DOCTOR).orEmpty(),
        doctorUserId = getStringExtra(EXTRA_DOCTOR_ID),
        doctorAvatarUrl = getStringExtra(EXTRA_AVATAR),
        patientUserId = getStringExtra(EXTRA_PATIENT_ID),
        title = getStringExtra(EXTRA_TITLE),
        message = getStringExtra(EXTRA_MESSAGE),
    )
}

/** A push Firebase showed itself brings its data as launch-intent extras. */
fun Bundle.callPush(): CallPush? =
    CallPush.parse(keySet().mapNotNull { k -> getString(k)?.let { k to it } }.toMap())
