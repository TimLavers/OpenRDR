package io.rippledown.integration

import io.rippledown.voice.VoiceRecognition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.awaitility.Awaitility.await
import java.time.Duration
import java.util.concurrent.atomic.AtomicReference

/**
 * In-process VoiceRecognition stand-in used by cucumber scenarios that
 * exercise the chat panel's voice-input wiring without touching a real
 * microphone or the Gemini API. The real flow is:
 *
 *   1. user clicks the mic button -> VoiceInputButton calls
 *      voiceRecognitionService.startListening(scope, onFinalResult)
 *   2. user speaks -> service captures audio
 *   3. user clicks the mic button again -> stopListening
 *   4. service transcribes audio and invokes onFinalResult(text)
 *
 * This fake collapses steps 2-4 into a single [simulateUtterance] call.
 * The captured `onFinalResult` is invoked synchronously, which is what the
 * UI sees from a real [VoiceRecognitionService] when transcription
 * completes. State writes from any thread are safe under Compose's
 * snapshot model.
 */
class FakeVoiceRecognition : VoiceRecognition {

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening

    private val _isTranscribing = MutableStateFlow(false)
    override val isTranscribing: StateFlow<Boolean> = _isTranscribing

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult

    private val capturedCallback = AtomicReference<((String) -> Unit)?>(null)

    override fun startListening(scope: CoroutineScope, onFinalResult: (String) -> Unit) {
        capturedCallback.set(onFinalResult)
        _isListening.value = true
    }

    override fun stopListening() {
        _isListening.value = false
    }

    override fun resetAccumulatedText() {
        _partialResult.value = ""
    }

    override fun close() {
        _isListening.value = false
        capturedCallback.set(null)
    }

    /**
     * Simulate the user having spoken [text]. Waits up to 5 seconds for
     * the UI to have called [startListening] (i.e. the mic button has
     * been clicked) and the resulting onFinalResult callback to be in
     * place, then invokes it.
     */
    fun simulateUtterance(text: String) {
        await().atMost(Duration.ofSeconds(5)).until { capturedCallback.get() != null }
        capturedCallback.get()!!.invoke(text)
    }
}
