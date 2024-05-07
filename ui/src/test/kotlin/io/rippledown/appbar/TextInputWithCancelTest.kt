package io.rippledown.appbar

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import io.kotest.matchers.shouldBe
import io.rippledown.constants.main.*
import org.junit.Before
import org.junit.Rule
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class TextInputWithCancelTest {

    class TIH() : TextInputHandler {
        var input: String? = null
        var cancelled = false

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
            waitUntilExactlyOneExists(hasTestTag(TEXT_INPUT_FIELD_TEST_TAG))
            onNodeWithTag(TEXT_INPUT_FIELD_TEST_TAG)
                .assertIsEnabled()
                .assertIsDisplayed()
            waitUntilExactlyOneExists(hasText(""))

            onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG)
                .assertIsNotEnabled()
                .assertIsDisplayed()
                .assertTextEquals(handler.confirmButtonText())
            onNodeWithTag(TEXT_INPUT_CANCEL_BUTTON_TEST_TAG)
                .assertIsEnabled()
                .assertIsDisplayed()
                .assertTextEquals(handler.cancelButtonText())
        }
    }

    @Test
    fun cancel() {
        with(composeTestRule) {
            setContent {
                TextInputWithCancel(handler)
            }
            handler.cancelled shouldBe false
            clickCancelButton()

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
            enterText(data)
            clickCreateButton()
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
            val okButton = onNodeWithTag(TEXT_INPUT_OK_BUTTON_TEST_TAG)
            okButton.assertIsNotEnabled()
            enterText("A")
            performTextClearance()
            okButton.assertIsNotEnabled()
            enterText("Bats")
            okButton.assertIsEnabled()
        }
    }
}
