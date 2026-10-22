package com.critetiontech.ctvitalio.consultation

import android.content.Context
import io.livekit.android.room.track.VideoPreset169
import io.livekit.android.room.track.VideoQuality

/**
 * Video quality the patient picks for calls: what their camera sends ([sendPreset]) and which
 * layer of the doctor's video they receive ([receive]). Remembered between calls.
 */
enum class CallQuality(
    val label: String,
    val description: String,
    val sendPreset: VideoPreset169,
    /** null: follow the window size (full screen high, PiP low); the server still adapts to bandwidth. */
    val receive: VideoQuality?,
) {
    AUTO("Auto", "Adjusts to your connection", VideoPreset169.H540, null),
    DATA_SAVER("Data saver", "Lower quality, uses less data", VideoPreset169.H360, VideoQuality.LOW),
    HD("HD", "Best picture, for Wi-Fi", VideoPreset169.H720, VideoQuality.HIGH);

    fun receiveQuality(inPip: Boolean): VideoQuality = receive ?: if (inPip) VideoQuality.LOW else VideoQuality.HIGH

    companion object {
        private const val PREFS = "video_call"
        private const val KEY = "quality"

        fun saved(context: Context): CallQuality =
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)
                ?.let { runCatching { valueOf(it) }.getOrNull() } ?: AUTO

        fun save(context: Context, quality: CallQuality) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, quality.name).apply()
        }
    }
}
