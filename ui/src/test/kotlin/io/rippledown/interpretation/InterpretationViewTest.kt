package io.rippledown.interpretation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import io.rippledown.constants.interpretation.COMMENTS_INFO_ICON
import io.rippledown.constants.interpretation.COMMENTS_NONE
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

    @Test
    fun `should show comments panel heading and empty state when there are no comments`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_TOGGLE).assertIsDisplayed()
            onNodeWithContentDescription(COMMENTS_NONE).assertIsDisplayed()
        }
    }

    @Test
    fun `should not show empty state when there are comments`() = runTest {
        val interpretation = createViewableInterpretation(
            mapOf("Best surf in the world!" to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_NONE).assertDoesNotExist()
            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertIsDisplayed()
        }
    }

    @Test
    fun `should show no comment rows when there are no comments`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            //The empty state stands in place of the table, so there is no table at all
            commentsShown() shouldBe emptyList()
            onNodeWithContentDescription(INTERPRETATION_TEXT_FIELD).assertDoesNotExist()
        }
    }

    @Test
    fun `should hide empty state when comments panel is collapsed`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_NONE).assertIsDisplayed()

            onNodeWithContentDescription(COMMENTS_TOGGLE).performClick()
            waitForIdle()

            onNodeWithContentDescription(COMMENTS_NONE).assertDoesNotExist()
        }
    }

    @Test
    fun `should show empty state again when collapsed comments panel is expanded`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
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

            onNodeWithContentDescription(COMMENTS_NONE).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when there are comments`() = runTest {
        val interpretation = createViewableInterpretation(
            mapOf("Best surf in the world!" to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when there are no comments`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when the panel is collapsed`() = runTest {
        val interpretation = createViewableInterpretation(
            mapOf("Best surf in the world!" to listOf())
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

            onNodeWithContentDescription(COMMENTS_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when the panel is expanded`() = runTest {
        val interpretation = createViewableInterpretation(
            mapOf("Best surf in the world!" to listOf())
        )
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_INFO_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `should show the info icon when panel is collapsed and there are no comments`() = runTest {
        val interpretation = createViewableInterpretation(mapOf())
        with(composeTestRule) {
            setContent {
                InterpretationView(
                    interpretation = interpretation,
                    handler = handler
                )
            }

            onNodeWithContentDescription(COMMENTS_TOGGLE).performClick()
            waitForIdle()

            onNodeWithContentDescription(COMMENTS_INFO_ICON).assertIsDisplayed()
        }
    }
}