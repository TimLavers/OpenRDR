package io.rippledown.chat

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

interface SpeechModel : AutoCloseable
interface SpeechRecognizer : AutoCloseable {
    fun acceptWaveForm(data: ByteArray, len: Int): Boolean
    val result: String
    val partialResult: String
    val finalResult: String
}

class VoiceRecognitionService(
    private val modelPath: String,
    private val sampleRate: Float = 16000f,
    private val modelFactory: (String) -> SpeechModel = { path -> VoskModel(Model(path)) },
    private val recognizerFactory: (SpeechModel, Float) -> SpeechRecognizer = { model, rate ->
        VoskRecognizer(Recognizer((model as VoskModel).delegate, rate))
    },
    private val microphoneFactory: (AudioFormat) -> TargetDataLine = Companion::defaultOpenMicrophone
) : VoiceRecognition {
    private val _isListening = MutableStateFlow(false)
    override val isListening: StateFlow<Boolean> = _isListening

    private val _partialResult = MutableStateFlow("")
    override val partialResult: StateFlow<String> = _partialResult

    private var recognitionJob: Job? = null
    private var targetDataLine: TargetDataLine? = null
    private var model: SpeechModel? = null

    private fun ensureModel(): SpeechModel {
        if (model == null) {
            val modelDir = File(modelPath)
            require(modelDir.exists() && modelDir.isDirectory) {
                "Vosk model not found at: $modelPath. " +
                        "Download a model from https://alphacephei.com/vosk/models and extract it to this path."
            }
            model = modelFactory(modelPath)
        }
        return model!!
    }

    private fun openMicrophone(): TargetDataLine {
        val format = AudioFormat(sampleRate, 16, 1, true, false)
        return microphoneFactory(format)
    }

    override fun startListening(
        scope: CoroutineScope,
        onFinalResult: (String) -> Unit
    ) {
        if (_isListening.value) return

        recognitionJob = scope.launch(Dispatchers.IO) {
            var recognizer: SpeechRecognizer? = null
            try {
                val speechModel = try {
                    ensureModel()
                } catch (e: IllegalArgumentException) {
                    System.err.println(e.message)
                    return@launch
                }
                recognizer = recognizerFactory(speechModel, sampleRate)
                val line = openMicrophone()
                targetDataLine = line
                _isListening.value = true

                val buffer = ByteArray(4096)
                while (isActive && _isListening.value) {
                    val bytesRead = line.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        if (recognizer.acceptWaveForm(buffer, bytesRead)) {
                            val result = extractText(recognizer.result)
                            if (result.isNotBlank()) {
                                onFinalResult(result)
                            }
                            _partialResult.value = ""
                        } else {
                            val partial = extractText(recognizer.partialResult)
                            if (partial.isNotBlank()) {
                                _partialResult.value = partial
                            }
                        }
                    }
                }
            } finally {
                stopMicrophone()
                _isListening.value = false
                // Deliver any remaining recognized text
                val remaining = extractText(recognizer?.finalResult ?: "")
                if (remaining.isNotBlank()) {
                    onFinalResult(remaining)
                }
                _partialResult.value = ""
                recognizer?.close()
            }
        }
    }

    override fun resetAccumulatedText() {
        _partialResult.value = ""
    }

    override fun stopListening() {
        _isListening.value = false
        stopMicrophone()
        recognitionJob?.cancel()
        recognitionJob = null
    }

    private fun stopMicrophone() {
        targetDataLine?.let {
            it.stop()
            it.close()
        }
        targetDataLine = null
    }

    override fun close() {
        val job = recognitionJob
        stopListening()
        runBlocking { job?.join() }
        model?.close()
        model = null
    }

    companion object {
        private fun defaultOpenMicrophone(format: AudioFormat): TargetDataLine {
            val info = DataLine.Info(TargetDataLine::class.java, format)
            require(AudioSystem.isLineSupported(info)) {
                "Microphone not available or audio format not supported."
            }
            val line = AudioSystem.getLine(info) as TargetDataLine
            line.open(format)
            line.start()
            return line
        }

        internal fun extractText(json: String): String {
            // Vosk returns JSON like {"text" : "hello world"} or {"partial" : "hello"}
            val regex = """"(?:text|partial)"\s*:\s*"([^"]*)"""".toRegex()
            return regex.find(json)?.groupValues?.get(1)?.trim() ?: ""
        }

        fun defaultModelPath(): String {
            val resource = VoiceRecognitionService::class.java.getResource("/vosk-model")
                ?: error("Vosk model not found in resources. Ensure vosk-model is in ui/src/main/resources/")
            return if (resource.protocol == "file") {
                File(resource.toURI()).absolutePath
            } else {
                extractModelFromJar()
            }
        }

        private fun extractModelFromJar(): String {
            val tempDir = File(System.getProperty("java.io.tmpdir"), "vosk-model")
            if (tempDir.exists() && tempDir.isDirectory && tempDir.list()?.isNotEmpty() == true) {
                return tempDir.absolutePath
            }
            tempDir.mkdirs()
            val resourceUrl = VoiceRecognitionService::class.java.getResource("/vosk-model")!!
            val jarPath = resourceUrl.path.substringAfter("file:").substringBefore("!")
            val jarFile = java.util.jar.JarFile(File(jarPath))
            jarFile.use { jar ->
                jar.entries().asSequence()
                    .filter { it.name.startsWith("vosk-model/") }
                    .forEach { entry ->
                        val outFile = File(tempDir.parentFile, entry.name)
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile.mkdirs()
                            jar.getInputStream(entry).use { input ->
                                outFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    }
            }
            return tempDir.absolutePath
        }
    }
}

class VoskModel(val delegate: Model) : SpeechModel {
    override fun close() = delegate.close()
}

class VoskRecognizer(private val delegate: Recognizer) : SpeechRecognizer {
    override fun acceptWaveForm(data: ByteArray, len: Int): Boolean = delegate.acceptWaveForm(data, len)
    override val result: String get() = delegate.result
    override val partialResult: String get() = delegate.partialResult
    override val finalResult: String get() = delegate.finalResult
    override fun close() = delegate.close()
}
