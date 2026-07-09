package io.rippledown.interpretation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.rippledown.constants.interpretation.COMMENTS_TOGGLE
import io.rippledown.constants.interpretation.INTERPRETATION_TEXT_FIELD
import io.rippledown.utils.createViewableInterpretation
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

class InterpretationViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    lateinit var handler: InterpretationViewHandler

    @Before
    fun setUp() {
        handler = mockk(relaxUnitFun = true)
    }

    @Test
    fun `should show interpretation text`() = runTest {
        //Given
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            //Then
            requireInterpretation(bondiComment)
        }
    }

    @Test
    fun `should show comments panel expanded by default`() = runTest {
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun `should collapse comments panel when toggle is clicked`() = runTest {
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_TOGGLE).performClick()
            waitForIdle()

            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertDoesNotExist()
        }
    }

    @Test
    fun `should expand comments panel when toggle is clicked again`() = runTest {
        val bondiComment = "Best surf in the world!"
        val interpretation = createViewableInterpretation(
            mapOf(bondiComment to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_TOGGLE).performClick()
            waitForIdle()

            onNodeWithContentDescription(COMMENTS_TOGGLE).performClick()
            waitForIdle()

            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertIsDisplayed()
        }
    }
}