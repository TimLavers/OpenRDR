package io.rippledown.voice

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class RecordingIndicatorTest {
    @get:Rule
    var composeTestRule = createComposeRule()

    private lateinit var voiceRecognitionService: VoiceRecognition
    private val isListeningFlow = MutableStateFlow(false)
    private val partialResultFlow = MutableStateFlow("")

    @Before
    fun setUp() {
        voiceRecognitionService = mockk()
        every { voiceRecognitionService.isListening } returns isListeningFlow
        every { voiceRecognitionService.partialResult } returns partialResultFlow
    }

    @Test
    fun `is hidden when not listening`() {
        with(composeTestRule) {
            isListeningFlow.value = false
            setContent {
                RecordingIndicator(voiceRecognitionService)
            }

            onNodeWithContentDescription(RECORDING_INDICATOR).assertDoesNotExist()
        }
    }

    @Test
    fun `is displayed when listening`() {
        with(composeTestRule) {
            isListeningFlow.value = true
            setContent {
                RecordingIndicator(voiceRecognitionService)
            }

            onNodeWithContentDescription(RECORDING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `appears when isListening flips true`() {
        with(composeTestRule) {
            isListeningFlow.value = false
            setContent {
                RecordingIndicator(voiceRecognitionService)
            }
            onNodeWithContentDescription(RECORDING_INDICATOR).assertDoesNotExist()

            isListeningFlow.value = true
            waitForIdle()

            onNodeWithContentDescription(RECORDING_INDICATOR).assertIsDisplayed()
        }
    }

    @Test
    fun `disappears when isListening flips false`() {
        with(composeTestRule) {
            isListeningFlow.value = true
            setContent {
                RecordingIndicator(voiceRecognitionService)
            }
            onNodeWithContentDescription(RECORDING_INDICATOR).assertIsDisplayed()

            isListeningFlow.value = false
            waitForIdle()

            onNodeWithContentDescription(RECORDING_INDICATOR).assertDoesNotExist()
        }
    }
}
