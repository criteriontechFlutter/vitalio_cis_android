package com.critetiontech.ctvitalio.voice

/** Voice service URLs, here so the phone and the watch share them. The app's ApiConfig points at these. */
object VoiceEndpoints {
    const val ECHO_AI = "http://food.shopright.ai:3478/api/echo/"
    const val DICTATION_WS = "ws://food.shopright.ai:3479/ws"
}
