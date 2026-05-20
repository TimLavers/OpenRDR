package io.rippledown.voice

import io.rippledown.llm.transcribeAudio
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Captures microphone audio while the user is "listening" and, on stop,
 * sends the captured PCM as a WAV blob to Gemini for transcription.
 * The result (or empty string on failure / silence) is delivered to the
 * caller via the onFinalResult callback supplied to startListening().
 *
 * Unlike the original Vosk-based implementation, Gemini does not stream
 * partial results back as the user speaks. partialResult is therefore
 * always empty; user feedback during recording comes from isListening
 * driving the mic icon in VoiceInputButton.
 *
 * Microphone capture and the transcription call are parameterised so
 * tests can drive the service without a real audio device or a live
 * network call.
 */
class VoiceRecognitionService(
    private val sampleRate: Float = DEFAULT_SAMPLE_RATE,
    private val microphoneFactory: (AudioFormat) -> TargetDataLine = ::openDefaultMicrophone,
    private val transcribe: (ByteArray) -> String = { transcribeAudio(it) }
) : VoiceRecognition {

    private val logger = lazyLogger

    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult

    private var captureJob: Job? = null
    private var line: TargetDataLine? = null
    private val pcmBuffer = ByteArrayOutputStream()
    private var closed = false

    override fun startListening(scope: CoroutineScope, onFinalResult: (String) -> Unit) {
        check(!closed) { "VoiceRecognitionService is closed" }
        if (_isListening.value) return

        val format = pcmFormat(sampleRate)
        val openedLine = try {
            microphoneFactory(format).also { it.open(format); it.start() }
        } catch (e: Exception) {
            logger.warn("VoiceRecognitionService: failed to open microphone: ${e.message}", e)
            return
        }
        logger.info("VoiceRecognitionService: started listening (sampleRate=${sampleRate.toInt()} Hz)")
        line = openedLine
        synchronized(pcmBuffer) { pcmBuffer.reset() }
        _isListening.value = true

        captureJob = scope.launch(Dispatchers.IO) {
            val frame = ByteArray(CAPTURE_FRAME_BYTES)
            try {
                while (isActive && _isListening.value) {
                    val read = openedLine.read(frame, 0, frame.size)
                    if (read > 0) {
                        synchronized(pcmBuffer) { pcmBuffer.write(frame, 0, read) }
                    }
                }
            } catch (_: Exception) {
                // Best-effort; fall through to transcription with whatever we have.
            } finally {
                runCatching { openedLine.stop() }
                runCatching { openedLine.close() }
            }

            val pcmBytes = synchronized(pcmBuffer) { pcmBuffer.toByteArray() }
            val nonZeroSamples = pcmBytes.count { it != 0.toByte() }
            logger.info("VoiceRecognitionService: stopped listening, captured ${pcmBytes.size} bytes (${nonZeroSamples} non-zero)")
            if (pcmBytes.isNotEmpty() && nonZeroSamples == 0) {
                logger.warn(
                    "VoiceRecognitionService: captured PCM is entirely silent. " +
                            "On macOS this typically means TCC denied microphone access " +
                            "to this JVM (no Info.plist / no NSMicrophoneUsageDescription). " +
                            "Grant the parent process (e.g. Terminal, IntelliJ) microphone " +
                            "access in System Settings \u2192 Privacy & Security \u2192 Microphone, " +
                            "then retry."
                )
            }
            val result = if (pcmBytes.isEmpty()) {
                ""
            } else {
                val wav = pcmToWav(pcmBytes, sampleRate)
                try {
                    withContext(Dispatchers.IO) { transcribe(wav) }
                } catch (e: Exception) {
                    logger.warn("VoiceRecognitionService: transcription failed: ${e.message}", e)
                    ""
                }
            }
            if (result.isNotBlank()) {
                logger.info("VoiceRecognitionService: transcription delivered (${result.length} chars)")
                onFinalResult(result.trim())
            } else if (pcmBytes.isNotEmpty()) {
                logger.info("VoiceRecognitionService: transcription returned empty string; nothing delivered")
            }
            _partialResult.value = ""
            _isListening.value = false
        }
    }

    override fun stopListening() {
        if (!_isListening.value) return
        _isListening.value = false
        runCatching { line?.stop() }
    }

    override fun resetAccumulatedText() {
        _partialResult.value = ""
    }

    override fun close() {
        if (closed) return
        closed = true
        _isListening.value = false
        captureJob?.cancel()
        captureJob = null
        runCatching { line?.stop() }
        runCatching { line?.close() }
        line = null
    }

    companion object {
        const val DEFAULT_SAMPLE_RATE = 16_000f
        private const val CAPTURE_FRAME_BYTES = 4_096

        fun pcmFormat(sampleRate: Float): AudioFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            false
        )

        fun openDefaultMicrophone(format: AudioFormat): TargetDataLine {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            return AudioSystem.getLine(info) as TargetDataLine
        }

        fun pcmToWav(pcm: ByteArray, sampleRate: Float): ByteArray {
            val channels = 1
            val bitsPerSample = 16
            val byteRate = (sampleRate.toInt() * channels * bitsPerSample) / 8
            val blockAlign = (channels * bitsPerSample) / 8
            val dataSize = pcm.size
            val chunkSize = 36 + dataSize

            val buffer = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
            buffer.put("RIFF".toByteArray(Charsets.US_ASCII))
            buffer.putInt(chunkSize)
            buffer.put("WAVE".toByteArray(Charsets.US_ASCII))
            buffer.put("fmt ".toByteArray(Charsets.US_ASCII))
            buffer.putInt(16)
            buffer.putShort(1)
            buffer.putShort(channels.toShort())
            buffer.putInt(sampleRate.toInt())
            buffer.putInt(byteRate)
            buffer.putShort(blockAlign.toShort())
            buffer.putShort(bitsPerSample.toShort())
            buffer.put("data".toByteArray(Charsets.US_ASCII))
            buffer.putInt(dataSize)
            buffer.put(pcm)
            return buffer.array()
        }
    }
}
