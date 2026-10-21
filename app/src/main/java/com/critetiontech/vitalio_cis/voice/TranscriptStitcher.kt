package com.critetiontech.ctvitalio.voice

/**
 * The dictation backend cuts its "final" segments on a timer, often mid-word, and strips their
 * whitespace: "I have fever, my heart rate" arrives as "I have f" + "ever, my he" + "art rate".
 * Joining them with spaces gives "f ever" / "he art", which nothing can parse.
 *
 * The running partial shows where the cut really was: just before a final arrives, the partial
 * holds that final's text followed by what came next. If the next character is a space the cut
 * was between words; otherwise the next segment continues the same word and must be glued on.
 */
object TranscriptStitcher {

    /**
     * True when the segment after [finalText] continues the same word (or punctuation run).
     * [recentPartials] are the latest raw (untrimmed) partials, newest first.
     */
    fun continuesWithoutSpace(finalText: String, recentPartials: List<String>): Boolean {
        val text = finalText.trim()
        if (text.isEmpty()) return false
        for (raw in recentPartials) {
            val partial = raw.trimStart()
            if (partial.length > text.length && partial.startsWith(text)) {
                return !partial[text.length].isWhitespace()
            }
        }
        return false   // no evidence → keep the word boundary
    }

    /** Joins (text, glueToNext) pieces, adding a space only where the cut was between words. */
    fun join(pieces: List<Pair<String, Boolean>>): String {
        val sb = StringBuilder()
        var glue = false
        for ((text, glueNext) in pieces) {
            val t = text.trim()
            if (t.isEmpty() || t == ".") continue
            if (sb.isNotEmpty() && !glue) sb.append(' ')
            sb.append(t)
            glue = glueNext
        }
        return sb.toString()
    }
}
