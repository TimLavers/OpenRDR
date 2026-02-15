package io.rippledown.chat

import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldBeEmpty
import io.kotest.matchers.string.shouldContain
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.vosk.Model
import org.vosk.Recognizer
import javax.sound.sampled.TargetDataLine

@OptIn(ExperimentalCoroutinesApi::class)
class VoiceRecognitionServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var mockModel: Model
    private lateinit var mockRecognizer: Recognizer
    private lateinit var mockTargetDataLine: TargetDataLine
    private lateinit var modelDir: java.io.File

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        mockModel = mockk(relaxed = true)
        mockRecognizer = mockk(relaxed = true)
        mockTargetDataLine = mockk(relaxed = true)
        modelDir = tempFolder.newFolder("vosk-model")
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    private fun createService(
        modelPath: String = modelDir.absolutePath,
        sampleRate: Float = 16000f
    ): VoiceRecognitionService {
        return VoiceRecognitionService(
            modelPath = modelPath,
            sampleRate = sampleRate,
            modelFactory = { mockModel },
            recognizerFactory = { _, _ -> mockRecognizer },
            microphoneFactory = { mockTargetDataLine }
        )
    }

    // --- Initial state ---

    @Test
    fun `should not be listening initially`() {
        // Given
        val service = createService()

        // Then
        service.isListening.value.shouldBeFalse()
    }

    @Test
    fun `should have empty partial result initially`() {
        // Given
        val service = createService()

        // Then
        service.partialResult.value.shouldBeEmpty()
    }

    // --- extractText ---

    @Test
    fun `should extract text from Vosk text result`() {
        // Given
        val json = """{"text" : "hello world"}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result shouldBe "hello world"
    }

    @Test
    fun `should extract text from Vosk partial result`() {
        // Given
        val json = """{"partial" : "hello"}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result shouldBe "hello"
    }

    @Test
    fun `should return empty string for empty Vosk text result`() {
        // Given
        val json = """{"text" : ""}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `should return empty string for empty Vosk partial result`() {
        // Given
        val json = """{"partial" : ""}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `should return empty string for malformed JSON`() {
        // Given
        val json = "not json at all"

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `should return empty string for empty input`() {
        // Given
        val json = ""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result.shouldBeEmpty()
    }

    @Test
    fun `should trim whitespace from extracted text`() {
        // Given
        val json = """{"text" : "  hello world  "}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result shouldBe "hello world"
    }

    @Test
    fun `should handle Vosk result with extra spacing around colon`() {
        // Given
        val json = """{"text"  :  "spaced out"}"""

        // When
        val result = VoiceRecognitionService.extractText(json)

        // Then
        result shouldBe "spaced out"
    }

    // --- defaultModelPath ---

    @Test
    fun `should return default model path in user home`() {
        // Given
        val userHome = System.getProperty("user.home")

        // When
        val path = VoiceRecognitionService.defaultModelPath()

        // Then
        path shouldContain userHome
        path shouldContain "vosk-model"
    }

    // --- stopListening ---

    @Test
    fun `should set isListening to false when stopListening is called`() {
        // Given
        val service = createService()

        // When
        service.stopListening()

        // Then
        service.isListening.value.shouldBeFalse()
    }

    // --- close ---

    @Test
    fun `should set isListening to false when close is called`() {
        // Given
        val service = createService()

        // When
        service.close()

        // Then
        service.isListening.value.shouldBeFalse()
    }

    // --- startListening guard ---

    @Test
    fun `should not start listening if already listening`() = runBlocking {
        // Given
        val service = createService()
        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            Thread.sleep(50)
            0
        }
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""
        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(1000) {
            while (!service.isListening.value) {
                delay(10)
            }
        }

        // When starting again while already listening
        var secondCallbackInvoked = false
        service.startListening(CoroutineScope(Dispatchers.IO)) { secondCallbackInvoked = true }
        delay(200)

        // Then the second call should be a no-op
        secondCallbackInvoked.shouldBeFalse()

        // Cleanup
        service.stopListening()
    }

    // --- startListening with final result ---

    @Test
    fun `should invoke onFinalResult when recognizer accepts a waveform`() = runBlocking {
        // Given
        val finalResults = mutableListOf<String>()
        val service = createService()
        var readCount = 0

        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            readCount++
            if (readCount <= 1) 4096 else {
                service.stopListening()
                0
            }
        }
        every { mockRecognizer.acceptWaveForm(any<ByteArray>(), any()) } returns true
        every { mockRecognizer.result } returns """{"text" : "hello world"}"""
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        // When
        service.startListening(CoroutineScope(Dispatchers.IO)) { finalResults.add(it) }
        withTimeout(2000) {
            while (finalResults.isEmpty()) {
                delay(10)
            }
        }

        // Then
        finalResults shouldContainExactly listOf("hello world")
        service.stopListening()
        Unit
    }

    // --- startListening with partial result ---

    @Test
    fun `should update partialResult when recognizer returns a partial`() = runBlocking {
        // Given
        val service = createService()
        var readCount = 0

        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            readCount++
            if (readCount <= 1) 4096 else {
                Thread.sleep(50)
                0
            }
        }
        every { mockRecognizer.acceptWaveForm(any<ByteArray>(), any()) } returns false
        every { mockRecognizer.partialResult } returns """{"partial" : "hel"}"""
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        // When
        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(2000) {
            while (service.partialResult.value != "hel") {
                delay(10)
            }
        }

        // Then
        service.partialResult.value shouldBe "hel"
        service.stopListening()
        Unit
    }

    // --- startListening clears partial on final ---

    @Test
    fun `should clear partialResult when a final result is received`() = runBlocking {
        // Given
        val service = createService()
        var readCount = 0

        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            readCount++
            if (readCount <= 1) 4096 else {
                service.stopListening()
                0
            }
        }
        every { mockRecognizer.acceptWaveForm(any<ByteArray>(), any()) } returns true
        every { mockRecognizer.result } returns """{"text" : "hello"}"""
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        // When
        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(2000) {
            // Wait for the final result to be processed (partial should be cleared)
            while (service.isListening.value) {
                delay(10)
            }
        }

        // Then
        service.partialResult.value.shouldBeEmpty()
        service.stopListening()
        Unit
    }

    // --- partialResult cleared on stop ---

    @Test
    fun `should clear partialResult when stopListening is called`() = runBlocking {
        // Given
        val service = createService()
        var readCount = 0

        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            readCount++
            if (readCount <= 1) 4096 else {
                Thread.sleep(50)
                0
            }
        }
        every { mockRecognizer.acceptWaveForm(any<ByteArray>(), any()) } returns false
        every { mockRecognizer.partialResult } returns """{"partial" : "in progress"}"""
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(2000) {
            while (service.partialResult.value != "in progress") {
                delay(10)
            }
        }
        service.partialResult.value shouldBe "in progress"

        // When
        service.stopListening()
        delay(200)

        // Then
        service.partialResult.value.shouldBeEmpty()
        Unit
    }

    // --- stopListening stops microphone ---

    @Test
    fun `should stop and close microphone when stopListening is called`() = runBlocking {
        // Given
        val service = createService()
        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            Thread.sleep(50)
            0
        }
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(1000) {
            while (!service.isListening.value) {
                delay(10)
            }
        }

        // When
        service.stopListening()
        delay(200)

        // Then
        service.isListening.value.shouldBeFalse()
        verify { mockTargetDataLine.stop() }
        verify { mockTargetDataLine.close() }
        Unit
    }

    // --- close releases model ---

    @Test
    fun `should close the model when close is called after startListening`() = runBlocking {
        // Given
        val service = createService()
        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            Thread.sleep(50)
            0
        }
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        withTimeout(1000) {
            while (!service.isListening.value) {
                delay(10)
            }
        }

        // When
        service.close()
        delay(200)

        // Then
        service.isListening.value.shouldBeFalse()
        verify { mockModel.close() }
        Unit
    }

    // --- ensureModel requires valid path ---

    @Test
    fun `should not start listening if model path does not exist`() = runBlocking {
        // Given
        val service = VoiceRecognitionService(
            modelPath = "/nonexistent/path",
            modelFactory = { mockModel },
            recognizerFactory = { _, _ -> mockRecognizer },
            microphoneFactory = { mockTargetDataLine }
        )

        // When
        service.startListening(CoroutineScope(Dispatchers.IO)) {}
        delay(200)

        // Then
        service.isListening.value.shouldBeFalse()
        Unit
    }

    // --- startListening ignores blank final results ---

    @Test
    fun `should not invoke onFinalResult for blank recognized text`() = runBlocking {
        // Given
        val finalResults = mutableListOf<String>()
        val service = createService()
        var readCount = 0

        every { mockTargetDataLine.read(any(), any(), any()) } answers {
            readCount++
            if (readCount <= 1) 4096 else {
                service.stopListening()
                0
            }
        }
        every { mockRecognizer.acceptWaveForm(any<ByteArray>(), any()) } returns true
        every { mockRecognizer.result } returns """{"text" : ""}"""
        every { mockRecognizer.finalResult } returns """{"text" : ""}"""

        // When
        service.startListening(CoroutineScope(Dispatchers.IO)) { finalResults.add(it) }
        withTimeout(2000) {
            while (service.isListening.value) {
                delay(10)
            }
        }

        // Then
        finalResults.shouldBeEmpty()
        Unit
    }
}
