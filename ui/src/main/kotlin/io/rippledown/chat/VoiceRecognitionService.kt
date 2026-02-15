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

class VoiceRecognitionService(
    private val modelPath: String,
    private val sampleRate: Float = 16000f,
    private val modelFactory: (String) -> Model = ::Model,
    private val recognizerFactory: (Model, Float) -> Recognizer = ::Recognizer,
    private val microphoneFactory: (AudioFormat) -> TargetDataLine = Companion::defaultOpenMicrophone
) {
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _partialResult = MutableStateFlow("")
    val partialResult: StateFlow<String> = _partialResult

    private var recognitionJob: Job? = null
    private var targetDataLine: TargetDataLine? = null
    private var model: Model? = null

    private fun ensureModel(): Model {
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

    fun startListening(
        scope: CoroutineScope,
        onFinalResult: (String) -> Unit
    ) {
        if (_isListening.value) return

        recognitionJob = scope.launch(Dispatchers.IO) {
            try {
                val voskModel = ensureModel()
                val recognizer = recognizerFactory(voskModel, sampleRate)
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
                                withContext(Dispatchers.Main) {
                                    onFinalResult(result)
                                }
                                _partialResult.value = ""
                            }
                        } else {
                            val partial = extractText(recognizer.partialResult)
                            _partialResult.value = partial
                        }
                    }
                }

                // Get any remaining result
                val finalResult = extractText(recognizer.finalResult)
                if (finalResult.isNotBlank()) {
                    withContext(Dispatchers.Main) {
                        onFinalResult(finalResult)
                    }
                }
                _partialResult.value = ""
                recognizer.close()
            } catch (e: Exception) {
                _partialResult.value = ""
                throw e
            } finally {
                stopMicrophone()
                _isListening.value = false
            }
        }
    }

    fun stopListening() {
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

    fun close() {
        stopListening()
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
            val userHome = System.getProperty("user.home")
            return "$userHome${File.separator}vosk-model"
        }
    }
}
