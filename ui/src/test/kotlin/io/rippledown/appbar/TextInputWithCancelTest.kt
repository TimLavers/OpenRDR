package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TextInputWithCancelTest {

    class TIH : TextInputHandler {
        var initialTextToShow = ""
        var input: String? = null
        var cancelled = false
        override fun initialText() = initialTextToShow
        override fun isValidInput(input: String) = input.length > 3
        override fun labelText() = "Label"
        override fun inputFieldDescription() = "Description"
        override fun confirmButtonText() = "Really"
        override fun confirmButtonDescription() = "Definitely"
        override fun cancelButtonText() = "Maybe not"
        override fun handleInput(value: String) {
            input = value
        }
        override fun cancel() {
           cancelled = true
        }
    }
    private lateinit var handler: TIH

    @get:Rule
    var composeTestRule = createComposeRule()

    @Before
    fun setup() {
        handler = TIH()
    }

    @Test
    fun `initial layout`() {
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(handler.inputFieldDescription()))
            onNodeWithContentDescription(handler.inputFieldDescription())
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(""))

            onNodeWithContentDescription(handler.confirmButtonDescription())
                .assertIsNotEnabled()
                .assertIsDisplayed()
                .assertTextEquals(handler.confirmButtonText())
            onNodeWithText(handler.cancelButtonText())
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals(handler.cancelButtonText())
        }
    }

    @Test
    fun `initial layout with non-empty text`() {
        handler.initialTextToShow = "Whatever"
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            waitUntilExactlyOneExists(hasContentDescription(handler.inputFieldDescription()))
            onNodeWithContentDescription(handler.inputFieldDescription())
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(handler.initialTextToShow))
        }
    }

    @Test
    fun cancel() {
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            handler.cancelled shouldBe false
            onNodeWithText(handler.cancelButtonText()).performClick()

            handler.cancelled shouldBe true
        }
    }

    @Test
    fun ok() {
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            val data = "Whatever"
            onNodeWithContentDescription(handler.inputFieldDescription()).performTextInput(data)
            onNodeWithContentDescription(handler.confirmButtonDescription()).performClick()
            handler.input shouldBe data
            handler.cancelled shouldBe false
        }
    }

    @Test
    fun `kb name validation`() {
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            val okButton = onNodeWithContentDescription(handler.confirmButtonDescription())
            okButton.assertIsNotEnabled()
            onNodeWithContentDescription(handler.inputFieldDescription()).performTextInput("A")
            onNodeWithContentDescription(handler.inputFieldDescription()).performTextClearance()
            okButton.assertIsNotEnabled()
            onNodeWithContentDescription(handler.inputFieldDescription()).performTextInput("Bats")
            okButton.assertIsEnabled()
        }
    }
}
