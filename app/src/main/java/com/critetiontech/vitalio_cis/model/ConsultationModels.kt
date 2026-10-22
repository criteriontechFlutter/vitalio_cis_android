package com.critetiontech.ctvitalio.model

/** Where to connect for a video consultation: the LiveKit server and this patient's access token for the room. */
data class CallJoin(val serverUrl: String, val token: String)

/**
 * A doctor's video call as it arrives in a push (sent by api/consultation/call-patient), e.g.
 * {type=INCOMING_CALL, roomName=TESTING, callerId=1432, callerName=Dr.YASIR,
 *  title=Incoming Video Consultation, body=Dr.YASIR is calling you...}
 */
data class IncomingCall(
    val roomName: String,
    val doctorName: String,
    val doctorUserId: String? = null,
    val doctorAvatarUrl: String? = null,
    /** The id the doctor's side called this patient by; also sent back on decline/end. */
    val patientUserId: String? = null,
    /** Notification wording from the server ("Incoming Video Consultation", "Dr.YASIR is calling you..."). */
    val title: String? = null,
    val message: String? = null,
)

/** What a push means for video consultations. */
sealed interface CallPush {
    data class Incoming(val call: IncomingCall) : CallPush
    /** The doctor cancelled or the call ended elsewhere: stop ringing. */
    data class Ended(val roomName: String) : CallPush

    companion object {
        /**
         * Reads a push's data. Field names aren't fixed on the backend yet, so common spellings
         * are accepted (logcat tag "FCM" shows the real payload). Null when it isn't about a call.
         */
        fun parse(data: Map<String, String>): CallPush? {
            fun get(vararg keys: String): String? =
                keys.firstNotNullOfOrNull { k -> data.entries.firstOrNull { it.key.equals(k, ignoreCase = true) }?.value?.takeIf { it.isNotBlank() } }

            val room = get("roomName", "room", "room_name", "callRoom") ?: return null
            val type = get("type", "callType", "event", "action", "notificationType", "status")?.lowercase().orEmpty()

            if (listOf("end", "cancel", "decline", "missed", "hangup").any { it in type }) return Ended(room)
            // A room alone, or a type that sounds like a call, is an incoming call
            if (type.isNotEmpty() && listOf("call", "consult", "video", "incoming", "ring").none { it in type }) return null

            return Incoming(
                IncomingCall(
                    roomName = room,
                    doctorName = get("callerName", "doctorName", "fromName", "name") ?: "Your doctor",
                    doctorUserId = get("doctorUserId", "doctorId", "callerId", "fromUserId"),
                    doctorAvatarUrl = get("doctorAvatarUrl", "avatarUrl", "doctorImage", "callerAvatar", "imageUrl", "profileUrl"),
                    patientUserId = get("patientUserId", "patientId", "userId"),
                    title = get("title"),
                    message = get("body", "message"),
                )
            )
        }
    }
}
