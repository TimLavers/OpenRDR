package io.rippledown.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface VoiceRecognition {
    val isListening: StateFlow<Boolean>

    /**
     * `true` after the user has stopped recording but the transcript has
     * not yet been delivered (i.e. while the Gemini call is in flight).
     * Distinct from [isListening] so the UI can show a "transcribing\u2026"
     * cue instead of looking unresponsive during the round-trip.
     */
    val isTranscribing: StateFlow<Boolean>
    val partialResult: StateFlow<String>
    fun startListening(scope: CoroutineScope, onFinalResult: (String) -> Unit)
    fun stopListening()
    fun resetAccumulatedText()
    fun close()
}
