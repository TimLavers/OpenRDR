package io.rippledown.chat

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VoiceInputButtonTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var voiceRecognitionService: VoiceRecognitionService
    private val isListeningFlow = MutableStateFlow(false)
    private val partialResultFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        voiceRecognitionService = mockk(relaxed = true)
        every { voiceRecognitionService.isListening } returns isListeningFlow
        every { voiceRecognitionService.partialResult } returns partialResultFlow
    }

    @Test
    fun `should show mic button`() {
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(voiceRecognitionService = voiceRecognitionService)
            }

            // Then
            onNodeWithContentDescription(CHAT_MIC_BUTTON).assertIsDisplayed()
        }
    }

    @Test
    fun `should call startListening on click when not listening`() {
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(voiceRecognitionService = voiceRecognitionService)
            }

            // When
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.startListening(any<CoroutineScope>(), any()) }
            verify(exactly = 0) { voiceRecognitionService.stopListening() }
        }
    }

    @Test
    fun `should call stopListening on click when listening`() {
        with(composeTestRule) {
            // Given
            isListeningFlow.value = true
            setContent {
                VoiceInputButton(voiceRecognitionService = voiceRecognitionService)
            }

            // When
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.stopListening() }
            verify(exactly = 0) { voiceRecognitionService.startListening(any(), any()) }
        }
    }

    @Test
    fun `should disable mic button when enabled is false`() {
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    enabled = false
                )
            }

            // Then
            onNodeWithContentDescription(CHAT_MIC_BUTTON).assertIsNotEnabled()
        }
    }

    @Test
    fun `should enable mic button when enabled is true`() {
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    enabled = true
                )
            }

            // Then
            onNodeWithContentDescription(CHAT_MIC_BUTTON).assertIsEnabled()
        }
    }

    @Test
    fun `should call onTextUpdated when partial result changes`() {
        val receivedTexts = mutableListOf<String>()
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    onTextUpdated = { receivedTexts.add(it) }
                )
            }

            // When
            partialResultFlow.value = "hello"
            waitForIdle()

            // Then
            receivedTexts shouldContain "hello"
        }
    }

    @Test
    fun `should not call onTextUpdated when partial result is blank`() {
        val receivedTexts = mutableListOf<String>()
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    onTextUpdated = { receivedTexts.add(it) }
                )
            }

            // When
            partialResultFlow.value = ""
            waitForIdle()

            // Then
            receivedTexts.shouldBeEmpty()
        }
    }

    @Test
    fun `should toggle from startListening to stopListening when state changes`() {
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(voiceRecognitionService = voiceRecognitionService)
            }

            // When not listening and clicking
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.startListening(any<CoroutineScope>(), any()) }

            // When listening state changes and clicking again
            isListeningFlow.value = true
            waitForIdle()
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.stopListening() }
        }
    }

    @Test
    fun `should toggle from stopListening to startListening when state changes back`() {
        with(composeTestRule) {
            // Given
            isListeningFlow.value = true
            setContent {
                VoiceInputButton(voiceRecognitionService = voiceRecognitionService)
            }

            // When listening and clicking
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.stopListening() }

            // When listening state changes back and clicking again
            isListeningFlow.value = false
            waitForIdle()
            clearMocks(voiceRecognitionService, answers = false)
            every { voiceRecognitionService.isListening } returns isListeningFlow
            every { voiceRecognitionService.partialResult } returns partialResultFlow
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()

            // Then
            verify { voiceRecognitionService.startListening(any<CoroutineScope>(), any()) }
        }
    }

    @Test
    fun `should call onTextUpdated with final result when startListening callback is invoked`() {
        val receivedTexts = mutableListOf<String>()
        val callbackSlot = slot<(String) -> Unit>()
        every {
            voiceRecognitionService.startListening(any(), capture(callbackSlot))
        } answers {
            callbackSlot.captured("final result")
        }

        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    onTextUpdated = { receivedTexts.add(it) }
                )
            }

            // When
            onNodeWithContentDescription(CHAT_MIC_BUTTON).performClick()
            waitForIdle()

            // Then
            receivedTexts shouldContain "final result"
        }
    }

    @Test
    fun `should call onTextUpdated for multiple partial results`() {
        val receivedTexts = mutableListOf<String>()
        with(composeTestRule) {
            // Given
            setContent {
                VoiceInputButton(
                    voiceRecognitionService = voiceRecognitionService,
                    onTextUpdated = { receivedTexts.add(it) }
                )
            }

            // When
            partialResultFlow.value = "hel"
            waitForIdle()
            partialResultFlow.value = "hello"
            waitForIdle()
            partialResultFlow.value = "hello world"
            waitForIdle()

            // Then
            receivedTexts shouldContain "hel"
            receivedTexts shouldContain "hello"
            receivedTexts shouldContain "hello world"
        }
    }
}
