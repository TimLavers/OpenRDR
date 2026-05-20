package io.rippledown.voice

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

interface VoiceRecognition {
    val isListening: StateFlow<Boolean>
    val partialResult: StateFlow<String>
    fun startListening(scope: CoroutineScope, onFinalResult: (String) -> Unit)
    fun stopListening()
    fun resetAccumulatedText()
    fun close()
}
