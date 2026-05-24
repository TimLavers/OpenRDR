package io.rippledown.voice

import io.rippledown.llm.transcribeAudio
import io.rippledown.log.lazyLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.*

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

    private val _isTranscribing = MutableStateFlow(false)
    override val isTranscribing: StateFlow<Boolean> = _isTranscribing

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
                val available = listInputMixers()
                logger.warn(
                    "VoiceRecognitionService: captured PCM is entirely silent. " +
                            "Either microphone access is denied to this JVM, or the macOS / " +
                            "system default input device is not the one you are speaking into. " +
                            "On macOS, check System Settings \u2192 Privacy & Security \u2192 Microphone " +
                            "(grant OpenRDR / the launching app) and System Settings \u2192 Sound " +
                            "\u2192 Input (pick the actual mic, watch the level meter move). " +
                            "Available input devices visible to Java Sound: $available"
                )
            }
            val result = if (pcmBytes.isEmpty()) {
                ""
            } else {
                _isTranscribing.value = true
                try {
                    val wav = pcmToWav(pcmBytes, sampleRate)
                    try {
                        withContext(Dispatchers.IO) { transcribe(wav) }
                    } catch (e: Exception) {
                        logger.warn("VoiceRecognitionService: transcription failed: ${e.message}", e)
                        ""
                    }
                } finally {
                    _isTranscribing.value = false
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

        /**
         * Checks if a microphone is available for audio capture.
         * Returns true if at least one TargetDataLine supporting the format is available.
         */
        fun isMicrophoneAvailable(): Boolean {
            val format = pcmFormat(DEFAULT_SAMPLE_RATE)
            val info = DataLine.Info(TargetDataLine::class.java, format)
            return try {
                AudioSystem.getLine(info) != null
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        fun pcmFormat(sampleRate: Float): AudioFormat = AudioFormat(
            AudioFormat.Encoding.PCM_SIGNED,
            sampleRate,
            16,
            1,
            2,
            sampleRate,
            false
        )

        /**
         * Opens a [TargetDataLine] for capture at [format].
         *
         * If the system property `io.rippledown.voice.inputMixer` (or the
         * environment variable `OPENRDR_VOICE_INPUT_MIXER`) is set, the
         * first input mixer whose name contains that substring (case
         * insensitive) is preferred. Useful when the macOS default input
         * device is a virtual / silent device and you want to force a
         * specific physical mic without changing system Sound settings.
         *
         * Otherwise the platform default is used (current behaviour).
         */
        fun openDefaultMicrophone(format: AudioFormat): TargetDataLine {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            val preferred = System.getProperty("io.rippledown.voice.inputMixer")
                ?: System.getenv("OPENRDR_VOICE_INPUT_MIXER")
            if (!preferred.isNullOrBlank()) {
                inputMixerInfos(info)
                    .firstOrNull { it.name.contains(preferred, ignoreCase = true) }
                    ?.let { mixerInfo ->
                        return AudioSystem.getMixer(mixerInfo).getLine(info) as TargetDataLine
                    }
            }
            try {
                return AudioSystem.getLine(info) as TargetDataLine
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("No microphone available: ${e.message}", e)
            }
        }

        /**
         * Names of every [Mixer] visible to Java Sound that can supply a
         * [TargetDataLine] in the requested format. Surfaced in the silent-
         * capture warning so users / logs can see what alternatives exist.
         */
        fun listInputMixers(format: AudioFormat = pcmFormat(DEFAULT_SAMPLE_RATE)): List<String> {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            return inputMixerInfos(info).map { it.name }
        }

        private fun inputMixerInfos(info: DataLine.Info): List<Mixer.Info> =
            AudioSystem.getMixerInfo()
                .filter { mi ->
                    runCatching { AudioSystem.getMixer(mi).isLineSupported(info) }.getOrDefault(false)
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
